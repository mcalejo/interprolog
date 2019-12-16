/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.examples;
import java.io.File;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.SolutionIterator;
import com.declarativa.interprolog.XSBSubprocessEngine;
import com.declarativa.interprolog.SWISubprocessEngine;
import com.declarativa.interprolog.util.IPPrologError;

public class HelloWorld{
	public static void main(String args[]) {
		PrologEngine engine = new SWISubprocessEngine();
		//PrologEngine engine = new NativeEngine();
		//engine.command("import append/3,member/2 from basics"); // Only for XSB Prolog
		//engine.setDebug(true);
		Object[] bindings = engine.deterministicGoal(
			"name(User,UL),append([104, 101, 108, 108, 111, 32], UL, ML), name(Message,ML)", // it execute all...
			"[string(User)]",
			new Object[]{System.getProperty("user.name")},
			"[string(Message)]");
		String message = (String)bindings[0];
		System.out.println("\nMessage:"+message);
		System.out.println("Undefined?"+engine.lastSolutionUndefined());
		// the above demonstrates object passing both ways; 
		// since we may simply concatenate strings, an alternative coding would be:
		bindings = engine.deterministicGoal(
			"name('"+System.getProperty("user.name")+"',UL),append([104, 101, 108, 108, 111, 32], UL, ML), name(Message,ML)",
			"[string(Message)]");
		// (notice the ' surrounding the user name, unnecessary in the first case)
		System.out.println("Same:"+bindings[0]);
		System.out.println("Undefined?"+engine.lastSolutionUndefined());
		
		// Usually deterministicGoal is enough for most apps; otherwise, the following provides solution-at-a-time:
		
		SolutionIterator si = engine.goal("member(X,[1,2,3]), buildTermModel(X,XM)", "[XM]");
		while(si.hasNext())
			System.out.println("X = " + si.next()[0]);
		
		si = engine.goal("member(X,[1,2,3]), buildTermModel(X,XM)", "[XM]");
		System.out.println("First:"+si.next()[0]);
		System.out.println("Second:"+si.next()[0]);
		si.cancel();
		try{ System.out.println("Third:"+si.next()[0]);}
		catch (java.util.NoSuchElementException e){System.out.println("e:"+e);}
		
		si = engine.goal("X=one; X is 2/one; X=three","[string(X)]"); 
		while(si.hasNext())
			try{
				System.out.println("next:"+si.next()[0]);
			} catch(IPPrologError e){
				System.err.println("Prolog error:"+e);
			}
			
		si = engine.goal("X=one; X=two; X=three","[string(X)]"); 
		while(si.hasNext())
				System.out.println("next:"+si.next()[0]+",undefined=="+si.lastSolutionUndefined());
		
		/* engine.consultAbsolute(new File(
			"/Users/mc/Dropbox/interprologsvn_unfuddle/interprologForJDK/fidjiExamples/silk/undefined/win-not-win-Prolog.P")
			);
		System.out.println("First solution:"+engine.deterministicGoal("win(X),buildTermModel(X,TM)","[TM]")[0]);
		System.out.println("Undefined?"+engine.lastSolutionUndefined());
		
		si = engine.goal("win(X),buildTermModel(X,TM)","[TM]"); 
		while(si.hasNext())
				System.out.println("next:"+si.next()[0]+",undefined=="+si.lastSolutionUndefined());
	 */
	 	System.out.println("Shutting down engine");
		engine.shutdown();
		System.exit(0);

	}
}