/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
import java.io.Serializable;

import com.declarativa.interprolog.AbstractPrologEngine;

/** Used to serialize results for deterministicGoal */
public class ResultFromProlog implements Serializable{
	private static final long serialVersionUID = 8121126564890923383L;
	/** Same as passed in GoalFromJava*/
	public int timestamp;
	/** Goal has succeeded */
	public boolean succeeded;
	/** Object array corresponding to the result variable list */
	public Object[] rVars;
	/** true if the result is logically undefined (third value) */
	public boolean undefined;
	/** Error message, null if none; used to be a String, now can be anything to cater for Prolog exceptions */
	public Object error;
	//public String error;
	public ResultFromProlog(int timestamp,boolean succeeded,int size,Object error,boolean undefined){
		rVars = new Object[size];
		this.timestamp=timestamp; this.succeeded=succeeded; this.error=error; this.undefined=undefined;
	}
	public String toString(){
		return "ResultFromProlog: timestamp=="+timestamp+", error=="+error+" ,undefined=="+undefined;
	}
	/** Prolog complaining about being interrupted. 
	The engine parameter is necessary as the interrupt detection may depend on Prolog implementation or version*/
	public boolean wasInterrupted(AbstractPrologEngine engine){
		// return  error!=null && ("_$abort_ball".equals(error.toString()) || "interprolog_interrupt".equals(error.toString()));
		return  error!=null && engine.getImplementationPeer().isInterrupt(error);		
	}
}

