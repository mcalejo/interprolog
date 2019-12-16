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
import com.declarativa.interprolog.remote.PrologServer;
import com.declarativa.interprolog.remote.RemoteXSBSubprocessEngine;
/** 
In order to run this you need to:
- Install/copy XSB Prolog to the remote machines, Java JRE as well as interprolog.jar
- Execute the interprolog daemon on the remote machine with
	java -classpath interprolog.jar com.declarativa.interprolog.remote.PrologServer XSBpath
	...and take note of the IP/port being listened to
- On the client machine you need only Java JRE and interprolog.jar
	java -classpath interprolog.jar com.declarativa.interprolog.examples.HelloRemote2 IP port
Example output:
....

*/
public class HelloRemote2{
	public static void main(String args[]) {
		int port = 0; String IP = null;
		if (args.length==2){
			IP = args[0];
			port = Integer.parseInt(args[1]);
		} else {
			System.err.println("Arguments are: Hostname PortNumber");
			System.exit(1);
		}
		
		long T0= System.currentTimeMillis();
		
		final SubprocessEngine[] engines = new SubprocessEngine[] {
			// This machine, remote InterProlog server/daemon
			new RemoteXSBSubprocessEngine(IP,port,true)
			//new RemoteXSBSubprocessEngine("localhost","interprolog","XSB/bin/xsb","interprolog.xwam",false),
			//new XSBSubprocessEngine(new String[]{"/Users/mc/Dropbox/XSB/bin/xsb"}),
		};
		System.out.println("Created "+engines.length+" engines");
		
		for (int i=0; i<engines.length; i++)
			engines[i].addPrologOutputListener(new SubprocessEngine.OutputDumper("Output"+i+".txt"));
		for (AbstractPrologEngine engine:engines)
			System.out.println(engine.deterministicGoal("getPrologPID(ID), cwd(D), buildTermModel(ID-D,TM)","[TM]")[0]);
		System.out.println("Now some messages from remote Prologs");
		
		for (int i=0; i<engines.length; i++)
			engines[i].deterministicGoal("javaMessage('java.lang.System'-out,println(string('Hello from engine "+i+", client World!')))");
		System.out.println("Ended javaMessaging");
		
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
		System.out.println("...testing availability...");
		for (AbstractPrologEngine engine:engines){
			// Insufficient to unstuck second engine on windows... engine.waitUntilIdle();
			stillTrue = engine.deterministicGoal("true") && stillTrue;
			System.out.print("*");
		}
		System.out.println("Engines working after interrupt ");
		
		for (AbstractPrologEngine engine:engines)
			engine.shutdown();
		System.out.println("Engines shut down. ");
		System.out.println("Time: "+(System.currentTimeMillis()-T0)+ " mS");
		PrologServer.shutdown(IP,port);
		System.exit(0);
	}
}