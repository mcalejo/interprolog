/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.examples;
import com.declarativa.interprolog.AbstractPrologEngine;
import com.declarativa.interprolog.SubprocessEngine;
import com.declarativa.interprolog.XSBSubprocessEngine;
import com.declarativa.interprolog.remote.RemoteXSBSubprocessEngine;
/** 
In order to run this you need to:
- Install/copy XSB Prolog and interprolog.xwam to the remote machines
- Create an interprolog login in each machine, with permissions to run ssh, and with passphrase configured
Test the above first by running on your machine (the one with the Java app):
	ssh interprolog@remotehost 'PathToXSBexecutable'
You should enter an interactive XSB session, without being asked a password.
Look at files named OutputK.txt for each engine K's Prolog output

java -classpath /Users/mc/Dropbox/interprologsvn_unfuddle/interprologForJDK/interPrologStudio.jar com.declarativa.interprolog.examples.HelloRemote
...
Created 6 engines
29796-/Users/interprolog
5012-C:\windows\system32
5724-C:\windows\system32
2367-/home/mc
2520-/home/mc
29862-/Users/mc/Dropbox/XSBInc/Warwick_and_ACES
Now some messages from remote Prologs
Hello from engine 0, client World!
Hello from engine 1, client World!
Hello from engine 2, client World!
Hello from engine 3, client World!
Hello from engine 4, client World!
Hello from engine 5, client World!
Ended javaMessaging
Engines shut down. 
*/
public class HelloRemote{
	public static void main(String args[]) {
		long T0= System.currentTimeMillis();
		
		final SubprocessEngine[] engines = new SubprocessEngine[] {
			// This machine, direct (classic)
			new XSBSubprocessEngine(new String[]{"/Users/mc/Dropbox/XSB/bin/xsb"}),
			// This machine, through ssh
			new RemoteXSBSubprocessEngine("localhost","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			// Windows laptop:
			//new RemoteXSBSubprocessEngine("192.168.1.76","mc","C:\\Users\\mc\\Desktop\\fidjiWithErgoAndXSB-win64\\fidjiXSB\\bin\\xsb64.bat","C:\\Users\\mc\\Desktop\\interprolog.xwam",true),
			new RemoteXSBSubprocessEngine("192.168.1.76","mc","C:\\Users\\mc\\Desktop\\fidjiWithErgoAndXSB-win64\\fidjiXSB\\bin\\xsb64.bat","C:\\Users\\mc\\Desktop\\interprolog.xwam",true)
			/* new RemoteXSBSubprocessEngine("192.168.1.76","mc","C:\\Users\\mc\\Desktop\\fidjiWithErgoAndXSB-win64\\fidjiXSB\\bin\\xsb64.bat","C:\\Users\\mc\\Desktop\\interprolog.xwam",true),
			new RemoteXSBSubprocessEngine("192.168.1.76","mc","C:\\Users\\mc\\Desktop\\fidjiWithErgoAndXSB-win64\\fidjiXSB\\bin\\xsb64.bat","C:\\Users\\mc\\Desktop\\interprolog.xwam",true),
			new RemoteXSBSubprocessEngine("192.168.1.76","mc","C:\\Users\\mc\\Desktop\\fidjiWithErgoAndXSB-win64\\fidjiXSB\\bin\\xsb64.bat","C:\\Users\\mc\\Desktop\\interprolog.xwam",true),
			new RemoteXSBSubprocessEngine("192.168.1.76","mc","C:\\Users\\mc\\Desktop\\fidjiWithErgoAndXSB-win64\\fidjiXSB\\bin\\xsb64.bat","C:\\Users\\mc\\Desktop\\interprolog.xwam",true),
			new RemoteXSBSubprocessEngine("192.168.1.76","mc","C:\\Users\\mc\\Desktop\\fidjiWithErgoAndXSB-win64\\fidjiXSB\\bin\\xsb64.bat","C:\\Users\\mc\\Desktop\\interprolog.xwam",true),
			new RemoteXSBSubprocessEngine("192.168.1.76","mc","C:\\Users\\mc\\Desktop\\fidjiWithErgoAndXSB-win64\\fidjiXSB\\bin\\xsb64.bat","C:\\Users\\mc\\Desktop\\interprolog.xwam",true),
			new RemoteXSBSubprocessEngine("192.168.1.76","mc","C:\\Users\\mc\\Desktop\\fidjiWithErgoAndXSB-win64\\fidjiXSB\\bin\\xsb64.bat","C:\\Users\\mc\\Desktop\\interprolog.xwam",true),
			new RemoteXSBSubprocessEngine("192.168.1.76","mc","C:\\Users\\mc\\Desktop\\fidjiWithErgoAndXSB-win64\\fidjiXSB\\bin\\xsb64.bat","C:\\Users\\mc\\Desktop\\interprolog.xwam",true),*/
			// Other Mac Laptop:
			//new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			//new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false)
			/*new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.72","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			// UBUNTU VM:
			new RemoteXSBSubprocessEngine("192.168.1.85","mc","/home/mc/newXSB/bin/xsb","/home/mc/interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.85","mc","/home/mc/newXSB/bin/xsb","/home/mc/interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.85","mc","/home/mc/newXSB/bin/xsb","/home/mc/interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.85","mc","/home/mc/newXSB/bin/xsb","/home/mc/interprolog.xwam",false),
			new RemoteXSBSubprocessEngine("192.168.1.85","mc","/home/mc/newXSB/bin/xsb","/home/mc/interprolog.xwam",false)*/
		};
		System.out.println("Created "+engines.length+" engines");
		
		/* Performance tests
		engines[0].consultFromPackage("tests.P",AbstractPrologEngine.class); // local engine...
		engines[1].consultAbsolute("tests.xwam");
		engines[2].consultAbsolute("C:\\Users\\mc\\Desktop\\tests.xwam");
		engines[3].consultAbsolute("C:\\Users\\mc\\Desktop\\tests.xwam");
		engines[4].consultAbsolute("tests.xwam");
		engines[5].consultAbsolute("tests.xwam");
		System.out.println("Starting SEQUENTIAL remote performance tests, one node at a time...");
		long T1 = System.currentTimeMillis();
		for (int i=0; i<engines.length; i++){
			System.out.println("Engine "+i+":");
			PerformanceTester.runit(engines[i],false);
			System.out.println("----end of tests of engine "+i+".");
		}
		System.out.println("SEQUENTIAL remote tests took "+ (System.currentTimeMillis()-T1) + " mS");
		long T2 = System.currentTimeMillis();
		System.out.println("Starting PARALLEL remote tests...");
		Thread[] threads = new Thread[engines.length];
		for (int i=0; i<engines.length; i++){
			final AbstractPrologEngine engine = engines[i];
			final int I = i; 
			threads[i] = new Thread(new Runnable(){
				public void run(){
					try{
						System.out.println("Starting work in engine "+I+"...");
						PerformanceTester.runit(engine,false);
						System.out.println("...Ended work in engine "+I);
					} catch (Exception e){
						System.out.println(I+" exception:"+e);
					}
				}
			});		
			threads[i].start();	
		}
		try{
			for (Thread thread:threads)
				thread.join();
			} 
		catch(InterruptedException ex){ System.err.println("BAD exception:"+ex);}
		System.out.println("PARALLEL remote tests took "+ (System.currentTimeMillis()-T2) + " mS");
		*/
		for (int i=0; i<engines.length; i++)
			engines[i].addPrologOutputListener(new SubprocessEngine.OutputDumper("Output"+i+".txt"));
		for (AbstractPrologEngine engine:engines)
			System.out.println(engine.deterministicGoal("getPrologPID(ID), cwd(D), buildTermModel(ID-D,TM)","[TM]")[0]);
		System.out.println("Now some messages from remote Prologs");
		
		for (int i=0; i<engines.length; i++)
			engines[i].deterministicGoal("javaMessage('java.lang.System'-out,println(string('Hello from engine "+i+", client World!')))");
		System.out.println("Ended javaMessaging");
		
		
		/* Interrupt tests
		for (int i=0; i<engines.length; i++){
			final AbstractPrologEngine engine = engines[i];
			final int I = i;
			new Thread(new Runnable(){
				public void run(){
					try{
						System.out.println(I+" result:"+engine.deterministicGoal("repeat,fail"));
					} catch (Exception e){
						System.out.println(I+" exception:"+e);
					}
				}
			}).start();
		} 
		//engines[0].setDebug(true);
		for (AbstractPrologEngine engine:engines)
			engine.interrupt();
		System.out.println("Interrupted...");
		boolean stillTrue = true;
		// Useless... try{Thread.sleep(1000);}catch(Exception ex){}
		System.out.println("...testing availability...");
		for (AbstractPrologEngine engine:engines){
			// Insufficient to unstuck second engine on windows... engine.waitUntilIdle();
			stillTrue = engine.deterministicGoal("true") && stillTrue;
			System.out.print("*");
		}
		System.out.println("Engines working after interrupt ");
		*/
		
		
		
		
		for (AbstractPrologEngine engine:engines)
			engine.shutdown();
		System.out.println("Engines shut down. ");
		System.out.println("Time: "+(System.currentTimeMillis()-T0)+ " mS");
		System.exit(0);
	}
}