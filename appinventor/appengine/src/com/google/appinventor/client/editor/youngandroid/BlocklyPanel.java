// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.youngandroid;

import com.google.appinventor.client.ComponentsTranslation;
import com.google.appinventor.client.DesignToolbar;
import com.google.appinventor.client.ErrorReporter;
import com.google.appinventor.client.Ode;
import com.google.appinventor.client.TopToolbar;
import com.google.appinventor.client.TranslationComponentParams;
import com.google.appinventor.client.TranslationDesignerPallete;
import com.google.appinventor.client.editor.simple.SimpleComponentDatabase;
import com.google.appinventor.client.explorer.project.ComponentDatabaseChangeListener;
import com.google.appinventor.client.output.OdeLog;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.appinventor.components.common.YaVersion;

import com.google.common.collect.Maps;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.appinventor.client.Ode.MESSAGES;

/**
 * Blocks editor panel.
 * The contents of the blocks editor panel is in an iframe identified by
 * the formName passed to the constructor. That identifier is also the hashtag
 * on the URL that is the source of the iframe. This class provides methods for
 * calling the Javascript Blockly code from the rest of the Designer.
 *
 * @author sharon@google.com (Sharon Perl)
 */
public class BlocklyPanel extends HTMLPanel implements ComponentDatabaseChangeListener{
  public static enum OpType {ADD, REMOVE, RENAME}

  // The currently displayed context (project_context)
  private static String currentContext;
  private static String languageSetting;

  private static class ComponentOp {
    public OpType op;
    public String instanceName;     // for ADD, REMOVE, RENAME
    public String uid;              // for ADD, REMOVE, RENAME
    public String typeDescription;  // for ADD
    public String typeName;         // for REMOVE, RENAME
    public String oldName;          // for RENAME
  }

  private static class LoadStatus {
    public boolean complete = false; // true if loading blocks completed
    public boolean error = false;     // true if got an error loading blocks
  }

  private final SimpleComponentDatabase COMPONENT_DATABASE;

  private static final String EDITOR_HTML =
    "<style>\n" +
    ".svg {\n" +
    "  height: 100%;\n" +
    "  width: 100%;\n" +
    "  border: solid black 1px;\n" +
    "}\n" +
    "</style>\n" +
    "<iframe src=\"blocklyframe.html#CONTEXT_NAME\" class=\"svg\">";

  // Keep track of component additions/removals/renames that happen before
  // blocks editor is inited for the first time, or before reinitialization
  // after the blocks editor's project has been detached from the document.
  // Replay them in order after initialized. Keys are context names. If there is
  // an entry for a given context name in the map, its blocks have not yet been
  // (re)inited.
  // Note: Javascript is single-threaded. Since this code is compiled by GWT
  // into Javascript, we don't need to worry about concurrent access to
  // this map.
  private static Map<String, List<ComponentOp>> componentOps = Maps.newHashMap();

  // When a user switches projects, the ProjectEditor widget gets detached
  // from the main document in the browser. If the user switches back to a
  // previously open project (in the same session), when the ProjectEditor
  // widget gets reattached, all of its FileEditors in its deckPanel get
  // reloaded, causing the Blockly objects for the blocks editors
  // to be created anew. Since the FileEditor Java objects themselves are
  // not recreated, we need to reconstruct the set of components in the Blockly
  // object when the object gets recreated. For each context, we keep track of the
  // components currently in that context, stored as "add" operations that can be
  // replayed to restore those components when the underlying Blockly state
  // is re-inited. This component state is updated as components are added,
  // removed, and renamed. The outer map is keyed by context name, and the
  // inner map is keyed by component uid.
  private static final Map<String, Map<String, ComponentOp>> currentComponents = Maps.newHashMap();

  // Pending blocks file content, indexed by context name. Waiting to be loaded when the corresponding
  // blocks area is initialized.
  private static final Map<String, String> pendingBlocksContentMap = Maps.newHashMap();

  // [lyn, 2014/10/27] added context for upgrading
  // Pending context JSON content, indexed by context name. Waiting to be loaded when the corresponding
  // blocks area is initialized.
  private static final Map<String, String> pendingContextJsonMap = Maps.newHashMap();

  // Status of blocks loading, indexed by context name.
  private static final Map<String, LoadStatus> loadStatusMap = Maps.newHashMap();

  // Map from a Project Id to the last opened Form for that Project.
  // This is used while sending relatedComponentData for Yail
  private static final Map<String, String> lastFormMap = Maps.newHashMap();

  // Blockly backpack
  private static String backpack = "[]";

  // My context name
  private String contextName;

  // My blocks editor
  private YaBlocksEditor myBlocksEditor;  // [lyn, 2014/10/28] Added to access current context json

  public static boolean isWarningVisible = false;

  public BlocklyPanel(YaBlocksEditor blocksEditor, String contextName) {
    super(EDITOR_HTML.replace("CONTEXT_NAME", contextName));
    this.contextName = contextName;
    this.myBlocksEditor = blocksEditor;
    COMPONENT_DATABASE = SimpleComponentDatabase.getInstance(blocksEditor.getProjectId());
    componentOps.put(contextName, new ArrayList<ComponentOp>());
    // note: using Maps.newHashMap() gives a type error in Eclipse in the following line
    currentComponents.put(contextName, new HashMap<String, ComponentOp>());
    initJS();
    OdeLog.log("Created BlocklyPanel for " + contextName);
  }

