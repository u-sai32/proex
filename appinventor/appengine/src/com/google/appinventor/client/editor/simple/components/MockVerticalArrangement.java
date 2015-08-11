// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.simple.components;

import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.appinventor.components.common.ComponentConstants;
import com.google.appinventor.client.output.OdeLog;

import com.google.appinventor.client.editor.youngandroid.palette.YoungAndroidPalettePanel;
import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.appinventor.client.Ode;
import com.google.appinventor.client.editor.youngandroid.YaFormEditor;
import com.google.appinventor.shared.rpc.project.ChecksumedLoadFile;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.shared.properties.json.JSONParser;
import com.google.appinventor.client.properties.json.ClientJsonParser;
import com.google.appinventor.shared.properties.json.JSONObject;
import com.google.appinventor.shared.properties.json.JSONArray;
import com.google.appinventor.shared.properties.json.JSONParser;
import com.google.appinventor.shared.properties.json.JSONValue;
import com.google.appinventor.shared.youngandroid.YoungAndroidSourceAnalyzer;
import com.google.appinventor.client.editor.FileEditor;

/**
 * Mock VerticalArrangement component.
 *
 * @author sharon@google.com (Sharon Perl)
 */
public final class MockVerticalArrangement extends MockHVArrangement {

  /**
   * Component type name.
   */
  public static final String TYPE = "VerticalArrangement";

  private static final JSONParser JSON_PARSER = new ClientJsonParser();

  /**
   * Creates a new MockVerticalArrangement component.
   *
   * @param editor  editor of source file the component belongs to
   */
  public MockVerticalArrangement(SimpleEditor editor) {
    super(editor, TYPE, images.vertical(),
        ComponentConstants.LAYOUT_ORIENTATION_VERTICAL);
  }

  @Override
  public void onAddedToContainer() {

    Ode ode = Ode.getInstance();
    long projectId = ode.getCurrentYoungAndroidProjectId();
    ode.getProjectService().load2(projectId, getAssetNode("Screen1.scmx").getFileId(),
        new OdeAsyncCallback<ChecksumedLoadFile>() {
          @Override
          public void onSuccess(ChecksumedLoadFile result) {
            try {
              JSONArray compTreeJson = YoungAndroidSourceAnalyzer.parseSourceFile(
                  result.getContent(), JSON_PARSER).get("Properties").asObject().get("$Components").asArray();
              for (JSONValue nestedComponent : compTreeJson.getElements()) {
                ((YaFormEditor) editor).createMockComponent(nestedComponent.asObject(), getForm());
              }
            } catch (Exception e) {
              OdeLog.log("message is " + e.getMessage());
            }


          }
        });
  }


}
