/* 
   Author: Miguel Calejo
   Contact: info@interprolog.com, www.interprolog.com
   Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
   Use and distribution, without any warranties, under the terms of the
   Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;
import com.declarativa.interprolog.util.*;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;
import java.lang.reflect.*;

/** A PrologEngine implemented over TCP/IP sockets. A SubprocessEngine object represents and gives access to a running Prolog process in background.
    Multiple instances correspond to multiple Prolog processes, outside the Java Virtual Machine. 
*/
public abstract class SubprocessEngine extends AbstractPrologEngine{

    private static final String STDOUT = "stdout";
    private static final String STDERR = "stderr";
    protected boolean outAndErrMerged;
    private boolean engineStarted = false;
    Process prolog;
    protected PrintWriter prologStdin;
    protected OutputHandler stdoutHandler, stderrHandler; // stderrHandler will be null if outAndErrMerged==true
    protected ServerSocket serverSocket;
    /** THE socket used for object streaming to/from Prolog */
    protected Socket socket;
    ServerSocket intServerSocket=null; protected Socket intSocket=null; // Used only for a particular way of interrupting Prolog
    String interruptCommand=null; // Used only for UNIX
    Vector<ClientRecognizer> listeners = new Vector<ClientRecognizer>();
    protected boolean available;
    Recognizer promptTrigger = peer.makePromptRecognizer();
    Recognizer breakTrigger = peer.makeBreakRecognizer();
    protected Recognizer errorTrigger = new Recognizer("++Error",true); // was "++Error: " for XSB 2.4
    boolean slowWindowsShutdown = false;
    protected boolean mustUseSocketInterrupt = false;

    protected RecognizerListener availableSetter = new RecognizerListener(){
            public void recognized(Recognizer source,Object extra,String originStd){
                boolean shouldFire = !isAvailable();
                available=true;
                if (shouldFire) fireAvailabilityChange();
                progressMessage("I'm available! source:"+source+" extra:"+extra);
            }
	};

	private boolean detectErrorMessages = true;            

    
    /**
	 * @return the detectErrorMessages
	 */
	public boolean isDetectErrorMessages() {
		return detectErrorMessages;
	}

	/** Some Prolog errors during deterministicGoal are not caught as exceptions, e.g. in XSB:
	 * ++Error[XSB]: [Runtime/C] Overflow in integer, returning MAX_INT
	 * ...during reads. So this engine (by default) looks for error markers too.
	 * @param detectErrorMessages false if you want to disable textual detection of errors
	 */
	public void setDetectErrorMessages(boolean detectErrorMessages) {
		this.detectErrorMessages = detectErrorMessages;
	}

	static class ClientRecognizer extends Recognizer implements RecognizerListener{
        PrologOutputListener client;
        ClientRecognizer(PrologOutputListener client){
            this.client=client;
            addRecognizerListener(this);
        }
        public void recognized(Recognizer source,Object extra,String originStd){
            // client.print((String)extra);
            if (originStd.equals(STDOUT))
            	client.printStdout((String)extra);
            else if (originStd.equals(STDERR))
            	client.printStderr((String)extra);
            else throw new IPException("Bad originStd:"+originStd);
        }
    }
    
    /**
     * Add a PrologOutputListener to this engine.  All stdout and stderr output will be routed to the client.
     * @param client An object interested in receiving messages depicting Prolog's progress
     * @see com.declarativa.interprolog.PrologOutputListener
     */
    public synchronized void addPrologOutputListener(PrologOutputListener client) {
        ClientRecognizer RE = new ClientRecognizer(client);
        listeners.addElement(RE);
        addPrologStdoutListener(RE);
        addPrologStderrListener(RE);
    }
	
    public synchronized void removePrologOutputListener(PrologOutputListener client){
        for (int i=0;i<listeners.size();i++) {
            ClientRecognizer cr = listeners.elementAt(i);
            if (cr.client==client) {
                listeners.removeElementAt(i);
                removePrologStdoutListener(cr);
                removePrologStderrListener(cr);
            }
        }
    }
	
    /** 
     * Add a OutputListener to get output from Prolog's standard output.
     * This is a lower level interface than addPrologOutputListener(PrologOutputListener).
     * @param client An object interested in Prolog's standard output
     * @see com.declarativa.interprolog.util.OutputListener
     */
    public void addPrologStdoutListener(OutputListener client){
        if (stdoutHandler!=null) stdoutHandler.addOutputListener(client);
    }
	