  /*
   * Do whatever is needed for App Inventor UI initialization. In this case
   * we just need to export the init components method so that we can
   * get called back from the Blockly Javascript when it finishes loading.
   */
  public static void initUi() {
    exportMethodsToJavascript();
    // Tell the blockly world about companion versions.
    setPreferredCompanion(YaVersion.PREFERRED_COMPANION, YaVersion.COMPANION_UPDATE_URL);
    for (int i = 0; i < YaVersion.ACCEPTABLE_COMPANIONS.length; i++) {
      addAcceptableCompanion(YaVersion.ACCEPTABLE_COMPANIONS[i]);
    }
    addAcceptableCompanionPackage(YaVersion.ACCEPTABLE_COMPANION_PACKAGE);
  }

  /*
   * Initialize the blocks area so that it can be updated as components are
   * added, removed, or changed. Replay any previous component operations that
   * we weren't able to run before the blocks editor was initialized. This
   * method is static so that it can be called by the native Javascript code
   * after it finishes loading. We export this method to Javascript in
   * exportInitComponentsMethod().
   */
  private static void initBlocksArea(String contextName) {

    OdeLog.log("BlocklyPanel: Got initBlocksArea call for " + contextName);

    // if there are any components added, add them first before we load
    // block content that might reference them

    Map<String, ComponentOp> savedComponents = currentComponents.get(contextName);
    if (savedComponents != null) { // shouldn't be!
      OdeLog.log("Restoring " + savedComponents.size() +
          " previous blockly components for context " + contextName);
      for (ComponentOp op : savedComponents.values()) {
        doAddComponent(contextName, op.typeDescription, op.instanceName, op.uid);
      }
    }

    if (componentOps.containsKey(contextName)) {
      OdeLog.log("Replaying " + componentOps.get(contextName).size() + " ops waiting in queue");
      for (ComponentOp op : componentOps.get(contextName)) {
        switch (op.op) {
          case ADD:
            doAddComponent(contextName, op.typeDescription, op.instanceName, op.uid);
            addSavedComponent(contextName, op.typeDescription, op.instanceName, op.uid);
            break;
          case REMOVE:
            doRemoveComponent(contextName, op.typeName, op.instanceName, op.uid);
            removeSavedComponent(contextName, op.typeName, op.instanceName, op.uid);
            break;
          case RENAME:
            doRenameComponent(contextName, op.typeName, op.oldName, op.instanceName, op.uid);
            renameSavedComponent(contextName, op.typeName, op.oldName, op.instanceName, op.uid);
            break;
        }
      }
      componentOps.remove(contextName);
    }

    // If we've gotten any block content to load, load it now
    // Note: Map.remove() returns the value (null if not present), as well as removing the mapping
    String pendingBlocksContent = pendingBlocksContentMap.remove(contextName);
    // [lyn, 2014/10/27] added contextJson for upgrading
    String pendingContextJson = pendingContextJsonMap.remove(contextName);
    if (pendingBlocksContent != null) {
      OdeLog.log("Loading blocks area content for " + contextName);
      loadBlocksContentNow(contextName, pendingContextJson, pendingBlocksContent);
    }
  }

  private static void blocklyWorkspaceChanged(String contextName, boolean sendRelated) {
    LoadStatus loadStat = loadStatusMap.get(contextName);
    // ignore workspaceChanged events until after the load finishes.
    if (loadStat == null || !loadStat.complete) {
      return;
    }
    if (loadStat.error) {
      YaBlocksEditor.setBlocksDamaged(contextName);
      ErrorReporter.reportError(MESSAGES.blocksNotSaved(contextName));
    } else {
      YaBlocksEditor.onBlocksAreaChanged(contextName, sendRelated);
    }
  }

  // Returns true if the blocks for contextName have been initialized (i.e.,
  // no componentOps entry exists for contextName).
  public static boolean blocksInited(String contextName) {
    return !componentOps.containsKey(contextName);
  }

  public static String getBackpack() {
    return backpack;
  }
  public static void setBackpack(String bp_contents, boolean doStore) {
    backpack = bp_contents;
    if (doStore) {
      Ode.getInstance().getUserInfoService().storeUserBackpack(backpack,
        new AsyncCallback<Void>() {
          @Override
          public void onSuccess(Void nothing) {
            // Nothing to do...
          }
          @Override
          public void onFailure(Throwable caught) {
            OdeLog.elog("Failed setting the backpack");
          }
        });
    }
  }

  public static boolean isFormBlockly(String contextName) {
    YaBlocksEditor yaBlocksEditor = YaBlocksEditor.getYaBlocksEditor(contextName);
    if (yaBlocksEditor != null && yaBlocksEditor.isFormBlocksEditor()) {
      return true;
    }
    return false;
  }

  public static boolean isTaskBlockly(String contextName) {
    YaBlocksEditor yaBlocksEditor = YaBlocksEditor.getYaBlocksEditor(contextName);
    if (yaBlocksEditor != null && yaBlocksEditor.isTaskBlocksEditor()) {
      return true;
    }
    return false;
  }

