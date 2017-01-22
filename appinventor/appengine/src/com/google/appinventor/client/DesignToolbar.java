// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client;

import com.google.appinventor.client.editor.FileEditor;
import com.google.appinventor.client.editor.ProjectEditor;
import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.appinventor.client.editor.youngandroid.BlocklyPanel;
import com.google.appinventor.client.editor.youngandroid.YaBlocksEditor;
import com.google.appinventor.client.editor.youngandroid.YaContextEditor;
import com.google.appinventor.client.explorer.commands.AddFormCommand;
import com.google.appinventor.client.explorer.commands.AddTaskCommand;
import com.google.appinventor.client.explorer.commands.ChainableCommand;
import com.google.appinventor.client.explorer.commands.DeleteFileCommand;
import com.google.appinventor.client.output.OdeLog;
import com.google.appinventor.client.tracking.Tracking;
import com.google.appinventor.client.widgets.DropDownButton.DropDownItem;
import com.google.appinventor.client.widgets.Toolbar;
import com.google.appinventor.common.version.AppInventorFeatures;
import com.google.appinventor.shared.rpc.project.ProjectRootNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidSourceNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

import java.util.*;

import static com.google.appinventor.client.Ode.MESSAGES;

/**
 * The design toolbar houses command buttons in the Young Android Design
 * tab (for the UI designer (a.k.a, Form Editor) and Blocks Editor).
 *
 */
public class DesignToolbar extends Toolbar {

  private boolean isReadOnly;   // If the UI is in read only mode

  /*
   * A Screen groups together the form editor and blocks editor for an
   * application screen. Name is the name of the screen (form) displayed
   * in the screens pull-down.
   */
  public static class Context {
    public final String contextName;
    public final YaContextEditor contextEditor;
    public final FileEditor blocksEditor;

    public Context(String name, FileEditor contextEditor, FileEditor blocksEditor) {
      this.contextName = name;
      this.contextEditor = (YaContextEditor) contextEditor;
      this.blocksEditor = blocksEditor;
    }
  }



  /*
   * A project as represented in the DesignToolbar. Each project has a name
   * (as displayed in the DesignToolbar on the left), a set of named screens,
   * and an indication of which screen is currently being edited.
   */
  public static class DesignProject {
    public final String name;
    public final Map<String, Context> screens; // screen name -> Screen
    public final Map<String, Context> tasks; // task name -> Task
    public String currentContext; // name of currently displayed screen/task
    public String lastScreen; // name of the last opened screen. May be equal to currentContext.

    public DesignProject(String name, long projectId) {
      this.name = name;
      screens = Maps.newHashMap();
      tasks = Maps.newHashMap();
      // Screen1 is initial screen by default
      currentContext = YoungAndroidSourceNode.SCREEN1_FORM_NAME;
      lastScreen = currentContext;
      // Let BlocklyPanel know which screen to send Yail for
      BlocklyPanel.setCurrentForm(projectId + "_" + currentContext);
    }

    public Context getContext(String contextName) {
      Context context = screens.get(contextName);
      if (context == null) {
        context = tasks.get(contextName);
      }
      return context;
    }

    public ArrayList<Context> getTasks() {
      ArrayList<Context> tasks = new ArrayList<>();
      for (Context task : this.tasks.values()) {
        tasks.add(task);
      }
      return tasks;
    }

    public Context getLastScreen() {
      return screens.get(lastScreen);
    }

    // Returns true if we added the screen (it didn't previously exist), false otherwise.
    public boolean addScreen(String name, FileEditor formEditor, FileEditor blocksEditor) {
      if (!screens.containsKey(name) && !tasks.containsKey(name)) {
        screens.put(name, new Context(name, formEditor, blocksEditor));
        return true;
      } else {
        return false;
      }
    }

    public void removeScreen(String name) {
      screens.remove(name);
    }

    public boolean addTask(String name, FileEditor taskEditor, FileEditor blocksEditor) {
      if (!tasks.containsKey(name) && !screens.containsKey(name)) {
        tasks.put(name, new Context(name, taskEditor, blocksEditor));
        return true;
      } else {
        return false;
      }
    }

