/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2015
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;

public class SWISubprocessEngineTest extends SubprocessEngineTest {
	public SWISubprocessEngineTest(String name){
		super(name);
	}
	protected void loadTestFile(AbstractPrologEngine engine){
		engine.consultFromPackage("tests.yap",AbstractPrologEngine.class);
	}
	
	// JUnit reloads all classes, clobbering variables, 
	// so the path should be obtained from System properties or other external means:
	protected AbstractPrologEngine buildNewEngine(){
		AbstractPrologEngine engine = new SWISubprocessEngine();
		//engine.setDebug(true);
		return engine;
	}
	/** No undefineds here */
	public void testUndefined(){}
	/** No term_to_atom/3, ergo no buildInitiallyFlatTermModel: */
	public void testErrorTermParse(){}
	public void testInitiallyFlatTermModel(){}
	public void testIFTperformance(){}
	
	//public void testNewInterrupt(){} 
	//public void testDoubles(){}
	//public void testDoubles2(){}
	public void testNaNetc(){} // not functional at the moment, apparently hangs serializing to Prolog
	//public void testGoalThreadedCB(){}
	//public void testGoalSameThreadCB(){}
}
