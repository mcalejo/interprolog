/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
/** An Exception thrown when a goal result is not available because the Prolog engine died somehow
*/
@SuppressWarnings("serial")
public class UnavailableResultException extends IPException{
	public UnavailableResultException(String s){super(s);}
	public UnavailableResultException(String s,Throwable cause){super(s,cause);}
}

