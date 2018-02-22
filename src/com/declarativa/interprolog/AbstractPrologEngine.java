/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/

package com.declarativa.interprolog;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.jar.JarFile;

import com.declarativa.interprolog.util.BasicTypeWrapper;
import com.declarativa.interprolog.util.GoalFromJava;
import com.declarativa.interprolog.util.GoalToExecute;
import com.declarativa.interprolog.util.IPAbortedException;
import com.declarativa.interprolog.util.IPClassObject;
import com.declarativa.interprolog.util.IPClassVariable;
import com.declarativa.interprolog.util.IPException;
import com.declarativa.interprolog.util.IPInterruptedException;
import com.declarativa.interprolog.util.IPPrologError;
import com.declarativa.interprolog.util.InvisibleObject;
import com.declarativa.interprolog.util.MessageExecuting;
import com.declarativa.interprolog.util.MessageFromProlog;
import com.declarativa.interprolog.util.ObjectRegistry;
import com.declarativa.interprolog.util.ResultFromJava;
import com.declarativa.interprolog.util.ResultFromProlog;
import com.declarativa.interprolog.util.VariableNode;

/** The heart of InterProlog; a PrologEngine represents a Prolog machine instance.  
This is an abstract class; you should use it just to declare variables, but must instantiate only subclasses.
*/
public abstract class AbstractPrologEngine implements PrologEngine{
	/** cache variable */
	private String prologVersion=null;
    /** Auxiliary object knowing about specific implementation details */
    protected PrologImplementationPeer peer;
    /** File path to directory with Prolog machine, or command path; this may NOT have 
    options appended (Windows oblige), use specific constructors in subclasses for that*/
    public String prologBinDirectoryOrCommand;
    protected long startTime;
    /** Convenience for newline; value for the OS where Prolog is running (which may be different from the Java side).
    So this may or not coincide with System.getProperty("line.separator") */
    public String nl;
    /** Temporary directory where Prolog files are unjared to. 
    It is NO LONGER (May 2014) automatically declared as a Prolog library_directory: Unjared files 
    are consulted on the spot anyway, so...*/
    protected File tempDirectory; 
    /** Some Prologs require a user:F form when consulting a file F in the user module, instead of the working module*/
    protected String fileModulePrefix = "";
    
    /** Table of object references that can be referred from Prolog without being serialized */
    protected ObjectRegistry knownObjects;
    protected boolean shutingDown = false;
    /** Prolog is handling an interrupt */
    protected boolean interrupting = false;
    /** Determines whether deterministicGoal machinery is in place... */
    public boolean interPrologFileLoaded = false;
    protected boolean debug=false;
    protected boolean isProfiling=false;
    /** InterProlog startup files should be loaded from the jar file, rather than from classname-relative file locations */
    protected boolean loadFromJar;
    /** A (first) top goal is active; so  deterministicGoal calls will be handled during the 
    execution of javaMessage. */
    protected boolean topGoalHasStarted=false;
    protected boolean detectsPauses = true;
    /** Undefined if !detectsPauses. Otherwise, true when the logic engine is idle (awaiting a command) in a break */
    protected boolean paused = false;
    /** Currently exclusive with detectsPauses */
	protected boolean usingTimedCall;
    /** goal counter */
    protected int goalTimestamp;
    /** Whether each javaMessage is executed in a new (separate) thread. False by default: callbacks try to run in the Java thread of the last pending goal call.*/
    protected boolean threadedCallbacks = false; 
    
	private boolean allowSimultaneousThreads = false;
    
    /** Prolog Goals whose execution has not yet finished or whose results have not yet been returned to their Java clients */
    Vector<GoalToExecute> goalsToExecute;
    /** javaMessage requests that have started execution and whose results have not yet been returned to Prolog. */
    protected Vector<MessageExecuting> messagesExecuting;
    
    /** There's an open call to 
    @see #goal(String, String, Object[], String)
     */
    protected boolean nonDeterministicGoalActive = false;
    
    /** So we can remember the third truth value for the last solution */
    protected boolean lastSolutionWasUndefined = true;
    
    /** The thread that handles javaMessage callbacks, and deterministicGoal results */
    protected Thread prologHandler = null; 
    
    /** "Constant" used for some special javaMessage handling */
	final Method getRealJavaObjectMethod;
    /** Name of first message sent to Java*/
	public final String firstJavaMessageName = "firstJavaMessage";
	
	protected ArrayList<PrologEngineListener> listeners = new ArrayList<PrologEngineListener>();
	
	/** Path to the interprolog Prolog file */
	protected String interprologPath=null;
	
	protected boolean localEngine;
	

