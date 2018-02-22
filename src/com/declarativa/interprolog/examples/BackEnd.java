/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.examples;
import com.declarativa.interprolog.*;
public class BackEnd{
	public static class Record implements java.io.Serializable{
		private static final long serialVersionUID = 3733177706802757177L;
		String plainText;
		int plainCount;
		String cyberText;
		float cyberCount;
		public String toString(){
			return "plain:"+plainText+" plainCount:"+plainCount+" \ncyberText:"+cyberText+" cyberCount="+cyberCount;
		}
	}
	public static void main(String args[]) {
		String[] prologCommands = com.declarativa.interprolog.gui.ListenerWindow.commandArgs(args);
		PrologEngine engine = new XSBSubprocessEngine(prologCommands);
		engine.deterministicGoal("import reverse/2,length/2 from basics"); //  list processing predicates
		if (!engine.deterministicGoal("length([1,2],2)")) System.err.println("Bad length/2 predicate!");
		engine.teachOneObject(new Record()); // send an object prototype to Prolog
		Record r = new Record();
		r.plainText="Declarative is good"; r.plainCount=r.plainText.length();
		Object[] objectsToGo = new Object[]{r};
		String goal = "ipObjectSpec('com.declarativa.interprolog.examples.BackEnd$Record',[plainCount=PC,plainText=string(S)],R)";
		goal += ", name(S,Chars), length(Chars,PC), reverse(Chars,Reversed), name(RS,Reversed), CC is PC+0.0";
		goal += ", ipObjectSpec('com.declarativa.interprolog.examples.BackEnd$Record',[plainCount=PC,plainText=string(S),cyberText=string(RS),cyberCount=CC],NewR)";
		System.out.println("Calling "+goal);
		Object[] bindings = engine.deterministicGoal(goal, "[R]", objectsToGo, "[NewR]");
		Record result = (Record)bindings[0];
		System.out.println(result);
		try{
			System.out.println("Success=="+engine.deterministicGoal("undefined_goal"));
		} catch(Exception e){
			System.out.println("Exception:"+e);
		}
		engine.shutdown();
		System.exit(0);
	}
}