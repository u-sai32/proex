// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2015 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.simple.components;

import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.appinventor.components.common.ComponentConstants;

public final class MockCompositeArrangement extends MockHVArrangement {

  public static final String TYPE = "CompositeArrangement";

  /**
   * Creates a new MockCompositeArrangement component.
   * It is basically a MockVerticalArrangement but with special configurations.
   * It uses extension.png as the image so that users view it as an extension.
   *
   * @param editor  editor of source file the component belongs to
   */
  public MockCompositeArrangement(SimpleEditor editor) {
    super(editor, TYPE, images.extension(),
        ComponentConstants.LAYOUT_ORIENTATION_VERTICAL);
  }

}
