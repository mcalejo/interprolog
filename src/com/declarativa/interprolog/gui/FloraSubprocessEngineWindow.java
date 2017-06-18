package com.declarativa.interprolog.gui;

import com.declarativa.interprolog.FloraSubprocessEngine;

@SuppressWarnings("serial")
public class FloraSubprocessEngineWindow extends XSBSubprocessEngineWindow {
	public FloraSubprocessEngineWindow(FloraSubprocessEngine e){
		this(e,true);
	}
	public FloraSubprocessEngineWindow(FloraSubprocessEngine e,boolean autoDisplay){
		this(e,autoDisplay,true);
	}	
	public FloraSubprocessEngineWindow(FloraSubprocessEngine e,boolean autoDisplay,boolean mayExitApp){
		super(e,autoDisplay,mayExitApp);
        setTitle("FloraSubprocessEngine listener ("+e.getFloraVersion()+")");
	}	
	/** Useful for launching the system, by passing the full Prolog executable path and 
	optionally extra arguments, that are passed to the Prolog command */
	public static void main(String[] args){
		commonMain(args);
		FloraSubprocessEngine e = new FloraSubprocessEngine(prologStartCommands,null,true,debug,loadFromJar);
		new FloraSubprocessEngineWindow(e,true,false);
	}
}