    public void addPrologStderrListener(OutputListener l){
        if (stderrHandler!=null)
            stderrHandler.addOutputListener(l);
    }
	
    public void removePrologStdoutListener(OutputListener l){
        if (stdoutHandler!=null) stdoutHandler.removeOutputListener(l);
    }
	
    public void removePrologStderrListener(OutputListener l){
        if (stderrHandler!=null)
            stderrHandler.removeOutputListener(l);
    }
    
    /** Set the character set to be assumed decoding Prolog's stream output, e.g. for Unicode:
     ?-  ipPrologEngine(E), java(E,setCharset(string('UTF-8'))).
     * 
     * @param charsetName
     * @see java.nio.charset.Charset
     */
    public void setCharset(String charsetName){
    	Charset C = Charset.forName(charsetName);
    	stdoutHandler.setCharset(C);
    	if (stderrHandler!=null) 
    		stderrHandler.setCharset(C);
    }
    /** Current engine stream charset name */
    public String getCharset(){
    	return stdoutHandler.getCharset().name();
    }
    
    /** For debugging purposes only */
    public void printPrologOutputToConsole(){
    	addPrologOutputListener(new PrologOutputListener() {
			
			@Override
			public void printStdout(String s) {
				System.out.print(s);
			}
			
			@Override
			public void printStderr(String s) {
				System.err.print(s);
			}
			
			@Override
			public void print(String s) {
				System.out.print(s);
			}
		});
    }
	
    /** For debugging purposes only 
     * e.g.:  ipPrologEngine(E), java(E,printPrologOutputToFile(string('/MyDir/xsb-output.txt'))).
     * @throws FileNotFoundException */
    public void printPrologOutputToFile(String F) throws FileNotFoundException{
    	final PrintStream ps = new PrintStream(new FileOutputStream(F));
    	addPrologOutputListener(new PrologOutputListener() {
			@Override
			public void printStdout(String s) {
				ps.print(s); ps.flush();
			}
			
			@Override
			public void printStderr(String s) {
				ps.print(ps); ps.flush();
			}
			
			@Override
			public void print(String s) {
				ps.println(s); ps.flush();
			}
		});
    }
	
