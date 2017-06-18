/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;

/** An Exception related to Prolog processing in general */
@SuppressWarnings("serial")
public class IPException extends RuntimeException{
	private Throwable cause;
	
	public IPException(String s){
		super(s);
		//System.err.println("IPException about to be thrown:"+this);
		cause=null;
	}
    
    public IPException(String s, Throwable cause) {
        super(s);
        this.cause=cause;
    }
    
    /** To allow compilation under JDK 1.3; this method is already defined in Throwable in later JDKs */
    public Throwable getCause(){
    	return cause;
    }
}

