/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;
/** If you're interested in an engine's overall activity, implement this interface on your class and make your 
object a listener to a PrologEngine. 
*/
public interface PrologEngineListener {
	/** Prolog is asking the Java layer permission to continue; return non null (a reason/message) if you want to abort the Prolog computation.
	During execution of this method you can call Prolog goals, say to introspect system state etc. 
	This requires 
	 * @param source the Prolog engine that is asking permission
	 * @return null if Prolog can proceed, an error message if it should abort
	@see com.declarativa.interprolog.AbstractPrologEngine#setTimedCallIntervall(int) */
	public String willWork(AbstractPrologEngine source);
	
	/** The Java side is starting processing a javaMessage Prolog goal (callback) request 
	 * @param source the engine being messaged by Prolog */
	public void javaMessaged(AbstractPrologEngine source);
	
	/** The value of isAvailable() has changed. Use source.isAvailable() to get the value. 
	This is currently fired only for SubprocessEngines 
	 * @param source the engine */
	public void availabilityChanged(AbstractPrologEngine source);
}
