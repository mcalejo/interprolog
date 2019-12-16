/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
/** An Exception thrown when Prolog is interrupted from the Java side
*/
@SuppressWarnings("serial")
public class IPInterruptedException extends IPException{
	public IPInterruptedException(String s){super(s);}
}

