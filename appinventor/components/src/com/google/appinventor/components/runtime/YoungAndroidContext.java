// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2016 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import android.content.Context;

public interface YoungAndroidContext {
  /**
   * Returns the application context .
   *
   * @return  activity context
   */
  Context $context();

  /**
   * Returns the form iff this is an Activity.
   *
   * @return  form
   */
  Form $form();

  /**
   * Returns the task iff this is a Service.
   *
   * @return  task
   */
  Task $task();

  /**
   * @return  Returns true only if the context is a form.
   */
  boolean isForm();

  /**
   * @return  Returns true only if the context is a task.
   */
  boolean isTask();




}