    public void removeTask(String name) {
      tasks.remove(name);
    }

    public void setCurrentContext(String name) {
      currentContext = name;
      if (screens.containsKey(currentContext)) {
        this.lastScreen = currentContext;
      }
    }
  }

  private static final String WIDGET_NAME_ADDFORM = "AddForm";
  private static final String WIDGET_NAME_ADDTASK = "AddTask";
  private static final String WIDGET_NAME_REMOVECONTEXT = "RemoveContext";

  private static final String WIDGET_NAME_SCREENS_DROPDOWN = "ScreensDropdown";
  private static final String WIDGET_NAME_SWITCH_TO_BLOCKS_EDITOR = "SwitchToBlocksEditor";
  private static final String WIDGET_NAME_SWITCH_TO_FORM_EDITOR = "SwitchToFormEditor";

  // Switch language
  private static final String WIDGET_NAME_SWITCH_LANGUAGE = "Language";
  private static final String WIDGET_NAME_SWITCH_LANGUAGE_ENGLISH = "English";
  private static final String WIDGET_NAME_SWITCH_LANGUAGE_CHINESE_CN = "Simplified Chinese";
  private static final String WIDGET_NAME_SWITCH_LANGUAGE_SPANISH_ES = "Spanish-Spain";
  //private static final String WIDGET_NAME_SWITCH_LANGUAGE_GERMAN = "German";
  //private static final String WIDGET_NAME_SWITCH_LANGUAGE_VIETNAMESE = "Vietnamese";

  // Enum for type of view showing in the design tab
  public enum View {
    CONTEXT,   // YoungAndroidContext editor view
    BLOCKS  // Blocks editor view
  }
  public View currentView = View.CONTEXT;

  public Label projectNameLabel;

  // Project currently displayed in designer
  private DesignProject currentProject;

  // Map of project id to project info for all projects we've ever shown
  // in the Designer in this session.
  public Map<Long, DesignProject> projectMap = Maps.newHashMap();

  // Stack of screens switched to from the Companion
  // We implement screen switching in the Companion by having it tell us
  // to switch screens. We then load into the companion the new Screen
  // We save where we were because the companion can have us return from
  // a screen. If we switch projects in the browser UI, we clear this
  // list of screens as we are effectively running a different application
  // on the device.
  public static LinkedList<String> pushedScreens = Lists.newLinkedList();

  /**
   * Initializes and assembles all commands into buttons in the toolbar.
   */
  public DesignToolbar() {
    super();

    isReadOnly = Ode.getInstance().isReadOnly();

    projectNameLabel = new Label();
    projectNameLabel.setStyleName("ya-ProjectName");
    HorizontalPanel toolbar = (HorizontalPanel) getWidget();
    toolbar.insert(projectNameLabel, 0);

    // width of palette minus cellspacing/border of buttons
    toolbar.setCellWidth(projectNameLabel, "222px");

    List<DropDownItem> screenItems = Lists.newArrayList();
    addDropDownButton(WIDGET_NAME_SCREENS_DROPDOWN, MESSAGES.screensButton(), screenItems);

    if (AppInventorFeatures.allowMultiScreenApplications() && !isReadOnly) {
      addButton(new ToolbarItem(WIDGET_NAME_ADDFORM, MESSAGES.addFormButton(),
          new AddFormAction()));
      addButton(new ToolbarItem(WIDGET_NAME_ADDTASK, MESSAGES.addTaskButton(),
          new AddTaskAction()));
      addButton(new ToolbarItem(WIDGET_NAME_REMOVECONTEXT, MESSAGES.removeButton(),
          new RemoveContextAction()));
    }

    addButton(new ToolbarItem(WIDGET_NAME_SWITCH_TO_FORM_EDITOR,
        MESSAGES.switchToFormEditorButton(), new SwitchToFormEditorAction()), true);
    addButton(new ToolbarItem(WIDGET_NAME_SWITCH_TO_BLOCKS_EDITOR,
        MESSAGES.switchToBlocksEditorButton(), new SwitchToBlocksEditorAction()), true);

    // Gray out the Designer button and enable the blocks button
    toggleEditor(false);
    Ode.getInstance().getTopToolbar().updateFileMenuButtons(0);
  }

