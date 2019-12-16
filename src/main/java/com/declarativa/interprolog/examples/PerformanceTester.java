/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.examples;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.declarativa.interprolog.AbstractPrologEngine;
import com.declarativa.interprolog.InitiallyFlatTermModel;
import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.declarativa.interprolog.XSBSubprocessEngine;
import com.declarativa.interprolog.SWISubprocessEngine;
import com.declarativa.interprolog.util.IPException;
import com.declarativa.interprolog.util.OutOfBandTermResource;
/** Run some tests regarding data passing throughput (bytes/second); tests in the InterProlog 
test suit already measure performance for number of calls/second
walltime in Prolog (cputime not reliable??), walltime in Java!!!*/
//ADD one with no serialized args :-)

public class PerformanceTester{
	static AbstractPrologEngine engine;
	
	@SuppressWarnings("null")
	static void prologSerializationTimes(String specBuilderGoal,PrologEngine engine){
		String G = 
			"walltime(PreStart),"+specBuilderGoal+",walltime(PreFinish),Pre is PreFinish-PreStart,ipObjectSpec('java.lang.Double',PreD,[Pre],_),"
			+timingGoal;
		int nbytes=0;
		double serial=0;
		double unserial=0;
		double specification=0;
		double serialresult=0;
		Object[] yetanother=null;
		for (int r=0;r<Nruns-1;r++){
			yetanother = engine.deterministicGoal(G,"[IntegerNB,DoubleD,DoubleD2,PreD,DoubleD3]");
			if (yetanother==null)
				System.err.println("prologSerializationTimes failed:"+G);
			nbytes += ((Integer)yetanother[0]).intValue();
			serial += ((Double)yetanother[1]).doubleValue() * 1000;
			unserial += ((Double)yetanother[2]).doubleValue() * 1000;
			specification += ((Double)yetanother[3]).doubleValue() * 1000;
			serialresult += ((Double)yetanother[4]).doubleValue() * 1000;
		}
		System.out.println(
			"Prolog grammar pure serializing took "+serial/Nruns+" mS, unserializing "+unserial/Nruns+ 
			" mS ("+ nbytes/Nruns + " bytes). Spec building took "+specification/Nruns+" mS. Result serialization "+serialresult/Nruns
			);
	}
	static final String timingGoal = "walltime(Start), streamContents([LM],handles(NH,_),Bytes,[]), walltime(Finish), Duration is Finish-Start, basics:length(Bytes,NB), "+
	 "ipObjectSpec('java.lang.Integer',IntegerNH,[NH],_), ipObjectSpec('java.lang.Integer',IntegerNB,[NB],_), ipObjectSpec('java.lang.Double',DoubleD,[Duration],_), "+
	 "walltime(Start2),streamContents([_LM],handles(_,_),Bytes,[]),walltime(Finish2),LM=_LM,Duration2 is Finish2-Start2, ipObjectSpec('java.lang.Double',DoubleD2,[Duration2],_), "+
	 "ipObjectSpec('ResultFromProlog',Result,[1,13,0,null,[LM]],_),!, walltime(Start3), streamContents([Result],_,_,[]), walltime(Finish3), Duration3 is Finish3-Start3, ipObjectSpec('java.lang.Double',DoubleD3,[Duration3],_)";
	 // ! needed otherwise "choice and trail stack growing"


	@SuppressWarnings("null")
	static void javaSerializationTimes(String message, Object X){
		try{
			ByteArrayOutputStream baos=null;
			ObjectOutputStream oos=null;
			long T0,T1;
			long javaSerial=0,javaUnserial=0;
			ObjectInputStream iis=null;
			for (int r=0;r<Nruns-1;r++){
				T0= System.currentTimeMillis();
				baos = new ByteArrayOutputStream();
				oos = new ObjectOutputStream(baos);
				if (X instanceof InitiallyFlatTermModel)
					((InitiallyFlatTermModel)X).deflate(); // before this X will certainly be !stillFlat...
				oos.writeObject(X);
				T1= System.currentTimeMillis();
				javaSerial += T1 - T0;
				iis = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
				iis.readObject();
				javaUnserial += System.currentTimeMillis() - T1;
			}
	
			System.out.println(message+": Java serialization takes "+(javaSerial/Nruns)+" mS, unserialization "+(javaUnserial/Nruns)+" mS ("+ baos.size() +" bytes total)");
		} catch (Exception e){throw new RuntimeException("Bad exception in javaSerializationTimes:"+e);}
	}	
	static int N=250;
	static int Nruns=4;
	static int DEPTH = 11; // for tree
	
	static void runit(PrologEngine engine){
		runit(engine,true);
	}
	
