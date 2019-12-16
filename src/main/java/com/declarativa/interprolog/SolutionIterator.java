/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/

package com.declarativa.interprolog;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.declarativa.interprolog.util.IPException;
import com.declarativa.interprolog.util.IPPrologError;

/** An Iterator object that actually evaluates a goal and collects its solutions incrementally. Performance could probably be improved 
by using wait/notify rather than plain Thread.yield() */
public class SolutionIterator implements Iterator<Object[]>{
	AbstractPrologEngine engine;
	String G; String OVar; Object[] objectsP; String RVars;
	Thread prologExecution = null;
	boolean cancellationRequested = false;
	boolean cancelled = false;
	boolean foundAllSolutions = false;
	Object[] lastSolution = null;
	Object lastError = null;
	boolean lastUndefined = false;
	
	SolutionIterator(AbstractPrologEngine engine,String G, String OVar, Object[] objectsP, String RVars){
		this.engine = engine;
		this.G=G; this.OVar=OVar; this.objectsP=objectsP; this.RVars=RVars;
	}
	public boolean hasNext(){
		if (cancellationRequested) throw new NoSuchElementException("Cancelled any remaining solutions of "+G);
		execute();
		while (lastSolution==null && lastError==null && !foundAllSolutions)
			Thread.yield(); //N3 or N1
		if (foundAllSolutions) engine.nonDeterministicGoalActive = false;
		return lastSolution!=null || lastError!=null;
	}
	/** Makes sure the Prolog goal evaluation has started */
	private void execute(){
		if (prologExecution==null) {
			prologExecution = new Thread("Multiple solutions thread"){
				public void run(){
					String GG =  "catch(call_tv(("+G+"),_IP_U),_IP_PE, handle_ip_exception(_IP_PE,_IP_Error2)) "+
						",(var(_IP_Error2)->_IP_Error2=null,_IP_NewRVars="+RVars+";_IP_NewRVars=[])";
					try{
						String goal = GG+", ipObjectSpec('ArrayOfObject',_IP_Bindings,[_IP_NewRVars],_), "+
						"checkUndefined(_IP_U,_IP_Uint), ipObjectSpec('boolean',_IP_U_model,[_IP_Uint],_)," +
						"javaMessage("+engine.registerJavaObject(SolutionIterator.this)+",_IP_R,recordSolution(_IP_Bindings,_IP_Error2,_IP_U_model)), " +
						"ipObjectSpec('java.lang.Boolean',_IP_R,[0],_) "; // 0/false means cancel the goal execution
						lastSolution = null; lastError = null;
						if (engine.deterministicGoal(goal, OVar,objectsP))
							cancelled = true;
						else 
							foundAllSolutions = true;
						// prologExecution.notifyAll(); // N1
						engine.nonDeterministicGoalActive = false;
						
					} catch (Throwable T){
						throw new IPException("Problem in non deterministic goal:"+T);
					}
				}
			}; 
			prologExecution.setName("Nondeterministic goal handler");
			prologExecution.start();
		}
	}
	/** This will messaged by Prolog. Returns false to it if we wish to cancel */
	public boolean recordSolution(Object[] bindings, Object error, boolean undefined){
		lastSolution = bindings; lastUndefined=undefined;
		if (error!=null) lastError = error;
		// prologExecution.notifyAll(); // N3
		while((lastSolution!=null || lastError!=null) && !cancellationRequested)
			Thread.yield(); // N2 or N4
		return !cancellationRequested;
	}
	public Object[] next(){
		if (cancellationRequested) throw new NoSuchElementException("Cancelled any remaining solutions of "+G);
		execute();
		while(lastSolution==null && lastError==null && !foundAllSolutions)
			Thread.yield();	// N3 or N1	
		if (lastError!=null){
			Object temp = lastError;
			lastError = null; lastSolution=null;
			throw new IPPrologError(temp);
		}
		if (lastSolution==null) throw new NoSuchElementException("No more solutions for "+G);
		Object[] temp = lastSolution;
		lastSolution = null; lastError = null;
		// prologExecution.notifyAll(); // N2
		return temp; 
	}
	
	public boolean lastSolutionUndefined(){
		return lastUndefined;
	}

	public void remove(){
		throw new UnsupportedOperationException("Can't remove a solution from its iterator");
	}
	/** terminates the non deterministic goal that returned this iterator, without finding more solutions */
	public void cancel(){
		cancellationRequested = true;
		//prologExecution.notifyAll(); // N4
		while (!cancelled&&!foundAllSolutions)
			Thread.yield();	// N1
		engine.nonDeterministicGoalActive = false;
		lastSolution=null; lastError=null;
			/*
			try{ prologExecution.yield();	// N1
			} catch (InterruptedException e) {throw new IPException("Bad interrupt");} */
	}
}