/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2015
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.gui;

import com.declarativa.interprolog.GNUSubprocessEngine;

@SuppressWarnings("serial")
public class GNUSubprocessEngineWindow extends SubprocessEngineWindow{
	public GNUSubprocessEngineWindow(GNUSubprocessEngine e){
		super(e);
	}
	public GNUSubprocessEngineWindow(GNUSubprocessEngine e,boolean autoDisplay){
		super(e,autoDisplay);
	}	
	/** Useful for launching the system, by passing the full Prolog executable path and 
	optionally extra arguments, that are passed to the Prolog command */
	public static void main(String[] args){
		commonMain(args);
		new GNUSubprocessEngineWindow(new GNUSubprocessEngine(prologStartCommands,debug,loadFromJar));
	}
}