  /**
   * Add a component to the blocks workspace
   *
   * @param typeDescription JSON string describing the component type,
   *                        formatted as described in
   *                        {@link com.google.appinventor.components.scripts.ComponentDescriptorGenerator}
   * @param instanceName    the name of the component instance
   * @param uid             the unique id of the component instance
   */
  public void addComponent(String typeDescription, String instanceName, String uid) {
    if (!blocksInited(contextName)) {
      ComponentOp cop = new ComponentOp();
      cop.op = OpType.ADD;
      cop.instanceName = instanceName;
      cop.typeDescription = typeDescription;
      cop.uid = uid;
      if (!componentOps.containsKey(contextName)) {
        componentOps.put(contextName, new ArrayList<ComponentOp>());
      }
      componentOps.get(contextName).add(cop);
    } else {
      doAddComponent(contextName, typeDescription, instanceName, uid);
      addSavedComponent(contextName, typeDescription, instanceName, uid);
    }
  }

  private static void addSavedComponent(String contextName, String typeDescription,
                                        String instanceName, String uid) {
    Map<String, ComponentOp> myComponents = currentComponents.get(contextName);
    if (!myComponents.containsKey(uid)) {
      // we expect there to be no saved component with this uid yet!
      ComponentOp savedComponent = new ComponentOp();
      savedComponent.op = OpType.ADD;
      savedComponent.instanceName = instanceName;
      savedComponent.typeDescription = typeDescription;
      savedComponent.uid = uid;
      myComponents.put(uid, savedComponent);
    } else {
      OdeLog.wlog("BlocklyPanel: already have component with uid " + uid
          + ", instanceName is " + myComponents.get(uid).instanceName);
    }

  }

  /**
   * Remove the component instance instanceName, with the given typeName
   * and uid from the workspace.
   *
   * @param typeName     component type name (e.g., "Canvas" or "Button")
   * @param instanceName instance name
   * @param uid          unique id
   */
  public void removeComponent(String typeName, String instanceName, String uid) {
    if (!blocksInited(contextName)) {
      ComponentOp cop = new ComponentOp();
      cop.op = OpType.REMOVE;
      cop.instanceName = instanceName;
      cop.typeName = typeName;
      cop.uid = uid;
      if (!componentOps.containsKey(contextName)) {
        componentOps.put(contextName, new ArrayList<ComponentOp>());
      }
      componentOps.get(contextName).add(cop);
    } else {
      doRemoveComponent(contextName, typeName, instanceName, uid);
      removeSavedComponent(contextName, typeName, instanceName, uid);
    }
  }

  private static void removeSavedComponent(String contextName, String typeName,
    String instanceName, String uid) {
    Map<String, ComponentOp> myComponents = currentComponents.get(contextName);
    if (myComponents.containsKey(uid)
        && myComponents.get(uid).instanceName.equals(instanceName)) {
      // we expect it to be there
      myComponents.remove(uid);
    } else {
      OdeLog.wlog("BlocklyPanel: can't find saved component with uid " + uid
          + " and name " + instanceName);
    }
  }

  /**
   * Rename the component whose old name is oldName (and whose
   * unique id is uid and type name is typeName) to newName.
   *
   * @param typeName component type name (e.g., "Canvas" or "Button")
   * @param oldName  old instance name
   * @param newName  new instance name
   * @param uid      unique id
   */
  public void renameComponent(String typeName, String oldName,
    String newName, String uid) {
    if (!blocksInited(contextName)) {
      ComponentOp cop = new ComponentOp();
      cop.op = OpType.RENAME;
      cop.instanceName = newName;
      cop.oldName = oldName;
      cop.typeName = typeName;
      cop.uid = uid;
      if (!componentOps.containsKey(contextName)) {
        componentOps.put(contextName, new ArrayList<ComponentOp>());
      }
      componentOps.get(contextName).add(cop);
    } else {
      doRenameComponent(contextName, typeName, oldName, newName, uid);
      renameSavedComponent(contextName, typeName, oldName, newName, uid);
    }
  }

  private static void renameSavedComponent(String contextName, String typeName,
    String oldName, String newName, String uid) {
    Map<String, ComponentOp> myComponents = currentComponents.get(contextName);
    if (myComponents.containsKey(uid)) {
      // we expect it to be there
      ComponentOp savedComponent = myComponents.get(uid);
      if (savedComponent.instanceName.equals(oldName)) {  // it should!
        savedComponent.instanceName = newName;  // rename saved component
      } else {
        OdeLog.wlog("BlocklyPanel: saved component with uid " + uid +
            " has name " + savedComponent.instanceName + ", expected " + oldName);
      }
    } else {
      OdeLog.wlog("BlocklyPanel: can't find saved component with uid " + uid +
          " and name " + oldName);
    }
  }

  /**
   * Show the drawer for component with the specified instance name
   *
   * @param name
   */
  public void showComponentBlocks(String name) {
    if (blocksInited(contextName)) {
      doShowComponentBlocks(contextName, name);
    }
  }

  /**
   * Hide the component blocks drawer
   */
  public void hideComponentBlocks() {
    if (blocksInited(contextName)) {
      doHideComponentBlocks(contextName);
    }
  }

