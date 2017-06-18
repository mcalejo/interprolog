/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
import java.io.Serializable;
/** Represents a Java class variable; allow the Prolog side to conveniently refer any class variable */
public class IPClassVariable implements Serializable{
	private static final long serialVersionUID = 7467632958522967351L;
	public String className;
	public String variableName;
	public IPClassVariable(String c,String v){
		if (c==null || v==null) throw new Error("null argument in IPClassVariable");
		className=c; variableName=v;
	}
	public String toString(){return "IPClassVariable:"+className+"."+variableName;}
}

