/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.gui;
import com.declarativa.interprolog.*;
/** Swing model for full contents of a XSB table
*/
public class XSBTableModel extends TermTreeModel{
	private static final long serialVersionUID = -4638540806176083169L;
	String state;
	public XSBTableModel(TermModel root,TermTreeModel[] children,String state){
		super(root,children);
		this.state=state;
	}
	public static ObjectExamplePair example(){
		return new ObjectExamplePair("XSBTableModel",new XSBTableModel(new TermModel("A"),null,"complete"));
		// we don't mind passing a pair of similar objects, because in this case we'll be using ipObjectTemplate,
		// rather than ipObjectSpec
	}
}