  /**
   * Show the built-in blocks drawer with the specified name
   *
   * @param drawerName
   */
  public void showBuiltinBlocks(String drawerName) {
    try {
      if (blocksInited(contextName)) {
        doShowBuiltinBlocks(contextName, drawerName);
      }
    } catch (JavaScriptException e) {
      ErrorReporter.reportInfo("Not yet implemented: " + drawerName);
    }
  }

  /**
   * Hide the built-in blocks drawer
   */
  public void hideBuiltinBlocks() {
    if (blocksInited(contextName)) {
      doHideBlocks(contextName);
    }
  }

  /**
   * Show the generic blocks drawer with the specified name
   *
   * @param drawerName
   */
  public void showGenericBlocks(String drawerName) {
    if (blocksInited(contextName)) {
      doShowGenericBlocks(contextName, drawerName);
    }
  }

  /**
   * Hide the generic blocks drawer
   */
  public void hideGenericBlocks() {
    if (blocksInited(contextName)) {
      doHideBlocks(contextName);
    }
  }

  public void renderBlockly() {
    if (blocksInited(contextName)) {
      doRenderBlockly(contextName);
    }
  }

  public static void toggleWarning(String contextName) {
    if (blocksInited(contextName)) {
      doToggleWarning(contextName);
    }
  }

  public static void switchWarningVisibility() {
    if (BlocklyPanel.isWarningVisible) {
      BlocklyPanel.isWarningVisible = false;
    } else {
      BlocklyPanel.isWarningVisible = true;
    }
  }

  public static void checkWarningState(String contextName) {
    if (BlocklyPanel.isWarningVisible) {
      toggleWarning(contextName);
    }
    doCheckWarnings(contextName);
  }

  public static void callToggleWarning() {
    YaBlocksEditor.toggleWarning();
  }

  /**
   * Remember any component instances for this context in case
   * the workspace gets reinitialized later (we get detached from
   * our parent object and then our blocks editor gets loaded
   * again later). Also, remember the current state of the blocks
   * area in case we get reloaded.
   */
  public void saveComponentsAndBlocks() {
    // Actually, we already have the components saved, but take this as an
    // indication that we are going to reinit the blocks editor the next
    // time it is shown.
    OdeLog.log("BlocklyEditor: prepared for reinit for context " + contextName);
    // Call doResetYail which will stop the timer that is polling the phone. It is important
    // that it be stopped to avoid a race condition where the last timer on this context fires
    // while the new context is loading.
    doResetYail(contextName);
    // Get blocks content before putting anything in the componentOps map since an entry in
    // the componentOps map is taken as an indication that the blocks area has not initialized yet.
    pendingBlocksContentMap.put(contextName, getBlocksContent());
    // [lyn, 2014/10/28] added contextJson for upgrading
    pendingContextJsonMap.put(contextName, getContextJson());
    componentOps.put(contextName, new ArrayList<ComponentOp>());
  }

  /**
   * @returns true if the blocks drawer is showing, false otherwise.
   */
  public boolean drawerShowing() {
    if (blocksInited(contextName)) {
      return doDrawerShowing(contextName);
    } else {
      return false;
    }
  }

  /**
   * Load the blocks described by blocksContent into the blocks workspace.
   *
   * @param blocksContent XML description of a blocks workspace in format expected by Blockly
   */
  // [lyn, 2014/10/27] added contextJson for upgrading
  public void loadBlocksContent(String contextJson, String blocksContent) {
    LoadStatus loadStat = new LoadStatus();
    loadStatusMap.put(contextName, loadStat);
    if (blocksInited(contextName)) {
      OdeLog.log("Loading blocks content for " + contextName);
      loadBlocksContentNow(contextName, contextJson, blocksContent);
    } else {
      // save it to load when the blocks area is initialized
      OdeLog.log("Caching blocks content for " + contextName + " for loading when blocks area inited");
      pendingBlocksContentMap.put(contextName, blocksContent);
      // [lyn, 2014/10/27] added contextJson for upgrading
      pendingContextJsonMap.put(contextName, contextJson);
    }
  }

  // [lyn, 2014/10/27] added contextJson for upgrading
  public static void loadBlocksContentNow(String contextName, String contextJson, String blocksContent) {
    LoadStatus loadStat = loadStatusMap.get(contextName);  // should not be null!
    try {
      doLoadBlocksContent(contextName, contextJson, blocksContent);
    } catch (JavaScriptException e) {
      ErrorReporter.reportError(MESSAGES.blocksLoadFailure(contextName));
      OdeLog.elog("Error loading blocks for screen " + contextName + ": "
          + e.getDescription());
      loadStat.error = true;
    }
    loadStat.complete = true;
  }

  /**
   * Return the XML string describing the current state of the blocks workspace
   */
  public String getBlocksContent() {
    if (blocksInited(contextName)) {
      return doGetBlocksContent(contextName);
    } else {
      // in case someone clicks Save before the blocks area is inited
      String blocksContent = pendingBlocksContentMap.get(contextName);
      return (blocksContent != null) ? blocksContent : "";
    }
  }