	/** Create a Prolog executor, possibly spawning it in a different process or loading it into memory, depending on the implementation by our subclass.
	@param prologBinDirectoryOrCommand File path to Prolog machine, see subclass docs for precise semantics
	@param debug if true, print progress messages to aid debugging
	@param loadFromJar if true, startup files should be loaded from the jar file, rather than directly from the file system
	<p>IF prologBinDirectoryOrCommand is null then InterProlog needs to find
	values for one or more properties, XSB_BIN_DIRECTORY and/or others, depending on which Prolog(s) you're using.<p>
	First it looks for an 'interprolog.defs' file 
	(a Properties file in the format described in http://java.sun.com/j2se/1.4.2/docs/api/java/util/Properties.html, 
	in the directory where the jar file containing the InterProlog classes is). <br>
	If 'interprolog.defs' does not exist, an attempt is made 
	to get the property through System.getProperties(); in this scenario you can define the property by using the -D java (command) switch;
	if it doesn't find it, it will try to find an environment variable
	*/
	public AbstractPrologEngine(String prologBinDirectoryOrCommand, boolean debug, boolean loadFromJar){
		startTime= System.currentTimeMillis();
		localEngine = true; // subclss may change this...
		nl = "\r\n"; // assume the worst... and on startup we'll introspect into Prolog's system to get the real value 
		peer = makeImplementationPeer();
		this.loadFromJar=loadFromJar;
		if (prologBinDirectoryOrCommand==null){
			prologBinDirectoryOrCommand = findInterPrologProperty(peer.getBinDirectoryEnvVar());
			if (prologBinDirectoryOrCommand!=null) 
				prologBinDirectoryOrCommand = executablePath(prologBinDirectoryOrCommand);				
		}
		if (prologBinDirectoryOrCommand==null)
		throw new IPException("PrologEngine with null prologBinDirectory");
		this.debug=debug; // can't setDebug: too soon and class dependent
		this.prologBinDirectoryOrCommand = prologBinDirectoryOrCommand;
		makeTempDirectory(); // not just for InterProlog startup! That's why this is NOT conditional
		knownObjects = new ObjectRegistry();
		goalsToExecute = new Vector<GoalToExecute>();
		messagesExecuting = new Vector<MessageExecuting>();
        try{ getRealJavaObjectMethod = findMethod (getClass(),"getRealJavaObject",new Class[]{Object.class});} 
        catch(Exception ex){throw new IPException("could not find special getRealJavaObject method:"+ex);}
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
              if (!isDebug())
            	  deleteTempFiles();
            }
        });
	}
	
	/** If you don't trust the Java VM to execute earlier "deleteOnExit", call this */
	public void deleteTempFiles(){
		deleteAll(tempDirectory);
	}
	
	public String getInterprologPath(){
		return interprologPath;
	}
	
	protected String findInterPrologProperty(String P){
		String R = null;
		File prefs = new File(getJarDirectory(),"interprolog.defs");
		if (prefs.exists()){
			System.err.println("Using "+prefs);
			try{
				FileInputStream fr = new FileInputStream(prefs);
				Properties p = new Properties();
				p.load(fr);
				R = p.getProperty(P);
				fr.close();
				/* if (prologBinDirectoryOrCommand==null)
					throw new IPException("Bad ..._BIN_DIRECTORY in interprolog.defs file: "+prologBinDirectoryOrCommand); */
			} catch(IOException e){
				throw new IPException("Could not read interprolog.defs file");
			}
		} 
		if (R!=null) return R;
		R = System.getProperties().getProperty(P);
		if (R!=null) return R;
		return System.getenv(P);
	}
	/*
	public AbstractPrologEngine(String prologCommand, boolean debug){
		this(prologCommand,debug,true);
	}
	
	public AbstractPrologEngine(String startPrologCommand){
		this(startPrologCommand,false);
	}
	
	public AbstractPrologEngine(boolean debug){
		this(null,debug);
	}
	
	public AbstractPrologEngine(){
		this(false);
	}
	*/
	protected void loadInitialFiles(){
        progressMessage("Loading InterProlog initial file...");
        String F2 = peer.interprologFilename();
        if (loadFromJar) interprologPath = consultFromPackage(F2,AbstractPrologEngine.class);
        else interprologPath=consultRelative(F2,AbstractPrologEngine.class);
        waitUntilAvailable();
	}
	
	protected abstract PrologImplementationPeer makeImplementationPeer();
	
	public PrologImplementationPeer getImplementationPeer(){
		return peer;
	}
	
	protected String getBinDirectoryProperty(Properties p){
		return peer.getBinDirectoryProperty(p);
	}

	protected String executablePath(String d){
		return peer.executablePath(d);
	}

	/** Returns the Prolog system name and version for this engine */
	public String getPrologVersion(){
		if (prologVersion==null)
			prologVersion = peer.getPrologVersion();
		return prologVersion;
	}
	
	/** Returns the Prolog numeric version for this engine; useful for functionality that depends on features of a particular version. 
	It is requested to Prolog dynamically, but only once. Should not be used only after the engine objects is initialized (constructed). */
	public String getPrologNumericVersion(){
		return peer.getPrologNumericVersion();
	}
	/** Returns git log hash kept in our jar's manifest */
	public String getGitHash() {
		File classesFile;
		try {
			classesFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
			return new JarFile(classesFile).getManifest().getMainAttributes().getValue("Implementation-Version");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "???";
	}
	
	/** Returns the installation directory for the Prolog system, without the trailing separator char */
	public String getPrologBaseDirectory(){
		return prologBinToBaseDirectory(prologBinDirectoryOrCommand);
	}

	/** Computes the installation directory for the Prolog system, without the trailing separator char */
	public String prologBinToBaseDirectory(String binDirectoryOrStartCommand){
		return peer.prologBinToBaseDirectory(binDirectoryOrStartCommand);
	}
	
	/** Whether the Prolog machine is a Windows box */
	public boolean serverIsWindows(){
		return isWindowsOS();
	}
	
	public char serverFileSeparatorChar(){
		return File.separatorChar;
	}
	
	/** whether we (Java side) are re under Windows  -- isWindowsOS */
	public static boolean isWindowsOS(){ 
		return (System.getProperty("os.name").toLowerCase().indexOf("windows")!=-1);
	}
	
	public static boolean is64WindowsOS(){ // if we're under 64-bit Windows 
		return isWindowsOS() && (System.getenv("ProgramFiles(x86)") != null);
	}
	
	public static boolean isMacOS(){ 
		return (System.getProperty("os.name").toLowerCase().indexOf("mac os x")!=-1);
	}
	
	public static boolean isLinuxOS(){ 
		return (System.getProperty("os.name").toLowerCase().indexOf("linux")!=-1);
	}
	
	public boolean hasPrologExtension(String filename){
		return peer.hasPrologExtension(filename);
	}
	
	/** Release Prolog engine resources, making it unusable*/
	public void shutdown(){
		progressMessage("Shutting down");
		if (isShutingDown()) progressMessage("Was already shuting down");
		shutingDown = true;
		//TODO: should this be done??: endAllJavaMessages(new IPException("Shutting down"));
		abortTasks();
		// prologHandler.interrupt(); // cannot do this here, in NativeEngine crashes the whole JVM
		knownObjects.clear();
	}

	/** The engine is in the process of shuting down */
	public boolean isShutingDown(){
		return shutingDown;
	}
	
	void makeTempDirectory(){
		if (tempDirectory!=null)
            throw new IPException("Inconsistency in makeTempDirectory");
		tempDirectory = createTempDirectory();
	}
	/** Create a new temporary directory
	 * @return directory path
	 */
	public File createTempDirectory(){
		final File td = createTempDirectory(2);
		/*
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
              if (!isDebug())
            	  deleteAll(td);
            }
        });*/
		return td;
	}
	/** Give the OS another try at creating the temporary directory; although very rarely, sometimes Windows fails with no reason */
	private File createTempDirectory(int tries){
		File tempDirectory;
		tries--;
		try{
			File dummyFile = File.createTempFile("IP_","");
			if (!dummyFile.delete())
                            throw new IPException("Could not delete dummy file");
			tempDirectory = new File(dummyFile.getAbsolutePath());
			if (!tempDirectory.mkdir())
                            throw new IPException("Could not create temporary directory");
		} catch (IOException e){
			if (tries>0) {
				progressMessage("Failed makeTempDirectory: "+e+"\nTrying again");
				return createTempDirectory(tries);
			}
			else throw new IPException("Problems creating temporary directory: "+e);
		}
		return tempDirectory;
	}
	
	public static void deleteAll(File D){
	  if (D == null)
		  return;
	  if (D.isDirectory()) {
	    for (File c : D.listFiles())
	    	deleteAll(c);
	  }
	  if (!D.delete())
		  System.err.println("FAILED to delete "+D);
	}
	
	/** Returns the directory containing the jar with the engine class */
	public File getJarDirectory(){
		return getJarDirectory(getClass());
	}
	
	/** Returns the directory containing the jar with the given class */
	public static File getJarDirectory(Class<? extends AbstractPrologEngine> aClass){
	    String classPath=aClass.getName().replace('.','/') + ".class";
		URL u = aClass.getClassLoader().getResource(classPath);
		if (!u.getProtocol().equals("jar")) return null;
		String s = u.getFile();
	    if (!s.startsWith("file:/"))
		throw new IPException("Jar file is not in the local file system");
		int end = s.indexOf("!");
		if (end==-1) throw new IPException("Bad jar URL");
	    // MK changed 6 to 5!!!
	    String path = s.substring(5,end);
	    if (!path.endsWith(".jar"))
		throw new IPException("Jar file name does not end with .jar");
		path=path.replace('/',File.separatorChar);
		return new File(path).getParentFile();
	}
	
	File copyToTemp(String filename,Object requester){
		progressMessage("copyToTemp: "+filename+",requester=="+requester);
		// if (filename.indexOf('.')==-1) filename=filename+".P";
		File tempFile = null;
		Class<?> rc;
		if (requester instanceof Class) rc = (Class<?>)requester;
		else rc = requester.getClass();
		// rc.getPackage() null when using JUnit's class loader...
		// String packageName = rc.getPackage().getName();
		ClassLoader rcl = rc.getClassLoader();
		String className = rc.getName();
		String packageName = className.substring(0,className.lastIndexOf('.'));
	    String path = packageName.replace('.','/' /*File.separatorChar*/);
		progressMessage("path=="+path+",packageName=="+packageName);
		//if (filename.indexOf('.')==-1) throw new IPException("File name missing extension");
		String newfilename=null;
		try {
			String resourceName = path+/*File.separatorChar*/'/';
			InputStream resourceStream=null;
			if (!(filename.indexOf('.')==-1)){
				// filename has extension
				newfilename = filename;
				resourceStream = rcl.getResourceAsStream(resourceName+filename);
			} else{
				// let's try the possible extensions
				String[] alternatives = alternativePrologExtensions(filename);
				for (int a=0;a<alternatives.length;a++){
					newfilename = alternatives[a];
					resourceStream = rcl.getResourceAsStream(resourceName+newfilename);
					if(resourceStream!=null) break;
				}
			}
		    if (resourceStream==null) throw new IPException("Resource not found: "+resourceName+newfilename);
			tempFile = new File(tempDirectory,resourceName+newfilename);
			
			File parentDir = tempFile.getParentFile();
			if (parentDir!=null && !parentDir.exists()) {
				if(!parentDir.mkdirs()) 
				throw new IPException("Could not create parent directories for "+tempFile);
                
                //code below added by C. Rojo, June 13th. Modified to make any additional 
                // directories created in temp by the above call to "mkdirs()" 
                // delete on exit of the VM.
                progressMessage("About to check for parent directories...");
                setTempDirectoriesToDelete(parentDir);
                //end of C. Rojo added code. See also setTempDirectoriesToDelete
			}			
			
			progressMessage("Created file object for tempDirectory:"+tempFile);
			
			FileOutputStream fos = new FileOutputStream(tempFile);
			progressMessage("Created file output stream in tempDirectory: "+fos);
		    byte[] buffer = new byte[512]; int len;
		    while ((len = resourceStream.read(buffer, 0, buffer.length)) != -1) 
				fos.write(buffer, 0, len);
		    fos.close(); resourceStream.close();
		} catch (IOException e){
			throw new IPException("I/O problem obtaining "+newfilename+": "+e);
		}
		progressMessage("exiting copyToTemp: "+tempFile);
		return tempFile;
	}
	
     /**
     * Recursive helper function designed to set any directories nested within 
     * the temp directory to be deleted on exit of the virtual machine. 
     * Directories with lesser depth must be set to be deleted first.
     * @Pre-Condition: nestedTempDirectories must be within or be the system's
     *     default Temp directory.
     * @param nestedTempDirectories Directory path containing new directories to set for deletion.
     */
    private void setTempDirectoriesToDelete(File nestedTempDirectories)
    {
        if (nestedTempDirectories != null 
            && !nestedTempDirectories.getAbsolutePath().equals(tempDirectory.getAbsolutePath()))
        {
           setTempDirectoriesToDelete(nestedTempDirectories.getParentFile());
           progressMessage("Temp parent directory detected! Path: "
                + nestedTempDirectories.getAbsolutePath()
                + " \nSetting it to delete on exit.");
           nestedTempDirectories.deleteOnExit();
        }
    }
    
    
    
	/** Some Prologs use '\' as an escape character in atoms, which can affect file paths under Windows. Use this method to preprocess
	all file paths passed to Prolog.  */
	public String unescapedFilePath(String p){
		return peer.unescapedFilePath(p);
	}

	protected static String currentOSpath(String javaPath){
		if (File.separatorChar=='/') return javaPath;
		StringBuffer sb = new StringBuffer(javaPath);
		for(int c=0;c<sb.length();c++)
			if (sb.charAt(c)=='/') sb.setCharAt(c,File.separatorChar);
		return sb.toString();
	}
	
	/** Given a Prolog filename without extension, return it completed with all extensions that may exist*/
	protected String[] alternativePrologExtensions(String path){
		return peer.alternativePrologExtensions(path);
	}
	
	public String consultFromPackage(String filename,Object requester){
		return consultFromPackage(filename,requester,false);
	}
	
	/** 
         * Extracts a Prolog file from the jar file or directory where the requester's class came from,
         * and asks Prolog process to reconsult it. You should use this method only after your
         * program is stable.
         * The Prolog file is extracted into a temporary file, that is automatically deleted on exiting the application.
         * @param filename The Prolog file path relative to the requester's package, including extension; if it has no extension, the Prolog file extensions are appended in turn until a file is found
         * @param requester Defines where the Prolog file resides
         * @param firstTimeOnly If true a second call will NOT repeat the consult on the Prolog end
         * @return The path where the Prolog file copy effectively consulted resides
         * @see #consultRelative(String, Object)
         * @see #load_dynRelative(String, Object)
         */
	
	public String consultFromPackage(String filename,Object requester,boolean firstTimeOnly){
		String path = copyFileToConsult(filename, requester);
		String tempBase = cleanPath(tempDirectory.getAbsolutePath());
		String relevantPath = unescapedFilePath(path.substring(1+tempBase.length()));
		progressMessage("consultFromPackage: "+path);
		String D = unescapedFilePath(new File(path).getParent());
		String libDirGoal = "("+add_lib_goalString()+"('"+D+"')) ";
		String epath = unescapedFilePath(path);
		if (firstTimeOnly){
			if ( !doConsultFromPackage(epath,relevantPath,libDirGoal) )
			throw new IPException("Problem consulting from package archive:  "+path);
		} else {
			if ( !doConsultFromPackage(epath,libDirGoal) )
			throw new IPException("Problem consulting from package archive:  "+path);
		}
		return epath;
	}
	
	/** Adds a directory to Prolog's library search paths */
	public boolean add_lib_dir(File D){
		return command(add_lib_goalString()+"('"+unescapedFilePath(D.getAbsolutePath())+"') ");
	}
	
	protected String add_lib_goalString(){
		return "consult:add_lib_dir";
	}
	
	protected String copyFileToConsult(String filename,Object requester){
		if (filename.endsWith(".H")){ // XSB Hack, should not assume extension exists, and needs to move to peer or subclass
			// copy dependencies first
			String D = filename.substring(0,filename.length()-2) + ".c";
			try{ copyToTemp(D,requester);}
			catch(IPException e){
				throw new IPException("Missing dependency: "+D+". "+e);
			}
		}
		return cleanPath(copyToTemp(filename,requester).getPath());
	}
	
	protected boolean doConsultFromPackage(String unescaped_path, String libDirGoal){
		return command(libDirGoal+",consult("+fileModulePrefix+"'"+unescaped_path+"')");
	}
	
	/** Consults only once, to cater for multiple invocations of this engine in the same Prolog session.
	 * @param epath real physical path to be consulted
	 * @param relevantPath user name for the above, relative to tempDirectpry
	 * @param libDirGoal
	 * @return
	 */
	private boolean doConsultFromPackage(String epath, String relevantPath, String libDirGoal) {
		String S = "'"+relevantPath+"'=_Rpath, (interprolog:already_loaded_path(_Rpath)->true;" 
				+ libDirGoal+",consult("+fileModulePrefix+"'"+epath+"'),interprolog:did_load_path(_Rpath))";
		return command( S);
	}

	/** Consults a Prolog file */
	public boolean consultAbsolute(File f){
		return consultAbsolute(f.getAbsolutePath());
	}
	
	public boolean consultAbsolute(String f){
        return command("consult("+fileModulePrefix+"'"+unescapedFilePath(f)+"')") ;
    }
	
	public boolean load_dynAbsolute(File f){
		return command("load_dyn("+fileModulePrefix+"'"+unescapedFilePath(f.getAbsolutePath())+"')");
	}
	
	
	/** Consults a Prolog file from the directory where the requester's class would come from if it
	did not come from a jar file. Adds that directory to the library_directory relation, so modules can be found there
@param filename The Prolog file name, including suffix; if a path it should use '/' as the separator, independently of the OS
@param requester Defines where the Prolog file resides
	*/
	public final /* used during IP startup */ String consultRelative(String filename,Object requester){
		return operationRelative("consult", currentOSpath(filename), requester);
	}
	
	public String load_dynRelative(String filename,Object requester){
		return operationRelative("load_dyn", filename, requester);
	}
	
	/** Returns the real path where the file is */
	protected String operationRelative(String operation,String filename,Object requester){
		Class<?> rc;
		if (requester instanceof Class) rc = (Class<?>)requester;
		else rc = requester.getClass();
		String path = rc.getPackage().getName().replace('.',File.separatorChar);
		path=cleanPath(getJarDirectory().getPath())+File.separatorChar+path; 
		// make this insensitive to Prolog's current directory
		//if (filename.indexOf('.')==-1) filename=filename+".P";
		filename = path+File.separatorChar+filename;
		// This should work both for XSB and SWI
		path = unescapedFilePath(path);
		filename = unescapedFilePath(filename);
		String g = "("+add_lib_goalString()+"('"+path+"')), "+operation+"("+fileModulePrefix+"'"+filename+"')";
		if (! command(g))
		throw new IPException("Problem in operationRelative");
		return filename;
	}
			

    /** Makes sure no %20 characters are in a String, applying URLDecode if
     * necessary, only for Java 1.4 and later.
     * From java version 1.4 paths returned by File.getPath contain %20 instead
     * of spaces
     */
    private String cleanPath(String p){
    	progressMessage("Cleaning path "+p);
    	return p.replaceAll("%20"," "); //fix email Daniel Elenius 28 Jul 2010 23:29...
        /* was replacing + with space on Mac OS 10.6 
        if (System.getProperty("java.version").compareTo("1.4")>=0 && !isMacOS() ) {
            try{
                Method decode = URLDecoder.class.getMethod("decode", new
                Class[]{String.class, String.class});
                return (String)decode.invoke(new URLDecoder(),new Object[]{p,"UTF-8"});
            } catch (Exception e){
                throw new IPException("Inconsistency in PrologEngine.cleanPath"+e);
            }
        } else return p; */
    }
	
	/** Interrupt Prolog and make it return to its top level. This is the equivalent to performing a ctrl+c or similar command
	 when using Prolog under a standard console shell. */
	protected void interrupt(boolean wait){
		if (isAvailable() && isIdle()) return; // to prevent some race conditions
		// clean up engine controller
	    if (getThePrologListener() instanceof EngineController)
	    	((EngineController)getThePrologListener()).interruptCleanupHack();
	    interrupting = true;
	    doInterrupt(wait,true,true);
	    waitUntilIdle();
	    interrupting = false;
	}
	
	public void interrupt(){
		interrupt(true);
	}
	
	protected abstract void doInterrupt(boolean wait,boolean killGoals, boolean abortEntirely);
	
	
	/** Execute a Prolog "command"; except until the basic InterProlog is loaded, 
	this in fact delegates to deterministicGoal, which synchronizes at the end and returns a reliable result.  */
    public boolean command(String s){
        if (topGoalHasStarted||interPrologFileLoaded) return deterministicGoal(s);
        else return realCommand(s);
    }
    
    public boolean command(ArrayList<String> commands){
    	return command(commands.toArray(new String[0]));
    }
    /**
     * @param commands to be executed as a single conjunction in order 0,1,...
     * @return whether they all succeed
     */
    public boolean command(String[] commands){
    	boolean first = true;
    	StringBuilder sb = new StringBuilder();
    	for (String C:commands){
    		if (!first) sb.append(",");
    		else first = false;
    		sb.append(C);
    	}
    	return command(sb.toString());
    }
    /** A command expected to always succeed; throws an IPException if it fails */
    public void exec(String S){
    	if (!command(S))
    	throw new IPException("command failed: "+S);
    }
    	
	/** Implementation of a simple parameterless Prolog goal; does not support recursive nor multithreaded operation, use command instead
	@see #command(String) */
	public abstract boolean realCommand(String s);
	
	/** Debugging aid */
	public void progressMessage(String s){
		if (debug) System.err.println(System.currentTimeMillis()-startTime+ "ms: "+s + "("+Thread.currentThread()+")");
	}

	public void progressMessage(String s, Object X){
		if (debug) System.err.println(System.currentTimeMillis()-startTime+ "ms: "+s + "("+Thread.currentThread()+"): " + X);
	}

	/** Profiling aid */
	public void profilingMessage(String s){
		if (isProfiling) System.out.println(System.currentTimeMillis()-startTime+ "ms: "+s + "("+Thread.currentThread()+")");
	}

	/** Debug messages are being written, both Java and Prolog side, cf. ipIsDebugging/0
	*/
	public boolean isDebug(){ return debug;}
	
	/** Show (or hide) debug messages, both Java and Prolog side, cf. ipIsDebugging/0. Beware that this may try to assert/retract
	a flag (ipIsDebugging/0) on the Prolog side, be sure to invoke this method sooner rather than later as it may get "stuck" if there
	are problems communicating with the Prolog engine
	*/
	public void setDebug(boolean d){
		if (d) 
			System.err.println(System.currentTimeMillis()-startTime+ "ms: "+"now showing debugging information");
		if(d && !debug && !command("assert(ipIsDebugging)"))
			throw new IPException("Could not set ipIsDebugging");
		if(!d && debug && !command("retractall(ipIsDebugging)"))
			throw new IPException("Could not clear ipIsDebugging");
		debug=d;
	}
	
	public boolean isProfiling(){ return isProfiling;}
	
	public void setProfiling(boolean d){
		if (d) System.out.println(System.currentTimeMillis()-startTime+ "ms: "+"now showing profiling information");
		if(d && !isProfiling && !command("assert(ipIsProfiling)"))
			throw new IPException("Could not set ipIsProfiling");
		if(!d && isProfiling && !command("retractall(ipIsProfiling)"))
			throw new IPException("Could not clear ipIsProfiling");
		isProfiling=d;
	}
	
	public boolean getLoadFromJar(){
		return loadFromJar;
	}
	
	/** Convenience for debugging deterministicGoal() messages */
	public static void printBindings(Object[] b){
		if (b==null) System.out.println("Empty bindings");
		else 
			for (int i=0;i<b.length;i++) System.out.println("Binding "+i+":"+b[i]);
	}
		
	protected final void teachIPobjects(ObjectOutputStream obs) throws IOException{
		Object [] oa1 = {new Integer(13)};
		obs.writeObject(
			new ObjectExamplePair(
				"GoalFromJava",
				new GoalFromJava(1,"a","aa",new Object[0],"A"),
				new GoalFromJava(2,"b","bb",oa1,"B")
			)); 
		obs.writeObject(
			new ObjectExamplePair(
				"ResultFromProlog",
				new ResultFromProlog(1,true,0,null,false),
				new ResultFromProlog(2,false,1,"some error",true)
			)); 
		MessageFromProlog mpA = new MessageFromProlog();
		mpA.methodName="methodA";
		mpA.arguments=new Object[0];
		mpA.returnArguments=true;
		MessageFromProlog mpB = new MessageFromProlog();
		mpB.timestamp=2;
		mpB.target = "target";
		mpB.methodName="methodB";
		mpB.arguments=new Object[1];
		mpB.returnArguments=false;
		obs.writeObject(
			new ObjectExamplePair("MessageFromProlog",mpA,mpB)
		); 
		/* crashed JRE 1.4 with new Exception():*/
		obs.writeObject(
			new ObjectExamplePair("ResultFromJava",
				new ResultFromJava(1,null,null,new Object[0]),
				new ResultFromJava(2,"result here","new Exception()...",new Object[1])
			)
		); 
		obs.writeObject(
			new ObjectExamplePair("InvisibleObject",new InvisibleObject(1),new InvisibleObject(2))
		); 
		obs.writeObject(
			new ObjectExamplePair("IPClassObject",new IPClassObject("A"),new IPClassObject("B"))
		); 
		obs.writeObject(
			new ObjectExamplePair("IPClassVariable",
				new IPClassVariable("ClassA","VariableA"), new IPClassVariable("ClassB","VariableB")
				)
		); 
	}

    /** Provide Prolog with material to construct special objects that 
	actually represent basic type values, so that Prolog can call
	(through javaMessage) methods with basic type (non-object) arguments.
	Also provides material for some other convenience objects */	
	protected void teachBasicObjects(ObjectOutputStream obs) throws IOException{
		ObjectExamplePair[] objects = getBasicObjects();
		for (ObjectExamplePair o:objects)
			obs.writeObject(o);
	}
	
	ObjectExamplePair[] getBasicObjects(){
		return new ObjectExamplePair[]{
				new ObjectExamplePair(
						"boolean",new BasicTypeWrapper(new Boolean(true)),new BasicTypeWrapper(new Boolean(false))
					),
					new ObjectExamplePair(
						"byte",
						new BasicTypeWrapper(new Byte((new Integer(1)).byteValue())),
						new BasicTypeWrapper(new Byte((new Integer(2)).byteValue()))
					),
					new ObjectExamplePair(
						"char",new BasicTypeWrapper(new Character('A')),new BasicTypeWrapper(new Character('B'))
					),
					new ObjectExamplePair(
						"double",new BasicTypeWrapper(new Double(1)),new BasicTypeWrapper(new Double(2))
					),
					new ObjectExamplePair(
						"float",new BasicTypeWrapper(new Float(1)),new BasicTypeWrapper(new Float(2))
					),
					new ObjectExamplePair(
						"int",new BasicTypeWrapper(new Integer(1)),new BasicTypeWrapper(new Integer(2))
					),
					new ObjectExamplePair(
						"long",new BasicTypeWrapper(new Long(1)),new BasicTypeWrapper(new Long(2))
					),
					new ObjectExamplePair(
						"short",
						new BasicTypeWrapper(new Short( (new Integer(1)).shortValue() )),
						new BasicTypeWrapper(new Short( (new Integer(2)).shortValue() ))
					),
					new ObjectExamplePair(new Boolean(true),new Boolean(false)),
					new ObjectExamplePair(new Byte((byte)1),new Byte((byte)2)),
					new ObjectExamplePair(new Short((short)1),new Short((short)2)),
					new ObjectExamplePair(new Integer(1),new Integer(2)),
					new ObjectExamplePair(new Long(20),new Long((long)Math.pow(2.0,35.0))),
					new ObjectExamplePair(new Float(20.59375),new Float(2)),
					new ObjectExamplePair(new Double(-20.053),new Double(23924.312874)),
					new ObjectExamplePair(new Character('A'),new Character('B')),
					TermModel.example(),
					VariableNode.example(),
					InitiallyFlatTermModel.example(),
					new ObjectExamplePair("ArrayOfTermModel",new TermModel[0],new TermModel[1]),
					new ObjectExamplePair("ArrayOfString",new String[0],new String[1]),
					new ObjectExamplePair("ArrayOfObject",new Object[0],new Object[1]),
					new ObjectExamplePair("ArrayOfbyte",new byte[]{}, new byte[]{1,2}),
					new ObjectExamplePair("ArrayOfint",new int[]{}, new int[]{1,2}),
					new ObjectExamplePair("ArrayOffloat",new float[]{}, new float[]{1.0f,2.0f})
			};
	}

	/** Same as #teachMoreObjects(ObjectExamplePair[]), but the single example pair is constructed repeating the object
	@see #teachMoreObjects(ObjectExamplePair[]) */
	public boolean teachOneObject(Object example){
		return teachMoreObjects(new Object[]{example});
	}
	
	/** Same as #teachMoreObjects(ObjectExamplePair[]), but example pairs are constructed with (2) repeated examples for each object
	@see #teachMoreObjects(ObjectExamplePair[]) */
	public boolean teachMoreObjects(Object[] examples){
		if (examples[0] instanceof ObjectExamplePair) 
			throw new IPException("Bad method invocation in teachMoreObjects");
		ObjectExamplePair[] pairs = new ObjectExamplePair[examples.length];
		for (int i=0;i<pairs.length;i++)
			pairs[i] = new ObjectExamplePair(examples[i]);
		return teachMoreObjects(pairs);		
	}
	
	/** Send an array of object example pairs to Prolog and generate ipObjectSpec facts. 
	Returns true if this succeeds, false otherwise.
	@param examples The examples
	@see ObjectExamplePair */
	public boolean teachMoreObjects(ObjectExamplePair[] examples){
		// simply call "dg" to teach examples...
		Object[] temp = new Object[examples.length];
		for (int i=0;i<examples.length;i++)
			temp[i] = examples[i];
		return deterministicGoal("ipProcessExamples(Examples)", "Examples", temp);
	}

	public boolean teachMoreObjects(ObjectExamplePair example){
		return teachMoreObjects(new ObjectExamplePair[]{example});
	}
	
	protected GoalFromJava makeDGoalObject(String G, String OVar, Object[] objectsP, String RVars,int timestamp){
		// RVars can not be tested here, only at the Prolog side after the goal
       	if (G==null)
            throw new IPException ("Null Goal in deterministicGoal");
        if (OVar==null)
            OVar="_";
        if (objectsP == null) objectsP = new Object[0];
        else if (!(objectsP.getClass().getName().equals("[Ljava.lang.Object;"))) 
        	// safeguard against typical usage mistake, may however be too strict...
        	throw new IPException("objectsP argument must be an array of Object");
        if (G.trim ().endsWith ("."))
            throw new IPException ("Goal argument should have no trailing '.', in deterministicGoal");
        for(int i=0;i<objectsP.length;i++)
        	if (objectsP[i]!=null && !isSerializable(objectsP[i])) 
        		objectsP[i] = makeInvisible(objectsP[i]);
        return new GoalFromJava(timestamp,G,OVar,objectsP,RVars);
	}
	/** Dispatches goal encoded as a JSON object and returns a JSON response
	 * Example: 
	 * ipPrologEngine(E), JSON='{"g":s("string:concat_atom([hello_,I],O)"),"i":s("I"),"o":s("O"),"x":s("miguel")}', java(E,string(R),deterministicGoalJSON(string(JSON),int(1))).
	 * @param JSON : {"g":"some goal with var","i":"VarWithInputJSONstring","o":"VarWithOutputJSONString","x":JSONinput}
	 * @param timestamp : some unique client identifier for this call; it may be in JSON, but the caller should extract it
	 * @return JSON: {"t":int_same_timestamp,"s":true/false/null(for undefined) , "e":null_or_error_object, "r":resultJSON}
	 */
	public String deterministicGoalJSON(String JSON,int timestamp){
		Object[] bindings = null;
		try{
			bindings = deterministicGoal("jsonDGhandler(I,O)","[string(I)]",new Object[]{JSON},"[string(O)]");
			if (bindings==null)
				return "{\"t\":"+timestamp+",\"s\":false, \"e\":null, \"r\":null}";
			else
				return "{\"t\":"+timestamp+",\"s\":true, \"e\":null, \"r\":"+(String)bindings[0]+"}";
		} catch (Exception e){
			return "{\"t\":"+timestamp+",\"s\":false, \"e\":\""+e.toString()+"\", \"r\":null}";
		}
	}
	
	/** Synchronously calls a Prolog goal. 
	Only the first solution is considered. G should contain a syntactically correct
	Prolog term, without the trailing dot (.). Throws an IPAbortedException if a Prolog abort happens, and an
	IPInterruptedException if the interrupt() method was invoked.
	@see #deterministicGoal(String)
	@see #deterministicGoal(String,String)
	@see #deterministicGoal(String,String,Object[])
	@return	a new array containing an object for each term in the rVars list, or null if goal fails
	@param G Prolog goal term
	@param OVar Prolog variable that will be bound to objectsP array
	@param objectsP Array of Java objects to pass to Prolog goal; nonserializable objects are encapsulated in InvisibleObject references, changing this array
	@param RVars Prolog list with object specifications, typically containing variables occurring in g. 
	If null a single binding will be returned, containing a TermModel object representing the goal term solution
	*/	
	public Object[] deterministicGoal(String G, String OVar, Object[] objectsP, String RVars){
		profilingMessage("Entering dg");
        //available=false;
        Object[] resultToReturn=null;
 		int mytimestamp = incGoalTimestamp();
        try{
       		GoalFromJava GO = makeDGoalObject(G, OVar, objectsP, RVars, mytimestamp);
       		progressMessage("Prepared GoalFromJava: "+GO);
            progressMessage("Schedulling (in PrologEngine) goal "+G+", timestamp "+mytimestamp+" in thread "+Thread.currentThread().getName());
            GoalToExecute goalToDo = new GoalToExecute(GO);
            
            scheduleGoal(goalToDo);
            progressMessage("Schedulled "+goalToDo);
            
            //System.out.println("CALLING waitForResult (Abstract):"+this);
            ResultFromProlog result = goalToDo.waitForResult();
            
            lastSolutionWasUndefined = result.undefined;
            
            profilingMessage("dG - Got result"); progressMessage("dG - Got result for "+goalToDo);
            // goalToDo is forgotten by handleCallback                     
            if (goalToDo.wasAborted()) throw new IPAbortedException(G+" was aborted by Java-side cascading");
            if (goalToDo.wasInterrupted()) throw new IPInterruptedException(G+" was interrupted by Java-side cascading"); 
            if (result.wasInterrupted(this)) {
            	while(interrupting) Thread.yield(); // Let the interrupt request be fully served
            	throw new IPInterruptedException(G+" was interrupted, Prolog detected\n"+result.error); 
            }
            if (result.error!=null)
                throw new IPPrologError(result.error);
            if (result.timestamp!=mytimestamp)
                throw new IPException("bad timestamp in deterministicGoal, got "+result.timestamp+" instead of "+goalTimestamp);
            if (result.succeeded)
                resultToReturn = result.rVars;
        } catch (IPException e) {
            throw e;
        } catch (Exception e) {
            throw new IPException("Problem in deterministicGoal: "+e);
        }
        return resultToReturn;
    }
	
	/** A parameterless goal with no result other than success/failure. Same as deterministicGoal(G, null,null,"[]") */
	public boolean deterministicGoal(String G){
		return (deterministicGoal(G, null,null,"[]")!=null);
	}
	
	/** Useful when you're constructing objects from Prolog, but don't need to pass any from Java. Same as deterministicGoal(G,null,null,RVars) */
	public Object[] deterministicGoal(String G,String RVars){
		return deterministicGoal(G,null,null,RVars);
	}

	/** Useful when you want to pass objects to Prolog but don't need objects returned. Same as deterministicGoal(G, OVar,objectsP,"[]") */
	public boolean deterministicGoal(String G, String OVar, Object[] objectsP){
		return (deterministicGoal(G, OVar,objectsP,"[]")!=null);
	}
	
	/** Useful for inter-Prolog goal calling through Java. For example, to call a goal G in another PrologEngine E:
	buildTermModel(G,GM), javaMessage(E,SM,deterministicGoal(GM)), recoverTermModel(SM,Solution) */
	public TermModel deterministicGoal(TermModel G){
		Object[] bindings = deterministicGoal("recoverTermModel(GM,G), call(G), buildTermModel(G,SM)","[GM]",new Object[]{G},"[SM]");
		if (bindings==null) return null;
		else return (TermModel)bindings[0];
	}
	
	/** Similar to deterministicGoal, but rather than just the first solution returns an Iterator which lazily returns bindings for solutions.
	This method returns immediately, because the actual Prolog execution will happen on a different thread during the messages sent to the iterator.
	If G fails, the Iterator will have no elements. 
	 */
	public SolutionIterator goal(String G, String OVar, Object[] objectsP, String RVars){
		if (nonDeterministicGoalActive) throw new IPException("Can not call non deterministic goal until the previous goal ends");
		nonDeterministicGoalActive= true;
		return new SolutionIterator(this,G,OVar,objectsP,RVars); // That's where the real action will be		
	}
	
	/** Useful when you're constructing objects from Prolog, but don't need to pass any from Java. Same as goal(G,null,null,RVars) */
	public SolutionIterator goal(String G,String RVars){
		return goal(G,null,null,RVars);
	}
	
	public boolean lastSolutionUndefined(){
		return lastSolutionWasUndefined;
	}
	
	// Increment the goal counter
	protected int incGoalTimestamp(){
		goalTimestamp++; 
		if (goalTimestamp<0) throw new IPException("goalTimestamp did wrap around, please improve it...");
		return goalTimestamp;
	}
	
	/** Adds goal to pool awaiting execution by Prolog */
	protected synchronized void scheduleGoal(GoalToExecute g){
		goalsToExecute.addElement(g);
	}	
	
	protected synchronized GoalToExecute moreRecentToExecute(){
		for (int i=goalsToExecute.size()-1; i>=0; i--){
			GoalToExecute gte = goalsToExecute.elementAt(i);
			if (!gte.hasStarted()) return gte;
		}
		return null;
	}
	
	protected synchronized GoalToExecute findLastGTERunning(){
		for (int i=goalsToExecute.size()-1; i>=0; i--){
			GoalToExecute gte = goalsToExecute.elementAt(i);
			// System.out.println("findLastGTEWithProperThread: "+gte.getCallerThread());
			if (gte.hasStarted()&&!gte.hasEnded()) 
				return gte;
		}
		return null;
		// throw new IPException("Could not find thread for callback; currentDGthread=="+currentDGthread()+"; "+goalsToExecute.size()+" GTEs");
	}
	
	/** Currently does a dumb linear search, enough for our scenarios. 
	2 future possibilities: use Hashtable; make ResultFromProlog bring back a reference to GoalFromJava */
	protected synchronized GoalToExecute forgetGoal(int timestamp){
		for (int i=0; i<goalsToExecute.size(); i++){
			GoalToExecute gte = goalsToExecute.elementAt(i);
			if (gte.getTimestamp()==timestamp){
				goalsToExecute.removeElementAt(i);
				return gte;
			}
		}
		return null;
	}
	
	/** Just adds to messagesExecuting */
	protected synchronized void addMessage(MessageExecuting m){
		messagesExecuting.addElement(m);
	}
	
	protected synchronized void forgetMessage(MessageExecuting m){
		messagesExecuting.removeElement(m);
	}
	
	protected synchronized MessageExecuting lastMessageRequest(){
		MessageExecuting last;
		if (messagesExecuting.size()==0) last=null;
		else last = messagesExecuting.lastElement();
		return last;
	}
	
	/** The engine is doing nothing: no pending Prolog goals nor Java callbacks */
	public synchronized boolean isIdle(){
		//System.out.println("messagesExecuting.size()=="+messagesExecuting.size()+",goalsToExecute.size()=="+goalsToExecute.size());
		return messagesExecuting.size()==0 && goalsToExecute.size()==0;
	}
	
	/**
	 * @param active thread set to be filled
	 * @return the paused (javaMessage) thread, if it exists; null otherwise
	 */
	protected synchronized Thread activeThreads(HashSet<Thread> active){
		Thread pausedThread = null;
		for (GoalToExecute gte:goalsToExecute){
			Thread GT = gte.getCallerThread();
			if (GT!=null) // tolerate dummy goals present...
				active.add(GT);
		}
		for (MessageExecuting me:messagesExecuting){
			active.add(me.getCallingThread());
			if (isPausedStateMessage(me)) 
				pausedThread = me.getCallingThread();
		}
		return pausedThread;
	}

	protected synchronized boolean canAcceptNewGoal(Thread thread){
		progressMessage("entering canAcceptNewGoal for "+thread);
		if (allowSimultaneousThreads) 
			return true;
		HashSet<Thread> active = new HashSet<Thread>();
		Thread pausedThread = activeThreads(active);
		
		if (active.size()==0) { //if (active.size()==0) return true;
			if (!isIdle()) return false; // e.g. "rogue javaMessage" injected by logic shell mechanics: make the goal wait
			else return true;
		} 		 
		
		else if (active.size()==1){
			Thread T = active.iterator().next();
			if (T == thread) return true;
			else if (T == pausedThread) // timed_call pauses case
				return true;
			else if (isPaused()) // ctrl-C pauses
				return true; 
		} else if (active.contains(thread))
			return true;
		return false;
	}
	
	
	
	/** To be used only if this engine has a EngineController (single) listener 
	 * @return Whether the engine is paused in mid computation and receptive to (possibly restricted) commands */
	public synchronized boolean isPaused(){
		if (detectsPauses)
			return paused;
		if (getThePrologListener() instanceof EngineController)
			return ((EngineController)getThePrologListener()).isInPause();
		else return false;
	}

	private boolean isPausedStateMessage(MessageExecuting me){
		Object target = me.getM().target;
        if (target instanceof InvisibleObject)
        	target = getRealJavaObject((InvisibleObject)target);
        return target == this && me.getM().getRealMethodName().equals("prologCanWork") && me.getM().arguments.length==0;

	}
	public boolean isAllowSimultaneousThreads() {
		return allowSimultaneousThreads;
	}

	/** @param allowSimultaneousThreads 
	 * @see #canAcceptNewGoal(Thread) */
	public void setAllowSimultaneousThreads(boolean allowSimultaneousThreads) {
		this.allowSimultaneousThreads = allowSimultaneousThreads;
	}

	/** Do not invoke this */
    public /*no point, really...: synchronized*/ void endAllTasks(Exception e) {
        for (int i=goalsToExecute.size()-1; i>=0 ; i--){ // abort most recent first
            GoalToExecute gte = goalsToExecute.elementAt(i);
            gte.setResult(new ResultFromProlog(gte.getTimestamp(), false, 0, e, false));
        }
        cleanupTasks();
    }
    
    /** Do not invoke this. Due to the use of several Java packages in InterProlog this method must be qualified as public */
	public synchronized void abortTasks(){
		for (int i=0; i<goalsToExecute.size(); i++){
			GoalToExecute gte = goalsToExecute.elementAt(i);
			gte.abort();
		}
		cleanupTasks();
	}
	
    /** Do not invoke this. Due to the use of several Java packages in InterProlog this method must be qualified as public */
	public synchronized void interruptTasks(){
		for (int i=0; i<goalsToExecute.size(); i++){
			GoalToExecute gte = goalsToExecute.elementAt(i);
			gte.interrupt();
		}
		cleanupTasks();
	}
	
	protected void cleanupTasks(){
		for (GoalToExecute gte: goalsToExecute){
			Thread t = gte.getCallerThread();
			if (t!=null && t.isAlive() && shutingDown) 
				t.interrupt();
		}
		goalsToExecute.removeAllElements();
		messagesExecuting.removeAllElements();
	}
	
	/** This does NOT call cleanupTasks */
	protected void endAllJavaMessages(Exception e){
		for (int m=messagesExecuting.size()-1 ; m>=0; m--){
			MessageExecuting M = messagesExecuting.elementAt(m);
			M.setResult(new ResultFromJava(M.getTimestamp(), null, e, null));
		}
	}
	
	/** Present implementation is always available, so this always returns true. Subclass implementations should return false if the Prolog engine 
	is not available for work, e.g. prompt was not yet recognized in a subprocess (socket) engine */
	public boolean isAvailable(){
		return true;
	}
	
	public void waitUntilAvailable(){
		try{
            while(!isAvailable()) Thread.sleep(0,1);
		} catch (InterruptedException e){
			throw new IPException("Bad interrupt: "+e); 
		}
	}
	
	/** Sleeps the current Java thread until this engine is idle. If this never happens, we're in trouble. */
	public void waitUntilIdle(){
		try{
			while(!isIdle()) Thread.sleep(0,1);
		} catch (InterruptedException e){
			throw new IPException("Bad interrupt: "+e); 
		}
	}
	
	/** Handling of javaMessages and deterministicGoals. This is where most things happen. 
	@param x Argument of the callback predicate*/
	public Object handleCallback(Object x){
		progressMessage("Entering handleCallback");
		if (x instanceof ClassNotFoundException) 
			return new ResultFromJava(0,null,(ClassNotFoundException)x,null); 
		else if (x instanceof MessageFromProlog){
			MessageFromProlog mfp = (MessageFromProlog)x;
			progressMessage("handling "+mfp);
			// System.err.println("MessageFromProlog "+mfp.methodName+" in:"); System.out.println(Thread.currentThread()); System.out.println(this);
			// first message is a dummy just to get us here:
			if (!isFirstJavaMessage(mfp)){
				GoalToExecute gte = findLastGTERunning();
				Thread calling = (gte == null ? null : gte.getCallerThread());
				MessageExecuting me = new MessageExecuting(mfp,this,calling);
				synchronized(this){
					addMessage(me); // here?
					if (threadedCallbacks || mfp.requiresNewThread()) new Thread(me,"threaded callback").start();
					else {
						GoalToExecute lastGTE = findLastGTERunning();
						if (lastGTE!=null && calling!=null /* there is a real dg running, not just some dummy concocted for OnTop...*/) {
							//if (lastGTE.getCallerThread()!=currentDGthread())
							//	throw new IPException("Inconsistent threads. Expected "+currentDGthread()+" and got "+lastGTE.getCallerThread());
							lastGTE.executeInThread(me);
						} else new Thread(me,"GoalToExecute").start(); // had to uncomment this, as top level javaMessages would break
						// else throw new IPException("Could not find proper GoalToExecute");
						// problematic... if lastGTE == null me.run(); // If there's no active deterministicGoal, use this thread...
					}
				}
				// not here anymore ??? addMessage(me);
			} else {
				progressMessage("received first (dummy) javaMessage");
				topGoalHasStarted = true; // this branch actually executes only for AbstractNativeEngines
			}
		} else if (x instanceof ResultFromProlog){
			ResultFromProlog rfp = (ResultFromProlog)x;
			synchronized(this){
				progressMessage("handling "+rfp);
				GoalToExecute gte = forgetGoal(rfp.timestamp);
				progressMessage("forgot goal "+gte+"; isIdle()=="+isIdle());
				// if (gte==null) throw new IPException("Could not find goal "+rfp.timestamp);
				if (gte!=null)
					gte.setResult(rfp);
			}
		} else throw new IPException("bad object in handleCallback: "+x);
		// no errors so far
		progressMessage("About to leave handleCallback");
		return doSomething();
	}
	/** return result to last javaMessage or pick more recent GoalToExecute */	
	protected Object doSomething(){
		while(!shutingDown){
			// let some work proceed elsewhere; this seems light enough for now, if not use wait/notify around here too:
			if (isIdle())
				try { Thread.sleep(50,100); }
				catch (InterruptedException e){throw new IPException("Bad interrupt: "+e);}
			else // Thread.yield(): too heavy, for ex. when in a modal dialog
				try { Thread.sleep(50,100); }
				catch (InterruptedException e){throw new IPException("Bad interrupt: "+e);}
			synchronized(this){
				MessageExecuting last = lastMessageRequest();
				//System.err.println("last message:"+last);
				if (last!=null && last.hasEnded()) {
					return last;
				} 
				GoalToExecute gte = moreRecentToExecute();
				//System.err.println("Last goal to execute:"+gte);
				if (gte!=null){
					// insufficient! currentDGthread = gte.getCallerThread();
					// gte.prologWasCalled();
					return gte; 
				} // else should clear all messages executing ??
			}
		}
		return null;
	}
	
	private boolean isFirstJavaMessage(MessageFromProlog mfp){
		if (!mfp.getRealMethodName().equals(firstJavaMessageName)) return false;
		if ((mfp.target instanceof InvisibleObject)&& getRealJavaObject((InvisibleObject)mfp.target)==this)
			return true;
		else return false;
	}
	
	/** Dummy method, whose name is used to start the callback thread */
	public final void firstJavaMessage(){
		throw new IPException("This should never be called, bad javaMessage handling");
	}
	
	/** Execute a Prolog->Java call */
    public ResultFromJava doCallback(Object x){
        progressMessage ("Starting handling of XSB->Java callback: "+nl+x);
        if (x==null | ! (x instanceof MessageFromProlog) )
            return new ResultFromJava (0,null,null,null);
        MessageFromProlog callback = (MessageFromProlog)x;
        fireJavaMessaged();
        Class<?> formalArguments[] = new Class[callback.arguments.length];
        Object[] localArguments = new Object[callback.arguments.length];
        
        Object target=null;
        Object result = null; Exception exception=null;
        
        try {
            if (callback.target instanceof InvisibleObject){
                target = getRealJavaObject((InvisibleObject)callback.target);
            } else if(callback.target instanceof IPClassObject){
                target = Class.forName(((IPClassObject)(callback.target)).classname);
            } else if (callback.target instanceof IPClassVariable) {
                IPClassVariable tempTarget = (IPClassVariable)(callback.target);
                Class<?> tempClass = Class.forName(tempTarget.className);
                target = tempClass.getField(tempTarget.variableName).get(tempClass);
            } else
                target = callback.target;
            
            for(int a=0;a<formalArguments.length;a++){
                localArguments[a] = callback.arguments[a];
                if (localArguments[a]!=null){
                    if(localArguments[a] instanceof BasicTypeWrapper) {
                        BasicTypeWrapper btw = (BasicTypeWrapper)(localArguments[a]);
                        formalArguments[a] = btw.basicTypeClass();
                        localArguments[a] = btw.wrapper; // Integer instead of int ???
                    } else if (localArguments[a] instanceof InvisibleObject) {
                        localArguments[a] = getRealJavaObject((InvisibleObject)localArguments[a]);
                        formalArguments[a] = localArguments[a].getClass();
                    } else if (localArguments[a] instanceof IPClassObject) {
                        localArguments[a] = Class.forName(((IPClassObject)(localArguments[a])).classname);
                        formalArguments[a] = Class.class;
                    } else if (localArguments[a] instanceof IPClassVariable) {
                        IPClassVariable IPCV = (IPClassVariable)(localArguments[a]);
                        Class<?> tempClass = Class.forName(IPCV.className);
                        localArguments[a] = tempClass.getField(IPCV.variableName).get(null);
                        formalArguments[a] = localArguments[a].getClass();
                    } else formalArguments[a] = localArguments[a].getClass();
                }
            }
            Method method=null;
            if (target instanceof Class){
                if (shortClassName ((Class<?>)target).equals (callback.getRealMethodName())) {
                    // It's a (public...) constructor invocation
                    //Constructor constructor = ((Class)target).getConstructor(formalArguments);
                    Constructor<?> constructor = findConstructor ((Class<?>)target,formalArguments);
                    constructor.setAccessible(true);
                    result = constructor.newInstance(localArguments);
                } else {
                    // It's a class (static) method invocation; 
                    // some day we may want to consider messaging the class object directly, e.g. to fetch annotations
                    // Method method = ((Class)target).getMethod(callback.methodName,formalArguments);
                    method = findMethod((Class<?>)target,callback.getRealMethodName(),formalArguments);
                    //result = method.invoke(target,localArguments);
                    method.setAccessible(true);
                    result = method.invoke(target,localArguments);
                }
            } else if (target.getClass().isArray()) {
            	if (callback.getRealMethodName().equals("get"))
            	// obtain array element
            		result = Array.get(target, ((Integer)localArguments[0]).intValue());
            	else if (callback.getRealMethodName().equals("length"))
            		result = Array.getLength(target);
            	else
            		throw new IPException("Bad message to array object:"+callback.getRealMethodName());
            } else {
                // A regular instance method invocation
                // Method method = target.getClass().getMethod(callback.methodName,formalArguments);
                //result = findMethod(target.getClass(),callback.methodName,formalArguments).invoke(target,localArguments);
                method = findMethod(target.getClass (),callback.getRealMethodName(),formalArguments);
                method.setAccessible(true);
                result = method.invoke(target,localArguments);
            }
            // The result will be an invisible object, except if a String or a wrapper or a TermModel or a serializable array...
            // ...or if this is a getRealJavaObject message sent to the PrologEngine
            if (result!=null && !(target==this && method.equals(getRealJavaObjectMethod)) && !(result instanceof InvisibleObject) 
            && !(result instanceof String) && !(result instanceof TermModel) 
            && !(result.getClass().isArray() && isSerializable(result))
            && ! BasicTypeWrapper.instanceOfWrapper(result))
                result = makeInvisible(result);
        } catch (Exception e) {
            exception=e;
        }
        String exceptionMessage = null;
        if (exception!=null) {
            if (debug) {
                System.err.println("Exception in "+callback.getRealMethodName()+" in doCallback: ");
                exception.printStackTrace();
            }
            if (exception.getCause() instanceof Exception)
            	exception = (Exception)exception.getCause();
            exceptionMessage = exception.getMessage();
            // exceptionMessage will be a ClassName-Message term
            if (exceptionMessage==null)
            	exceptionMessage = "'"+exception.getClass().getName()+"'-''";
            else
            	exceptionMessage = "'"+exception.getClass().getName()+"'-'"+TermModel.doubleQuotes(exceptionMessage)+"'";
        }
        if (callback.returnArguments)
            return new ResultFromJava (callback.timestamp,result,exceptionMessage,callback.arguments);
        else
            return new ResultFromJava (callback.timestamp,result,exceptionMessage,null);
    }
	
	/** Returns true if the object is Serializable or, being an array, its element type is primitive or Serializable */
	public static boolean isSerializable(Object x){
		if (x.getClass().isArray())
			return x.getClass().getComponentType().isPrimitive() || Serializable.class.isAssignableFrom(x.getClass().getComponentType());
		else return (x instanceof Serializable);
	}
	
    /** An utility building on the functionality of getMethod(), to provide the javaMessage predicate with method argument
     * polimorphism. If the type signatures do not match exactly, searches all method signatures to see if their arguments
     * are type-compatible.
     */
    public static Method findMethod (Class<?> targetClass,String name,Class<?>[] formalArguments) throws NoSuchMethodException{
        Method m = null;
        try{
            m = targetClass.getMethod (name,formalArguments);
            return m;
        }
        catch(NoSuchMethodException e) {
            // Let's try to find "by hand" an acceptable method
            Method[] allMethods = targetClass.getMethods ();
            for (int i=0; i<allMethods.length;i++){
                if (allMethods[i].getName ().equals (name) && allMethods[i].getParameterTypes ().length == formalArguments.length){
                    boolean compatible = true;
                    for (int j=0; j<formalArguments.length; j++) {
                        if (!assignableType (allMethods[i].getParameterTypes ()[j],formalArguments[j])) {
                            compatible = false;
                            break;
                        }
                    }
                    if (compatible){
                        m = allMethods[i];
                        break;
                    }
                }
            }
            if (m==null) {
				/*
				System.err.println ("Could not find "+name+" in "+targetClass);
				System.err.println ("Argument types:");
				for (int i=0; i<formalArguments.length;i++)
					System.err.println (i+":"+formalArguments[i]);
					*/
                throw e;
            }else return m;
        }
    }
    
    /** Similar to findMethod(), but for constructors rather than regular methods */
    public static Constructor<?> findConstructor (Class<?> targetClass,Class<?>[] formalArguments) throws NoSuchMethodException{
        Constructor<?> m = null;
        try{
            m = targetClass.getConstructor (formalArguments);
            return m;
        }
        catch(NoSuchMethodException e) {
            // Let's try to find "by hand" an acceptable constructor
            Constructor<?>[] allConstructors = targetClass.getConstructors ();
            for (int i=0; i<allConstructors.length;i++){
              //System.out.println("Lengths"+":"+allConstructors[i].getParameterTypes ().length +":"+ formalArguments.length);
                if (allConstructors[i].getParameterTypes ().length == formalArguments.length){
                    boolean compatible = true;
                    for (int j=0; j<formalArguments.length; j++) {
                    	//System.out.println("arg "+j+":"+allConstructors[i].getParameterTypes ()[j]+":"+formalArguments[j]);
                        if (!assignableType (allConstructors[i].getParameterTypes ()[j],formalArguments[j])) {
                            compatible = false;
                            break;
                        }
                    }
                    if (compatible){
                        m = allConstructors[i];
                        break;
                    }
                }
            }
            if (m==null) throw e;
            else return m;
        }
    }
    
    /** It is OK to assign an expression typed right to a variable typed left. Delegates on isAssignableFrom*/
    public static boolean assignableType (Class<?> left,Class<?> right){
        if (right==null) return true;
        else return left.isAssignableFrom (right);
    }
    
    /**
     * Returns just the name of the class, with no package information.
     * That is, if <CODE>foo.bar.Mumble</CODE> were the class passed in, the
     * string <CODE>"Mumble"</CODE> would be returned.  Similarly, if the class
     * that is passed in is <CODE>a.b.Class$InnerClass</CODE> the string
     * returned would be <CODE>InnerClass</CODE>.
     */
    public static String shortClassName (Class<?> c){
        String s = c.getName ();
        int dot = s.lastIndexOf (".");
        int dollar = s.lastIndexOf ("$");
        if (dot==-1 && dollar==-1) return s;
        else return s.substring (Math.max (dot,dollar)+1,s.length ());
    }

	/** Add a jar to the runtime classpath
	 * Grabbed from http://stackoverflow.com/questions/1010919/adding-files-to-java-classpath-at-runtime?answertab=active#tab-top */
	public static void addSoftwareLibrary(File file) throws Exception {
		Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
		method.setAccessible(true);
		method.invoke(ClassLoader.getSystemClassLoader(), new Object[]{file.toURI().toURL()});
	}
	
	public static void addSoftwareLibrary(String file) throws Exception {
		addSoftwareLibrary(new File(file));
	}

	/** Register an object with this Engine, so it later can be referred from Prolog without serializing it.
	@param x Object to be registered
	@return Integer denoting the object. In Prolog one can then refer to it by using the InvisibleObject class.
	@see InvisibleObject
	*/
	public int registerJavaObject(Object x){
		return knownObjects.registerJavaObject(x);
	}
	
	/** Register an object with this Engine, so it later can be referred from Prolog without serializing it, and returns
	an InvisibleObject encapsulating the reference.
	@param x Object to be registered
	@return InvisibleObject denoting the object. In Prolog one can then refer to it by using the InvisibleObject class.
	@see InvisibleObject
	*/
	public Object makeInvisible(Object x){
		return knownObjects.makeInvisible(x);
	}
	
	/** Get the object referred by the integer in a InvisibleObject wrapper.
	@param o An InvisibleObject
	@return The real object denoted by o in the context of this engine
	@see InvisibleObject
	*/
	public Object getRealJavaObject(InvisibleObject o){
		return knownObjects.getRealJavaObject(o);
	}
	/** Same as getRealJavaObject(InvisibleObject), but accepts an integer ID as argument instead */
	public Object getRealJavaObject(int ID){
		return knownObjects.getRealJavaObject(ID);
	}

	/** Just returns the object, untouched (but "dereferenced" if called from Prolog). This serves the need to get objects in 
	javaMessage because of the way CallbackHandler.doCallback works. For example:
	ipPrologEngine(_E), stringArraytoList(_O,[miguel,calejo]), 
	javaMessage(_E,_R,getRealJavaObject(_O)),stringArraytoList(_R,List).
	... will bind List to [miguel,calejo] and not to an InvisibleObject specification as ordinarly would happen
	 */
	public Object getRealJavaObject(Object o){
		return o;
	}
	
	/**
	 * Removes reference to the object from the registry. This method should be 
	 * used with extreme caution since any further prolog calls to the object by means 
	 * of reference to it in the registry might result in unpredictable behaviour.
	 */
	public boolean unregisterJavaObject(int ID){
		return knownObjects.unregisterJavaObject(ID);
	}

			/**
	 * Removes reference to the object from the registry. This method should be 
	 * used with extreme caution since any further prolog calls to the object by means 
	 * of reference to it in the registry might result in unpredictable behaviour.
	 */
	public boolean unregisterJavaObject(Object obj){
		return knownObjects.unregisterJavaObject(obj);
	}
	
	/**
	 * Removes references to objects of class <code>cls</code> from the registry. This method should be 
	 * used with extreme caution since any further prolog calls to the unregistered objects by means 
	 * of reference to them in the registry might result in unpredictable behaviour.
	 */
	public boolean unregisterJavaObjects(Class<?> cls){
		return knownObjects.unregisterJavaObjects(cls);
	}
        
	/** If true, the Java execution of javaMessage predicates will happen in new threads (default);
	if false, execution will be under the thread of the most recent deterministicGoal currently executing in Prolog */
	public void setThreadedCallbacks(boolean yes){
		threadedCallbacks = yes;
	}
	
	public boolean isThreadedCallbacks(){
		return threadedCallbacks;
	} 
	
	// test with javaMessage('com.declarativa.interprolog.AbstractPrologEngine',printAllStackTraces).
	/** If !quite, does not print to the standard (error) output */
	public static String printAllStackTraces(boolean quiet){
		StringBuilder S = new StringBuilder();
		Map<Thread,StackTraceElement[]> stacks = Thread.getAllStackTraces();
		for (Thread T : stacks.keySet()){
			S.append(T+"\n");
			StackTraceElement[] stack = stacks.get(T);
			for (int i=0; i<stack.length; i++)
				S.append("  "+ stack[i]+"\n");
		}
		if (!quiet) System.err.println(S.toString());
		return S.toString();
	}
	public static String printAllStackTraces(){
		return printAllStackTraces(false);
	}
	
	public static void printStackTrace(){
		System.err.println(Arrays.toString(Thread.currentThread().getStackTrace()));
	}

    /** We know the engine to be in the Prolog shell, so Prolog goals do not need a postfix. Subclasses 
    supporting other languages over Prolog may need to redefine this */
    public boolean inPrologShell(){
    	return true;
    }
    
    public void addPrologEngineListener(PrologEngineListener L){
    	listeners.add(L);
    }
    public void removePrologEngineListener(PrologEngineListener L){
    	listeners.remove(L);
    }
    
    /** Kludgy method to be used only if a single listener exists */
    public PrologEngineListener getThePrologListener(){
    	if (listeners.isEmpty())
    		return null;
    	if (!(listeners.size()==1))
    		System.err.println("Bad use of getThePrologListener");
    	return listeners.get(0);
    }
    
    /** Gracefully stop the engine computation */
    public void stop(){
    	stop(true);
    }
    protected void stop(boolean attemptInterrupt){
    	if (usingTimedCall && listeners.size()==1 && listeners.get(0) instanceof EngineController)
    		((EngineController)listeners.get(0)).stop();
    	else if (attemptInterrupt)
    		interrupt();
    	else throw new IPException("Inconsistency stopping engine");
    }
    
    /** Returns null if all listeners allow Prolog to continue execution. Stops after the first refusal, returning the reason (message)*/
    protected String fireWillWork(){
    	String M=null;
    	for (PrologEngineListener L:listeners)
    		if ((M=L.willWork(this))!=null) return M;
    	return null;
    }
    protected synchronized void fireJavaMessaged(){
    	for (PrologEngineListener L:listeners)
    		L.javaMessaged(this);
    }
    protected synchronized void fireAvailabilityChange(){
    	for (PrologEngineListener L:listeners)
    		L.availabilityChanged(this);
    }
    /** If ms>0, all future deterministic goals will be executed under control so that the Java side is messaged (with @see#willWork messages) 
    every ms, with the possibility to abort the Prolog computation, by having the Prolog engine interrupt normal execution periodically;
     otherwise the goal will run uninterrupted.
    At startup goals execute without willWork messages.
    NOTE: This functionality requires a fj_ABORT_NOTRACE/2 predicate in usermod, see comments in interprolog.P */
    public void setTimedCallIntervall(int ms){
    	if (!interPrologFileLoaded)
    		throw new IPException("timed calls can be requested only after initialization");
    	if (ms <= 0) {
    		deterministicGoal("retractall(ipTimedCallActive(_))");
    		usingTimedCall = false;
    		return;
    	}
    	if (detectsPauses)
    		System.err.println("Likely inconsistenty: using timed_call and detecting breaks");
    	if (!deterministicGoal("retractall(ipTimedCallActive(_)), asserta(ipTimedCallActive("+ms+"))"))
    		throw new IPException("Could not set up timed calls");
    	usingTimedCall = true;
    }
    /** Never call this directly; the Prolog side does call this automatically during timed calls 
     * @return null if the computation can proceed, or an error message if not */
    // Do NOT change this method's name without revising isPausedStateMessage()
    public String prologCanWork(){
    	return fireWillWork();
    }
    
    /** Never call this directly; the Prolog side does call this automatically if detectsPauses */
    public void prologEnteredBreak(){
    	paused = true;
    	// prior to breaking, write out the break stats and cheatsheet
    	deterministicGoal("flora2:print_break_message");
    	fireAvailabilityChange();
    }
    
    /** Never call this directly; the Prolog side does call this automatically if detectsPauses */
    public void prologResumedComputation(){
    	paused = false;
    	fireAvailabilityChange();
    }
    
    /** Never call this directly; the Prolog side does call this automatically if detectsPauses */
    public void prologReturnedToTopLevel(){
    	paused = false;
    	fireAvailabilityChange();
    }
    
    public String toString(){
    	return getClass().getName()+
    			"\ngoalsToExecute:"+goalsToExecute+
    			"\nmessagesExecuting:"+messagesExecuting;
    }
    
    /** Infinite Java loop, for testing only! 
     * @throws InterruptedException */
    public static void loop() throws InterruptedException{
    	while(true)
			Thread.sleep(1);
    }
    public void loop2(){
    	deterministicGoal("java('com.declarativa.interprolog.AbstractPrologEngine',loop)");
    }
    
    public synchronized boolean executingOnJavaSide(){
    	return messagesExecuting.size() > goalsToExecute.size(); 
    }

    public static double currentTimeSecs() {
        return ((double) System.currentTimeMillis())/1000;
        /*
          // This does not seem to be giving the current CPU time
          // for the Studio thread
        ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
        return bean.isCurrentThreadCpuTimeSupported( ) ?
            ((double)bean.getCurrentThreadUserTime())/1000000 
            : ((double) System.currentTimeMillis())/1000;
        */
    }
}