  private class AddFormAction implements Command {
    @Override
    public void execute() {
      Ode ode = Ode.getInstance();
      if (ode.screensLocked()) {
        return;                 // Don't permit this if we are locked out (saving files)
      }
      ProjectRootNode projectRootNode = ode.getCurrentYoungAndroidProjectRootNode();
      if (projectRootNode != null) {
        ChainableCommand cmd = new AddFormCommand();
        cmd.startExecuteChain(Tracking.PROJECT_ACTION_ADDFORM_YA, projectRootNode);
      }
    }
  }

  private class AddTaskAction implements Command {
    @Override
    public void execute() {
      Ode ode = Ode.getInstance();
      if (ode.screensLocked() || ode.taskExists()) {
        return;
      }
      ProjectRootNode projectRootNode = ode.getCurrentYoungAndroidProjectRootNode();
      if (projectRootNode != null) {
        ChainableCommand cmd = new AddTaskCommand();
        cmd.startExecuteChain(Tracking.PROJECT_ACTION_ADDTASK_YA, projectRootNode);
      }
    }
  }

  private class RemoveContextAction implements Command {
    @Override
    public void execute() {
      Ode ode = Ode.getInstance();
      if (ode.screensLocked()) {
        return;                 // Don't permit this if we are locked out (saving files)
      }
      YoungAndroidSourceNode sourceNode = ode.getCurrentYoungAndroidSourceNode();
      FileEditor fileEditor = ode.getCurrentFileEditor();
      if (sourceNode != null && !sourceNode.isScreen1()) {
        // DeleteFileCommand handles the whole operation, including displaying the confirmation
        // message dialog, closing the form editor and the blocks editor,
        // deleting the files in the server's storage, and deleting the
        // corresponding client-side nodes (which will ultimately trigger the
        // screen deletion in the DesignToolbar).
        String confirmMessage = "";
        String trackingAction = "";
        boolean isForm = false;
        boolean isTask = false;
        if (fileEditor instanceof YaContextEditor) {
          YaContextEditor yaContextEditor = (YaContextEditor) fileEditor;
          if (yaContextEditor.isFormEditor()) {
            isForm = true;
          } else if (yaContextEditor.isTaskEditor()) {
            isTask = true;
          }
        } else if (fileEditor instanceof YaBlocksEditor) {
          YaBlocksEditor blocksEditor = (YaBlocksEditor) fileEditor;
          if (blocksEditor.isFormBlocksEditor()) {
            isForm = true;
          }
          else if (blocksEditor.isTaskBlocksEditor()) {
            isTask = true;
          }
        }
        if (isForm) {
          confirmMessage = MESSAGES.reallyDeleteForm(sourceNode.getContextName());
          trackingAction = Tracking.PROJECT_ACTION_REMOVEFORM_YA;
        } else if (isTask) {
          confirmMessage = MESSAGES.reallyDeleteTask(sourceNode.getContextName());
          trackingAction = Tracking.PROJECT_ACTION_REMOVETASK_YA;
        }
        if (isForm || isTask) {
          final String deleteConfirmationMessage = confirmMessage;
          ChainableCommand cmd = new DeleteFileCommand() {
            @Override
            protected boolean deleteConfirmation() {
              return Window.confirm(deleteConfirmationMessage);
            }
          };
          cmd.startExecuteChain(trackingAction, sourceNode);
        }
      }
    }
  }

  private class SwitchScreenAction implements Command {
    private final long projectId;
    private final String name;  // screen name

    public SwitchScreenAction(long projectId, String screenName) {
      this.projectId = projectId;
      this.name = screenName;
    }

    @Override
    public void execute() {
      doSwitchScreen(projectId, name, currentView);
    }
  }

