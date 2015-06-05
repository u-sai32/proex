package com.google.appinventor.components.runtime;

import com.google.appinventor.components.runtime.util.YailList;

public class ExternalClass {

   public class UselessObject {
     
     public String toString(){
       return "Most Useless Object";
     }
   }
  
  public boolean Negate(boolean input){
    return !input;
  }
  
  public String Reverse(String input){
    String reverse = "";
    int length = input.length();
    for(int i = length -1; i>=0; --i){
      reverse += input.charAt(i);
    }
    return reverse;
  }
  
  public int TwoTimes(int input){
    return 2*input;
  }
  
  public short ThreeTimes(short input){
    return (short) (input*3);
  }
  
  public long Square(long input){
    return input*input;
  }
  
  public float Half(float input){
    return input/2;
  }
  
  public double SquareRoot(double input){
    return Math.sqrt(input);
  }
 
  
  public Object Object(Object input){
    return input;
  }
  
  public Component Component(Component input){
    
    return input;
  }
  
  public YailList YailList(YailList List){
    
    return List;
  }
  
  
}