	static void runit(PrologEngine engine, boolean doOutOfBandTests){
		System.out.println("-----Times averaged over "+Nruns+" runs-----");
		System.out.println("-----Inter language call timing:");
		long T0= System.currentTimeMillis();
		for (int r=0;r<Nruns-1;r++){
			if (!engine.deterministicGoal("ipPrologEngine(E), doGroundGoal("+N/10+",javaMessage(E,isAvailable))")) 
				System.err.println("FAILED javaMessage loop");
		}
		long T1= System.currentTimeMillis();
		System.out.println("Called minimal javaMessage "+(N/10)+" times in "+(T1-T0)/Nruns+" mS");
		
		T0= System.currentTimeMillis();
		for (int r=0;r<Nruns-1;r++){
			for(int g=0;g<N/10;g++)
				if (!engine.deterministicGoal("true")) throw new IPException("FAILED deterministicgoal loop");
		}
		T1= System.currentTimeMillis();
		System.out.println("Called minimal deterministicGoal "+(N/10)+" times in "+(T1-T0)/Nruns+" mS");
		
		// engine.setProfiling(true);
		
		System.out.println("\n-----list of ints:");
		String specBuilderGoal = "buildIntList("+N*10+",L), buildInitiallyFlatTermModel(L,LM)";
		TermModel LM=null;
		T0= System.currentTimeMillis();
		for (int r=0;r<Nruns-1;r++){
			LM = (TermModel)engine.deterministicGoal(specBuilderGoal,"[LM]")[0];
		}
		T1= System.currentTimeMillis();
		System.out.println("Received InitiallyFlatTermModel list with "+N*10+" ints in "+(T1-T0)/Nruns+" mS");
		prologSerializationTimes(specBuilderGoal,engine);
		javaSerializationTimes("list of ints",LM);
		//System.out.println(LM);
		//System.out.println("arity:"+LM.getChildCount());

		specBuilderGoal = "buildIntList("+N+",L), buildTermModel(L,LM)";
		T0= System.currentTimeMillis();
		for (int r=0;r<Nruns-1;r++){
			LM = (TermModel)engine.deterministicGoal(specBuilderGoal,"[LM]")[0];
		}
		T1= System.currentTimeMillis();
		System.out.println("Received TermModel list with "+N+" ints in "+(T1-T0)/Nruns+" mS");
		prologSerializationTimes(specBuilderGoal,engine);
		javaSerializationTimes("list of ints",LM);
		
		System.out.println("\n-----list of terms:");
		specBuilderGoal = "buildTermList("+N*10+",L), buildInitiallyFlatTermModel(L,LM)";
		T0= System.currentTimeMillis();
		for (int r=0;r<Nruns-1;r++)
			LM = (TermModel)engine.deterministicGoal(specBuilderGoal,"[LM]")[0];
		T1= System.currentTimeMillis();
		System.out.println("Received InitiallyFlatTermModel list with "+N*10+" terms in "+(T1-T0)/Nruns+" mS");
		//engine.setDebug(true);
		prologSerializationTimes(specBuilderGoal,engine); 
		javaSerializationTimes("list of term",LM);
		
		if (doOutOfBandTests){
			T0= System.currentTimeMillis();
			OutOfBandTermResource oobr = new OutOfBandTermResource(engine);
			Double DD = (Double)engine.deterministicGoal(oobr.prologFileAtom() +" = F, buildTermList("+N*10+",L), walltime(T0), ipPutTermList(L,F), walltime(T1), "+
				"Duration2 is T1-T0, ipObjectSpec('java.lang.Double',DoubleD2,[Duration2],_)",
			"[DoubleD2]")[0];
			TermModel[] L = oobr.getTermList();
			T1= System.currentTimeMillis();
			System.out.println("Received out of band same list with "+L.length+" terms in "+(T1-T0)/1+" mS. ipPutTermList took "+DD);
		}
		
		specBuilderGoal = "buildTermList("+N+",L), buildTermModel(L,LM)";
		T0= System.currentTimeMillis();
		for (int r=0;r<Nruns-1;r++)
			LM = (TermModel)engine.deterministicGoal(specBuilderGoal,"[LM]")[0];
		T1= System.currentTimeMillis();
		System.out.println("Received TermModel list with "+N+" terms in "+(T1-T0)/Nruns+" mS");
		prologSerializationTimes(specBuilderGoal,engine);
		javaSerializationTimes("list of term",LM); 


		System.out.println("\n-----tree:");
		specBuilderGoal = "buildTermTree("+DEPTH+",L), buildInitiallyFlatTermModel(L,LM)";
		T0= System.currentTimeMillis();
		for (int r=0;r<Nruns-1;r++)
			LM = (TermModel)engine.deterministicGoal(specBuilderGoal,"[LM]")[0];
		T1= System.currentTimeMillis();
		System.out.println("Received term tree in InitiallyFlatTermModel with "+(Math.pow(2,(DEPTH+1))-1)+" nodes in "+(T1-T0)/Nruns+" mS");
		prologSerializationTimes(specBuilderGoal,engine);
		javaSerializationTimes("tree",LM); 

		specBuilderGoal = "buildTermTree("+DEPTH+",L), buildTermModel(L,LM)";
		T0= System.currentTimeMillis();
		for (int r=0;r<Nruns-1;r++)
			LM = (TermModel)engine.deterministicGoal(specBuilderGoal,"[LM]")[0];
		T1= System.currentTimeMillis();
		System.out.println("Received term tree in TermModel with "+(Math.pow(2,(DEPTH+1))-1)+" nodes in "+(T1-T0)/Nruns+" mS");
		prologSerializationTimes(specBuilderGoal,engine);
		javaSerializationTimes("tree",LM); 


		System.out.println("\n-----huge string:");
		specBuilderGoal = "makeHugeList("+(1024*N)+",L), atom_codes(A,L), LM=string(A)";
		T0= System.currentTimeMillis();
		String S=null;
		for (int r=0;r<Nruns-1;r++)
			S = (String)engine.deterministicGoal(specBuilderGoal,"[LM]")[0];
		T1= System.currentTimeMillis();
		//engine.setDebug(false);
		System.out.println("Received String with "+(1024*N)+" chars (happen to be bytes) in "+(T1-T0)/Nruns+" mS");
		//engine.setDebug(true);
		prologSerializationTimes(specBuilderGoal,engine);
		javaSerializationTimes("String",S); 

		System.out.println("\n-----huge string from Java:");
		double duration=0;
		for (int r=0;r<Nruns-1;r++)
			duration = ((Double)engine.deterministicGoal("walltime(Start), javaMessage('com.declarativa.interprolog.examples.PerformanceTester',_R,getHugeString), walltime(Finish), "+
			"Duration is (Finish-Start)*1000, ipObjectSpec('java.lang.Double',DoubleD,[Duration],_)", "[DoubleD]")[0]).doubleValue();
		System.out.println("Received String with "+(1024*N)+" chars (happen to be bytes) from JAVA in "+duration/Nruns+" mS");
		
		/*
		try{
			File F = null;
			T0= System.currentTimeMillis();
			for (int r=0;r<Nruns-1;r++){
				F = File.createTempFile("prefix",".txt");
				F.deleteOnExit();
				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(F));
				osw.write(S,0,S.length());
				osw.flush(); osw.close();
			}
			T1= System.currentTimeMillis();
			System.out.println("Same String ("+(1024*N)+" bytes) was written to disk in "+(T1-T0)/Nruns+" mS");
			T0= System.currentTimeMillis();
			byte[] buffer = new byte[S.length()];
			for (int r=0;r<1-1;r++){
				FileInputStream fis = new FileInputStream(F);
				fis.read(buffer);
				fis.close();
			}
			T1= System.currentTimeMillis();
			System.out.println("Same String ("+(1024*N)+" bytes) was read once from disk in "+(T1-T0)/1+" mS");
			System.out.println("First,second bytes:"+buffer[0]+","+buffer[1]);
		} catch (IOException ex){
			System.err.println("Problems:"+ex);
		}	*/	
	}
	
