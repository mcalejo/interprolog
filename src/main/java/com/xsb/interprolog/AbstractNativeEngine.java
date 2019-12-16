/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com, http://www.xsb.com
** Copyright (C) XSB Inc., USA, 2001-2005
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.xsb.interprolog;

import com.declarativa.interprolog.*;
import com.declarativa.interprolog.util.*;

import java.io.*;

/** A PrologEngine implemented using the Java Native Interface. This class (which may become XSB-agnostic sometime, but not now)
depends on interprolog_callback.c and other files, 
that are included in the emu directory of XSB Prolog 2.5 and later */
public abstract class AbstractNativeEngine extends AbstractPrologEngine{
    /** If true, all JNI calls (including the Prolog callback predicate/function) run in the same thread.
    If false, the initialization JNI calls run in the constructor thread, others in a separate thread.
    Was used in past experimentation. */
    public static final boolean JNIinSameThread = true;
    
    private ByteArrayOutputStream bao = new ByteArrayOutputStream();

    public AbstractNativeEngine(String XSB_BIN_DIR, boolean debug, boolean loadFromJar){
    	super(XSB_BIN_DIR,debug, loadFromJar);
    	nl=System.getProperty("line.separator") ;
    }
        
    /** Accepts a serialized object in the argument and returns another; handleCallback does the actual work*/
    protected byte[] callback(byte[] in) {
    	progressMessage("entering callback(byte[])");
    	profilingMessage("entering callback(byte[])");
    	byte[] out;
    	Object x;
    	try{
	        ByteArrayInputStream bai = new ByteArrayInputStream(in);
			ObjectInputStream ios = new ObjectInputStream(bai);
			x = ios.readObject();
			ios.close();
		} catch (ClassNotFoundException e){
			x = e;
		} catch (IOException e){
			throw new IPException("Bad exception before callback handling:"+e);
		}
		Object y = handleCallback(x);
		GoalToExecute GTE = null;
		if (y instanceof GoalToExecute){
			GTE = (GoalToExecute)y;
			y = ((GoalToExecute)y).getGoal(); // a bit of a hack, for coherence with setupCallbackServer() in SubprocessEngine
		} else if (y instanceof MessageExecuting){
			y = ((MessageExecuting)y).getResult();
			forgetMessage((MessageExecuting)y); // this should actually be done after this method returns?? doesn't seem a problem with JNI (but this sounds wishful thinking)
		}
		try {
			synchronized(bao){
				bao.reset();
    			ObjectOutputStream oos = new ObjectOutputStream(bao);
				oos.writeObject(y); oos.flush();
				out = bao.toByteArray();
				if (GTE!=null)
					GTE.prologWasCalled();
			}
		} catch (IOException e){
			throw new IPException("Bad exception after callback handling:"+e);
		}
    	profilingMessage("leaving callback(byte[])");
		return out;
    }
 	public Object[] deterministicGoal(String G, String OVar, Object[] objectsP, String RVars){
		if (!topGoalHasStarted) 
			throw new IPException("Premature invocation of deterministicGoal");
   		return super.deterministicGoal(G, OVar, objectsP, RVars);
   	}
   	
   	protected abstract boolean commandWithArray(String Functor, byte[] array, int nBytes);

    protected void setupPrologSide(){
        try {     	
            loadInitialFiles();
            
            progressMessage("Teaching examples to Prolog...");
            ByteArrayOutputStream serializedTemp = new ByteArrayOutputStream();
            ObjectOutputStream bootObjects = new ObjectOutputStream(serializedTemp);
            teachIPobjects(bootObjects);
            teachBasicObjects(bootObjects);
            bootObjects.flush();
            
            byte[] b = serializedTemp.toByteArray();
            // more bytes in 1.4 with Throwable: System.out.println(b.length+" bytes to teach");
            // this is the only teaching of objects not occurring over the deterministicGoal/javaMessage mechanism:
            if(!commandWithArray("ipLearnExamples",b, b.length))
            	throw new IPException("ipLearnExamples failed");
            progressMessage("Initial examples taught.");            
            if (!command("ipObjectSpec('InvisibleObject',E,["+registerJavaObject(this)+"],_), assert(ipPrologEngine(E))"))
            	throw new IPException("assert of ipPrologEngine/1 failed");
            if(debug&&!command("assert(ipIsDebugging)"))
            	throw new IPException("assert of ipIsDebugging failed");
        } catch (Exception e){
            throw new IPException("Could not initialize XSB:"+e);
        }
    }

	/** Calls the first Prolog goal in a background thread. That goal will return only if an interrupt or error occurs;
	this method should handle these conditions properly  */
	protected void startTopGoal(){
		Thread mayBecomeAthread = new Thread("Native engine thread"){
			public void run(){
				boolean ended = false;
				try{
					while(!ended){
						progressMessage("Calling "+firstJavaMessageName+"...");
						// under normal operation the following never returns:
						//int rc = xsb_command_string("ipPrologEngine(E), javaMessage(E,"+firstJavaMessageName+").");
						boolean succeeded = realCommand("ipPrologEngine(E), javaMessage(E,"+firstJavaMessageName+")");
						if (!succeeded && interrupting) {
							/* Since Prolog is assumed to be able to execute one single goal at a time we need 
							   to cascade the interrupt to all pending goals:*/
							progressMessage("Prolog execution interrupted");
							interruptTasks();
						} else if (!succeeded){
							// ...ditto for aborting:
							progressMessage("Prolog execution aborted and restarted");
							abortTasks();
							if (isShutingDown()) ended=true;
						}else {
							ended=true;
							System.err.println("NativeEngine ending abnormally");
						}
					}
				} catch (Throwable e){
					System.err.println("Terrible obscure error:\n"+e);
				}
			}
		};
		//topGoalHasStarted = true; moved to handleCallback, where we detect that the above javaMessage request arrived
		if (JNIinSameThread) 
			mayBecomeAthread.run();
		else{
			mayBecomeAthread.setName("Prolog handler");
			prologHandler=mayBecomeAthread;
			prologHandler.start();
		}
	}

}
