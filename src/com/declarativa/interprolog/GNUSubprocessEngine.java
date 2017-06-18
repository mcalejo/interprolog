/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2015
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;
/** A PrologEngine encapsulating a <a href='http://www.gprolog.org/'>GNU Prolog</a> engine, accessed over TCP/IP sockets. 
*/
public class GNUSubprocessEngine extends SubprocessEngine{
    protected PrologImplementationPeer makeImplementationPeer(){
    	return new GNUPeer(this);
    }
    public GNUSubprocessEngine(String[] prologCommands, boolean outAndErrMerged, boolean debug, boolean loadFromJar){
    	super(prologCommands, outAndErrMerged, debug, loadFromJar);
    	nl=System.getProperty("line.separator") ;
    	initSubprocess(prologCommands);
    }
    public GNUSubprocessEngine(String[] prologCommands, boolean debug, boolean loadFromJar){
    	this(prologCommands, true, debug, loadFromJar);
    }
    public GNUSubprocessEngine(String[] prologCommands, boolean debug){
    	this(prologCommands, true, debug, true);
    }
    public GNUSubprocessEngine(String[] prologCommands){
    	this(prologCommands,false);
    }
    public GNUSubprocessEngine(String prologCommand, boolean debug){
    	this(new String[]{prologCommand}, debug);
    }
    public GNUSubprocessEngine(String prologCommand){
    	this(new String[]{prologCommand});
    }
    public GNUSubprocessEngine(boolean debug){
    	this((String[])null,debug);
    }
    /** @see com.declarativa.interprolog.AbstractPrologEngine#AbstractPrologEngine(String prologBinDirectoryOrCommand, boolean debug, boolean loadFromJar)*/
    public GNUSubprocessEngine(){
    	this(false);
    }	
	protected String add_lib_goalString(){
		return "_dummy = "; // hack to cancel default for XSB
	}
    /** Assumes the engine is in a (tracer) break state */
    public void abortEngine(){
        sendAndFlushLn("a");
    }
}


