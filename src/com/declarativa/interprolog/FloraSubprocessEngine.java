/* 
   Author: Miguel Calejo
   Contact: info@interprolog.com, www.interprolog.com
   Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
   Use and distribution, without any warranties, under the terms of the
   Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;
import java.io.File;
import java.util.ArrayList;

import com.declarativa.interprolog.util.GoalToExecute;
import com.declarativa.interprolog.util.IPException;
import com.declarativa.interprolog.util.Recognizer;
import com.declarativa.interprolog.util.RecognizerListener;

/** A PrologEngine encapsulating a <a href='http://xsb.sourceforge.net'>XSB Prolog</a> engine, accessed over TCP/IP sockets, and running the Flora2 shell. 
    Note that deterministicGoal and nonDeterministicGoal call Prolog goals, not Flora goals. It also supports Ergo, a proprietary Flora variant from
    Coherent Knowledge Systems.
    Unlike Flora2's standard Java API, this class allows both the interactive use of the Flora2/Ergo shell and programmatic usage with the javaMessage/java 
    Prolog predicates and deterministicGoal and related methods. It also allows shifting from the Flora2 shell to Prolog's shell and back.
*/
public class FloraSubprocessEngine extends XSBSubprocessEngine{
    private boolean inFloraShell = false;
    private boolean floraShellStarted = false;
    protected static final String SUFFIX_FOR_PROLOGALL = "@\\prologall";
    protected static final String SUFFIX_FOR_PROLOG = "@\\prolog";
    // TODO: we should actually have a recognizer that strips output from the Flora preprocessor etc.:
    protected Recognizer floraPromptTrigger; 
    /** A safer complement to 
        @see com.declarativa.interprolog.SubprocessEngine.errorTrigger */
    protected Recognizer errorTrigger2 = new Recognizer("\001"+"E"); // Ergo provides this in Studio mode 
    /** whether Flora/Ergo shows loading messages etc. */
    protected boolean verbose = true;
    protected boolean quietDeterministicGoals;
    protected String floraDirectory;
    protected String floraReleaseDate = null, floraVersion = null, floraBuild=null;
    private boolean ergo;
    public static final String FLORA_AUX_BASE = ".flora_aux_files/";
    public static final String ERGO_AUX_BASE = ".ergo_aux_files/";
    public static final String ERGOTEXT_EXTENSION = ".ergotxt";
	
