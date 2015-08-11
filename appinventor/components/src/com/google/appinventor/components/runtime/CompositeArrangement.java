// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2015 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.ComponentConstants;
import com.google.appinventor.components.common.YaVersion;

/**
 * A vertical arrangement with special configurations for composite components
 * All properties are hidden from users
 * Height and width are fixed to automatic
 * It is not expected to be used elsewhere
 */
@DesignerComponent(version = YaVersion.COMPOSITE_ARRANGEMENT_VERSION,
    category = ComponentCategory.INTERNAL)
@SimpleObject
public class CompositeArrangement extends VerticalArrangement {

  private static final int LENGTH_PREFERRED = -1;

  public CompositeArrangement(ComponentContainer container) {
    super(container);

    Height(LENGTH_PREFERRED);
    Width(LENGTH_PREFERRED);
  }

  @Override
  public int Height() {
    return super.Height();
  }

  @Override
  public void Height(int height) {
    // ignore
  }

  @Override
  public int Width() {
    return super.Width();
  }

  @Override
  public void Width(int width) {
    // ignore
  }

  @Override
  public int AlignHorizontal() {
    return super.AlignHorizontal();
  }

  @Override
  public void AlignHorizontal(int alignment) {
    super.AlignHorizontal(alignment);
  }

  @Override
  public int AlignVertical() {
    return super.AlignVertical();
  }

  @Override
  public void AlignVertical(int alignment) {
    super.AlignVertical(alignment);
  }
}