	static String hugeString = makeHugeString();
	
	private static String makeHugeString(){
		StringBuffer sb = new StringBuffer();
		for (int c=0;c<N*1024; c++) sb.append("a");
		return sb.toString();
	}
	public static String getHugeString(){
		return hugeString;
	}

	public static void main(String args[]) {
		com.declarativa.interprolog.gui.ListenerWindow.commonGreeting();
		//engine = new NativeEngine();
		//engine = new XSBSubprocessEngine();
		engine = new SWISubprocessEngine();
		engine.consultFromPackage("tests.P",AbstractPrologEngine.class);
		if (engine instanceof XSBSubprocessEngine)
			engine.deterministicGoal("import append/3,length/2 from basics");
		
		System.out.println("---------------STARTING without ipUsesNativeNonterminals...--------------");
		engine.deterministicGoal("retractall(ipUsesNativeNonterminals)");
		runit(engine);
		
		/*
		if (!engine.deterministicGoal("assert(ipUsesNativeNonterminals)")) throw new IPException("assert failed!");;
		System.out.println("-----pre growing stacks...------");
		engine.deterministicGoal("makeHugeList("+(400*N)+",L), atom_codes(A,L), LM=string(A)","[LM]");
		System.out.println("---------------NOW WITH NATIVE NONTERMINALS!!!--------------");
		runit(engine);
		*/
		engine.shutdown();
		System.exit(0);
	}
	
	
}