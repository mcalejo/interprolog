/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;
/** If you're interested in knowing that a (Java) term representation has changed implement this.
Prior to listening, a TermModel must have its root set with setRoot(). 
All changes to TermModel node and children variables will be reported */
public interface TermModelListener{
	public void termChanged(TermModel source);
}