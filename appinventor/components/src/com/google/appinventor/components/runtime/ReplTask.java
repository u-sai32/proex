// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2016 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;

import com.google.appinventor.components.runtime.util.AppInvHTTPD;
import com.google.appinventor.components.runtime.util.RetValManager;
import com.google.appinventor.components.runtime.util.SdkLevel;
import com.google.appinventor.components.runtime.util.EclairUtil;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Activity;
import android.content.Context;


public class ReplTask extends Task {

  private AppInvHTTPD httpdServer = null;
  public static ReplTask task;
  private static final String REPL_ASSET_DIR = "/sdcard/AppInventor/assets/";
  private boolean assetsLoaded = false;
  private boolean IsUSBRepl = false;
  private boolean isDirect = false; // True for USB and emulator (AI2)

  public ReplTask() {
    super();
    Log.i("ReplTask", "constructorrrrrrr");
    task = this;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d("ReplTask", "onCreate");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (httpdServer != null) {
      httpdServer.stop();
      httpdServer = null;
    }
    stopSelf();                  // Must really exit here, so if you hits the back button we terminate completely.
    System.exit(0);
  }

  protected void processExtras(Intent intent, boolean restart) {
    Bundle extras = intent.getExtras();
    if (extras != null) {
      Log.d("ReplTask", "extras: " + extras);
      Iterator<String> keys = extras.keySet().iterator();
      while (keys.hasNext()) {
        Log.d("ReplTask", "Extra Key: " + keys.next());
      }
    }
    if ((extras != null) && extras.getBoolean("rundirect")) {
      Log.d("ReplTask", "processExtras rundirect is true and restart is " + restart);
      isDirect = true;
      if (restart) {
        if (httpdServer != null) {
          httpdServer.resetSeq();
        } else {                // User manually started the Companion on her phone
          startHTTPD(true);     // but never typed in the UI and then connected via
          httpdServer.setHmacKey("emulator"); // USB. This is an ugly hack
        }
      }
    }
  }

  public boolean isDirect() {
    return isDirect;
  }

  public void setIsUSBrepl() {
    IsUSBRepl = true;
  }

  // Called from the Phone Status Block to start the Repl HTTPD
  public void startHTTPD(boolean secure) {
    try {
      if (httpdServer == null) {
        httpdServer = new AppInvHTTPD(8001, new File(REPL_ASSET_DIR), secure, null); // Probably should make the port variable
        Log.i("ReplForm", "started AppInvHTTPD");
      }
    } catch (IOException ex) {
      Log.e("ReplForm", "Setting up NanoHTTPD: " + ex.toString());
    }
  }

  // We return true if the assets for the Companion have been loaded and
  // displayed so we should look for all future assets in the sdcard which
  // is where assets are placed for the companion.
  // We return false until setAssetsLoaded is called which is done
  // by the phone status block
  public boolean isAssetsLoaded() {
    return assetsLoaded;
  }

  public void setAssetsLoaded() {
    assetsLoaded = true;
  }

}