// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2016 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.simple.components;

import static com.google.appinventor.client.Ode.MESSAGES;

import java.util.HashSet;

import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.TreeItem;

/**
 * Mock Form component.
 *
 */
public final class MockTask extends MockContext {

  /**
   * Component type name.
   */
  public static final String TYPE = "Task";

  private static final String VISIBLE_TYPE = "Task";

  // Form UI components
  private MockComponent selectedComponent;


  // Set of listeners for any changes of the form
  final HashSet<FormChangeListener> formChangeListeners = new HashSet<FormChangeListener>();

  private MockFormLayout myLayout;

  // flag to control attempting to enable/disable vertical
  // alignment when scrollable property is changed
  private boolean initialized = false;

  /**
   * Creates a new MockForm component.
   *
   * @param editor  editor of source file the component belongs to
   */
  public MockTask(SimpleEditor editor) {
    super(editor, TYPE, images.form(), MockFormHelper.makeLayout());
    AbsolutePanel formWidget = new AbsolutePanel();
    formWidget.setStylePrimaryName("ode-SimpleMockForm");
    initComponent(formWidget);
    initialized = true;
  }


  @Override
  public final MockForm getForm() {
    return null;
  }

  @Override
  public final MockTask getTask() {
    return this;
  }

  @Override
  public final boolean isForm() {
    return false;
  }

  @Override
  public final boolean isTask() {
    return true;
  }



  /**
   * Forces a re-layout of the child components of the container.
   */
  public final void refresh() {
  }

  /**
   * Adds an {@link FormChangeListener} to the listener set if it isn't already in there.
   *
   * @param listener  the {@code FormChangeListener} to be added
   */
  public void addFormChangeListener(FormChangeListener listener) {
    formChangeListeners.add(listener);
  }

  /**
   * Removes an {@link FormChangeListener} from the listener list.
   *
   * @param listener  the {@code FormChangeListener} to be removed
   */
  public void removeFormChangeListener(FormChangeListener listener) {
    formChangeListeners.remove(listener);
  }

  /**
   * Triggers a component property change event to be sent to the listener on the listener list.
   */
  protected void fireComponentPropertyChanged(MockComponent component,
                                              String propertyName, String propertyValue) {
    for (FormChangeListener listener : formChangeListeners) {
      listener.onComponentPropertyChanged(component, propertyName, propertyValue);
    }
  }

  /**
   * Triggers a component removed event to be sent to the listener on the listener list.
   */
  protected void fireComponentRemoved(MockComponent component, boolean permanentlyDeleted) {
    for (FormChangeListener listener : formChangeListeners) {
      listener.onComponentRemoved(component, permanentlyDeleted);
    }
  }

  /**
   * Triggers a component added event to be sent to the listener on the listener list.
   */
  protected void fireComponentAdded(MockComponent component) {
    for (FormChangeListener listener : formChangeListeners) {
      listener.onComponentAdded(component);
    }
  }

  /**
   * Triggers a component renamed event to be sent to the listener on the listener list.
   */
  protected void fireComponentRenamed(MockComponent component, String oldName) {
    for (FormChangeListener listener : formChangeListeners) {
      listener.onComponentRenamed(component, oldName);
    }
  }

  /**
   * Triggers a component selection change event to be sent to the listener on the listener list.
   */
  protected void fireComponentSelectionChange(MockComponent component, boolean selected) {
    for (FormChangeListener listener : formChangeListeners) {
      listener.onComponentSelectionChange(component, selected);
    }
  }

  /**
   * Changes the component that is currently selected in the form.
   * <p>
   * There will always be exactly one component selected in a form
   * at any given time.
   */
  public final void setSelectedComponent(MockComponent newSelectedComponent) {
    MockComponent oldSelectedComponent = selectedComponent;

    if (newSelectedComponent == null) {
      throw new IllegalArgumentException("at least one component must always be selected");
    }
    if (newSelectedComponent == oldSelectedComponent) {
      return;
    }

    selectedComponent = newSelectedComponent;

    if (oldSelectedComponent != null) {     // Can be null initially
      oldSelectedComponent.onSelectedChange(false);
    }
    newSelectedComponent.onSelectedChange(true);
  }

  public final MockComponent getSelectedComponent() {
    return selectedComponent;
  }

  /**
   * Builds a tree of the component hierarchy of the form for display in the
   * {@code SourceStructureExplorer}.
   *
   * @return  tree showing the component hierarchy of the form
   */
  public TreeItem buildComponentsTree() {
    return buildTree();
  }
}
