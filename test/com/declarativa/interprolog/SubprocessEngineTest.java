/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2000-2005
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.interprolog;
import com.declarativa.interprolog.util.IPException;
import com.declarativa.interprolog.util.IPPrologError;
import com.declarativa.interprolog.util.PrologHaltedException;
import com.declarativa.interprolog.util.UnavailableResultException;

public abstract class SubprocessEngineTest extends PrologEngineTest {
	public SubprocessEngineTest(String name){
		super(name);
	}
	protected void setUp() throws java.lang.Exception{
		engine = buildNewEngine();
		//System.out.println("SubprocessEngineTest version:"+engine.getPrologVersion());
		thisID = engine.registerJavaObject(this);
		loadTestFile(engine); engine.waitUntilAvailable();
    }
	protected void tearDown() throws java.lang.Exception{
 		engine.shutdown();
    }
	
	public void testDeterministicGoal(){ 
		super.testDeterministicGoal();
		
		try{ // Now working thanks to catch:
			engine.waitUntilAvailable();
			engine.deterministicGoal("nowaythisisdefined");
			fail("should raise an IPException... with undefined predicate message");
		} catch (IPException e){
			// Too strict for the stream-based recognizers:
			// assertTrue("proper message in exception",e.toString().indexOf("Undefined")!=-1);
			assertTrue("No more listeners",((SubprocessEngine)engine).errorTrigger.numberListeners()==0);
		}
	}
	public void testManyEngines(){
		SubprocessEngine[] engines = new SubprocessEngine[10]; // 3 hangs on my Windows 98, at least 10 work on NT 4 Workstation
		for (int i=0;i<engines.length;i++) {
			//System.out.println("Creating engine "+i);
			engines[i] = (SubprocessEngine)buildNewEngine();
		}
		for (int i=0;i<engines.length;i++) 
			//engines[i].waitUntilAvailable();
			assertTrue(engines[i].isAvailable());
		for (int i=0;i<engines.length;i++) 
			assertTrue(engines[i].deterministicGoal("javaMessage(string(hello),string(hello),toString)"));
		for (int i=0;i<engines.length;i++) 
			engines[i].shutdown();
	}
	StringBuffer buffer;
	public void testOutputListening(){
		buffer = new StringBuffer();
		PrologOutputListener listener = new PrologOutputListener(){
			@Override
			public void print(String s){
			}
			@Override
			public void printStdout(String s) {
				buffer.append(s);
			}
			@Override
			public void printStderr(String s) {
			}
		};
		assertEquals(0,((SubprocessEngine)engine).listeners.size());
		((SubprocessEngine)engine).addPrologOutputListener(listener);
		assertEquals(1,((SubprocessEngine)engine).listeners.size());
		engine.deterministicGoal("write('hello,'), write(' tester'), nl");
		engine.waitUntilAvailable();
		try{Thread.sleep(100);} catch(Exception e){fail(e.toString());} // let the output flow first...
		assertTrue("printed something",buffer.toString().indexOf("hello, tester") != -1);
		assertTrue("available",engine.isAvailable());
		assertTrue("detecting regular and break prompts",((SubprocessEngine)engine).isDetectingPromptAndBreak());
		//engine.setDebug(true);
		//try{Thread.sleep(2000);} catch(Exception e){fail(e.toString());}
		try{
			engine.deterministicGoal("thisIsUndefined");// hangs with command...
			fail("should have thrown exception showing undefined predicate");
		} catch(IPPrologError e){/*System.out.println("caught it:"+e);*/}
		//System.out.println("hanging here6..."+Thread.currentThread());
		engine.waitUntilAvailable();
		
		//FAILING ALWAYS:
		//engine.sendAndFlushLn("bad term."); 
		//System.out.println("now waiting; buffer:"); System.out.println(buffer);
		//engine.sendAndFlushLn("true."); //This "fixes" the problem on Win98 but not on NT4...
		engine.waitUntilAvailable();
		
		((SubprocessEngine)engine).removePrologOutputListener(listener);
		assertEquals(0,((SubprocessEngine)engine).listeners.size());
	}
	
	public void testPrologHaltedException(){
		int N = 0;
		// Test orderly shutdown:
		SubprocessEngine e = (SubprocessEngine)buildNewEngine();
	  	e.waitUntilAvailable();
		buffer = new StringBuffer();
		new PrologOutputListener(){
			@Override
			public void print(String s){
				buffer.append(s);
			}
			@Override
			public void printStdout(String s) {
			}
			@Override
			public void printStderr(String s) {
			}
		};
		N = buffer.length();
		e.shutdown();
		try{Thread.sleep(500);}catch(Exception ex){}
		// System.out.println("output received:"+buffer.substring(N));
		// try{Thread.sleep(1000);}catch(Exception ex){}
		assertEquals(N,buffer.length());
		// Test unexpected shutdown:
		final SubprocessEngine e2 = (SubprocessEngine)buildNewEngine();
		try{
			e2.deterministicGoal("halt");
			fail("should have thrown PrologHaltedException"); 
		} catch (PrologHaltedException ex){
		}	
	}
	
	public void testUnavailableResultException(){
		(new Thread(){
			public void run(){
				try{
					engine.deterministicGoal("repeat,fail");
					fail("should have thrown PrologHaltedException"); 
				} 
				catch (UnavailableResultException ex){}
				catch (Exception ex){fail("should have thrown PrologHaltedException:"+ex); }
			}
		}).start();
		while(engine.isIdle()) Thread.yield();
		engine.shutdown();
	}

}