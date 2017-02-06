// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2016 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;

import android.os.Looper;
import android.os.Message;
import com.google.appinventor.components.runtime.util.AppInvHTTPD;
import com.google.appinventor.components.runtime.util.RetValManager;
import com.google.appinventor.components.runtime.util.SdkLevel;
import com.google.appinventor.components.runtime.util.EclairUtil;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.content.Context;


public class ReplTask extends Task {

  protected static Task replTask = null;
  private static final String LOG_TAG = "ReplTask";
  private boolean assetsLoaded = true;
  private HashMap<String, TaskThread> TaskThreads = new HashMap<String, TaskThread>();

  final protected class ReplTaskHandler extends Task.TaskHandler {

    protected ReplTaskHandler(Looper looper) {
      super(looper);
    }

    @Override
    public void handleMessage(Message msg) {


    }
  }


  public ReplTask() {
    super();
    Log.i("ReplTask", "Started");
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d("ReplTask", "Task onCreate");
    replTask = this;
    getReplTask();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    stopSelf();                  // Must really exit here, so if you hits the back button we terminate completely.
    System.exit(0);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d("ReplTask", "ReplTask onStartCommand Called");
    replTask = this;
    Log.d("ReplTask", "set replTask=");
    String startValue = intent.getStringExtra(Form.SERVICE_ARG);
    String taskName = intent.getStringExtra(Form.SERVICE_NAME);
    if (taskName == null) { // This is ReplTask itself Starting
      return START_STICKY;
    }
    Object decodedStartVal = Form.decodeJSONStringForForm(startValue, "get start value");
    TaskStarted(decodedStartVal);
    Log.d("Task", "Done Dispatch but not Returned");
    return START_STICKY;
  }


  public void runTaskCode(String taskName, Runnable runnable) {
    Log.d("ReplTask", "Got executed. Thank God");
    TaskThread taskThread = this.TaskThreads.get(taskName);
    if (taskThread == null) {
      taskThread = new TaskThread(taskName, this);
      this.TaskThreads.put(taskName, taskThread);
    }
    TaskHandler taskHandler = taskThread.getTaskHandler();
    taskHandler.post(runnable);
  }


  public boolean isAssetsLoaded() {
    return assetsLoaded;
  }


  public static Task getReplTask() {
    if (replTask == null) {
      Log.d(LOG_TAG, "replTask is null yet");
    }
    else Log.d(LOG_TAG, "replTask is VALID yet");
    return replTask;
  }

  @Override
  public String getDispatchContext() {
    return Thread.currentThread().getName();
  }


}