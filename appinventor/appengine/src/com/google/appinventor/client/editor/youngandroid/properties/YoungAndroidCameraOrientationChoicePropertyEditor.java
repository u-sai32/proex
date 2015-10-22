package com.google.appinventor.client.editor.youngandroid.properties;

import static com.google.appinventor.client.Ode.MESSAGES;
import com.google.appinventor.client.widgets.properties.ChoicePropertyEditor;

/**
 *  Property Editor for Camera Orientation
 *
 *
 */

public class YoungAndroidCameraOrientationChoicePropertyEditor extends ChoicePropertyEditor {


  //Camera Orientation choices
  private static final Choice[] orientations = new Choice[] {
      new Choice(MESSAGES.automaticCameraOrientation(), "0"),
      new Choice(MESSAGES.topCameraOrientation(), "1"),
      new Choice(MESSAGES.rightCameraOrientation(), "2"),
      new Choice(MESSAGES.bottomCameraOrientation(), "3"),
      new Choice(MESSAGES.leftCameraOrientation(), "4")
  };


  public YoungAndroidCameraOrientationChoicePropertyEditor() {
    super(orientations);
  }

}