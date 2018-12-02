/* File:   FloraProgramEditor.java
**
** Author(s): Miguel Calejo
**
** Contact:   mc@interprolog.com
**
** Copyright (C) Coherent Knowledge Systems, LLC, 2015 - 2016.
** All rights reserved.
**
*/

package com.declarativa.interprolog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;

import com.declarativa.interprolog.util.GoalFromJava;
import com.declarativa.interprolog.util.GoalToExecute;
import com.declarativa.interprolog.util.IPException;

/**
 * A SubprocessEngine which actually... implements the InterProlog API upside down: Java is a subprocess of xsb.
<pre>
xsb
?- ['....../interprolog.P'], spawn_java('..../interprolog.jar'). % instead of interprolog.jar, a classpath including your classes too
?- ipPrologEngine(E), java(E,R,toString).
% If your Java classes create a GUI with an event thread (and even if it doesn't), before you exit xsb:
?- kill_java.
</pre>
 * @author Miguel Calejo
 * Copyright Coherent Knowledge Systems LLC 2015
 * Released as open source with the rest of InterProlog Java bridge in November 2018,
 * as previously agreed on Oct 5, 2015
 */
public class OnTopSubprocessEngine extends SubprocessEngine {
	boolean constructed = false;
	public OnTopSubprocessEngine(String[] prologCommands,boolean outAndErrMerged, boolean debug, boolean loadFromJar) {
		super(prologCommands, outAndErrMerged, debug, loadFromJar);
	}

	@Override
    protected PrologImplementationPeer makeImplementationPeer(){
    	return new XSBPeer(this);
	}
	
	public void setDebug(boolean d){
		if (constructed) super.setDebug(d);
		else debug=d;
	}
	
	/* No interrupts allowed, would break Prolog's initial javaMessage, not just some sub deterministicGoal
	 * @see com.declarativa.interprolog.AbstractPrologEngine#interrupt()
	 */
	@Override
	public void interrupt() {
		throw new IPException("Interrupting not supported in 'OnTop' engines");
		// However the implementation is in place below
	}

	private void simulateFirstGoal(){
   		GoalFromJava GO = makeDGoalObject("dummyGoal", null, null, null, incGoalTimestamp());
        GoalToExecute goalToDo;
        goalToDo = new GoalToExecute(GO,null /* no relevant thread */);
        goalToDo.setFirstGoalStatus(true);
     
    	scheduleGoal(goalToDo);
		goalToDo.prologWasCalled();
	}
	
	

	/**
	 * @see com.declarativa.interprolog.SubprocessEngine#isAvailable()
	 */
	@Override
	public boolean isAvailable() {
		return !constructed || messagesExecuting.size()!=0;
	}

	protected synchronized boolean canAcceptNewGoal(Thread thread){
		HashSet<Thread> active = new HashSet<Thread>();
		activeThreads(active);
		if (active.size()==0) return false;
		else return super.canAcceptNewGoal(thread);
	}
	
	/** NOTE: InterProlog and XJ files loaded later by Java are assumed bundled in jars
	 * @param args First is XSB dir, second is debug (true/false)
	 */
	public static void main(String[] args) {
		String XSBdir = args[0];
		// int prologPID = Integer.valueOf(args[1]);
		File tmpFile = new File(args[2]);
		File tmpFileFlag = new File(args[3]);
		boolean debug = args[4].equals("true");

		OnTopSubprocessEngine engine = new OnTopSubprocessEngine(new String[]{XSBdir},false,debug,true);
		try{
			engine.initSubprocessSocket();
			String CH = engine.clientHostname(); int P = engine.serverSocket.getLocalPort(); // concentrate exceptions here
			// int interruptPort = engine.prepareInterrupt(engine.clientHostname(),prologPID); 
			int interruptPort = 0; // Java interrupts pointless in our context 
			String nl = System.getProperty("line.separator");
			// Let Prolog know:
			FileOutputStream fos = new FileOutputStream(tmpFile);
			// PrintStream ps = System.out;
			PrintStream ps = new PrintStream(fos);
			ps.print(CH); ps.print(nl);
			ps.print(P); ps.print(nl);
			ps.print(interruptPort); ps.print(nl);
			ps.print(engine.registerJavaObject(engine)); ps.print(nl);
			ps.close(); fos.close();
			
			FileOutputStream fos2 = new FileOutputStream(tmpFileFlag);
			fos2.close();
	
			engine.progressMessage("Waiting for the socket to accept...");
			engine.socket = engine.serverSocket.accept();
			
			// Let's simulate engine.deterministicGoal("dummyGoal") running, thus javaMessage ready
	    	engine.topGoalHasStarted = true;
	    	engine.simulateFirstGoal();
	    	engine.available = true;
	    	engine.setEngineStarted();

	    	//setEngineStarted superfluous, Prolog PID can be received above... no dg during this startup!
	    	
			engine.initSubprocess2();
			if (interruptPort!=0)
				engine.activateWindowsInterrupt(CH, interruptPort,false);
			engine.setThreadedCallbacks(true);
			engine.constructed = true;
		} catch(IOException ex){
			System.err.println("Could not initialize Java"); 
			System.exit(1);
		}
		// This will not exit because of the callback handler thread

	}
	
	public static String testDG(OnTopSubprocessEngine engine,String goal,String resultVar){
		return engine.deterministicGoal(goal, "["+resultVar+"]")[0].toString();
	}

}
