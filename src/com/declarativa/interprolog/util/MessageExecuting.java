/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
import com.declarativa.interprolog.AbstractPrologEngine;

/** Object to help coordinate a javaMessage execution in the Java side. */
public class MessageExecuting implements Runnable{
	AbstractPrologEngine engine;
	private MessageFromProlog m;
	private ResultFromJava result;
	private boolean ended;
	/** Estimated thread */
	private Thread callingThread;
	/** Thread actually executing this work */
	private Thread executor;
	
	public MessageExecuting(MessageFromProlog m, AbstractPrologEngine engine, Thread calling){
		this.m = m; 
		result=null;
		this.engine=engine;
		ended=false;
		callingThread = calling;
	}
	
	public Thread getCallingThread(){
		return callingThread;
	}
	
	public Thread getExecutor(){
		return executor;
	}
	
	public synchronized void setResult(ResultFromJava result){
		if (this.result!=null) // throw new IPException("Inconsistency in MessageExecuting");
			return;
		this.result=result;
		ended = true;
	}
	
	public void run(){
		if (!ended){
			executor = Thread.currentThread();
			setResult(engine.doCallback(m));
		}
	}
	public boolean hasEnded(){
		return ended;
	}
	public ResultFromJava getResult(){
		if (!hasEnded()) throw new IPException("bad use of MessageExecuting");
		return result;
	}
	public int getTimestamp(){
		return m.timestamp;
	}
	
	public String toString(){
		return "MessageExecuting ("+ended+"):"+m.methodName;
	}
	public MessageFromProlog getM(){
		return m;
	}
}