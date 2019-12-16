/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;
import java.util.Properties;

import com.declarativa.interprolog.util.Recognizer;

/** Centers most of the Prolog implementation-dependent information */
public abstract class PrologImplementationPeer{
	public static String REGULAR_PROMPT = "?-";
	AbstractPrologEngine engine;
	PrologOperatorsContext operators;
	/** User-friendly name of the Prolog system, without version */
	protected String systemName;
	protected String systemVersion;

	/** Variant to support only the partial functionality which does not depend on the existence of an engine*/
	public PrologImplementationPeer(){this(null);}
	public PrologImplementationPeer(AbstractPrologEngine engine){
		this.engine=engine;
		systemName = "???";
	}
	/* Prolog goal to obtain the install directory as an atom in Prolog variable V; the goal should also bind Prolog variable F to an atom. 
	This is usually obtained from a generic system predicate that returns the values (V) for differente flags (F).*/
	//basically useless, and unsupported by some Prologs: public abstract String getFVInstallDirGoal();
	public String getBinDirectoryProperty(Properties p){
		return p.getProperty(getBinDirectoryEnvVar());
	}
    public abstract String getBinDirectoryEnvVar();
	public abstract String executablePath(String binDirectory);
	public String getPrologVersion(){
		return systemName + " " + getPrologNumericVersion();
	}
	public String getPrologNumericVersion(){
		if(systemVersion==null) systemVersion = fetchPrologNumericVersion();
		return systemVersion;
	}
	protected abstract String fetchPrologNumericVersion();
	public abstract String[] alternativePrologExtensions(String filename);
	public boolean hasPrologExtension(String filename){
		return filename.endsWith(".P");
	}
	public abstract String prologBinToBaseDirectory(String binDirectoryOrStartCommand);
	public abstract Recognizer makePromptRecognizer();
	public abstract Recognizer makeBreakRecognizer();
	/** True if an error, as obtained in a ResultFromProlog object, looks like an interrupt detection on the Prolog side, 
	cf. predicate handleDeterministicGoal in interprolog.P/pl/etc. resultError is not null*/
	public abstract boolean isInterrupt(Object resultError);  
	/** Returns the path for the Prolog file that must be loaded for InterProlog to function, USING '/' AS THE SEPARATION CHARACTER independently of the OS platform*/
	public abstract String interprologFilename();
	/** Returns the path for the Prolog file that must be loaded for InterProlog's visualization predicates to function, typically
	in the context of using a ListenerWindow. Although the file is common for all Prologs, some (eg XSB) have a compiled form, others do not*/
	public abstract String visualizationFilename();
	public String executablePath(Properties p){
		return executablePath(getBinDirectoryProperty(p));
	}
	/** Some Prologs use '\' as an escape character in atoms, which can affect file paths under Windows. Use this method to preprocess
	all file paths passed to Prolog. this default implementation does no preprocessing, subclasses should define their own if needed */
	public String unescapedFilePath(String p){
		return p;
	}
	public PrologOperatorsContext getOperators(){return operators;}
}
