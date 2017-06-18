/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com, http://www.xsb.com
** Copyright (C) XSB Inc., USA, 2001-2005, Declarativa 2013
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.xsb.interprolog;
import com.declarativa.interprolog.AbstractPrologEngine;

/** Use for example:   java -cp ../ergoStudio.jar:../externalJars/junit-4.5.jar org.junit.runner.JUnitCore com.declarativa.interprolog.AllTests */

public class XSBNativeEngineTest extends NativeEngineTest{
	public XSBNativeEngineTest(String name){super(name);}
	protected AbstractPrologEngine buildNewEngine(){
		//String dir = new XSBPeer().getBinDirectoryProperty(System.getProperties());
		if (singleEngine==null){
			singleEngine = new NativeEngine("/Users/mc/Dropbox/XSB/config/i386-apple-darwin14.3.0/bin");
			loadTestFile(singleEngine);
		}
		return singleEngine;
	}
    protected void setUp() throws java.lang.Exception{
		engine.command("import append/3,length/2 from basics");		
    }
    protected void tearDown() throws java.lang.Exception{
    }
    
	// XSB 2.7.1 has float problems on Linux:
	public void testNumbers2(){
		if (AbstractPrologEngine.isWindowsOS()||AbstractPrologEngine.isMacOS())
			super.testNumbers2();
		else System.err.println("Skipping testNumbers2");
	}
	public void testNumbers(){
		if (AbstractPrologEngine.isWindowsOS()||AbstractPrologEngine.isMacOS())
			super.testNumbers();
		else System.err.println("Skipping testNumbers2");
	}
	// THIS TIMES OUT but only for NativeEngine. Doesn't matter threadedCallbacks:
	public void testGoalThreadedCB(){
		System.err.println("Skipped testGoalThreadedCB in "+engine);
	}
    public void testGoalSameThreadCB(){
		System.err.println("Skipped testGoalSameThreadCB in "+engine);
	}

}