  /**
   * Return the JSON string describing the current state of the associated context
   */
  // [lyn, 2014/10/28] Handle these cases
  public String getContextJson() {
    if (blocksInited(contextName)) {
      return myBlocksEditor.encodeContextAsJsonString(true);
    } else {
      // in case someone clicks Save before the blocks area is inited
      String contextJson = pendingContextJsonMap.get(contextName);
      return (contextJson != null) ? contextJson : "";
    }
  }



  /**
   * Get Yail code for current blocks workspace
   *
   * @return the yail code as a String
   * @throws YailGenerationException if there was a problem generating the Yail
   */
  public String getYail(String contextJson, String packageName) throws YailGenerationException {
    if (!blocksInited(contextName)) {
      throw new YailGenerationException("Blocks area is not initialized yet", contextName);
    }
    try {
      if (myBlocksEditor.isFormBlocksEditor()) {
        return doGetFormYail(contextName, contextJson, packageName);
      } else if (myBlocksEditor.isTaskBlocksEditor()) {
        return doGetTaskYail(contextName, contextJson, packageName);
      } else {
        throw new YailGenerationException("BlocklyPanel for unknown context", contextName);
      }
    } catch (JavaScriptException e) {
      throw new YailGenerationException(e.getDescription(), contextName);
    }
  }

  /**
   * Send component data (json and context name) to Blockly for building
   * yail for the REPL.
   *
   * @throws YailGenerationException if there was a problem generating the Yail
   */
  public void sendComponentData(String contextJson, String packageName) throws YailGenerationException {
    if (myBlocksEditor.isFormBlocksEditor() && !currentContext.equals(contextName)) { // Not working on the current form...
      OdeLog.log("Not working on " + currentContext + " (while sending for " + contextName + ")");
      return;
    }
    if (!blocksInited(contextName)) {
      throw new YailGenerationException("Blocks area is not initialized yet", contextName);
    }
    try {
      if (myBlocksEditor.isFormBlocksEditor()) {
        doSendFormJson(contextName, contextJson, packageName);
      } else if (myBlocksEditor.isTaskBlocksEditor()) {
        doSendTaskJson(contextName, contextJson, packageName);
      }

    } catch (JavaScriptException e) {
      throw new YailGenerationException(e.getDescription(), contextName);
    }
  }

  // TODO(jusrkg): Check if Nuking YAIL is necessary
  public void showDifferentContext(String newContextName) {
    OdeLog.log("showDifferentContext changing from " + contextName + " to " + newContextName);
    // Nuke Yail for context we are leaving so it will reload when we return
//    if (!contextName.equals(newContextName))
//      doResetYail(contextName);
    contextName = newContextName;
    blocklyWorkspaceChanged(contextName, true);
  }

  public void startRepl(boolean alreadyRunning, boolean forEmulator, boolean forUsb) { // Start the Repl
    doStartRepl(contextName, alreadyRunning, forEmulator, forUsb);
  }

  public void hardReset() {
    doHardReset(contextName);
  }

  public void verifyAllBlocks() {
    doVerifyAllBlocks(contextName);
  }

  public static boolean checkIsAdmin() {
    return Ode.getInstance().getUser().getIsAdmin();
  }

  // Set currentScreen
  // We use this to determine if we should send Yail to a
  // a connected device.
  public static void setCurrentContext(String contextName) {
    currentContext = contextName;
    if (blocksInited(contextName))
      blocklyWorkspaceChanged(contextName, true); // Update the device now if the blocks are ready.
  }

  public static void setProjectLastForm(long projectId, String formName) {
    BlocklyPanel.lastFormMap.put(Long.toString(projectId), formName);
  }

  public static String getProjectLastForm(long projectId) {
      return BlocklyPanel.lastFormMap.get(Long.toString(projectId));
  }

  public static void indicateDisconnect() {
    TopToolbar.indicateDisconnect();
    DesignToolbar.clearScreens();
  }

  public static boolean pushScreen(String newScreen) {
    return DesignToolbar.pushScreen(newScreen);
  }

  public static void popScreen() {
    DesignToolbar.popScreen();
  }

  public void getBlocksImage(Callback callback) {
    doFetchBlocksImage(contextName, callback);
  }

  // The code below (4 methods worth) is for creating a GWT dialog box
  // from the blockly code. See the comment in replmgr.js for more
  // information.

  /**
   * Create a Dialog Box. We call this from Javascript (blockly) to
   * display a dialog box.  We do this here because we can get calls
   * from the blocklyframe when it is not visible.  Because we are in
   * the parent window, we can display dialogs that will be visible
   * even when the blocklyframe is not visible.
   *
   * @param title      Title for the Dialog Box
   * @param mess       The message to display
   * @param buttonName The string to display in the "OK" button.
   * @param size       0 or 1. 0 makes a smaller box 1 makes a larger box.
   * @param callback   an opague JavaScriptObject that contains the
   *                   callback function provided by the Javascript code.
   * @return The created dialog box.
   */

