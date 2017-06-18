/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.gui;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

@SuppressWarnings("serial")
public class PredicateTableWindow extends JFrame{
	public PredicateTableWindow(PredicateTableModel ptm){
		super(ptm.toString());
		JTable table = new JTable(ptm);
		table.setColumnSelectionAllowed(true); // prettier...
		JScrollPane scroller = new JScrollPane(table); 
		Dimension screenSize = getToolkit().getScreenSize();
		int x = screenSize.width; int y = screenSize.height;
		Dimension ps = table.getPreferredScrollableViewportSize();
		if (ps.width<x) x=ps.width;
		if (ps.height<y) y=ps.height;
		getContentPane().add("Center",scroller);
		setSize(x,y);
		setVisible(true);
		//System.out.println("Created window for PredicateTableModel "+ptm);
	}
}