    public boolean isQuietDeterministicGoals() {
        return quietDeterministicGoals;
    }
    /** Determines whether Flora/Ergo standard output (such as "Yes", variable bindings etc) is produced during deterministicGoal
     * @param quietDeterministicGoals
     */
    public void setQuietDeterministicGoals(boolean quietDeterministicGoals) {
        this.quietDeterministicGoals = quietDeterministicGoals;
    }
    /** 
	@see com.declarativa.interprolog.AbstractPrologEngine fj_floraSupported is asserted on the Prolog side
        * @param prologCommands
        * @param floraDirectory
        * @param outAndErrMerged 
        * @param debug
        * @param loadFromJar
        */
    public FloraSubprocessEngine(String[] prologCommands, String floraDirectory, boolean outAndErrMerged, boolean debug, boolean loadFromJar){
    	super(prologCommands, outAndErrMerged, debug, loadFromJar);
    	if (!interPrologFileLoaded)
            throw new IPException("Bad initialization of FloraSubprocessEngine");
    	if (floraDirectory==null) floraDirectory = findInterPrologProperty("FLORADIR");
    	if (floraDirectory==null) throw new IPException("Could not find FLORADIR");
    	this.floraDirectory = floraDirectory;
    	quietDeterministicGoals = true;
    	ArrayList<String> commands = new ArrayList<String>();
        floraPromptTrigger = new Recognizer("\001\001");
    	commands.add("add_lib_dir(a('"+unescapedFilePath(floraDirectory)+"')), [flora2]");
    	commands.add("flora2:add_flora_configuration_dir(installdir,'"+unescapedFilePath(floraDirectory)+"')");
    	if (!command(commands))
            throw new IPException("Could not initialize Flora layer"); 
    	String[] moreCommands = new String[]{
            //"flora2:add_flora_configuration_dir(installdir,'"+unescapedFilePath(floraDirectory)+"')",
            "retractall(flrregistry:flora_configuration(promptctl,_)), assert(flrregistry:flora_configuration(promptctl,yes)), assert(fj_floraSupported)"
    	};
    	if (!command(moreCommands))
            throw new IPException("Could not patch Flora's installdir"); 
    	// This needs to come after as it depends on flora2 being loaded:
    	if (isErgo(floraDirectory)) {
            ergo = true;
            // floraPromptTrigger = new Recognizer("ergo>");
            // Now defind in interprolog.P: command("asserta(fjIsErgo(yes))");
    	} else{
            ergo=false;
            // Now defind in interprolog.P: command("asserta((fjIsErgo(no)))");
            // floraPromptTrigger = new Recognizer("flora2 ?-"); 
    	}

        if (!outAndErrMerged)
            stderrHandler.addOutputListener(floraPromptTrigger);
        stdoutHandler.addOutputListener(floraPromptTrigger);
    	setFloraShell(false);
    	waitUntilAvailable();
        realCommand("ipPrologEngine(?_E)"+SUFFIX_FOR_PROLOGALL+", javaMessage(?_E,setFloraShellStarted)"+SUFFIX_FOR_PROLOGALL);
    	while(!floraShellStarted || !isIdle() /*messagesExecuting.size()>0*/) Thread.yield();
    	waitUntilAvailable();
    }
    /**
     * @param prologCommands
     * @param debug
     * @param loadFromJar
     */
    public FloraSubprocessEngine(String[] prologCommands, boolean debug, boolean loadFromJar){
    	this(prologCommands, null, true, debug, loadFromJar);
    }
    public FloraSubprocessEngine(String[] prologCommands, boolean debug){
    	this(prologCommands, debug,true);
    }
    public FloraSubprocessEngine(String[] prologCommands, String floraDirectory){
    	this(prologCommands,floraDirectory,true,false,true);
    }
    public FloraSubprocessEngine(String[] prologCommands){
    	this(prologCommands,false);
    }
    public FloraSubprocessEngine(boolean debug){
    	this(null,debug);
    }
    /** @see com.declarativa.interprolog.AbstractPrologEngine#AbstractPrologEngine(String prologBinDirectoryOrCommand, boolean debug, boolean loadFromJar)
     */
    public FloraSubprocessEngine(){
    	this(false);
    }	
    public static boolean isErgo(String floraDirectory){
    	File hooks = new File(floraDirectory,"ergoisms");
    	return new File(hooks,"ergo.switch").exists();
    }
    /** This Flora2 engine happens to also be an Ergo engine. If true, the Prolog side will have a fact fjIsErgo(yes).
     * @return whether it's an Ergo engine
     */
    // This would be more informative, but we need the info before starting Flora/Ergo:
    //   deterministicGoal("flrporting:flora_running_as(ergo)")
    public boolean isErgo(){
    	return ergo;
    }
    /**
     * @return the more advanced language supported
     */
    public String getLanguage(){
    	return isErgo()?"Ergo":"Flora2";
    }
    /**
     * 
     */
    public void setFloraShellStarted(){
    	floraShellStarted = true;
    }
    protected String deterministicGoalString(int timestamp){
    	if (inFloraShell) 
            if (quietDeterministicGoals)
                //return "\\notrace,feedback{tempoff},prompt{tempoff},deterministicGoal("+timestamp+")"+SUFFIX_FOR_PROLOGALL;
                return "flora_studio_probe(deterministicGoal("+timestamp+")" + SUFFIX_FOR_PROLOG + ")"+SUFFIX_FOR_PROLOG + "(flrutils)";
            else
                //return "\\notrace,deterministicGoal("+timestamp+")"+SUFFIX_FOR_PROLOGALL;
                return "flora_studio_probe_fdbk(deterministicGoal("+timestamp+")" + SUFFIX_FOR_PROLOG + ")"+SUFFIX_FOR_PROLOG + "(flrutils)";
    	else
            return super.deterministicGoalString(timestamp);
    }
    
    @Override
    protected RecognizerListener setupErrorHandling(GoalToExecute goalToDo) {
    	RecognizerListener errorHandler = super.setupErrorHandling(goalToDo);
    	if (errorTrigger2!=null){
            errorTrigger2.addRecognizerListener(errorHandler);
            if (stderrHandler!=null)
                stderrHandler.addOutputListener(errorTrigger2); 
            stdoutHandler.addOutputListener(errorTrigger2); // needed because some engines may fuse stdout and stderr
    	}
        return errorHandler;
    }
    @Override
    protected void removeErrorHandling(RecognizerListener errorHandler) {
    	if (errorTrigger2!=null){
            errorTrigger2.removeRecognizerListener(errorHandler);
            if (stderrHandler!=null)
                stderrHandler.removeOutputListener(errorTrigger2);
            stdoutHandler.removeOutputListener(errorTrigger2);
    	}
        super.removeErrorHandling(errorHandler);
    }
	
