/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
/** An Exception originated by a Prolog error */
@SuppressWarnings("serial")
public class IPPrologError extends IPException{
	public final Object t;
	public IPPrologError(Object t){
		super(t.toString());
		this.t=t;
	}
	public String toString(){return "IPPrologError:"+t.toString();}
	
	public Object getError(){return t;}
}