    /** Construct a SubprocessEngine, launching a Prolog process in background.
     * @param prologCommands The command array to launch Prolog, as if given from a console shell.
     * Must not be null. First element will be the prolog executable, subsequent ones will be startup args for the Prolog engine
     * @param outAndErrMerged default (assumed in constructor variants) is true
     * @param debug If true this engine will send debugging messages to System.err
     * @see SubprocessEngine#shutdown
     * @see SubprocessEngine#teachMoreObjects(ObjectExamplePair[])
     * @see SubprocessEngine#setDebug(boolean)
     */
    protected SubprocessEngine(String[] prologCommands, boolean outAndErrMerged, boolean debug, boolean loadFromJar) {
        super((prologCommands==null?null:prologCommands[0]),debug,loadFromJar);
        this.outAndErrMerged = outAndErrMerged;
        // Let's make sure PrologEngines get their finalize() message when we exit
        if (System.getProperty("java.version").compareTo("1.3")>=0) {
            Runtime.getRuntime().addShutdownHook(new Thread("Subprocess shutdown"){
                    public void run(){
                        if (prolog!=null) prolog.destroy();
                    }
                });
        } else {
            // For JDK 1.2 - considered unsafe
            // To avoid seeing warnings about deprecated methods
            // call the following instead of
            // System.runFinalizersOnExit(true);
            try{
                Method finalizeOnExit = System.class.getMethod("runFinalizersOnExit",
                                                               new Class[]{boolean.class});
                finalizeOnExit.invoke(null,new Object[]{new Boolean(true)}); // for static methods first arg of invoke is ignored
            } catch (Exception e){
                System.err.println("Could not call runFinalizersOnExit"); 
            }
                
        }
            
        promptTrigger.addRecognizerListener(availableSetter);
        breakTrigger.addRecognizerListener(availableSetter);                	
    }
    /*
      public SubprocessEngine(String prologCommand, boolean debug){
      super(prologCommand,debug);
      }
	
      public SubprocessEngine(String startPrologCommand){
      super(startPrologCommand);
      }
	
      public SubprocessEngine(boolean debug){
      super(debug);
      }
	
      public SubprocessEngine(){
      super();
      }
    */
    protected void initSubprocess(String[] prologCommands){
        try {
            if (prologCommands==null) 
                prologCommands = new String[]{prologBinDirectoryOrCommand};
            else { // if prologCommand is just the Prolog base dir, make sure to obtain the executable path:
                prologBinDirectoryOrCommand = executablePath(prologCommands[0]);
                prologCommands[0] = prologBinDirectoryOrCommand;
            }

            prolog = createProcess(prologCommands);
            // No explicit buffering, because it's already being done by our Process's streams
            // If not, OutputHandler will handle the issue
            stdoutHandler = new OutputHandler(prolog.getInputStream(),(debug?System.err:null),STDOUT);
            if (!outAndErrMerged)
                stderrHandler = new OutputHandler(prolog.getErrorStream(),(debug?System.err:null),STDERR);
            setDetectPromptAndBreak(true);
            stdoutHandler.start();
            if (!outAndErrMerged)
                stderrHandler.start();
            Thread.yield(); // let's try to catch Prolog output ASAP
            prologStdin = new PrintWriter(prolog.getOutputStream());
			
            postCreateHack(prologCommands);
            loadInitialFiles();
			
            initSubprocessSocket();
			
            // waitUntilAvailable(); // Hangs Yap
            command("ipinitialize('"+clientHostname()+"',"+
                    serverSocket.getLocalPort()+","+
                    registerJavaObject(this)+","+
                    debug +
                    ")");
            progressMessage("Waiting for the socket to accept...");
            socket = serverSocket.accept();
			
            initSubprocess2();
            initSubprocess3();
			
            progressMessage("Ended SubprocessEngine constructor");
        } catch (IOException e){
            throw new IPException("Could not launch Prolog executable:"+e);
        }
    }

    protected void initSubprocessSocket() throws IOException {
        progressMessage("Allocating the ServerSocket...");
        serverSocket = new ServerSocket(0); // let the system pick a port
        progressMessage("server port: "+serverSocket.getLocalPort());
        // this does not seem to help with frequent failures on Windows
        //serverSocket.setSoTimeout(60000);   // wait for response for 1 min
    }

    protected void initSubprocess2() throws IOException {
        progressMessage("Teaching examples to Prolog...");
        PrologOutputObjectStream bootobjects = buildPrologOutputObjectStream(socket.getOutputStream());
        ObjectOutputStream oos = bootobjects.getObjectStream();
        teachIPobjects(oos);
        teachBasicObjects(oos); 
        bootobjects.flush();
        progressMessage("Sent all examples...");
        waitUntilAvailable();
        setupCallbackServer();
        interPrologFileLoaded = true;
        progressMessage("Exiting initSubprocess2");
    }

    protected void initSubprocess3() throws IOException {
        prepareInterrupt(clientHostname()); // OS-dependent Prolog interrupt generation, must be after the previous step
        waitUntilAvailable();            
        deterministicGoal("ipPrologEngine(_E), javaMessage(_E,setEngineStarted)");
        while(!engineStarted && !isIdle()) Thread.yield();
        //sendAndFlushLn("");
        waitUntilAvailable();
    }
	
    protected void postCreateHack(String[] prologCommands){}
	
    protected String clientHostname(){
        return "127.0.0.1"; // to avoid annoying Windows dialup attempt
    }
	
    public void setEngineStarted(){
        engineStarted = true;
        progressMessage("setEngineStarted!");
    }
	
    protected PrologOutputObjectStream buildPrologOutputObjectStream(OutputStream os) throws IOException{
        return new PrologOutputObjectStream(os);
    }
	
    protected Process createProcess(String[] prologCommands) throws IOException{
        progressMessage("Launching subprocess "+Arrays.toString(prologCommands));
        ProcessBuilder PB = new ProcessBuilder(prologCommands);
        PB.redirectErrorStream(outAndErrMerged);
        // return Runtime.getRuntime().exec(prologCommands);
        return PB.start();
    }
    public void setDebug(boolean debug){
        if (stdoutHandler!=null)
            stdoutHandler.setDebugStream(debug?System.err:null);
        if (stderrHandler!=null)
            stderrHandler.setDebugStream(debug?System.err:null);
        super.setDebug(debug);
    }
	