    // second time we don't know enough to recognize availability, so...
    public void setFloraShell(){
    	setFloraShell(true);
    }
    private void setFloraShell(boolean assumeAvailable){
    	if (!inFloraShell){
            floraPromptTrigger.addRecognizerListener(availableSetter);
            inFloraShell = true;
            String FLSHELL = (isErgo()?"flora2:ergo_shell":"flora2:flora_shell");
            realCommand(FLSHELL+"\n"); sendAndFlush(nl);
            if (assumeAvailable) {
                boolean shouldFire = !isAvailable();
                available=true;
                if (shouldFire) fireAvailabilityChange();
            }
    	}
    }
    /**
     * 
     */
    public void setPrologShell(){
    	if (inFloraShell){
            inFloraShell = false;
            floraPromptTrigger.removeRecognizerListener(availableSetter);
            realCommand("flora_end_internal@\\plg"); // sendAndFlush(nl);
    	}
    }
    /** We know the engine to be in the Flora shell, so Prolog goals need a Flora suffix
     * @return true if in Flora/Ergo, otherwise Prolog
     */
    public boolean inFloraShell(){
    	return floraShellStarted && inFloraShell;
    }
    /** We know the engine to be in the Prolog shell, so Prolog goals do not need a postfix */
    /* (non-Javadoc)
     * @see com.declarativa.interprolog.AbstractPrologEngine#inPrologShell()
     */
    public boolean inPrologShell(){
    	return !inFloraShell;
    }
    /** Sets the Flora/Ergo engines's chatter level
     * @param verbose
     */
    public void setVerbose(boolean verbose){
    	if (verbose==this.verbose) return;
    	String G;
    	// too strong: flrporting:flora_set_banner_control(nofeedback,1), 
    	if (verbose) G = "flrutils:flora_chatter_control(on), flrporting:flora_set_banner_control(nobanner,0), flrporting:flora_set_banner_control(quietload,0)";
    	else G = "flrutils:flora_chatter_control(on), flrporting:flora_set_banner_control(nobanner,1), flrporting:flora_set_banner_control(quietload,1)";
    	if (!command(G)) throw new RuntimeException("failed to change verbosity");
    	this.verbose = verbose;
    }
    
    public boolean isVerbose(){ return verbose; }
    
    /** Before delegating to superclass, changes the mode to Prolog shell, where the interrupt will take us */
    /*
      protected synchronized void doInterrupt(){
      if (inFloraShell){
      inFloraShell = false;
      floraPromptTrigger.removeRecognizerListener(availableSetter);
      }
      super.doInterrupt();
      }*/
    protected void doHalt(){
        if (inFloraShell)
            realCommand("\\halt");
        else super.doHalt();
    }	
	
    protected void finishInterrupt(boolean wait){
        if (inFloraShell){
            abortEngine();
            if (wait)
                waitUntilAvailable();
        }
    }
    public void interrupt(){
        interrupt(false); // hack so ErgoStudio does not hang when aborting on incomplete
    }
    /** Assumes the engine is in a break state */
    public void resumeEngine(){
        sendAndFlushLn("\\resume.");
    }
	
    /** Assumes the engine is in a break state */
    public void abortEngine(){
        sendAndFlushLn("\\toplevel.");
    }
		