  private void doSwitchScreen(final long projectId, final String screenName, final View view) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        @Override
        public void execute() {
          if (Ode.getInstance().screensLocked()) { // Wait until I/O complete
            Scheduler.get().scheduleDeferred(this);
          } else {
            doSwitchScreen1(projectId, screenName, view);
          }
        }
      });
  }

  private void doSwitchScreen1(long projectId, String screenName, View view) {
    if (!projectMap.containsKey(projectId)) {
      OdeLog.wlog("DesignToolbar: no project with id " + projectId
          + ". Ignoring SwitchScreenAction.execute().");
      return;
    }
    DesignProject project = projectMap.get(projectId);
    if (currentProject != project) {
      // need to switch projects first. this will not switch screens.
      if (!switchToProject(projectId, project.name)) {
        return;
      }
    }
    String newScreenName = screenName;
    boolean isScreen = currentProject.screens.containsKey(newScreenName);
    boolean isTask = currentProject.tasks.containsKey(newScreenName);
    if (!isScreen && !isTask) {
      // Can't find the requested context in this project. This shouldn't happen, but if it does
      // for some reason, try switching to Screen1 instead.
      OdeLog.wlog("Trying to switch to non-existent screen " + newScreenName +
          " in project " + currentProject.name + ". Trying Screen1 instead.");
      if (currentProject.screens.containsKey(YoungAndroidSourceNode.SCREEN1_FORM_NAME)) {
        newScreenName = YoungAndroidSourceNode.SCREEN1_FORM_NAME;
      } else {
        // something went seriously wrong!
        ErrorReporter.reportError("Something is wrong. Can't find Screen1 for project "
            + currentProject.name);
        return;
      }
    }
    currentView = view;
    if (isScreen) {
      Context screen = currentProject.screens.get(newScreenName);
      ProjectEditor projectEditor = screen.contextEditor.getProjectEditor();
      currentProject.setCurrentContext(newScreenName);
      setDropDownButtonCaption(WIDGET_NAME_SCREENS_DROPDOWN, newScreenName);
      OdeLog.log("Setting currentContext to " + newScreenName);
      if (currentView == View.CONTEXT) {
        projectEditor.selectFileEditor(screen.contextEditor);
        toggleEditor(false);
        Ode.getInstance().getTopToolbar().updateFileMenuButtons(1);
      } else {  // must be View.BLOCKS
        projectEditor.selectFileEditor(screen.blocksEditor);
        toggleEditor(true);
        Ode.getInstance().getTopToolbar().updateFileMenuButtons(1);
      }
    } else if (isTask) {
      Context task = currentProject.tasks.get(newScreenName);
      ProjectEditor projectEditor = task.contextEditor.getProjectEditor();
      currentProject.setCurrentContext(newScreenName);
      setDropDownButtonCaption(WIDGET_NAME_SCREENS_DROPDOWN, newScreenName);
      OdeLog.log("Setting currentContext to " + newScreenName);
      if (currentView == View.CONTEXT) {
        projectEditor.selectFileEditor(task.contextEditor);
        toggleEditor(false);
        Ode.getInstance().getTopToolbar().updateFileMenuButtons(1);
      } else {
        projectEditor.selectFileEditor(task.blocksEditor);
        toggleEditor(true);
        Ode.getInstance().getTopToolbar().updateFileMenuButtons(1);
      }
    }
    // Inform the Blockly Panel which project/screen (aka form) we are working on
    BlocklyPanel.setCurrentForm(projectId + "_" + newScreenName);
  }

  private class SwitchToBlocksEditorAction implements Command {
    @Override
    public void execute() {
      if (currentProject == null) {
        OdeLog.wlog("DesignToolbar.currentProject is null. "
            + "Ignoring SwitchToBlocksEditorAction.execute().");
        return;
      }
      if (currentView != View.BLOCKS) {
        long projectId = Ode.getInstance().getCurrentYoungAndroidProjectRootNode().getProjectId();
        switchToScreen(projectId, currentProject.currentContext, View.BLOCKS);
        toggleEditor(true);       // Gray out the blocks button and enable the designer button
        Ode.getInstance().getTopToolbar().updateFileMenuButtons(1);
      }
    }
  }

  private class SwitchToFormEditorAction implements Command {
    @Override
    public void execute() {
      if (currentProject == null) {
        OdeLog.wlog("DesignToolbar.currentProject is null. "
            + "Ignoring SwitchToFormEditorAction.execute().");
        return;
      }
      if (currentView != View.CONTEXT) {
        long projectId = Ode.getInstance().getCurrentYoungAndroidProjectRootNode().getProjectId();
        switchToScreen(projectId, currentProject.currentContext, View.CONTEXT);
        toggleEditor(false);      // Gray out the Designer button and enable the blocks button
        Ode.getInstance().getTopToolbar().updateFileMenuButtons(1);
      }
    }
  }

  public void addProject(long projectId, String projectName) {
    if (!projectMap.containsKey(projectId)) {
      projectMap.put(projectId, new DesignProject(projectName, projectId));
      OdeLog.log("DesignToolbar added project " + projectName + " with id " + projectId);
    } else {
      OdeLog.wlog("DesignToolbar ignoring addProject for existing project " + projectName
          + " with id " + projectId);
    }
  }

  // Switch to an existing project. Note that this does not switch screens.
  // TODO(sharon): it might be better to throw an exception if the
  // project doesn't exist.
  private boolean switchToProject(long projectId, String projectName) {
    if (projectMap.containsKey(projectId)) {
      DesignProject project = projectMap.get(projectId);
      if (project == currentProject) {
        OdeLog.wlog("DesignToolbar: ignoring call to switchToProject for current project");
        return true;
      }
      pushedScreens.clear();    // Effectively switching applications clear stack of screens
      clearDropDownMenu(WIDGET_NAME_SCREENS_DROPDOWN);
      OdeLog.log("DesignToolbar: switching to existing project " + projectName + " with id "
          + projectId);
      currentProject = projectMap.get(projectId);
      // TODO(sharon): add screens to drop-down menu in the right order
      for (Context screen : currentProject.screens.values()) {
        addDropDownButtonItem(WIDGET_NAME_SCREENS_DROPDOWN, new DropDownItem(screen.contextName,
            screen.contextName, new SwitchScreenAction(projectId, screen.contextName)));
      }
      for (Context task : currentProject.tasks.values()) {
        addDropDownButtonItem(WIDGET_NAME_SCREENS_DROPDOWN, new DropDownItem(task.contextName,
            task.contextName, new SwitchScreenAction(projectId, task.contextName)));
      }
      projectNameLabel.setText(projectName);
    } else {
      ErrorReporter.reportError("Design toolbar doesn't know about project " + projectName +
          " with id " + projectId);
      OdeLog.wlog("Design toolbar doesn't know about project " + projectName + " with id "
          + projectId);
      return false;
    }
    return true;
  }

  public void addContext(long projectId, String name, FileEditor contextEditor,
     FileEditor blocksEditor) {
    if (!projectMap.containsKey(projectId)) {
      OdeLog.wlog("DesignToolbar can't find project " + name + " with id " + projectId
          + ". Ignoring addScreen().");
      return;
    }


  }



  /*
   * Add a screen name to the drop-down for the project with id projectId.
   * name is the form name, formEditor is the file editor for the form UI,
   * and blocksEditor is the file editor for the form's blocks.
   */
  public void addScreen(long projectId, String name, FileEditor formEditor,
      FileEditor blocksEditor) {
    if (!projectMap.containsKey(projectId)) {
      OdeLog.wlog("DesignToolbar can't find project " + name + " with id " + projectId
          + ". Ignoring addScreen().");
      return;
    }
    DesignProject project = projectMap.get(projectId);
    if (project.addScreen(name, formEditor, blocksEditor)) {
      if (currentProject == project) {
        addDropDownButtonItem(WIDGET_NAME_SCREENS_DROPDOWN, new DropDownItem(name,
            name, new SwitchScreenAction(projectId, name)));
      }
    }
  }

  /*
   * Add a task name to the drop-down for the project with id projectId.
   * name is the task name, taskEditor is the file editor for the task,
   * and blocksEditor is the file editor for the tasks's blocks.
   */
  public void addTask(long projectId, String name, FileEditor taskEditor,
                      FileEditor blocksEditor) {
    if (!projectMap.containsKey(projectId)) {
      OdeLog.wlog("DesignToolbar can't find project " + name + " with id " + projectId
          + ". Ignoring addTask().");
      return;
    }
    DesignProject project = projectMap.get(projectId);
    if (project.addTask(name, taskEditor, blocksEditor)) {
      if (currentProject == project) {
        addDropDownButtonItem(WIDGET_NAME_SCREENS_DROPDOWN, new DropDownItem(name,
            name, new SwitchScreenAction(projectId, name))); //TODO:
      }
    }
  }

