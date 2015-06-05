package com.google.appinventor.components.runtime;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.YailList;

import dalvik.system.DexClassLoader;


@DesignerComponent(version = YaVersion.EXTRA_COMPONENT_VERSION,
    description = "The Extra component that contains Extra Method"
        + "and LoadDynamic Method. LoadDynamic which loads a dex file. Extra Method calls "
        + "the function of the loaded dex file and is available in the Blocks Editor",
    category = ComponentCategory.EXTERNAL,
    nonVisible = true,
    iconName = "images/externalComponent.png")
@UsesPermissions(permissionNames = "android.permission.READ_EXTERNAL_STORAGE")
@SimpleObject
public class ExtraComponent extends AndroidNonvisibleComponent {


  
  private String classToLoad = "com.google.appinventor.components.runtime.ExternalClass";
  private String methodToCall = "";
  private Class<?> extClass = null;
  private Object extObject = null;
  private List<Method> extMethods = null;
  private Method extMethod = null;
  
  public ExtraComponent(ComponentContainer container) {
    super(container.$form());
    
  }
  
  
  // The properties do nothing for now.. 
  
  @SimpleProperty(description = "Full Identifier Name of the Class to Load")
  public String Class(){
    return classToLoad;
  }
  
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
  @SimpleProperty
  public void Class(String classname){
    classToLoad = classname;
  }
  
  @SimpleProperty(description = "Name of the Method to Call")
  public String Method(){
    return methodToCall;
  }
  
  @DesignerProperty ( editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
  @SimpleProperty
  public void Method(String methodname){
    methodToCall = methodname;
  }
  
  @SimpleFunction
  public String Extra(String method, String strinp){
    Log.d("AI2",method+"("+strinp+")");
    Method tobeCalled = getMethod(method);
    if(tobeCalled == null)  return "Method Not Found";
    Object result = null;
    try{
      // The code written below is very inconvenient, since we have to handle the parameter type 
      // TODO Blockly needs to evolve .. so that we can generate blocks with correct types at runtime
      //  Currently we just accept all input as String.. 
      if(method.equals("Negate"))
        result = tobeCalled.invoke(extObject, Boolean.parseBoolean(strinp));
      else if(method.equals("Reverse"))
        result = tobeCalled.invoke(extObject, strinp);
      else if(method.equals("TwoTimes"))
        result = tobeCalled.invoke(extObject, Integer.parseInt(strinp));
      else if(method.equals("ThreeTimes"))
        result = tobeCalled.invoke(extObject, Short.parseShort(strinp));
      else if(method.equals("Square"))
        result = tobeCalled.invoke(extObject, Long.parseLong(strinp));
      else if(method.equals("Half"))
        result = tobeCalled.invoke(extObject, Float.parseFloat(strinp));
      else if(method.equals("SquareRoot"))
        result = tobeCalled.invoke(extObject, Double.parseDouble(strinp));
      else if(method.equals("Object"))
        result = tobeCalled.invoke(extObject, this);
      else if(method.equals("Component"))
        result = tobeCalled.invoke(extObject, this);
      else if(method.equals("YailList"))
        result = tobeCalled.invoke(extObject, YailList.makeList(new String[]{"Abc","Def"}));
      else {
        List<Object> params = new ArrayList<Object>(); 
        for(Class<?> cls : tobeCalled.getParameterTypes()){
          params.add(cls.newInstance());
        }
        
        result = tobeCalled.invoke(extObject, params.toArray() );
      }
        
    } catch (IllegalAccessException e) {
      Log.d("AI2",e.getMessage());
    } catch (IllegalArgumentException e) {
      Log.d("AI2",e.getMessage());
    } catch (InvocationTargetException e) {
      Log.d("AI2",e.getMessage());
    } catch (InstantiationException e){
      Log.d("AI2",e.getMessage());
    }
    return result!=null? result.toString() : "Invalid Method";
  }
  
  @SimpleFunction
  public YailList LoadDynamic(String file, String classtoload){
    String sresult = "";
    String lpmethods = "";
    YailList ylresult;;// = YailList.makeEmptyList();
    if(extClass != null){
      extClass = null;
      extObject = null;
      extMethods = null;
      ylresult = YailList.makeList(new String[] {sresult, lpmethods});//add(" ");
      return ylresult;
    }
    try{
      // First Set this location to where our External Components could be saved 
      File dexInput = new File( Environment.getExternalStorageDirectory().getAbsolutePath() , file );
      
      Log.d("AI2", "dexInput = " + dexInput.getAbsolutePath());
      // Now set the dexOutput path 
      File dexOutput = form.$context().getDir("externComps", form.$context().MODE_PRIVATE); 
      // we chose this method instead of context.getCacheDir(), to avoid chances of running  into problems with 
      // low storage devices. 
      DexClassLoader dexCloader = new DexClassLoader(dexInput.getAbsolutePath(), dexOutput.getAbsolutePath(), null, form.$context().getClassLoader()); 
      extClass = dexCloader.loadClass(classtoload);
      extObject = extClass.newInstance();
      extMethods = getPublicDeclaredMethods(Arrays.asList(extClass.getDeclaredMethods())); //in the future maybe  .. ?
      //extMethod = extClass.getMethod(methodToCall, double.class);
      
      List<String> extMethNames = new ArrayList<String>();
      for(Method meth : extMethods){
        extMethNames.add(meth.getName());
      }
      lpmethods = TextUtils.join(",",extMethNames);
      
    }
    catch (Exception e){
      // bad to catch generic expressions. 
      Log.d("AI2",e.getMessage());
      sresult =  "Failed to Load :" + file;
    }
    
    sresult =  "Loaded : " + file ;
    ylresult = YailList.makeList(new String[] {sresult, lpmethods});
    return ylresult;
  }
  
  
  
  protected Method getMethod(String methodName) {//, Class<?>... parameterTypes){
    for(Method meth : extMethods){
      if(meth.getName().equals(methodName)) { //... && meth.getParameterTypes() == parameterTypes){
        return meth;                           
     
      }
    }
    return null;
  }
  
  protected List<Method> getPublicDeclaredMethods(List<Method> list){
    List<Method> methods = new ArrayList<Method>();
    for(Method meth : list){
      if(Modifier.isPublic(meth.getModifiers())){
        methods.add(meth);
      }
    }
    return methods;
  }

}
