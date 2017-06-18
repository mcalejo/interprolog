/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2015
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;

/** A PrologEngine encapsulating a <a href='http://www.ncc.up.pt/~vsc/Yap/'>YAP Prolog</a> engine, accessed over TCP/IP sockets. 
*/
public class YAPSubprocessEngine extends SubprocessEngine{
    protected PrologImplementationPeer makeImplementationPeer(){
    	return new YAPPeer(this);
    }
    public YAPSubprocessEngine(String[] prologCommands, boolean outAndErrMerged, boolean debug, boolean loadFromJar){
    	super(prologCommands, outAndErrMerged, debug, loadFromJar);
    	nl=System.getProperty("line.separator") ;
    	fileModulePrefix = "user:";
    	initSubprocess(prologCommands);
    }
    public YAPSubprocessEngine(String[] prologCommands, boolean debug, boolean loadFromJar){
    	this(prologCommands, true, debug, loadFromJar);
    }
    public YAPSubprocessEngine(String[] prologCommands, boolean debug){
    	this(prologCommands, true, debug, true);
    }
    public YAPSubprocessEngine(String[] prologCommands){
    	this(prologCommands,false);
    }
    public YAPSubprocessEngine(String prologCommand, boolean debug){
    	this(new String[]{prologCommand}, debug);
    }
    public YAPSubprocessEngine(String prologCommand){
    	this(new String[]{prologCommand});
    }
    public YAPSubprocessEngine(boolean debug){
    	this((String[])null,debug);
    }
    /** @see com.declarativa.interprolog.AbstractPrologEngine#AbstractPrologEngine(String prologBinDirectoryOrCommand, boolean debug, boolean loadFromJar)*/
    public YAPSubprocessEngine(){
    	this(false);
    }	
    public boolean realCommand(String s){
		progressMessage("COMMAND:"+s+".");
		sendAndFlushLn("("+s+"), write('"+YAPPeer.REGULAR_PROMPT+"'), flush_output, !."); 
		sendAndFlushLn(nl); // to ignore further solutions (and not hang the top level...)
		return true; // we do not really know
	}
    /* Use SIG_USR1 to interrupt YAP. Why?
     * kill -s INT PID works fine from another Mac shell window... but in our context we get: "YAP exiting: cannot handle signal 2"
     */
    protected String unixSimpleInterruptCommand(String PID){
        return "/bin/kill -s USR1 "+PID;
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
        // sendAndFlushLn("a");
    }

	/** VERY Unsafe method: does NOT make the engine unavailable (as YAP's prompts do no appear) */
    public synchronized void sendAndFlush(String s){
        prologStdin.print(s); prologStdin.flush();
    }
}
