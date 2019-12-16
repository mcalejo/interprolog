/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.gui;
import javax.swing.JFrame;

/** Shows a multiple-pane hierarchical browser, defined by a Prolog tree
*/
@SuppressWarnings("serial")
public class TermTreeWindow extends JFrame{
	public TermTreeWindow(TermTreeModel m){
		this(m,null);
	}
	public TermTreeWindow(TermTreeModel m, Object[] levelTitles){
		super();
		getContentPane().add("Center",new TermTreePane(m,levelTitles));
		if (levelTitles.length>0) setTitle(levelTitles[0].toString());
		else setTitle(m.node.toString());
		setSize(400,200);
		setVisible(true);
	}
	
}

