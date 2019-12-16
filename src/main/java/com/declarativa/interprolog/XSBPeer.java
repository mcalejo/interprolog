/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;
import com.declarativa.interprolog.util.IPException;
import com.declarativa.interprolog.util.Recognizer;

public class XSBPeer extends PrologImplementationPeer{
    
    public XSBPeer(AbstractPrologEngine engine){
		super(engine);
		operators = new PrologOperatorsContext(PrologOperatorsContext.standardXSBOperators);
		systemName = "XSB Prolog";
	}
        
    public String getBinDirectoryEnvVar(){
    	return "XSB_BIN_DIRECTORY";
    }
    
    /** This returns the same path d if it ends in ...bin */
	public String executablePath(String d){
		String pattern = engine.serverFileSeparatorChar()+"bin"+engine.serverFileSeparatorChar();
		//if (new File(d).getParentFile().getName().equals("bin"))
		if (d.contains(pattern)) // weaker test, to tolerate command arguments
			return d;
		if (!engine.localEngine)
			throw new IPException("Remote engines need a full executable path to start");
		if (d.endsWith(engine.serverFileSeparatorChar()+"bin")) 
			d = d+engine.serverFileSeparatorChar();
		else
			d = d+engine.serverFileSeparatorChar()+"bin"+engine.serverFileSeparatorChar();
		if (AbstractPrologEngine.is64WindowsOS()) 
		return d + "xsb64.bat";
		else if (AbstractPrologEngine.isWindowsOS())
		return d + "xsb.bat";
		else return d + "xsb";
	}
    
	protected String fetchPrologNumericVersion(){
		Object[] bindings = engine.deterministicGoal("xsb_configuration(version,V)","[string(V)]");
		return (String)bindings[0];
	}
    
    /*
	public String getFVInstallDirGoal(){
		return "(F=install_dir, xsb_configuration(F,V))";
	}*/
    
	public String[] alternativePrologExtensions(String filename){
		if (!(filename.indexOf('.')==-1)) throw new IPException("Bad use of alternativePrologExtensions");
		if (xsbUsesXwamExtension()) return new String[]{filename+".xwam",filename+".P"};
		else return new String[]{filename+".O",filename+".P"};
	}
	public boolean hasPrologExtension(String filename){
		return filename.endsWith(".xwam") || super.hasPrologExtension(filename);
	}
	/** This XSB compiles Prolog files into .xwam files. This method works by XSB file presence detection, to be available early in the startup process */
	private boolean xsbUsesXwamExtension(){
		/* Enough is enough (of dealing with Windows path names with spaces AND ancient compiled file extensins!
		File XSBbase = new File(engine.getPrologBaseDirectory());
		File compilePath = new File(XSBbase,"cmplib");
		// String compilePath = XSBbase+File.separator+"cmplib"+File.separator+"compile";
		if ((new File(compilePath,"compile.xwam")).exists()) return true;
		if ((new File(compilePath,"compile.O")).exists()) return false;
		
		throw new IPException("Weird XSB Prolog installation, could find neither compile.O nor compile.xwam in cmplib:"+compilePath);
		*/
		return true;
	}
    
	/** Assumes that Prolog options can not include "/bin/"... */
	public String prologBinToBaseDirectory(String binDirectoryOrStartCommand){
		binDirectoryOrStartCommand = binDirectoryOrStartCommand.trim();
		/* CAN'T DO: directories may have spaces within...
		int firstSpace = binDirectoryOrStartCommand.indexOf(' ');
		if (firstSpace!=-1) 
			binDirectoryOrStartCommand = binDirectoryOrStartCommand.substring(0,firstSpace);
			*/
		/* This would be nice to get rid of relative paths, but would lose the drive under Windows:
		try{
			binDirectoryOrStartCommand = new File(binDirectoryOrStartCommand).getCanonicalPath();
		} catch (IOException e){
			throw new IPException("Bad file path:"+e);
		}*/
		int baseEnd = binDirectoryOrStartCommand.lastIndexOf(engine.serverFileSeparatorChar()+"config"+engine.serverFileSeparatorChar());
		if (baseEnd==-1){
		    baseEnd = binDirectoryOrStartCommand.lastIndexOf(engine.serverFileSeparatorChar()+"bin" /*+File.separator*/);
		    if (baseEnd==-1)
				throw new IPException("Can not determine base directory, missing config in known path! "+binDirectoryOrStartCommand);
		}
			
		binDirectoryOrStartCommand = binDirectoryOrStartCommand.substring(0,baseEnd);
		if (binDirectoryOrStartCommand.endsWith(""+engine.serverFileSeparatorChar())) 
			binDirectoryOrStartCommand = binDirectoryOrStartCommand.substring(0,binDirectoryOrStartCommand.length()-1);
		// Windows hack for spaces in paths...:
		if (binDirectoryOrStartCommand.startsWith("\"") && !binDirectoryOrStartCommand.endsWith("\""))
			binDirectoryOrStartCommand = binDirectoryOrStartCommand.substring(1);
		return binDirectoryOrStartCommand;
	}
    
	public Recognizer makePromptRecognizer(){
		return new Recognizer("| ?-");
	}
    
	public Recognizer makeBreakRecognizer(){
		return new Recognizer(": ?-");
	}
    
    public String interprologFilename() {
		return "interprolog.xwam";
	}
    
	public String visualizationFilename(){
		return "visualization.xwam";
	}
	
	public boolean isInterrupt(Object error){
		//return error.toString().startsWith("Aborting");
		String E = error.toString();
		return E.contains("Aborting") /* && E.contains(AbstractPrologEngine.MAGIC_INTERRUPT_MARKER)*/;
	}

	public String unescapedFilePath(String p){
                //HACK: Right slashes are less error-prone with XSB
		//return super.unescapedFilePath(p).replace('\\','/');  
		return super.unescapedFilePath(p).replace("\\","\\\\"); // This is less aggressive to Flora2
	}
}
