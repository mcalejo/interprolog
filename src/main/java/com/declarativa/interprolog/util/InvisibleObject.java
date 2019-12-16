/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
import java.io.Serializable;

/** Instances represent objects which are not serialized to/from Prolog, 
	and that are kept in a table by a Prolog engine */
public class InvisibleObject implements Serializable{
	private static final long serialVersionUID = 2583398726949783086L;
	int ID;
	public InvisibleObject(int ID){this.ID=ID;}
	public String toString(){return "InvisibleObject:ID="+ID;}
}

