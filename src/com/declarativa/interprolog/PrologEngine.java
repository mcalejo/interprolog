/*
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
 */

package com.declarativa.interprolog;

import com.declarativa.interprolog.util.InvisibleObject;
import java.io.File;

/**
 * The heart of InterProlog; a PrologEngine represents a Prolog machine instance.
 */
public interface PrologEngine{
    
    /** InterProlog version */
    public final static String version = "3.0.0";

    /** Returns the Prolog system version for this engine */
    public String getPrologVersion();
    
    /** Returns the git log hash */
    public String getGitHash();
    
    // Ideally these should be static methods in the peer or implementation of
    // PrologEngine:s
    /**
     * Maximum integer value in any supported Prolog. XSB Prolog uses 28 bits.
     * Unfortunately, it is pointless to intercept writeInt in
     * <CODE>ObjectOutputStream</CODE>, because
     *   <ol>
     *     <li> the method is invoked in several situations, not just to
     *          serialize int values, and</li>
     *     <li> some classes may serialize in very weird ways</li>
     *   </ol>
     * so in general it is not possible to detect int values beyond these
     * boundaries
     */
	public static final int MAX_INT_VALUE = 134217727;
	/** Minimum integer value.*/
	public static final int MIN_INT_VALUE = -134217728;

    /**
     * Release Prolog engine resources, making it unusable
     */
    public void shutdown();
    
    /**
     * Extracts a Prolog file from the jar file or directory where the requester's class came from,
     * and asks Prolog process to reconsult it. You should use this method only after your
     * program is stable.
     * The Prolog file is extracted into a temporary file, that is automatically deleted on exiting the application.
     * @param filename The Prolog file name, including extension; if it has no extension, the Prolog file extensions are appended in turn until a file is found
     * @param requester Defines where the Prolog file resides
     * @see #consultRelative(String, Object)
     * @see #load_dynRelative(String, Object)
     */
    public String consultFromPackage(String filename,Object requester);

    /** Subsequent calls will be ignored */
    public String consultFromPackage(String filename,Object requester,boolean firstTimeOnly);

    /** Interrupt Prolog and make it return to its top level. This is the equivalent to performing a ctrl+c or similar command
     * when using Prolog under a standard console shell. */
    public void interrupt();
    
    
    /** Execute a Prolog "command". This is the simplest way to call a Prolog goal, but it's also the less reliable under error conditions;
    unless you have a good reason for using it, use deterministicGoal(String) instead. @see #deterministicGoal(String) */
    public boolean command(String s);
    
    /** Debug messages are being written, both Java and Prolog side, cf. ipIsDebugging/0
     */
    public boolean isDebug();
    
    /** Show (or hide) debug messages, both Java and Prolog side, cf. ipIsDebugging/0. Beware that this may try to assert/retract
     * a flag (ipIsDebugging/0) on the Prolog side, be sure to invoke this method sooner rather than later as it may get "stuck" if there
     * are problems communicating with the Prolog engine
     */
    public void setDebug(boolean d);
    
    /** Synchronously calls a Prolog goal.
     * Only the first solution is considered. G should contain a syntactically correct
     * Prolog term, without the trailing dot (.). Throws an IPAbortedException if a Prolog abort happens, and an
     * IPInterruptedException if the interrupt() method was invoked.
     * @see #deterministicGoal(String)
     * @see #deterministicGoal(String,String)
     * @see #deterministicGoal(String,String,Object[])
     * @return	a new array containing an object for each term in the rVars list, or null if goal fails
     * @param G Prolog goal term
     * @param OVar Prolog variable that will be bound to objectsP array
     * @param objectsP Array of Java objects to pass to Prolog goal
     * @param RVars Prolog list with object specifications, typically containing variables occurring in g.
     * If null a single binding will be returned, containing a TermModel object representing the goal term solution
     */
    public Object[] deterministicGoal(String G, String OVar, Object[] objectsP, String RVars);
    
    /** A parameterless goal with no result other than success/failure. Same as deterministicGoal(G, null,null,"[]") */
    public boolean deterministicGoal(String G);
    
    /** Useful when you're constructing objects from Prolog, but don't need to pass any from Java. Same as deterministicGoal(G,null,null,RVars) */
    public Object[] deterministicGoal(String G,String RVars);
    
    /** Useful when you want to pass objects to Prolog but don't need objects returned. Same as deterministicGoal(G, OVar,objectsP,"[]") */
    public boolean deterministicGoal(String G, String OVar, Object[] objectsP);
    
    /**
     * Useful for inter-Prolog goal calling through Java. For example, to call a
     * goal G in another PrologEngine E:
     *   <PRE>
     *    buildTermModel(G,GM),
     *    javaMessage(E,SM,deterministicGoal(GM)),
     *    recoverTermModel(SM,Solution)
     *   </PRE>
     */
    public TermModel deterministicGoal(TermModel G);
    
