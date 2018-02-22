/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2015
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;

import com.declarativa.interprolog.util.BasicTypeWrapper;

public class SWISubprocessEngineTest extends SubprocessEngineTest {
	public SWISubprocessEngineTest(String name){
		super(name);
	}
	protected void loadTestFile(AbstractPrologEngine engine){
		engine.consultFromPackage("tests.P",AbstractPrologEngine.class);
	}
	// JUnit reloads all classes, clobbering variables, 
	// so the path should be obtained from System properties or other external means:
	protected AbstractPrologEngine buildNewEngine(){
		AbstractPrologEngine engine = new SWISubprocessEngine();
		//engine.setDebug(true);
		return engine;
	}
	public void testUndefined(){} // No undefineds in SWI
	public void testInitiallyFlatTermModelFlora(){}  // over stresses the limitations for InitiallyFlatTermModels in SWI, cf. simple_enough_for_IFTM 
	public void testNaNetcFromProlog(){ // SWI does not allow building inf with 1/0
		// MD's test case:
		Object[] bindings = engine.deterministicGoal("X is inf, ipObjectSpec(double,Plus,[X],_), Y is -inf, ipObjectSpec(double,Minus,[Y],_)", "[Plus,Minus]");
		// not working:  Z is nan, ipObjectSpec(double,Nan,[Z],_), ...
		assertEquals("Fabricated +inf",
				bindings[0],
				new BasicTypeWrapper(new Double(Double.POSITIVE_INFINITY)) );
		assertEquals("Fabricated -inf",
				bindings[1],
				new BasicTypeWrapper(new Double(Double.NEGATIVE_INFINITY)) );
		// not working: assertEquals("Fabricated -inf",bindings[2],new BasicTypeWrapper(new Double(Double.NaN)) );
		// SWI crash: [junit] Assertion failed: (signbit(f1) != signbit(f2)), function compare_primitives, file pl-prims.c, line 1681.
	}
	@SuppressWarnings("null")
	public void testNaNetcFromJava(){
		Object[] objects = {new Double(Double.NaN),new Double(Double.NEGATIVE_INFINITY), new Double(Double.POSITIVE_INFINITY)};
		Object[] bindings = engine.deterministicGoal(
			"Objects=[D0,D1,D2]",
			"Objects",
			objects,
			//"[D0,D1,D2]"
			"[D1,D2]" // NaN 
			);
		assertTrue("Got a result",bindings!=null);
		// assertEquals("First result",bindings[0],objects[0]);
		assertEquals("Second result",bindings[0],objects[1]);
		assertEquals("Third result",bindings[1],objects[2]);
	}

}