  public static DialogBox createDialog(String title, String mess, final String buttonName, final String cancelButtonName, int size, final JavaScriptObject callback) {
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setStylePrimaryName("ode-DialogBox");
    dialogBox.setText(title);
    if (size == 0) {
      dialogBox.setHeight("150px");
    } else {
      dialogBox.setHeight("400px");
    }
    dialogBox.setWidth("400px");
    dialogBox.setGlassEnabled(true);
    dialogBox.setAnimationEnabled(true);
    dialogBox.center();
    VerticalPanel DialogBoxContents = new VerticalPanel();
    HTML message = new HTML(mess);
    message.setStyleName("DialogBox-message");
    HorizontalPanel holder = new HorizontalPanel();
    if (buttonName != null) {           // If buttonName and cancelButtonName are null
      Button ok = new Button(buttonName); // We won't have any buttons and other
      ok.addClickListener(new ClickListener() { // code is needed to dismiss us
          public void onClick(Widget sender) {
            doCallBack(callback, buttonName);
          }
        });
      holder.add(ok);
    }
    if (cancelButtonName != null) {
      Button cancel = new Button(cancelButtonName);
      cancel.addClickListener(new ClickListener() {
          public void onClick(Widget sender) {
            doCallBack(callback, cancelButtonName);
          }
        });
      holder.add(cancel);
    }
    DialogBoxContents.add(message);
    DialogBoxContents.add(holder);
    dialogBox.setWidget(DialogBoxContents);
    dialogBox.show();
    return dialogBox;
  }

  /**
   * Hide a dialog box. This function is here so it can be called from
   * the blockly code. We cannot call "hide" directly from the blockly
   * code because when this code is compiled, the "hide" method disappears!
   *
   * @param dialog The dialogbox to hide.
   */

  public static void HideDialog(DialogBox dialog) {
    dialog.hide();
  }

  public static void SetDialogContent(DialogBox dialog, String mess) {
    HTML html = (HTML) ((VerticalPanel) dialog.getWidget()).getWidget(0);
    html.setHTML(mess);
  }

  public static String getComponentInfo(String typeName) {
    return YaBlocksEditor.getComponentInfo(typeName);
  }

  public static String getComponentsJSONString(String projectId) {
    return YaBlocksEditor.getComponentsJSONString(Long.parseLong(projectId));
  }

  public static String getComponentInstanceTypeName(String formName, String instanceName) {
    return YaBlocksEditor.getComponentInstanceTypeName(formName, instanceName);
  }

  public static int getYaVersion() {
    return YaVersion.YOUNG_ANDROID_VERSION;
  }

  public static int getBlocksLanguageVersion() {
    return YaVersion.BLOCKS_LANGUAGE_VERSION;
  }

  public static String getQRCode(String inString) {
    if (currentContext == null) {  // Cannot build a QR code without a current context
      return "";                // This only happens when you have no projects
    }
    return doQRCode(currentContext, inString);
  }

  /**
   * Update the language setting within BlocklyPanel.java and switch to the
   * desired language.
   *
   * @param newLanguage The desired new language setting
   */
  public void switchLanguage(String newLanguage) {
    languageSetting = newLanguage;
    doSwitchLanguage(contextName, languageSetting);
  }

  /**
   * Update the language setting within BlocklyPanel.java and switch to the
   * desired language.
   *
   * @param newLanguage The desired new language setting
   * @param contextName
   */
  public static void switchLanguage(String contextName, String newLanguage) {
    languageSetting = newLanguage;
    doSwitchLanguage(contextName, languageSetting);
  }

  /**
   * Trigger and Update of the Companion if the Companion is connected
   * and an update is available. Note: We do not compare the currently
   * running Companion's version against the version we are going to load
   * we just do it. If YaVersion.COMPANION_UPDATE_URL is "", then no
   * Update is available.
   */

  public void updateCompanion() {
    updateCompanion(contextName);
  }

  public static void updateCompanion(String contextName) {
    doUpdateCompanion(contextName);
  }

  public static String getLocalizedPropertyName(String key) {
    return ComponentsTranslation.getPropertyName(key);
  }

  public static String getLocalizedParameterName(String key) {
    return TranslationComponentParams.getName(key);
  }

  public static String getLocalizedMethodName(String key) {
    return ComponentsTranslation.getMethodName(key);
  }

  public static String getLocalizedEventName(String key) {
    return ComponentsTranslation.getEventName(key);
  }

  public static String getLocalizedComponentType(String key) {
    return TranslationDesignerPallete.getCorrespondingString(key);
  }

  @Override
  public void onComponentTypeAdded(List<String> componentTypes) {
    populateComponentTypes(contextName);
    verifyAllBlocks();

  }

  @Override
  public boolean beforeComponentTypeRemoved(List<String> componentTypes) {
    return true;
  }

  @Override
  public void onComponentTypeRemoved(Map<String, String> componentTypes) {
    populateComponentTypes(contextName);
  }

  @Override
  public void onResetDatabase() {
    populateComponentTypes(contextName);
  }
  // ------------ Native methods ------------

  /**
   * Take a Javascript function, embedded in an opaque JavaScriptObject,
   * and call it.
   *
   * @param callback the Javascript callback.
   */
  private static native void doCallBack(JavaScriptObject callback, String buttonName) /*-{
    callback.call(null, buttonName);
  }-*/;

