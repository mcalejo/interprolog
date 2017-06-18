/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
/** An Exception thrown when Prolog has unexpectedly died, typically fue to some unforeseen and serious error 
*/
@SuppressWarnings("serial")
public class PrologHaltedException extends IPException{
	public PrologHaltedException(String s){super(s);}
	public PrologHaltedException(String s,Throwable cause){super(s,cause);}
}

