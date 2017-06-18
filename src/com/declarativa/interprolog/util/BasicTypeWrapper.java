/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/

package com.declarativa.interprolog.util;
import java.io.*;
/** A class used to serialize basic type values (int, boolean,...). 
Contains a basic type wrapper object which at the proper time will provide the basic type value.
Useful to use Java methods whose arguments include basic types. */
public class BasicTypeWrapper implements Serializable{
	private static final long serialVersionUID = -5739953728564068920L;
	public Object wrapper; // a (serializable...) basic type wrapper
	public Class<?> basicTypeClass(){
		if (wrapper instanceof Boolean) return Boolean.TYPE;
		if (wrapper instanceof Character) return Character.TYPE;
		if (wrapper instanceof Byte) return Byte.TYPE;
		if (wrapper instanceof Double) return Double.TYPE;
		if (wrapper instanceof Float) return Float.TYPE;
		if (wrapper instanceof Integer) return Integer.TYPE;
		if (wrapper instanceof Long) return Long.TYPE;
		if (wrapper instanceof Short) return Short.TYPE;
		throw new RuntimeException("Bad BasicTypeWrapper:"+wrapper);
	}
	public BasicTypeWrapper(Object w){
		wrapper=w;
	}
	public String toString(){return "BasicTypeWrapper:"+wrapper.toString();}
	
	public static boolean instanceOfWrapper(Object x){
		if (x instanceof Boolean) return true;
		else if (x instanceof Character) return true;
		else if (x instanceof Byte) return true;
		else if (x instanceof Double) return true;
		else if (x instanceof Float) return true;
		else if (x instanceof Integer) return true;
		else if (x instanceof Long) return true;
		else if (x instanceof Short) return true;
		else return false;
	}
	public Object getObject(){
		return wrapper;
	}
	/** This is useful for test assertions */
	public boolean equals(Object x){
		if (x instanceof BasicTypeWrapper) 
			return ((BasicTypeWrapper)x).wrapper.equals(wrapper);
		else return false;
	}
}
