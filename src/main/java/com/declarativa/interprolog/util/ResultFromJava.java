/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
import java.io.Serializable;
/** Used to serialize results for javaMessage */
public class ResultFromJava implements Serializable{
	private static final long serialVersionUID = -3166439104814410267L;
	int timestamp; // same as of the corresponding MessageFromProlog
	Object result;
	/** cause of the exception thrown directly by java.lang.reflect.Method#invoke(). 
	It may eventually encapsulate an exception thrown by the invoked method
	@see      java.lang.reflect.Method#invoke(Object,Object[])*/
	public Object /*instead of Exception... to avoid serialization difficulties...*/ exception;
	/** So Prolog may get the new state of the (object) arguments */
	Object[] arguments;
	public ResultFromJava(int t,Object r,Object e,Object[] a){
		timestamp=t; result=r; exception=e; 
		if (a==null) arguments = new Object[0];
		else arguments=a;
	}
	public String toString(){
		StringBuffer args = new StringBuffer(500);
		for (int i=0; i<arguments.length; i++)
			args.append("\narguments["+i+"]="+arguments[i]);
		return "timestamp="+timestamp+"\nresult="+result+"\nexception="+exception+args;
	}
}

