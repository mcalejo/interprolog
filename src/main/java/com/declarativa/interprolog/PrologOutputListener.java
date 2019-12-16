/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;
/** If you're interested in Prolog's textual output, implement this interface on your class and make your 
object a listener to a SubprocessEngine */
public interface PrologOutputListener {
	/** Prolog stream output was sent, any stream */
	@Deprecated
	public void print(String s);
	public void printStdout(String s);
	public void printStderr(String s);
}