  private static native void exportMethodsToJavascript() /*-{
    $wnd.BlocklyPanel_initBlocksArea =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::initBlocksArea(Ljava/lang/String;));
    $wnd.BlocklyPanel_blocklyWorkspaceChanged =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::blocklyWorkspaceChanged(Ljava/lang/String;Z));
    $wnd.BlocklyPanel_switchLanguage =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::switchLanguage(Ljava/lang/String;Ljava/lang/String;));
    $wnd.BlocklyPanel_checkWarningState =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::checkWarningState(Ljava/lang/String;));
    $wnd.BlocklyPanel_callToggleWarning =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::callToggleWarning());
    $wnd.BlocklyPanel_checkIsAdmin =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::checkIsAdmin());
    $wnd.BlocklyPanel_indicateDisconnect =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::indicateDisconnect());
    // Note: above lines are longer than 100 chars but I'm not sure whether they can be split
    $wnd.BlocklyPanel_pushScreen =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::pushScreen(Ljava/lang/String;));
    $wnd.BlocklyPanel_popScreen =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::popScreen());
    $wnd.BlocklyPanel_createDialog =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::createDialog(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILcom/google/gwt/core/client/JavaScriptObject;));
    $wnd.BlocklyPanel_hideDialog =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::HideDialog(Lcom/google/gwt/user/client/ui/DialogBox;));
    $wnd.BlocklyPanel_setDialogContent =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::SetDialogContent(Lcom/google/gwt/user/client/ui/DialogBox;Ljava/lang/String;));
    $wnd.BlocklyPanel_getComponentInstanceTypeName =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::getComponentInstanceTypeName(Ljava/lang/String;Ljava/lang/String;));
    $wnd.BlocklyPanel_getComponentInfo =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::getComponentInfo(Ljava/lang/String;));
    $wnd.BlocklyPanel_getComponentsJSONString =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::getComponentsJSONString(Ljava/lang/String;));
    $wnd.BlocklyPanel_getYaVersion =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::getYaVersion());
    $wnd.BlocklyPanel_getBlocksLanguageVersion =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::getBlocksLanguageVersion());
    $wnd.BlocklyPanel_getLocalizedPropertyName =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::getLocalizedPropertyName(Ljava/lang/String;));
    $wnd.BlocklyPanel_getLocalizedParameterName =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::getLocalizedParameterName(Ljava/lang/String;));
    $wnd.BlocklyPanel_getLocalizedMethodName =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::getLocalizedMethodName(Ljava/lang/String;));
    $wnd.BlocklyPanel_getLocalizedEventName =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::getLocalizedEventName(Ljava/lang/String;));
    $wnd.BlocklyPanel_getLocalizedComponentType =
        $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::getLocalizedComponentType(Ljava/lang/String;));
    $wnd.BlocklyPanel_getBackpack =
      $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::getBackpack());
    $wnd.BlocklyPanel_setBackpack =
      $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::setBackpack(Ljava/lang/String;Z));
    $wnd.BlocklyPanel_isFormBlockly =
      $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::isFormBlockly(Ljava/lang/String;));
    $wnd.BlocklyPanel_isTaskBlockly =
      $entry(@com.google.appinventor.client.editor.youngandroid.BlocklyPanel::isTaskBlockly(Ljava/lang/String;));
  }-*/;

  private native void initJS() /*-{
    $wnd.myBlocklyPanel = this;
    $wnd.Blockly = null;  // will be set to our iframe's Blockly object when
                          // the iframe finishes loading
  }-*/;

  private static native void doAddComponent(String contextName, String typeDescription,
                                            String instanceName, String uid) /*-{
    $wnd.Blocklies[contextName].Component.add(instanceName, uid);
  }-*/;

  private static native void doRemoveComponent(String contextName, String typeName,
                                               String instanceName, String uid) /*-{
    $wnd.Blocklies[contextName].Component.remove(typeName, instanceName, uid);
  }-*/;

  private static native void doRenameComponent(String contextName, String typeName, String oldName,
                                               String newName, String uid) /*-{
    $wnd.Blocklies[contextName].Component.rename(oldName, newName, uid)
  }-*/;

  private static native void doShowComponentBlocks(String contextName, String name) /*-{
    $wnd.Blocklies[contextName].Drawer.showComponent(name);
  }-*/;

  public static native void doHideComponentBlocks(String contextName) /*-{
    $wnd.Blocklies[contextName].Drawer.hide();
  }-*/;

  private static native void doShowBuiltinBlocks(String contextName, String drawerName) /*-{
    var myBlockly = $wnd.Blocklies[contextName];
    myBlockly.Drawer.hide();
    myBlockly.Drawer.showBuiltin(drawerName);
  }-*/;

  public static native void doHideBlocks(String contextName) /*-{
    $wnd.Blocklies[contextName].Drawer.hide();
  }-*/;

  private static native void doShowGenericBlocks(String contextName, String drawerName) /*-{
    var myBlockly = $wnd.Blocklies[contextName];
    myBlockly.Drawer.hide();
    myBlockly.Drawer.showGeneric(drawerName);
  }-*/;

  public static native boolean doDrawerShowing(String contextName) /*-{
    return $wnd.Blocklies[contextName].Drawer.isShowing();
  }-*/;

