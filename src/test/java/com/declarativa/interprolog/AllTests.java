/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2000-2005
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.interprolog; 
import com.xsb.interprolog.XSBNativeEngineTest;

import junit.framework.Test;
import junit.framework.TestSuite;
public class AllTests{

	public static void main(String args[]) { 
		com.declarativa.interprolog.gui.ListenerWindow.commonGreeting();
		/*startCommand=args[0];
		if (args.length>1) System.out.println("Invoke tests with a single argument");
    	else*/ // 
		org.junit.runner.JUnitCore.runClasses(AllTests.class);
    	//org.junit.runner.JUnitCore.main("com.declarativa.interprolog.XSBSubprocessEngineTest");
		
		//org.junit.runner.JUnitCore.runClasses(XSBSubprocessEngineTest.class);
	}
	
	public static Test suite(){
		TestSuite suite= new TestSuite("Testing InterProlog"); 
		suite.addTestSuite(XSBSubprocessEngineTest.class);
		suite.addTest(new XSBNativeEngineTest("testNativeEngine")); // new tests must be added by hand to testNativeEngine
		suite.addTestSuite(SWISubprocessEngineTest.class);
		//suite.addTestSuite(YAPSubprocessEngineTest.class);
		return suite;
	}
}