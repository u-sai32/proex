// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2016 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.collect.Sets;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.OnInitializeListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Component underlying services, not directly accessible to Simple programmers.
 *
 * <p>This is the root container of any Android Service.
 *
 */
@DesignerComponent(version = YaVersion.TASK_COMPONENT_VERSION,
    category = ComponentCategory.INTERNAL,
    description = "Top-level component containing all other components in a task",
    showOnPalette = false)
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET,android.permission.ACCESS_WIFI_STATE,android.permission.ACCESS_NETWORK_STATE")
public class Task extends Service
    implements Component, ComponentContainer, HandlesEventDispatching {
  private static final String LOG_TAG = "Task";

  private static Task activeTask = null;

  private String taskName;

  // Application lifecycle related fields
  private final Set<OnDestroyListener> onDestroyListeners = Sets.newHashSet();

  // AppInventor lifecycle: listeners for the Initialize Event
  private final Set<OnInitializeListener> onInitializeListeners = Sets.newHashSet();

  private boolean taskInitialized = false;

  @Override
  public void onCreate() {
    super.onCreate();

    activeTask = this;

    String className = getClass().getName();
    int lastDot = className.lastIndexOf('.');
    taskName = className.substring(lastDot + 1);
    Log.d(LOG_TAG, "Task " + taskName + " got onCreate");

    if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
      Log.d(LOG_TAG, "Task " + taskName + " same Thread as Main UI" + Thread.currentThread() );
    }
    else {
      Log.d(LOG_TAG, "Task " + taskName + "thread not main UI thread" + Thread.currentThread() );
    }

    $define();

    Initialize();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  /**
   * An app can register to be notified when App Inventor's Initialize
   * block has fired.  They will be called in Initialize().
   *
   * @param component
   */
  public void registerForOnInitialize(OnInitializeListener component) {
    onInitializeListeners.add(component);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    String startValue = intent.getStringExtra(Form.SERVICE_ARG);
    Object decodedStartVal = Form.decodeJSONStringForForm(startValue, "get start value");
    TaskStarted(decodedStartVal);
    Log.d("Task", "Done Dispatch but not Returned");
    return START_STICKY;
  }

  @SimpleEvent(description = "Task has been started")
  public void TaskStarted(Object startValue) {
    EventDispatcher.dispatchEvent(this, "TaskStarted", startValue);
  }

  /**
   * ErrorOccurred event handler.
   */
  @SimpleEvent(
      description = "Event raised when an error occurs. Only some errors will " +
          "raise this condition.  For those errors, the system will show a notification " +
          "by default.  You can use this event handler to prescribe an error " +
          "behavior different than the default.")
  public void ErrorOccurred(Component component, String functionName, int errorNumber,
                            String message) {
    String componentType = component.getClass().getName();
    componentType = componentType.substring(componentType.lastIndexOf(".") + 1);
    Log.e(LOG_TAG, "Task " + taskName + " ErrorOccurred, errorNumber = " + errorNumber +
        ", componentType = " + componentType + ", functionName = " + functionName +
        ", messages = " + message);
    if (!EventDispatcher.dispatchEvent(
        this, "ErrorOccurred", component, functionName, errorNumber, message)
        && taskInitialized) {
      new Notifier(this).LogError(message);
    }
  }


  public void dispatchErrorOccurredEvent(final Component component, final String functionName,
                                         final int errorNumber, final Object... messageArgs) {
    Log.i(LOG_TAG, "TASK dispatchErrorOccurredEvent");
    String message = ErrorMessages.formatMessage(errorNumber, messageArgs);
    ErrorOccurred(component, functionName, errorNumber, message);
  }

  @Override
  public void onDestroy() {
    Log.i(LOG_TAG, "Task " + taskName + " got onDestroy");
    for (OnDestroyListener onDestroyListener : onDestroyListeners) {
      onDestroyListener.onDestroy();
    }
    super.onDestroy();
    // for debugging and future growth
    Log.i(LOG_TAG, "Task " + taskName + " got onDestroy");

    // Unregister events for components in this form.
    EventDispatcher.removeDispatchDelegate(this);
  }

  public void registerForOnDestroy(OnDestroyListener component) {
    onDestroyListeners.add(component);
  }

  /**
   * Compiler-generated method to initialize and add application components to
   * the form.  We just provide an implementation here to artificially make
   * this class concrete so that it is included in the documentation and
   * Codeblocks language definition file generated by
   * {@link com.google.appinventor.components.scripts.DocumentationGenerator} and
   * {@link com.google.appinventor.components.scripts.LangDefXmlGenerator},
   * respectively.  The actual implementation appears in {@code runtime.scm}.
   */
  protected void $define() {    // This must be declared protected because we are called from Screen1 which subclasses
    // us and isn't in our package.
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean canDispatchEvent(Component component, String eventName) {
    // Events can only be dispatched after the screen initialized event has completed.
    boolean canDispatch = taskInitialized || (component == this && eventName.equals("Initialize"));
    Log.e(LOG_TAG, "canDispatch " + canDispatch);
    return canDispatch;
  }

  /**
   * A trivial implementation to artificially make this class concrete so
   * that it is included in the documentation and
   * Codeblocks language definition file generated by
   * {@link com.google.appinventor.components.scripts.DocumentationGenerator} and
   * {@link com.google.appinventor.components.scripts.LangDefXmlGenerator},
   * respectively.  The actual implementation appears in {@code runtime.scm}.
   */
  @Override
  public boolean dispatchEvent(Component component, String componentName, String eventName,
                               Object[] args) {
    throw new UnsupportedOperationException();
  }

  /**
   * Initialize event handler.
   */
  @SimpleEvent(description = "Service starting")
  public void Initialize() {
    EventDispatcher.dispatchEvent(Task.this, "Initialize");
    taskInitialized = true;

    //  Call all apps registered to be notified when Initialize Event is dispatched
    for (OnInitializeListener onInitializeListener : onInitializeListeners) {
      onInitializeListener.onInitialize();
    }
  }

  // Component implementation

  @Override
  public HandlesEventDispatching getDispatchDelegate() {
    return this;
  }

  // ComponentContainer implementation

  @Override
  public Context $context() {
    return this.getApplicationContext();
  }

  @Override
  public Form $form() {
    return null;
  }

  @Override
  public Task $task() {
    return this;
  }

  @Override
  public boolean isContext() {
    return true;
  }

  @Override
  public boolean isForm() {
    return false;
  }

  @Override
  public boolean isTask() {
    return true;
  }

  @Override
  public boolean inForm() {
    return isForm();
  }

  @Override
  public boolean inTask() {
    return isTask();
  }

  // We don' need these but ComponentContainer implements
  // these so, we'll see what to do

  @Override
  public void $add(AndroidViewComponent component) {
  }

  @Override
  public void setChildWidth(AndroidViewComponent component, int width) {
  }

  @Override
  public void setChildHeight(AndroidViewComponent component, int height) {
  }

  @Override
  public int Width() {
    return 0;
  }

  @Override
  public int Height() {
    return 0;
  }

  public static Task getActiveTask() {
    return activeTask;
  }

  /**
   * Specifies the Version Code.
   *
   * @param vCode the version name of the application
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
      defaultValue = "1")
  @SimpleProperty(userVisible = false,
      description = "An integer value which must be incremented each time a new Android "
          +  "Application Package File (APK) is created for the Google Play Store.")
  public void VersionCode(int vCode) {
    // We don't actually need to do anything.
  }

  /**
   * Specifies the Version Name.
   *
   * @param vName the version name of the application
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = "1.0")
  @SimpleProperty(userVisible = false,
      description = "A string which can be changed to allow Google Play "
          + "Store users to distinguish between different versions of the App.")
  public void VersionName(String vName) {
    // We don't actually need to do anything.
  }

  public void deleteComponent(Object component) {
    if (component instanceof OnDestroyListener) {
      OnDestroyListener onDestroyListener = (OnDestroyListener) component;
      if (onDestroyListeners.contains(onDestroyListener)) {
        onDestroyListeners.remove(onDestroyListener);
      }
    }
  }

  // This is used by runtime.scm to call the Initialize of a component.
  public void callInitialize(Object component) throws Throwable {
    Method method;
    try {
      method = component.getClass().getMethod("Initialize", (Class<?>[]) null);
    } catch (SecurityException e) {
      Log.i(LOG_TAG, "Security exception " + e.getMessage());
      return;
    } catch (NoSuchMethodException e) {
      //This is OK.
      return;
    }
    try {
      Log.i(LOG_TAG, "calling Initialize method for Object " + component.toString());
      method.invoke(component, (Object[]) null);
    } catch (InvocationTargetException e){
      Log.i(LOG_TAG, "invoke exception: " + e.getMessage());
      throw e.getTargetException();
    }
  }

}
