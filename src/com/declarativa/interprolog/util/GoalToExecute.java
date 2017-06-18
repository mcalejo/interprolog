/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;

/** A goal scheduled to execute in Prolog whenever possible */
public class GoalToExecute{
	private GoalFromJava goal;
	private ResultFromProlog result;
	private boolean executing;
	private boolean ended;
	private boolean firstGoalStatus = false;
	// something to do in the creator thread
	private Runnable todo=null;
	private Thread callerThread;
	
	public GoalToExecute(GoalFromJava goal){
		this(goal,Thread.currentThread());
	}
	
	public GoalToExecute(GoalFromJava goal,Thread t){
		this.goal = goal; 
		result=null; ended=false; executing=false;
		callerThread = t;
	}
	
	public Thread getCallerThread(){
		return callerThread;
	}
	
	public synchronized void executeInThread(Runnable r){
		if(todo!=null||!executing/* comented because of SubprocessEngine:||firstGoalStatus*/||result!=null || hasEnded()){
			System.err.println("bad");
			System.err.println("r=="+r);
			System.err.println("todo=="+todo);
			System.err.println("executing=="+executing);
			System.err.println("firstGoalStatus=="+firstGoalStatus);
			System.err.println("result=="+result);
			System.err.println("hasEnded()=="+hasEnded());
			throw new IPException("bad execute");
		}
		todo=r;
		notifyAll();
	}
	
	/** Obtain result for a Prolog goal, blocking until it is available; meanwhile it will execute
	Runnables if so requested */
	public synchronized ResultFromProlog waitForResult(){
		if (ended) return result;
		while(true){
			if(!ended && todo!=null) {
				todo.run();
				todo=null;
				//notifyAll();
			}
			if(result!=null) break;
			try { wait();}
			catch(InterruptedException e){throw new IPException("Unexpected:"+e);}
			// System.out.println("waitForResult loop: result=="+result+",todo=="+todo);
			if (result==null && todo==null) throw new IPException("Inconsistency in GoalToExecute");
		}
		return result; 
	}
	
	public synchronized void setResult(ResultFromProlog result){
		/* This now is called legitimately when dealing with "meta syntax" errors in firstGoal
		if (this.result!=null || hasEnded() || todo!=null) {
			throw new IPException("Inconsistency in GoalToExecute");
		}*/
		this.result=result;
		ended=true;
		notifyAll();
	}
	
	public boolean wasInterrupted(){
		return hasEnded() && "interrupted".equals(result.error);
	}

	public boolean wasAborted(){
		return hasEnded() && "aborted".equals(result.error);
	}

	/** Used on the InterProlog Java side to "cascade" an interrupt over pending goals to execute */
	public synchronized void interrupt(){
		raiseError("interrupted");
	}
	
	/** Used on the InterProlog Java side to "cascade" an abort over pending goals to execute */
	public synchronized void abort(){
		raiseError("aborted");
	}
	
	private void raiseError(String s){
		if (result==null) result = new ResultFromProlog(-1,false,0,s,false);
		ended=true;
		notifyAll();
	}
	
	public GoalFromJava getGoal(){ return goal;}
	
	public void prologWasCalled(){
		if (executing) throw new IPException("Bad use of prologWasCalled");
		executing=true;
	}
	
	public boolean hasStarted(){
		return executing;
	}
	
	public boolean hasEnded(){
		return ended;
	}
	
	public int getTimestamp(){return goal.timestamp;}
	
	public void setFirstGoalStatus(boolean b){
		firstGoalStatus = b;
	}
	
	public boolean isFirstGoal(){
		return firstGoalStatus;
	}
	
	public String toString(){
		return "GoalToExecute ("+firstGoalStatus+","+executing+","+(result!=null)+"), called by "+callerThread+": timestamp=="+getTimestamp()+", goal=="+goal;
	}
}