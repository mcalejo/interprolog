/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;

/** A PrologEngine encapsulating a <a href='http://xsb.sourceforge.net'>XSB Prolog</a> engine, accessed over TCP/IP sockets. 
*/
public class XSBSubprocessEngine extends SubprocessEngine{
    protected PrologImplementationPeer makeImplementationPeer(){
    	return new XSBPeer(this);
    }
    public XSBSubprocessEngine(String[] prologCommands, boolean outAndErrMerged, boolean debug, boolean loadFromJar){
    	super(prologCommands, outAndErrMerged, debug, loadFromJar);
    	nl=System.getProperty("line.separator") ;
    	initSubprocess(prologCommands);
    }
    public XSBSubprocessEngine(String[] prologCommands, boolean debug, boolean loadFromJar){
    	this(prologCommands, true, debug, loadFromJar);
    }
    public XSBSubprocessEngine(String[] prologCommands, boolean debug){
    	this(prologCommands, true, debug, true);
    }
    public XSBSubprocessEngine(String[] prologCommands){
    	this(prologCommands,false);
    }
    public XSBSubprocessEngine(String prologCommand, boolean debug){
    	this(new String[]{prologCommand}, debug);
    }
    public XSBSubprocessEngine(String prologCommand){
    	this(new String[]{prologCommand});
    }
    public XSBSubprocessEngine(boolean debug){
    	this((String[])null,debug);
    }
    /** @see com.declarativa.interprolog.AbstractPrologEngine#AbstractPrologEngine(String prologBinDirectoryOrCommand, boolean debug, boolean loadFromJar)*/
    public XSBSubprocessEngine(){
    	this(false);
    }	
}