    /** Prolog is thought to be idle */
    public boolean isAvailable(){
        return available;
    }
	
    /* (non-Javadoc)
     * @see com.declarativa.interprolog.AbstractPrologEngine#waitUntilAvailable()
     */
    @Override
    public void waitUntilAvailable() {
        try{
            int pollCounter = 0;
            while(!isAvailable()) {
            	pollCounter ++;
                if (pollCounter > 1000 && prolog!=null){
                    // let's not do "system" things too often...
                    pollCounter = 0;
                    try{
                        int RC = prolog.exitValue();
                        throw new IPException("SubprocessEngine process exited unexpectedly:"+RC);
                    } catch(IllegalThreadStateException ex){
                        // We're still good
                    }
                }
            	Thread.sleep(0,1); // articulate this with pollCounter above
            }
        } catch (InterruptedException e){
            throw new IPException("Bad interrupt: "+e); 
        }

    }

    protected void setupCallbackServer(){
        prologHandler = new Thread(null,null,"Prolog handler" 
                                   /*,JAVA_STACK_SIZE platform dependend...if later used, the main thread also needs it*/){
                public void run(){
                    try{
                        while(!shutingDown) {
                            progressMessage("Waiting to receive object");
                            Object x = receiveObject();
                            progressMessage("Received object",x);
                            synchronized(this){
	                            Object y = handleCallback(x);
	                            progressMessage("Handled object and computed",y);
	                            if (y!=null) {
	                                //synchronized(this){
	                                    if (y instanceof GoalToExecute){
	                                        ((GoalToExecute)y).prologWasCalled();
	                                        ((GoalToExecute)y).setFirstGoalStatus(false); // Prolog sent us a message before this goal was sent via firstGoal...and that is carrying it over
	                                        y = ((GoalToExecute)y).getGoal();
	                                        sendObject(y);
	                                    } else if (y instanceof MessageExecuting){
	                    					Object yy = ((MessageExecuting)y).getResult();
	                    					sendObject(yy);
	                    					forgetMessage((MessageExecuting)y);
	                                    } else 
	                                    	throw new IPException("Inconsistency in setupCallbackServer:"+y);
	                                //}
	                            }
                            }
                        }
                    } 
                    catch (IOException e){
                        // If this happens, it means there was a communications error
                        // with prolog.  We have to abort all current goals so that
                        // calls are not wait()-ing forever.
                        if (!shutingDown) {
                            IPException toThrow = new PrologHaltedException("Prolog death detected in socket at setupCallbackServer, goal was "+
                                                                            (goalsToExecute.isEmpty()?"none!":goalsToExecute.lastElement().getGoal().getGoal()), 
                                                                            e);
                            SubprocessEngine.this.endAllTasks(toThrow);
                            available = false;
                        }
                    }
                    catch (Exception e){
                        IPException toThrow = new PrologHaltedException("Terrible exception in setupCallbackServer",e);
                        // The Prolog engine may not have died, but we're unable to communicate
                        SubprocessEngine.this.endAllTasks(toThrow);
                        available = false;
                        throw toThrow;
                    }
                    catch (Error e){
                	System.err.println("Obscure error:"+e);
                	System.err.println("Error cause:"+e.getCause());
                	//System.err.println("Its stack trace:");
                	//e.printStackTrace(System.out);
                	System.err.println("Stack traces for all threads follow:");
                	printAllStackTraces();
                        available = false;
                	throw e;
                    }
                }
            };
        progressMessage("Starting up callback service...");
        prologHandler.setName("Prolog handler");
        prologHandler.start();
    }
		
    protected Object receiveObject() throws IOException{
     	progressMessage("entering receiveObject()");
        Object x=null;
    	try{
            ObjectInputStream ios = new ObjectInputStream(socket.getInputStream());
            x = ios.readObject();
        } catch (ClassNotFoundException e){
            x = e;
        }
     	progressMessage("exiting receiveObject():"+x);
        return x;
    }
	
