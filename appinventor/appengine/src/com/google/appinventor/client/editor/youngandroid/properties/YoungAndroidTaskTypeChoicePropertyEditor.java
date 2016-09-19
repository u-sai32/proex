// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2016 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.youngandroid.properties;

import com.google.appinventor.client.widgets.properties.ChoicePropertyEditor;

import static com.google.appinventor.client.Ode.MESSAGES;

public class YoungAndroidTaskTypeChoicePropertyEditor extends ChoicePropertyEditor {

  // Button shape choices
  private static final Choice[] shapes = new Choice[] {
      new Choice(MESSAGES.quickTaskType(), "0"),
      new Choice(MESSAGES.longTaskType(), "1"),
      new Choice(MESSAGES.repeatingTaskType(), "2")
  };

  /**
   * Creates a new instance of the property editor.
   *
   * @param choices array of values to choose from
   */
  public YoungAndroidTaskTypeChoicePropertyEditor(Choice[] choices) {
    super(choices);
  }
}
