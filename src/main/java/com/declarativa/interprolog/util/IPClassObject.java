/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
import java.io.*;
/** Represents a Java class object; avoids the need to use a full blown object specifier for the real
Java class object (a Class instance) on the Prolog side, by relying on the Java call-back mechanism to interpret the contents
of this object at callback time */
public class IPClassObject implements Serializable{
	private static final long serialVersionUID = -1740667619486084399L;
	public String classname;
	public IPClassObject(String s){
		if (s==null) throw new Error("null classname in IPClassObject");
		classname=s;
	}
	public String toString(){return "IPClassObject:"+classname;}
}