    /** Execute a Prolog "command; returns true if it succeeds"
     *  @see com.declarativa.interprolog.AbstractPrologEngine#command(java.lang.String)
     */
    public boolean command(String s){
        return super.command(s);
    }
    /** Execute a Flora/Ergo "command"
     * @param s the command
     * @return true if it succeeds
     */
    public boolean floraCommand(String s){
        return floraDeterministicGoal(s);
    }
    public static boolean isFloraSourceFile(File F){
    	return isFloraSourceFile(F.getName());
    }
    public static boolean isFloraSourceFile(String filename){
    	return filename.endsWith(".flr");
    }
    public static boolean isErgoSourceFile(File F){
    	return isErgoSourceFile(F.getName());
    }
    public static boolean isErgoSourceFile(String filename){
    	return filename.endsWith(".ergo");
    }
    public static boolean isErgotextSourceFile(File F){
    	return isErgotextSourceFile(F.getName());
    }
    public static boolean isErgotextSourceFile(String filename){
    	return filename.endsWith(ERGOTEXT_EXTENSION);
    }
    /** \\load a Flora (into main module) or Prolog file
     * @param f
     * @return whether it succeeded
     * @see com.declarativa.interprolog.AbstractPrologEngine#consultAbsolute(java.io.File)
     */
    public boolean consultAbsolute(File f){
        return command("'\\load'('"+unescapedFilePath(f.getAbsolutePath())+"')") ;
    }
    /** \\load a Flora file into a specific module.  */
    public boolean consultFloraAbsolute(File f,String module){
        if (!isFloraSourceFile(f) && !isErgoSourceFile(f)) return false; 
        else return floraCommand("'\\load'('"+unescapedFilePath(f.getAbsolutePath())+"' >> "+module+")") ;
    }
    /** Same as superclass, but also accepts a Flora file, which is loaded into module 'main'
     * @see com.declarativa.interprolog.AbstractPrologEngine#consultFromPackage(String,Object) */
    public String consultFromPackage(String filename,Object requester){
        if (isFloraSourceFile(filename) || isErgoSourceFile(filename)) return consultFloraFromPackage(filename, requester);
        else return super.consultFromPackage(filename, requester);
    }
    /** So that consultFromPackage works for Flora files too */
    protected boolean doConsultFromPackage(String unescaped_path,String libDirGoal){
        if (!floraShellStarted || hasPrologExtension(unescaped_path))
            return super.doConsultFromPackage(unescaped_path,libDirGoal);
        else
            return command(libDirGoal+", '\\load'('"+unescaped_path+"')");
    }

    protected String add_lib_goalString(){
        // special treatment to avoid offending the Flora @\plg handling with ":" 
        if (floraShellStarted) return "ip_add_lib_dir";
        else return super.add_lib_goalString();
    }
	
    protected boolean doFloraConsult(String unescaped_path,String module){
        if (floraShellStarted && (isFloraSourceFile(unescaped_path)||isErgoSourceFile(unescaped_path)) )
            return floraCommand("'\\load'('"+unescaped_path+"' >> "+module+")");
        else return false;
    }
	
    /**
     * @param filename
     * @param requester
     * @return actual path of the consulted file
     */
    public String consultFloraFromPackage(String filename,Object requester){
        return consultFloraFromPackage(filename, "main", requester);
    }
	
    /**
     * @param filename
     * @param module
     * @param requester
     * @return actual path of the consulted file
     */
    public String consultFloraFromPackage(String filename,String module,Object requester){
        if (!isFloraSourceFile(filename) && !isErgoSourceFile(filename)) 
            throw new RuntimeException("Flora filename required");
        String path = copyFileToConsult(filename, module, requester);
        progressMessage("consultFromPackage:  "+path);
        String epath = unescapedFilePath(path);
        if ( !doFloraConsult(epath,module) )
            throw new IPException("Problem consulting from package archive:  "+path);
        return epath;
    }
	
    /*
      parameter with max number of solutions or...
      ... use goal() + AnswerIterator (subclass) ?? in this case, might use something other than -> in handleDeterministicGoal
      undefined? use lastSolutionWasUndefined to keep "WAM" state
	
      You can use lastSolutionUndefined() to check for undefinedness 
      public TermModel[] floraDeterministicQuery(String queryAtom,String outputVars){
      generate Args=[V1=Binding1,V2=Binding2,...]
      String prologQuery = "fj_flora_query(("+queryAtom+"),Args,Status,WAM,Ex), throw if Ex..... buildTermModels("+outputVars+",OutputModels)";
      ... = deterministicGoal(prologQuery,outputVars without ? ??)
      ...
      }*/
    /*
      public boolean floraDeterministicGoal(String C){
      String G = "fj_flora_query(C,[],Status,WAM,Ex), basics:memberchk(success,Status), Ex==normal, WAM =:=0";
      return deterministicGoal(G, "[string(C)]", new Object[]{C});
      }*/
    public boolean floraDeterministicGoal(String FG){
        TermModel[] T = floraDeterministicGoal(FG, "", new TermModel[]{}, ""); 
        return T!=null;
    }
	
