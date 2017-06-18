/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.gui;
import com.declarativa.interprolog.*;

@SuppressWarnings("serial")
public class XSBSubprocessEngineWindow extends SubprocessEngineWindow{
	public XSBSubprocessEngineWindow(XSBSubprocessEngine e){
		super(e);
	}
	public XSBSubprocessEngineWindow(XSBSubprocessEngine e,boolean autoDisplay){
		super(e,autoDisplay);
	}	
	public XSBSubprocessEngineWindow(XSBSubprocessEngine e,boolean autoDisplay,boolean mayExitApp){
		super(e,autoDisplay,mayExitApp);
	}	
	/** Useful for launching the system, by passing the full Prolog executable path and 
	optionally extra arguments, that are passed to the Prolog command */
	public static void main(String[] args){
		commonMain(args);
		XSBSubprocessEngine e = new XSBSubprocessEngine(prologStartCommands,debug,loadFromJar);
		new XSBSubprocessEngineWindow(e);
	}
}