  // [lyn, 2014/10/27] added contextJson for upgrading
  public static native void doLoadBlocksContent(String contextName, String contextJson, String blocksContent) /*-{
    $wnd.Blocklies[contextName].SaveFile.load(contextJson, blocksContent);
    $wnd.Blocklies[contextName].Component.verifyAllBlocks();
  }-*/;

  public static native String doGetBlocksContent(String contextName) /*-{
    return $wnd.Blocklies[contextName].SaveFile.get();
  }-*/;

  public static native String doGetYailRepl(String contextName, String contextJson, String packageName) /*-{
    return $wnd.Blocklies[contextName].Yail.getFormYail(contextJson, packageName, true);
  }-*/;

  public static native String doGetFormYail(String formName, String formJson, String packageName) /*-{
    return $wnd.Blocklies[formName].Yail.getFormYail(formJson, packageName);
  }-*/;

  public static native String doGetTaskYail(String taskName, String taskJson, String packageName) /*-{
    return $wnd.Blocklies[taskName].Yail.getTaskYail(taskJson, packageName);
  }-*/;

  public static native void doSendFormJson(String formName, String formJson, String packageName) /*-{
    $wnd.Blocklies[formName].ReplMgr.sendFormData(formName, formJson, packageName);
  }-*/;

  public static native void doSendTaskJson(String taskName, String taskJson, String packageName) /*-{
    $wnd.Blocklies[taskName].ReplMgr.sendTaskData(taskName, taskJson, packageName);
  }-*/;

  public static native void doResetYail(String contextName) /*-{
    $wnd.Blocklies[contextName].ReplMgr.resetYail();
  }-*/;

  public static native void doPollYail(String contextName) /*-{
    try {
      $wnd.Blocklies[contextName].ReplMgr.pollYail();
    } catch (e) {
      $wnd.console.log("doPollYail() Failed");
      $wnd.console.log(e);
    }
  }-*/;

  public static native void doStartRepl(String contextName, boolean alreadyRunning, boolean forEmulator, boolean forUsb) /*-{
    $wnd.Blocklies[contextName].ReplMgr.startRepl(alreadyRunning, forEmulator, forUsb);
  }-*/;

  public static native void doHardReset(String contextName) /*-{
    $wnd.Blocklies[contextName].ReplMgr.ehardreset(contextName);
  }-*/;

  public static native void doRenderBlockly(String contextName) /*-{
    $wnd.Blocklies[contextName].BlocklyEditor.render();
  }-*/;

  public static native void doToggleWarning(String contextName) /*-{
    $wnd.Blocklies[contextName].WarningHandler.warningToggle();
  }-*/;

  public static native void doCheckWarnings(String contextName) /*-{
    $wnd.Blocklies[contextName].WarningHandler.checkAllBlocksForWarningsAndErrors();
  }-*/;

  public static native String getCompVersion() /*-{
    return $wnd.PREFERRED_COMPANION;
  }-*/;

  static native void setPreferredCompanion(String comp, String url) /*-{
    $wnd.PREFERRED_COMPANION = comp;
    $wnd.COMPANION_UPDATE_URL = url;
  }-*/;

  static native void addAcceptableCompanionPackage(String comp) /*-{
    $wnd.ACCEPTABLE_COMPANION_PACKAGE = comp;
  }-*/;

  static native void addAcceptableCompanion(String comp) /*-{
    if ($wnd.ACCEPTABLE_COMPANIONS === null ||
        $wnd.ACCEPTABLE_COMPANIONS === undefined) {
      $wnd.ACCEPTABLE_COMPANIONS = [];
    }
    $wnd.ACCEPTABLE_COMPANIONS.push(comp);
  }-*/;

  static native String doQRCode(String contextName, String inString) /*-{
    return $wnd.Blocklies[contextName].ReplMgr.makeqrcode(inString);
  }-*/;

  /*
   * Switch the Blockly's language setting to "language" as specified in the
   * parameter argument.
   */
  public static native void doSwitchLanguage(String contextName, String language) /*-{
    $wnd.Blocklies[contextName].language_switch.switchLanguage(language);
  }-*/;

  public static native void doUpdateCompanion(String contextName) /*-{
    $wnd.Blocklies[contextName].ReplMgr.triggerUpdate();
  }-*/;

  public static native String getURL() /*-{
    return $wnd.location.href;
  }-*/;

  /*
   * Update Component Types in Blockly ComponentTypes
   */
  public static native void populateComponentTypes(String contextName) /*-{
      $wnd.Blocklies[contextName].ComponentTypes.populateTypes(top.location.hash.substr(1));
  }-*/;

  /*
   * Update Component Types in Blockly ComponentTypes
   */
  public static native void doVerifyAllBlocks(String contextName) /*-{
      $wnd.Blocklies[contextName].Component.verifyAllBlocks();
  }-*/;

  public static native void doFetchBlocksImage(String contextName, Callback<String,String> callback) /*-{
      var callb = $entry(function(result, error) {
          if (error) {
             callback.@com.google.gwt.core.client.Callback::onFailure(Ljava/lang/Object;)(error);
          } else {
             callback.@com.google.gwt.core.client.Callback::onSuccess(Ljava/lang/Object;)(result);
          }
      });
      $wnd.Blocklies[contextName].ExportBlocksImage.getUri(callb);
  }-*/;

}