	/** Similar to deterministicGoal, but rather than just the first solution returns an Iterator which lazily returns bindings for solutions.
	This method returns immediately, because the actual Prolog execution will happen during the messages sent to the iterator.
	If G fails, the Iterator will have no elements. 
	 */
    public SolutionIterator goal(String G, String OVar, Object[] objectsP, String RVars);
    public SolutionIterator goal(String G, String RVars);
    
    public boolean lastSolutionUndefined();

    /**
     * Register an object with this Engine, so it later can be referred from Prolog without serializing it.
     * @param x Object to be registered
     * @return Integer denoting the object. In Prolog one can then refer to it by using the InvisibleObject class.
     * @see InvisibleObject
     */
    public int registerJavaObject(Object x);
    
    /**
     * Register an object with this Engine, so it later can be referred from Prolog without serializing it, and returns
     * an InvisibleObject encapsulating the reference.
     * @param x Object to be registered
     * @return InvisibleObject denoting the object. In Prolog one can then refer to it by using the InvisibleObject class.
     * @see InvisibleObject
     */
    public Object makeInvisible(Object x);
    
    /**
     * Get the object referred by the integer in a InvisibleObject wrapper.
     * @param o An InvisibleObject
     * @return The real object denoted by o in the context of this engine
     * @see InvisibleObject
     */
    public Object getRealJavaObject(InvisibleObject o);
    
    /**
     * Same as getRealJavaObject(InvisibleObject), but accepts an integer ID as
     * argument instead
     */
    public Object getRealJavaObject(int ID);
    
    /**
     * Just returns the object, untouched (but "dereferenced" if called from Prolog). This serves the need to get objects in
     * javaMessage because of the way CallbackHandler.doCallback works. For example:
     * ipPrologEngine(_E), stringArraytoList(_O,[miguel,calejo]),
     * javaMessage(_E,_R,getRealJavaObject(_O)),stringArraytoList(_R,List).
     * ... will bind List to [miguel,calejo] and not to an InvisibleObject specification as ordinarly would happen
     */
    public Object getRealJavaObject(Object o);
    
    /**
     * Removes reference to the object from the registry. This method should be
     * used with extreme caution since any further prolog calls to the object by means
     * of reference to it in the registry might result in unpredictable behaviour.
     */
    public boolean unregisterJavaObject(int ID);
    
    /**
     * Removes reference to the object from the registry. This method should be
     * used with extreme caution since any further prolog calls to the object by means
     * of reference to it in the registry might result in unpredictable behaviour.
     */
    public boolean unregisterJavaObject(Object obj);
    
    /**
     * Removes references to objects of class <code>cls</code> from the registry. This method should be
     * used with extreme caution since any further prolog calls to the unregistered objects by means
     * of reference to them in the registry might result in unpredictable behaviour.
     */
    public boolean unregisterJavaObjects(Class<?> cls);
    
    /**
     * Returns a boolean value indicating whether the engine is available (that
     * is, ready to accept requests).
     */
    public boolean isAvailable();
    
    /**
     * This method blocks until <CODE>isAvailable()</CODE> returns true.
     */
    public void waitUntilAvailable();
    
    /**
     * This method returns true if this engine is idle (doing nothing: no pending Prolog goals nor Java callbacks), false otherwise.
     */
    public boolean isIdle();
    
    /**
     * This method blocks until <CODE>isIdle()</CODE> returns true.
     */
    public void waitUntilIdle();
    
    /**
     * Same as #teachMoreObjects(ObjectExamplePair[]), but the single example
     * pair is constructed repeating the object.
     *
     * @see #teachMoreObjects(ObjectExamplePair[])
     */
    public boolean teachOneObject(Object example);
    
    /**
     * Same as #teachMoreObjects(ObjectExamplePair[]), but example pairs are
     * constructed with (2) repeated examples for each object.
     *
     * @see #teachMoreObjects(ObjectExamplePair[]) */
    public boolean teachMoreObjects(Object[] examples);
    
    /** Send an array of object example pairs to Prolog and generate ipObjectSpec facts.
     * Returns true if this succeeds, false otherwise.
     * @param examples The examples
     * @see ObjectExamplePair */
    public boolean teachMoreObjects(ObjectExamplePair[] examples);
    
    /** Consults a Prolog file */
    public boolean consultAbsolute(File f);
    
    public boolean load_dynAbsolute(File f);
    
    /** Consults a Prolog file from the directory where the requester's class would come from if it
     * did not come from a jar file. Adds that directory to the library_directory relation, so modules can be found there
     * @param filename The Prolog file name, including suffix; if a path it should use '/' as the separator, independently of the OS
     * @param requester Defines where the Prolog file resides
     */
    public String consultRelative(String filename,Object requester);
    
    public String load_dynRelative(String filename,Object requester);
 
    public PrologImplementationPeer getImplementationPeer();
    
    /** Returns the installation directory for the Prolog system, without the trailing separator char */
	public String getPrologBaseDirectory();
    
	/** If true, the Java execution of javaMessage predicates will happen in new threads (default);
	if false, execution will be under the thread of the deterministicGoal currently executing in Prolog */
    public void setThreadedCallbacks(boolean yes);

	public boolean inPrologShell();	
	
	public String unescapedFilePath(String p);
}