    protected void sendObject(Object y) throws IOException{
    	progressMessage("entering sendObject",y);
        PrologOutputObjectStream poos = 
            buildPrologOutputObjectStream(socket.getOutputStream());
        poos.writeObject(y);
        poos.flush(); // this actually writes to the socket stream
    	progressMessage("exiting sendObject",y);
    }
	
    /** Shuts down the background Prolog process as well as the dependent Java threads.
     */
    public synchronized void shutdown(){
        super.shutdown();
        boolean shouldFire = isAvailable();
        available=false;
        if (shouldFire) fireAvailabilityChange();
        if (stdoutHandler!=null) stdoutHandler.setIgnoreStreamEnd(true);
        if (stderrHandler!=null)
            stderrHandler.setIgnoreStreamEnd(true);
        if (shouldFire){
            doHalt();
            if (serverIsWindows()&&slowWindowsShutdown)
                try{Thread.sleep(500);} // seems useless after all...
                catch(InterruptedException ie){}
        }
        try{
            socket.close();
            serverSocket.close();
        }catch(IOException e) {throw new IPException("Problems closing sockets:"+e);}
        
        if(intServerSocket!=null){
            try {
                // closing sockets will stop them, no need to deprecate:
                // stdoutHandler.stop(); stderrHandler.stop(); cbhandler.stop();
                intSocket.close(); intServerSocket.close();
            }
            catch (IOException e) {throw new IPException("Problems closing sockets:"+e);}
            finally{
                if (!shouldFire || !serverIsWindows())  prolog.destroy(); // tries to avoid ugly messages on Windows
            } 
            // Might there be a reason to send "halt" to Prolog assynchronously? Or to delay shutdown and send it synchronously?
            // Assuming not.
        }
        else
            if (!shouldFire || !serverIsWindows()) prolog.destroy();

        prologHandler.interrupt(); // kills javaMessage/deterministicGoal thread
    }
	
    protected void doHalt(){
        realCommand("halt");
    }
		
    public void setSlowWindowsShutdown(){
        slowWindowsShutdown = true;
    }
	
    /** Kill the Prolog background process. If you wish to make sure this message is sent on exiting, 
	use System.runFinalizersOnExit(true) on initialization
    */
    protected void finalize() throws Throwable{
        if (prolog!=null) prolog.destroy();
    }
	
    protected void setDetectPromptAndBreak(boolean yes){
        if (yes==isDetectingPromptAndBreak()) return;
        if(yes){
            if (stdoutHandler!=null) stdoutHandler.addOutputListener(promptTrigger);
            if (stdoutHandler!=null) stdoutHandler.addOutputListener(breakTrigger);
            if (stderrHandler!=null){
                stderrHandler.addOutputListener(promptTrigger);
                stderrHandler.addOutputListener(breakTrigger);
            }
        } else{
            if (stdoutHandler!=null) stdoutHandler.removeOutputListener(promptTrigger);
            if (stdoutHandler!=null) stdoutHandler.removeOutputListener(breakTrigger);
            if (stderrHandler!=null){
                stderrHandler.removeOutputListener(promptTrigger);
                stderrHandler.removeOutputListener(breakTrigger);
            }
        }
    }
    protected boolean isDetectingPromptAndBreak(){
        return stdoutHandler!=null && stdoutHandler.hasListener(promptTrigger) /*&& stderrHandler.hasListener(promptTrigger)*/ &&
            stdoutHandler.hasListener(breakTrigger) /*&& stderrHandler.hasListener(breakTrigger)*/;
    }
	
    /** Sends a String to Prolog's input. Its meaning will naturally depend on the current state of Prolog: it can be
        a top goal, or input to an ongoing computation */
    public synchronized void sendAndFlush(String s){
        boolean shouldFire = isAvailable();
        available=false;
        if (shouldFire) fireAvailabilityChange();
        prologStdin.print(s); prologStdin.flush();
    }
	
    public void sendAndFlushLn(String s){
        sendAndFlush(s+nl);
    }
	
