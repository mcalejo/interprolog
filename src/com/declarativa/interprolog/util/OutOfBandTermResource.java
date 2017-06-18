/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/

package com.declarativa.interprolog.util;
import java.io.*;
import com.declarativa.interprolog.*;

public class OutOfBandTermResource {
	PrologEngine engine;
	File resource;
	public OutOfBandTermResource(PrologEngine engine){
		this.engine = engine;
		try{
			resource = File.createTempFile("outOfBand",".P");
			resource.deleteOnExit();
		} catch (IOException ex){
			throw new IPException("Could not create temporary Prolog file:"+ex);
		}		
	}
	public String prologFileAtom(){
		return engine.unescapedFilePath("'"+resource.getAbsolutePath()+"'");
	}
	public TermModel[] getTermList(){
		return InitiallyFlatTermModel.getTermList(resource);
	}
}
