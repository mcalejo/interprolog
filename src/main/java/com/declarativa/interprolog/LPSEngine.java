package com.declarativa.interprolog;

import java.io.File;
import java.util.Arrays;

import com.declarativa.interprolog.util.IPException;

public class LPSEngine extends XSBSubprocessEngine {
	public interface CycleHandler{
		/** Notification that LPS is starting a cycle, reporting the current state and last actions, and returning input events for LPS to continue.
		 * LPS execution hangs during the execution of this method.
		 * Fluents and actions reported are those specified via {@link LPSEngine#go(String, String, String)}
		 * @param T the cycle about to start
		 * @param fluents 
		 * @param actions
		 * @return a well-formed Prolog list, e.g. "[]" or "[time_to_eat(philosopher(0)),time_to_eat(philosopher(1))]"; or null to stop the LPS computation
		 */
		public String handleCycle(int T, TermModel[] fluents, TermModel[] actions);
	}
	
	CycleHandler handler;

	/**
	 * @param prologCommands
	 * @param outAndErrMerged
	 * @param debug
	 * @param loadFromJar This does NOT affect the LPS files, which are never loaded from the jar
	 * @param LPSdir
	 */
	public LPSEngine(String[] prologCommands, boolean outAndErrMerged, boolean debug, boolean loadFromJar, String LPSdir) {
		super(prologCommands, outAndErrMerged, debug, loadFromJar);
    	File LD = new File(LPSdir,"engine"+File.separator+"interpreter.P");
    	File LP = new File(LPSdir,"utils"+File.separator+"psyntax.P");
    	File API = new File(LPSdir,"utils"+File.separator+"api.P");
    	if (!consultAbsolute(LD) || !consultAbsolute(LP)|| !consultAbsolute(API))
    		throw new IPException("Could not load LPS");
    	handler = null;
	}
	public LPSEngine(String prologCommand, String LPSdir){
		this(new String[]{prologCommand}, true, false, true, LPSdir);
	}

	/** Load, execute a LPS file
	 * @param f .lpsw or .lps file; .pl is taken as a .lps file; other file extensions are assumed to denote .lpsw (internal syntax) files
	 * @param fluents templates of fluent instances to inspect/receive at each cycle; must be a well-formed Prolog list, e.g. "[myfluent(1,_)]"
	 * @param actions ditto for (basic) actions
	 * @param moreOptions Prolog list with additional options for the LPS go(...) predicate
	 * @param evenMoreOptions Optional; appended to the previous
	 * @return whether the LPS interpreter top level predicate succeeds
	 */
	public boolean go(String f,String fluents,String actions,String moreOptions,String evenMoreOptions) {
		String options = "[cycle_hook(lps_java_hook,"+fluents+","+actions+"),silent]";
		if (evenMoreOptions==null) 
			evenMoreOptions="[]";
		String FF = new File(f).getAbsolutePath();
		boolean R;
		if (f.toLowerCase().endsWith(".lps") || f.toLowerCase().endsWith(".pl"))
			R = deterministicGoal("basics:append("+options+","+moreOptions+",AllOptions),basics:append(AllOptions,"+evenMoreOptions+",AllOptions_),psyntax:golps(FF,AllOptions_)", "[string(FF)]", new Object[]{FF});
		else 
			R = deterministicGoal("basics:append("+options+","+moreOptions+",AllOptions),basics:append(AllOptions,"+evenMoreOptions+",AllOptions_),interpreter:go(FF,AllOptions_)", "[string(FF)]", new Object[]{FF});
		return R;
	}
	public boolean go(String f,String fluents,String actions,String moreOptions){
		return go(f,fluents,actions,moreOptions,"[]"); 
	}
	public boolean go(String f,String fluents,String actions) {
		return go(f,fluents,actions,"[]","[]");
	}
	
	public String cycleHook(int T, TermModel[] fluents, TermModel[] actions){
		if (handler==null) return "[]";
		else return handler.handleCycle(T,fluents,actions);
	}
	
	public void setCycleHandler(CycleHandler H){
		if (!isAvailable())
			throw new IPException("The LPS cycle handler can only be changed after the execution ends.");
		handler = H;
	}
	/** Execute the dining philosophers example */
	public static void main(String[] args) {
		LPSEngine E = new LPSEngine("/Users/mc/subversion/XSB/bin/xsb","/Users/mc/git/lps_corner");
		//E.printPrologOutputToConsole(); // for debugging convenience only
		E.setCycleHandler(new CycleHandler(){
			@Override
			public String handleCycle(int T, TermModel[] fluents, TermModel[] actions) {
				System.out.println("Entering LPS cycle "+T);
				System.out.println("Fluents: "+Arrays.toString(fluents));
				System.out.println("Actions: "+Arrays.toString(actions));
				// Let's inject a couple observations at cycle 3:
				if (T==3) return "[time_to_eat(philosopher(0)),time_to_eat(philosopher(1))]";
				else if (T==10) return null; // end execution
				else return "[]"; // no input events
			}
		});
		// load and run dining philosophers
		boolean R = E.go(
				"/Users/mc/git/lps_corner/examples/CLOUT_workshop/diningPhilosophers.pl", 
				"[available(_)]", // at each cycle, collect state of these fluents...
				"[pickup(_,_)]" // ...and these events
				);
		System.out.println("End result:"+ R );
		E.shutdown();
		System.exit(0);
	}

}