    protected void prepareInterrupt(String myHost) throws IOException{
        prepareInterrupt(myHost, 0);
    }
    /** we'll resort to  Unix signals, or to a XSB built-in
     * @param myHost
     * @param prologPID  if 0, query Prolog
     * @return new socket port for interrupts under Windows (0 otherwise)
     * @throws IOException
     */
    protected int prepareInterrupt(String myHost, int prologPID) throws IOException{ // requires successful startup steps
        int intPort = 0;
        if (serverIsWindows()){ 
            intServerSocket = new ServerSocket(0);
            intPort = intServerSocket.getLocalPort();
            if (prologPID==0)
                activateWindowsInterrupt(myHost, intPort,true);
            return intPort;
        } else {
            String prologPIDstr = prologPID+"";
            if (prologPID==0) {
                waitUntilAvailable();
                Object bindings[] = deterministicGoal("getPrologPID(N), ipObjectSpec('java.lang.Integer',Integer,[N],_)",
                                                      "[Integer]");
                if (bindings==null) throw new IPException("Could not find Prolog's PID");
                progressMessage("Found Prolog process ID");
                prologPIDstr = bindings[0].toString();
            }
			
            if (mustUseSocketInterrupt)
                interruptCommand = unixSimpleInterruptCommand(prologPIDstr);
            else
                interruptCommand = unixInterruptCommand(prologPIDstr);
            return intPort;
        }
    }

    protected void activateWindowsInterrupt(String myHost, int intPort, boolean doPrologSide)
        throws IOException {
        if (doPrologSide)
            command("setupWindowsInterrupt('"+myHost+"',"+intPort+")");
        intSocket = intServerSocket.accept();
        progressMessage("interrupt prepared, using socket "+intSocket);
    }
	
    /** ...although in some remote Unix scenarios we may use socket-based interrupts */
    protected String unixInterruptCommand(String PID){
        return unixSimpleInterruptCommand(PID);
    }
    protected String unixSimpleInterruptCommand(String PID){
        return "/bin/kill -s INT "+PID;
    }
	
    public static byte ctrl_c=3;
    public static byte[] ctrlc = {3};
    /** @see #prepareInterrupt(String) **/
    protected void doInterrupt(boolean wait, boolean killGoals, boolean abortEntirely){
    	boolean javaSideHack = false;
    	if (executingOnJavaSide()) {
    		Thread T = lastMessageRequest().getExecutor();
    		T.interrupt();
    		javaSideHack = true;
    	} else {
	        setDetectPromptAndBreak(true);
	        try {
	            if(mustUseSocketInterrupt||serverIsWindows()){
	                progressMessage("Attempting to interrupt Prolog...");
	                OutputStream IS = intSocket.getOutputStream();
	                if (serverIsWindows()){ // ...thus having the ctrl-C built in for Java interrupting...
	                    IS.write(ctrlc); 
	                    IS.flush();
	                } else{ // ...someone will receive this and exec the Unix command
	                    DataOutputStream dos = new DataOutputStream(IS);
	                    System.err.println("Piping interrupt:"+interruptCommand);
	                    dos.writeUTF(interruptCommand);
	                    dos.flush();
	                }
					
	            } else{
	                // we'll just use a standard UNIX signal
	                progressMessage("Interrupting Prolog with "+interruptCommand);
	                Runtime.getRuntime().exec(interruptCommand);
	            }
				
	        } 
	        catch(IOException e) {throw new IPException("Exception in interrupt():"+e);}
    	}
        if (killGoals)
            interruptTasks(); // kludge
        if (wait && !javaSideHack)
            waitUntilAvailable();
        if (abortEntirely && !javaSideHack)
            finishInterrupt(wait);
        progressMessage("Leaving doInterrupt");
    }
	
    protected void finishInterrupt(boolean wait){
        abortEngine();
    }
	
    /** Pause the computation and enter a break (sub shell) state */
    public void breakEngine(){
        // clean up engine controller
        if (getThePrologListener() instanceof EngineController)
            ((EngineController)getThePrologListener()).interruptCleanupHack();
        //System.err.println("Breaking...");
        interrupting=true;
        doInterrupt(false,false,false);
        interrupting=false;
    }
	
    /** Assumes the engine is in a break state */
    public void resumeEngine(){
        sendAndFlushLn("end_of_file.");
    }
	
    /** Assumes the engine is in a break state */
    public void abortEngine(){
        sendAndFlushLn("abort.");
    }

