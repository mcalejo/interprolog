/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2015
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;
/** A PrologEngine encapsulating a <a href='http://www.swi-prolog.org/'>SWI Prolog</a> engine, accessed over TCP/IP sockets. 
*/
public class SWISubprocessEngine extends SubprocessEngine{
    protected PrologImplementationPeer makeImplementationPeer(){
    	return new SWIPeer(this);
    }
    public SWISubprocessEngine(String[] prologCommands, boolean outAndErrMerged, boolean debug, boolean loadFromJar){
    	super(prologCommands, outAndErrMerged, debug, loadFromJar);
    	nl=System.getProperty("line.separator") ;
    	fileModulePrefix = "user:";
    	initSubprocess(prologCommands);
    }
    public SWISubprocessEngine(String[] prologCommands, boolean debug, boolean loadFromJar){
    	this(prologCommands, true, debug, loadFromJar);
    }
    public SWISubprocessEngine(String[] prologCommands, boolean debug){
    	this(prologCommands, true, debug, true);
    }
    public SWISubprocessEngine(String[] prologCommands){
    	this(prologCommands,false);
    }
    public SWISubprocessEngine(String prologCommand, boolean debug){
    	this(new String[]{prologCommand}, debug);
    }
    public SWISubprocessEngine(String prologCommand){
    	this(new String[]{prologCommand});
    }
    public SWISubprocessEngine(boolean debug){
    	this((String[])null,debug);
    }
    /** @see com.declarativa.interprolog.AbstractPrologEngine#AbstractPrologEngine(String prologBinDirectoryOrCommand, boolean debug, boolean loadFromJar)*/
    public SWISubprocessEngine(){
    	this(false);
    }	
    public boolean realCommand(String s){
		progressMessage("COMMAND:"+s+".");
		// fails to make sure SWI doesn't hang showing variables; 
		// prints prompt because SWI (apparently from version 5.4.x onwards) will print it only when its input stream is 'user'
		sendAndFlushLn("("+s+"), write('"+SWIPeer.REGULAR_PROMPT+"'), ttyflush, !."); 
		sendAndFlushLn(nl); // to ignore further solutions
		return true; // we do not really know
	}
	protected String add_lib_goalString(){
		return "_dummy = "; // hack to cancel default for XSB
	}
	protected void loadInitialFiles(){
		super.loadInitialFiles();
		command("assert(ipInterprologModule('"+unescapedFilePath(interprologPath)+"'))");
	}
    /** Assumes the engine is in a (tracer) break state */
    public void abortEngine(){
        sendAndFlushLn("a");
    }

	/** VERY Unsafe method: does NOT make the engine unavailable (as YAP's prompts do no appear) */
    public synchronized void sendAndFlush(String s){
        prologStdin.print(s); prologStdin.flush();
    }
}