/*
 * PushScreen -- Static method called by Blockly when the Companion requests
 * That we switch to a new screen. We keep track of the Screen we were on
 * and push that onto a stack of Screens which we pop when requested by the
 * Companion.
 */
  public static boolean pushScreen(String screenName) {
    DesignToolbar designToolbar = Ode.getInstance().getDesignToolbar();
    long projectId = Ode.getInstance().getCurrentYoungAndroidProjectId();
    String currentScreen = designToolbar.currentProject.currentContext;
    if (!designToolbar.currentProject.screens.containsKey(screenName)) // No such screen -- can happen
      return false;                                                    // because screen is user entered here.
    pushedScreens.addFirst(currentScreen);
    designToolbar.doSwitchScreen(projectId, screenName, View.BLOCKS);
    return true;
  }

  public static void popScreen() {
    DesignToolbar designToolbar = Ode.getInstance().getDesignToolbar();
    long projectId = Ode.getInstance().getCurrentYoungAndroidProjectId();
    String newScreen;
    if (pushedScreens.isEmpty()) {
      return;                   // Nothing to do really
    }
    newScreen = pushedScreens.removeFirst();
    designToolbar.doSwitchScreen(projectId, newScreen, View.BLOCKS);
  }

  // Called from Javascript when Companion is disconnected
  public static void clearScreens() {
    pushedScreens.clear();
  }

  /*
   * Switch to screen name in project projectId. Also switches projects if
   * necessary.
   */
  public void switchToScreen(long projectId, String screenName, View view) {
    doSwitchScreen(projectId, screenName, view);
  }

  /*
   * Remove screen name (if it exists) from project projectId
   */
  public void removeScreen(long projectId, String name) {
    if (!projectMap.containsKey(projectId)) {
      OdeLog.wlog("DesignToolbar can't find project " + name + " with id " + projectId
          + " Ignoring removeScreen().");
      return;
    }
    OdeLog.log("DesignToolbar: got removeScreen for project " + projectId
        + ", screen " + name);
    DesignProject project = projectMap.get(projectId);
    if (!project.screens.containsKey(name)) {
      // already removed this screen
      return;
    }
    if (currentProject == project) {
      // if removing current screen, choose a new screen to show
      if (currentProject.currentContext.equals(name)) {
        // TODO(sharon): maybe make a better choice than screen1, but for now
        // switch to screen1 because we know it is always there
        switchToScreen(projectId, YoungAndroidSourceNode.SCREEN1_FORM_NAME, View.CONTEXT);
      }
      removeDropDownButtonItem(WIDGET_NAME_SCREENS_DROPDOWN, name);
    }
    project.removeScreen(name);
  }

  private void toggleEditor(boolean blocks) {
    setButtonEnabled(WIDGET_NAME_SWITCH_TO_BLOCKS_EDITOR, !blocks);
    setButtonEnabled(WIDGET_NAME_SWITCH_TO_FORM_EDITOR, blocks);

    if (AppInventorFeatures.allowMultiScreenApplications() && !isReadOnly) {
      if (getCurrentProject() == null || getCurrentProject().currentContext == "Screen1") {
        setButtonEnabled(WIDGET_NAME_REMOVECONTEXT, false);
      } else {
        setButtonEnabled(WIDGET_NAME_REMOVECONTEXT, true);
      }
    }
  }

  public DesignProject getCurrentProject() {
    return currentProject;
  }
}
