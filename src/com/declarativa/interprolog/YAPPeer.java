/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2015
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;
import java.io.File;
import java.util.Properties;

import com.declarativa.interprolog.util.*;

public class YAPPeer extends PrologImplementationPeer{
	public YAPPeer(AbstractPrologEngine engine){
		super(engine);
		operators = new PrologOperatorsContext(PrologOperatorsContext.standardCommonOperators);
		systemName = "YAP";
	}
	public String getBinDirectoryProperty(Properties p){
		return p.getProperty("YAP_BIN_DIRECTORY");
	}
	public String executablePath(String d){
		return d + File.separator + "yap";
	}
	public String fetchPrologNumericVersion(){
		Object[] bindings = engine.deterministicGoal(
			"current_prolog_flag(version,V),atom_codes(V,[Y,A,P,Dash|Number]),atom_codes(Version,Number)",
			"[string(Version)]");
		return (String)bindings[0];
	}
	public String[] alternativePrologExtensions(String filename){
		if (!(filename.indexOf('.')==-1)) throw new IPException("Bad use of alternativePrologExtensions");
		return new String[]{filename+".yap"};
	}
	public boolean hasPrologExtension(String filename){
		return filename.endsWith(".yap");
	}
	/** Assumes that Prolog options can not include "/bin/"... */
	public String prologBinToBaseDirectory(String binDirectoryOrStartCommand){
		binDirectoryOrStartCommand = binDirectoryOrStartCommand.trim();
		/* CAN'T DO: directories may have spaces within...
		int firstSpace = binDirectoryOrStartCommand.indexOf(' ');
		if (firstSpace!=-1) // get rid of option arguments
			binDirectoryOrStartCommand = binDirectoryOrStartCommand.substring(0,firstSpace);
			*/
		/* This would be nice to get rid of relative paths, but would lose the drive under Windows:
		try{
			binDirectoryOrStartCommand = new File(binDirectoryOrStartCommand).getCanonicalPath();
		} catch (IOException e){
			throw new IPException("Bad file path:"+e);
		}*/
		int baseEnd = binDirectoryOrStartCommand.lastIndexOf(File.separator+"bin"+File.separator);
		if (baseEnd==-1) 
			throw new IPException("Can not determine base directory from "+binDirectoryOrStartCommand);
		binDirectoryOrStartCommand = binDirectoryOrStartCommand.substring(0,baseEnd);
		if (binDirectoryOrStartCommand.endsWith(File.separator)) 
			return binDirectoryOrStartCommand.substring(0,binDirectoryOrStartCommand.length()-1);
		else return binDirectoryOrStartCommand;
	}
	public Recognizer makePromptRecognizer(){
		return new Recognizer("?-");
	}
	public Recognizer makeBreakRecognizer(){
		return new Recognizer(" ?-");
	}
	public String interprologFilename()
	{
		return "yap/interprolog.yap";
	}
	public String visualizationFilename(){
		return "visualization.P"; // without the extension InterProlog would search for .pl, which does not exist
	}
	public String unescapedFilePath(String p){
		if (File.separatorChar!='\\' || p.indexOf(File.separator)==-1) return p;
		StringBuffer newPath = new StringBuffer(p.length()+10);
		for(int c=0;c<p.length();c++){
			char ch = p.charAt(c);
			if (ch==File.separatorChar) newPath.append(ch); // Duplicate backslashes
			newPath.append(ch);
		}
		return newPath.toString();
	}
	public boolean isInterrupt(Object error){
		//System.out.println("error=="+error);
		return error.toString().indexOf("interprolog_interrupt")!=-1; // TODO! error.toString().startsWith("?????");
	}
	@Override
    public String getBinDirectoryEnvVar(){
    	return "YAP_BIN_DIRECTORY";
    }
}