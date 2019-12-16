/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com, http://www.xsb.com
** Copyright (C) XSB Inc., USA, 2001-2005
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.xsb.interprolog;
import com.declarativa.interprolog.AbstractPrologEngine;
import com.declarativa.interprolog.PrologEngineTest;

public abstract class NativeEngineTest extends PrologEngineTest{
	static AbstractPrologEngine singleEngine = null;
    public NativeEngineTest(String name){
		super(name);
		engine = buildNewEngine();
		//System.out.println("NativeEngineTest version:"+engine.getPrologVersion());
		thisID = engine.registerJavaObject(this);
		// loadTestFile();
   }
   
    public void testNativeEngine() throws InterruptedException{ // to avoid multiple NativeEngines
       	System.out.println("starting testNativeEngine");
       	testNewInterrupt();
    	testNumbers();
    	testPrototypeStuff();
    	testAutoTermModel();
    	testBuildTermModel();
    	testNumbers2();
    	testDoubles();
    	testNaNetcFromJava();
    	testNaNetcFromProlog();
    	testDigestingBadGoal();
    	testDeterministicGoal();
    	testDG2();
        System.out.println("Ended testDG2");
        testJavaMessage();
        testIPobjects();
        //tested only in XSBSubprocessEngineTest: testPrologInstallDir();
        testLoops();
        testSomaN();
        testFibonaci();
       	testFactorial();
       	testGetRealJavaObject();
        System.out.println("Ended testGetRealJavaObject");
      	testStrangeChar();
      	testBlockdataSerialization1();
        System.out.println("testBlockdataSerialization1");
      	testBlockdataSerialization2();
       	testMultipleThreads();
        System.out.println("testMultipleThreads");
      	testReceiveExternalCall();
       	testReceiveExternalCall2();
       	testReceiveExternalCall3();
      	testTermModelIdentity();
        System.out.println("testTermModelIdentity");
      	testInitiallyFlatTermModel();
       	System.out.println("Finished testNativeEngine");
   }
	// Callback torture:
	// SUBPROCSS ENGINE:
	// Win98, Celeron 400 MHz: 220 ms/message
	// Win NT4 Workstation, Pentium 400 MHz: 441 mS/message
	// Win 2k, Pentium 400 MHz: 57 mS/message
	// NATIVE ENGINE, 19/9/2001:
	// Win 2k, Pentium 400 MHz: 9 mS/message
	// 11/5/2002
	// Win 2k,Celeron 700MHz: 7 mS/goal

	// Bulk torture:
	// SUBPROCESS ENGINE:
	// Win98, Celeron 400 MHz: 4670 ms
	// Win NT4 Workstation, Pentium 400 MHz: 4607 mS (7128 bytes gone and returned / second)
	// Win 2k, Pentium 400 MHz: 7221 mS (4548 bytes gone and returned / second, 3072 handles)
	// NATIVE ENGINE, 19/9/2001:
	// Win 2k, Pentium 400 MHz: 3255 mS (10090 bytes gone and returned / second, 3072 handles)
	// 11/5/2002
	// Win 2k,Celeron 700MHz: 4015 mS (8180 bytes gone and returned / second)
	
	// Busy torture:
	// SUBPROCESS ENGINE:
	// Win98, Celeron 400 MHz: 203 ms/goal
	// Win NT4 Wokstation, Pentium 400 MHz: 402 mS/goal
	// Win 2k, Pentium 400 MHz: 36 mS/goal
	// NATIVE ENGINE, 19/9/2001:
	// Win 2k, Pentium 400 MHz: 7 mS/goal	
	// 11/5/2002
	// Win 2k,Celeron 700MHz: 13 mS/goal
		
	
	public void testMultipleThreads(){
		DGClient client1 = new DGClient("Heavenly",10,10,10,10,50,50);
		DGClient client2 = new DGClient("Mount Snow",5,5,10,10,10,10);
		client1.start();
		// adding this second client hangs the Feb 27 version, but not the Mar 3 version :-):
		client2.start(); 
		while (client1.isAlive()||client2.isAlive()) Thread.yield();
	}
	
	public class DGClient extends Thread{
		int myID;
		long T1,T2,T3,T4,T5,T6;
		DGClient(String name,long T1, long T2, long T3, long T4, long T5, long T6){
			myID = engine.registerJavaObject(DGClient.this);
			this.T1=T1; this.T2=T2; this.T3=T3; this.T4=T4; this.T5=T5; this.T6=T6;
			setName(name);
		}
		public void run(){
			try{
				Thread.sleep(T1);
				//System.out.println(getName()+" calling first top dG...");
				assertTrue(engine.deterministicGoal("javaMessage("+myID+",method1)"));
				Thread.sleep(T2);
				//System.out.println(getName()+" calling second top dG...");
				assertTrue(engine.deterministicGoal("javaMessage("+myID+",method1)"));
				//System.out.println(getName()+" ended top dGs.");
			} catch (Exception e){
				throw new RuntimeException(e.toString());
			}
		}
		public void method1(){
			try{
				Thread.sleep(T3);
				//System.out.println(getName()+" calling 1st of second level dGs...");
				assertTrue(engine.deterministicGoal("javaMessage("+myID+",method2)"));
				Thread.sleep(T4);
				//System.out.println(getName()+" calling 2nd of second level dGs...");
				assertTrue(engine.deterministicGoal("javaMessage("+myID+",method2)"));
				//System.out.println(getName()+" ended second level dGs.");
			} catch (Exception e){
				throw new RuntimeException(e.toString());
			}
		}
		public void method2(){
			try{
				Thread.sleep(T5);
				//System.out.println(getName()+" calling 1st of third level dGs...");
				assertTrue(engine.deterministicGoal("true"));
				Thread.sleep(T6);
				//System.out.println(getName()+" calling 2nd of third level dGs...");
				assertTrue(engine.deterministicGoal("true"));
				//System.out.println(getName()+" ended third level dGs.");
			} catch (Exception e){
				throw new RuntimeException(e.toString());
			}
		}
	}
}
