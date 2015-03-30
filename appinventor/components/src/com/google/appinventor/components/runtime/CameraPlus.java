package com.google.appinventor.components.runtime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.ErrorMessages;

/**
 * @author quixotic
 * 
 * Camera Plus allows to use the hardware Camera directly.
 * 
 */
@DesignerComponent(version = YaVersion.CAMERA_PLUS_COMPONENT_VERSION,
description = "<p>Use directly the hardware camera to take a picture.</p>",
category = ComponentCategory.MEDIA,
nonVisible = true,
iconName = "images/camera.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.CAMERA,android.hardware.CAMERA,android.permission.WRITE_EXTERNAL_STORAGE")
public class CameraPlus extends AndroidNonvisibleComponent implements Component, SurfaceHolder.Callback {
  
  private final ComponentContainer container;
  private SurfaceView liveFeedView; // This should be used more effectively in the future, when someday Canvas grows.. 
  //private SurfaceTexture liveFeedTexture; // This is for redirecting Camera Preview to a non View 
  private Uri imageFile;
  private ContentResolver mContentResolver;
  private static int mCameraId;
  private static Camera mCamera;
  
 //whether to open into the front-facing camera
  private boolean useFront;
  

  
  public CameraPlus(ComponentContainer container) {
    super(container.$form());
    this.container = container;
    mContentResolver = form.getContentResolver();
    if(Build.VERSION.SDK_INT<11){
      if(liveFeedView==null) liveFeedView=new SurfaceView(form.$context());
      liveFeedView.getHolder().addCallback(this);
    }
    else {
     // liveFeedTexture = new SurfaceTexture(10);
    }
    // Default property values
    UseFront(false);
  }
  
  /**
   * Returns true if the front-facing camera is to be used (when available)
   *
   * @return {@code true} indicates front-facing is to be used, {@code false} will open default
   */
  @SimpleProperty()
  public boolean UseFront() {
    return useFront;
  }

  /**
   * Specifies whether the front-facing camera should be used (when available)
   *
   * @param front
   *          {@code true} for front-facing camera, {@code false} for default
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
  @SimpleProperty(description = "Specifies whether the front-facing camera should be used (when available). "
    + "If the device does not have a front-facing camera, this option will be ignored "
    + "and the camera will open normally.")
  public void UseFront(boolean front) {
    useFront = front;
  }

 
  
 
  
  
    /**
     * Takes a picture, then raises the AfterPicture event.
     * If useFront is true, adds an extra to the intent that requests the front-facing camera.
     */
    @SimpleFunction
  public void TakePicture() {
    // TODO Auto-generated method stub
    if(!checkCameraHardware(form.$context())){
      form.dispatchErrorOccurredEvent(this, "TakePicture",
          ErrorMessages.ERROR_CAMERA_NOT_FOUND); // New Error Message Defined in ErrorMessages.java
      return;
    }
    Date date = new Date();
    String state = Environment.getExternalStorageState();

    if (Environment.MEDIA_MOUNTED.equals(state)) {
      Log.i("CameraComponent", "External storage is available and writable");

      imageFile = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
        "/Pictures/app_inventor_" + date.getTime()
        + ".jpg"));

           
      if(mCamera!=null) {mCamera.stopPreview();mCamera.release();mCamera=null;}
      mCameraId=getCameraId();
      Toast.makeText(form.$context() , "Camera Id "+mCameraId, Toast.LENGTH_SHORT).show();
      
      mCamera = getCameraInstance();
      if(mCamera==null)return;
      try {
        if(Build.VERSION.SDK_INT<11){
          mCamera.setPreviewDisplay(liveFeedView.getHolder());
        }
        else{
         // mCamera.setPreviewTexture(liveFeedTexture);
          mCamera.setPreviewTexture(new SurfaceTexture(10));
        }
      } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
      }
      mCamera.startPreview();
      Toast.makeText(form.$context() , "Camera Preview Start ", Toast.LENGTH_SHORT).show();
      mCamera.takePicture(null,null,jpegCallback);
      Toast.makeText(form.$context() , "Camera Reserved  Start ", Toast.LENGTH_SHORT).show();
      
      //container.$context().startActivityForResult(intent, requestCode);
    } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
      form.dispatchErrorOccurredEvent(this, "TakePicture",
          ErrorMessages.ERROR_MEDIA_EXTERNAL_STORAGE_READONLY);
    } else {
      form.dispatchErrorOccurredEvent(this, "TakePicture",
          ErrorMessages.ERROR_MEDIA_EXTERNAL_STORAGE_NOT_AVAILABLE);
    }
  }

  

  /**
   * Indicates that a photo was taken with the camera and provides the path to
   * the stored picture.
   */
  @SimpleEvent
  public void AfterPicture(String image) {
    EventDispatcher.dispatchEvent(this, "AfterPicture", image);
  }
  
  private static Camera getCameraInstance(){
      Camera cam = null;
      try {
        if(mCameraId!=-1)
          cam = Camera.open(mCameraId); 
        else cam = Camera.open(); 
      }
      catch (Exception e){
        // do something here 
        cam=null;
      }
      return cam;
  }
  
  private boolean checkCameraHardware(Context context) {
    if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
        // this device has a camera
        return true;
    } else {
        // no camera on this device
        return false;
    }
  }
  
  private int getCameraId(){
    int cameraId=-1;
    int cameraCount= Camera.getNumberOfCameras();
    Toast.makeText(form.$context() , "No Cameras " + cameraCount, Toast.LENGTH_SHORT).show();
    CameraInfo cameraInfo = new CameraInfo();
    for(int cId=0; cId<cameraCount; cId++){
      Camera.getCameraInfo(cId, cameraInfo);
      if(useFront && cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT){
        cameraId=cId;
      }
      if(!useFront && cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK){
        cameraId=cId;
      }
    }
    return cameraId;
  }
  
  PictureCallback jpegCallback = new PictureCallback() {
    
    public void onPictureTaken(byte[] data, Camera camera) 
    {
        FileOutputStream outputStream = null;
        try {
          outputStream = new FileOutputStream(imageFile.getPath());
          outputStream.write(data);
          outputStream.close();
          AfterPicture(imageFile.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally 
        {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Toast.makeText(form.$context(), "Camera Done",Toast.LENGTH_LONG).show();
        }
        
    }

    };



  @Override
  public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
    // TODO Auto-generated method stub
    Toast.makeText(form.$context(), "SH Changed  : ",Toast.LENGTH_LONG).show();
  }

  @Override
  public void surfaceCreated(SurfaceHolder arg0) {
    // TODO Auto-generated method stub
    Toast.makeText(form.$context(), "SH Created  : ",Toast.LENGTH_LONG).show();
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder arg0) {
    // TODO Auto-generated method stub
    
    Toast.makeText(form.$context(), "SH Destroyed  : ",Toast.LENGTH_LONG).show();
    
  }
  
}