    // ..."foobar(?X,?Y,?Z)", "?X,?Y", new TermModel[]{new TermModel(new Integer(13)), ....}, "?Z"
    public TermModel floraDeterministicGoal(String FG,String FloraOutputVar){
        if (FloraOutputVar.contains(","))
            throw new IPException("Too many input vars");
        if (!FloraOutputVar.contains("?"))
            throw new IPException("Flora var required");
        TermModel[] T = floraDeterministicGoal(FG, "", new TermModel[]{}, FloraOutputVar);
        if (T==null) return null;
        else return T[0];
    }
	
    /** Execute Flora goal FG, binding terms to the variables in FloraInputVars, and returning as many TermModels as vars in FloraOutputVars.
	Example: 
	TermModel Z = engine.floraDeterministicGoal("?Z is ?X+?Y","?X,?Y",new TermModel[]{new TermModel(2),new TermModel(3)},"?Z")[0];
	More:
        System.out.println("command:  "+e.floraDeterministicGoal("true"));
        System.out.println("one term:  "+e.floraDeterministicGoal("?X=2+3","?X"));
        System.out.println("sum:  "+e.floraDeterministicGoal("?Z is ?X+?Y","?X,?Y",new TermModel[]{new TermModel(2),new TermModel(3)},"?Z")[0]);
	Flora/Ergo goals are always called from the XSB Prolog layer, using flora_query/ergo_query  (See Ergo Manual, "Passing Arbitrary Queries to ERGO").
        * @param FG
        * @param FloraInputVars
        * @param terms
        * @param FloraOutputVars
        * @return bindings
        */
    public TermModel[] floraDeterministicGoal(String FG, String FloraInputVars, TermModel[] terms, String FloraOutputVars){
        StringBuilder FloraVars = new StringBuilder();
        StringBuilder inputModels = new StringBuilder();
        StringBuilder PrologInputVars = new StringBuilder();
        StringBuilder PrologOutputVars = new StringBuilder();
        StringBuilder outputModels = new StringBuilder();
		
        FG = FG.trim();
        if (!FG.endsWith(".")) FG = FG + ".";
		
        String[] FIV = (FloraInputVars.contains("?")?FloraInputVars.split(","): new String[]{});
        String[] FOV = (FloraOutputVars.contains("?")?FloraOutputVars.split(","): new String[]{});
		
        boolean first=true;
        for (int v=0;v<FIV.length;v++){
            if (!first) {
                FloraVars.append(",");
                inputModels.append(",");
                PrologInputVars.append(",");
            }
            inputModels.append("M"+v);
            PrologInputVars.append("V"+v);
            FloraVars.append("'"+FIV[v]+"'=V"+v);
            first = false;
        }
        boolean first2=true;
        for (int v=FIV.length;v<FIV.length+FOV.length;v++){
            if (!first2) {
                FloraVars.append(",");
                outputModels.append(",");
                PrologOutputVars.append(",");
            } else if (!first) FloraVars.append(",");
            outputModels.append("M"+v);
            PrologOutputVars.append("V"+v);
            FloraVars.append("'"+FOV[v-FIV.length]+"'=V"+v);
            first2=false;
        }

        // FloraVars = ... '?X'=V1,'?Y'=V2,'?Z'=V3
        // inputModels = M1,M2
        // PrologInputVars = V1,V2
        // PrologOutputVars = V3
        // outputModels = M3
	
		
        String G = "recoverTermModelArray(TermArray,["+PrologInputVars+"]), ";
        G += "fj_flora_query(FG,["+FloraVars+"],Status,WAM,Ex), basics:memberchk(success,Status), Ex==normal, WAM =:=0, ";
        //G += "buildTermModels(["+PrologOutputVars+"],["+outputModels+ "])";
        G += "buildInitiallyFlatTermModels(["+PrologOutputVars+"],["+outputModels+ "])";
		
        // System.err.println("G:  "+G);
        Object[] bindings = deterministicGoal(G, "[string(FG),TermArray]", new Object[]{FG,terms}, "["+outputModels+"]");
        if (bindings==null) return null;
        TermModel[] result = new TermModel[bindings.length];
        for (int t=0;t< result.length; t++)
            result[t]=(TermModel)bindings[t];
        return result;
    }
	
