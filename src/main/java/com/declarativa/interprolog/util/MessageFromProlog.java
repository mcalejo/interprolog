/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
import java.io.Serializable;

/** Represents a message from Prolog, performed by predicate javaMessage. If a method name starts with "&", the message will be executed in a new Java thread */
public class MessageFromProlog implements Serializable{
	private static final long serialVersionUID = -6262728987237163637L;
	public int timestamp;
	public Object target; // To contain an InvisibleObject if target (de)serialization is not desired
	public String methodName;
	public Object[] arguments;
	public boolean returnArguments;
	public String toString(){
		StringBuffer args = new StringBuffer(500);
		for (int i=0; i<arguments.length; i++)
			args.append("\narguments["+i+"]="+arguments[i]);
		return "MessageFromProlog, timestamp="+timestamp+"\ntarget="+target+
			"\nmethodName="+methodName+args;
	}
	public boolean requiresNewThread() {
		return methodName.startsWith("&");
	}
	/**
	 * @return the real methodName
	 */
	public String getRealMethodName() {
		if (requiresNewThread()) return methodName.substring(1); 
		else return methodName;
	}
}