    /** This implementation may get stuck if the command includes variables, because the Prolog
	top level interpreter may offer to compute more solutions; use variables prefixed with '_' */
    public boolean realCommand(String s){
        progressMessage("COMMAND",s); // not displaying the "." to avoid building a new Java string here...
        sendAndFlushLn(s+".");
        return true; // we do not really know
    }
	
    public Object[] deterministicGoal(String G, String OVar, Object[] objectsP, String RVars){
        // System.out.println("deterministicGoal "+G+" in:"); System.out.println(Thread.currentThread()); System.out.println(this);
        // No Prolog threads being used by InterProlog in this version, so if necessary let's wait until...
        while(!canAcceptNewGoal(Thread.currentThread())) 
            Thread.yield();
        //TODO: further wait() / synchronization still missing here... the initial segments of the next 
        // two methods should be synchronized
        // Possibly related, currently (Oct 9, 2015) a warning at XJ's wiki: 
        /* when using the Studio listener, if you call a Prolog goal which creates lazy Java UI objects 
         * (e.g. non eager trees and lists, that paint themselves by lazily fetching data from Prolog) 
         * do NOT include (non anonymous) variables in your goal: if you do, the Prolog top level will 
         * pause after showing your variable bindings, thus forbidding (pending, e.g. AWT thread) Java 
         * deterministicGoals to evaluate. The system will deadlock and you will have to kill it. 
         * Killing just the Prolog subprocess may allow you to save some work, but possibly not.
         * UPDATE: XJ now calls XJDesktop.waitForSwing to prevent this, but the pattern may occur
         * with user (non XJ) containers
         */
        if (isIdle() || (detectsPauses && isPaused() && messagesExecuting.isEmpty())) 
            // ...we may be lacking some synchronization here
            return firstGoal(G, OVar, objectsP, RVars);
        else 
            return super.deterministicGoal(G, OVar, objectsP, RVars);
    }
	
    /** Very alike deterministicGoal except that it sends the initial GoalFromJava object over the socket */
    protected Object[] firstGoal(String G, String OVar, Object[] objectsP, String RVars) {

    	topGoalHasStarted = true;
    	Object[] resultToReturn=null;
        GoalToExecute goalToDo;
        ResultFromProlog result;
        int mytimestamp = incGoalTimestamp();
        boolean wasAvailable = isAvailable();
        available=false;
        if (wasAvailable) fireAvailabilityChange();
        try{
            GoalFromJava GO = makeDGoalObject(G, OVar, objectsP, RVars, mytimestamp);
            progressMessage("Prepared GoalFromJava",GO);
            progressMessage("Schedulling (first) goal ",G);
            goalToDo = new GoalToExecute(GO);
            
            goalToDo.setFirstGoalStatus(true);
            
            RecognizerListener errorHandler = null;
            if (detectErrorMessages)
            	errorHandler = setupErrorHandling(goalToDo);
            synchronized(this){
            	scheduleGoal(goalToDo);
                goalToDo.prologWasCalled();
                realCommand(deterministicGoalString(mytimestamp)); // asynchronous
               	sendObject(GO);
                // doesn't solve it... sendAndFlushLn(""); // needed for break modes??
            }
            //pushDGthread(goalToDo.getCallerThread());
            //System.out.println("CALLING waitForResult (Subprocess):"+this);
            result = goalToDo.waitForResult();
            // System.out.println("result.succeeded for "+G+": "+result.succeeded);
            if (errorHandler!=null)
            	removeErrorHandling(errorHandler);
            lastSolutionWasUndefined = result.undefined;
            progressMessage("firstGoal - Got result for ",goalToDo);
            // goalToDo is forgotten by handleCallback
            if (result.succeeded)
                resultToReturn = result.rVars;
            // so we can dispense with prompt recognition insofar as firstGoal goes; 
            //but consider the case where a Prolog user goal was injected before
            available = wasAvailable; 
            fireAvailabilityChange();
        } catch (IPException e) {
            throw e;
        } catch (SocketException e) {
            if (shutingDown) throw new UnavailableResultException("Goal was "+G);
            else throw new IPException("Problem in deterministicGoal:"+e);
        } catch (Exception e) {
            throw new IPException("Problem in deterministicGoal:"+e);
        } finally{
            topGoalHasStarted = false; // is this OK? this assumes no initiative from the Prolog side, which is probably correct
            //removeErrorHandling();
            progressMessage("Leaving firstGoal for ",G);
        }
        if (goalToDo.wasAborted()) {
            if (shutingDown) throw new UnavailableResultException("IP aborted goal was "+G);
            else throw new IPAbortedException(G+" was aborted");
        }
        if (goalToDo.wasInterrupted()) throw new IPInterruptedException(G+" was interrupted");
        if (result.wasInterrupted(this)) {
        	while(interrupting) Thread.yield(); // Let the interrupt request be fully served
            throw new IPInterruptedException(G+" was interrupted, Prolog detected:\n"+result.error); 
        }
        // if (result.error!=null) throw new IPException (result.error.toString());
        if (result.error!=null) {
            if (result.error instanceof IPException) throw (IPException)result.error;
            else {
                if (result.error.equals(META_SYNTAX_ERROR)){
                    // Let's be drastic
                    endAllJavaMessages(new IPException("Meta system abort"));
                    endAllTasks(new IPException("Meta system abort"));
                    //System.err.println("META_SYNTAX_ERROR");
                    return resultToReturn; // more useful in general than the following:
                }
                else throw new IPPrologError(result.error /*+" in goal "+G*/);
            }
        }
        if (result.timestamp!=mytimestamp)
            throw new IPException("bad timestamp in deterministicGoal, got "+result.timestamp+" instead of "+goalTimestamp);
        return resultToReturn;
    }
    