    protected String copyFileToConsult(String flrFilename,String module,Object requester){
        String filedir, filebase;
        if (!isFloraSourceFile(flrFilename) && !isErgoSourceFile(flrFilename)) 
            throw new RuntimeException("Flora filename required");
        int dirMark = flrFilename.lastIndexOf('/');
        if (dirMark==-1) {
            filedir = "";
            filebase = flrFilename.substring(0,flrFilename.length()-4);
        } else{
            filedir = flrFilename.substring(0,dirMark+1);
            filebase = flrFilename.substring(dirMark+1,flrFilename.length()-4);
        }
        String aux_base = (isErgo() ? ERGO_AUX_BASE : FLORA_AUX_BASE);
        try {
            //copyFileToConsult(filedir+aux_base+filebase+".P", requester);  Apparently Ergo no longer lets .P files in there
            copyFileToConsult(filedir+aux_base+filebase+".fld", requester);
            copyFileToConsult(filedir+aux_base+filebase+".flm", requester);
            copyFileToConsult(filedir+aux_base+filebase+".fls", requester);
            copyFileToConsult(filedir+aux_base+filebase+".fpj", requester);
            copyFileToConsult(filedir+aux_base+filebase+".ftx", requester);
            copyFileToConsult(filedir+aux_base+filebase+".pl", requester);
            copyFileToConsult(filedir+aux_base+filebase+".fdb", requester); 
            copyFileToConsult(filedir+aux_base+filebase+"_"+module+".xwam", requester);
            copyFileToConsult(filedir+aux_base+filebase+"_"+module+".fld", requester);
            copyFileToConsult(filedir+aux_base+filebase+"_"+module+".flm", requester);
            copyFileToConsult(filedir+aux_base+filebase+"_"+module+".fls", requester);
            copyFileToConsult(filedir+aux_base+filebase+"_"+module+".fdb", requester); 
        } catch (Exception e){
            System.err.println("You should precompile Flora files before packaging them:  "+e);
        }
        return copyFileToConsult(flrFilename, requester);
    }
	
    /**
     * @return Flora/Ergo install directory
     */
    public String getFloraDirectory(){
        return floraDirectory;
    }
	
    /** Return Flora's release date as YYYY-MM-DD */
    public String getFloraReleaseDate(){
        if (floraReleaseDate==null){
            fetchFloraConfiguration();
        }
        return floraReleaseDate;
    }
	
    public String getFloraBuild(){
        if (floraBuild==null){
            fetchFloraConfiguration();
        }
        return floraBuild;
    }
	
    /** Return Flora's version */
    public String getFloraVersion(){
        if (floraVersion==null){
            fetchFloraConfiguration();
        }
        return floraVersion;
    }
	
    private void fetchFloraConfiguration(){
        Object[] bindings = deterministicGoal("flrregistry:flora_configuration(releasedate,D),flrregistry:flora_configuration(version,V),flrregistry:flora_configuration(build,B)","[string(D),string(V),string(B)]");
        floraReleaseDate = ((String)bindings[0]).substring(0,10);
        floraVersion = (String)bindings[1];
        floraBuild = (String)bindings[2];
    }
	
    /** Example to create an engine and run a simple query. Source must be edited for correct paths.
     * @param args ignored
     */
    public static void main(String[] args){
        final String[] PROLOG = new String[]{"/Users/mc/Dropbox/XSB/bin"}; // no option switches
        final String FLORADIR = "/Users/mc/git/coherent-engine/flora2";
        FloraSubprocessEngine engine = new FloraSubprocessEngine(PROLOG,FLORADIR);
        File someProgram = new File(engine.getFloraDirectory(),"demos/family_obj.flr");
        if (!engine.consultFloraAbsolute(someProgram, "main"))
            System.err.println("Failed to load "+someProgram);
        TermModel result1 = engine.floraDeterministicGoal("?S = setof{?Y | eva[uncle->?Y] }", "?S");
        System.out.println("Eva uncles:  "+result1);
        File anotherProgram = new File(engine.getFloraDirectory(),"demos/flogic_basics.flr");
        if (!engine.consultFloraAbsolute(anotherProgram, "basic_mod"))
            System.err.println("Failed to load "+anotherProgram);
        long start = System.currentTimeMillis();
        TermModel result2 = engine.floraDeterministicGoal("?S = setof{?B | ?X[believes_in -> ?Y]@basic_mod, ?X = ?B }", "?S");
        System.out.println("Beliefs query took "+(System.currentTimeMillis()-start)+" mS");
        System.out.println("People beliefs:  "+result2);
    }
}


