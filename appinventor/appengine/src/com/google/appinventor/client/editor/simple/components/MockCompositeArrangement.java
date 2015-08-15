// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2015 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.simple.components;

import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.appinventor.components.common.ComponentConstants;

import com.google.gwt.user.client.ui.TreeItem;

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

  @Override
  protected TreeItem buildTree() {
    TreeItem compositeArrangementNode = super.buildTree();
    compositeArrangementNode.removeItems();
    return compositeArrangementNode;
  }

  @Override
  protected boolean isPropertyVisible(String propertyName) {
    // hide all properties except Visible
    if (propertyName.equals(PROPERTY_NAME_WIDTH) ||
        propertyName.equals(PROPERTY_NAME_HEIGHT) ||
        propertyName.equals(PROPERTY_NAME_HORIZONTAL_ALIGNMENT) ||
        propertyName.equals(PROPERTY_NAME_VERTICAL_ALIGNMENT)) {
      return false;
    }

    return super.isPropertyVisible(propertyName);
  }

  @Override
  public boolean acceptableTarget() {
    return false;
  }

  @Override
  protected void onComponentAdded(MockComponent component) {
    super.onComponentAdded(component);
    int[] eventBits = new int[]{Event.ONMOUSEDOWN, Event.ONMOUSEUP,
        Event.ONMOUSEMOVE, Event.ONMOUSEOVER, Event.ONMOUSEOUT, Event.ONCLICK};
    for (int eventBit : eventBits) {
      component.unsinkEvents(eventBit);
    }
  }
}