    final String META_SYNTAX_ERROR = "\001"+" Syntax error, may be from a previous listener goal";
    
    /** Depending on the engine mode (e.g. Prolog toploop, Flora shell...) we may need to wrap the Prolog goal into something more
     * @param mytimestamp */
    protected String deterministicGoalString(int mytimestamp){
    	return "interprolog:deterministicGoal("+mytimestamp+")";
    }

    protected Object doSomething(){
        if (onlyFirstGoalSchedulled()) return null;
        else return super.doSomething();
    }
	
    protected synchronized boolean onlyFirstGoalSchedulled(){
        return isIdle() || (messagesExecuting.size()==0 && goalsToExecute.size()==1 && 
                            ((GoalToExecute)goalsToExecute.elementAt(0)).isFirstGoal());
    }
	
    // deterministicGoal helpers
	
    protected RecognizerListener setupErrorHandling(final GoalToExecute goalToDo){
        setDetectPromptAndBreak(false);
        RecognizerListener errorHandler = new RecognizerListener(){
                public void recognized(Recognizer source,Object extra,String originStd){
                    goalToDo.setResult(new ResultFromProlog(goalToDo.getTimestamp(), false, 0, META_SYNTAX_ERROR, false)); 
                }
            };
        errorTrigger.addRecognizerListener(errorHandler);
        if (stderrHandler!=null)
            stderrHandler.addOutputListener(errorTrigger); 
        if (stdoutHandler!=null) stdoutHandler.addOutputListener(errorTrigger); // needed because some engines may fuse stdout and stderr
        return errorHandler;
    }
    
    protected void removeErrorHandling(RecognizerListener errorHandler){
    	errorTrigger.removeRecognizerListener(errorHandler);
    	if (stderrHandler!=null)
            stderrHandler.removeOutputListener(errorTrigger);
    	if (stdoutHandler!=null) stdoutHandler.removeOutputListener(errorTrigger);
        setDetectPromptAndBreak(true);
    }
    
    /** Useful for testing */
    public static class OutputDumper implements PrologOutputListener{
        Writer w; 
        boolean available = true;
        String filename;
        public OutputDumper(String filename){
            this.filename=filename;
            try {
                w = new FileWriter(filename);
            } catch (IOException ioe){
                throw new RuntimeException("Failed to create dumper:"+ioe);
            }
        }
        public void print(String s){
            try{
                if (available) w.write(s); // Do not flush, might affect performance measurement
                else System.err.println("Lost output to file "+filename+":"+s);
            } catch (IOException ioe){
                throw new RuntimeException("Failed to write dumper:"+ioe);
            }
        }
        public void close(){
            try{
                w.close();
                available = false;
            } catch (IOException ioe){
                throw new RuntimeException("Failed to close dumper:"+ioe);
            }
        }
        @Override
        public void printStdout(String s) {
        }
        @Override
        public void printStderr(String s) {			
        }
    }
}
