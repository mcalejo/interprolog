/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.gui;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;

import com.declarativa.interprolog.TermModel;

/** Displays a list of Prolog terms
*/
@SuppressWarnings("serial")
public class TermListWindow extends JFrame{
	public TermListWindow(TermListModel ptm){
		super();
		setTitle("List of "+ ptm.getSize()+" terms");
		final JList<TermModel> list = new JList<TermModel>(ptm);
		JScrollPane scrollPane = new JScrollPane(list);
		getContentPane().add("Center",scrollPane);
		final TermListModel theModel=ptm;
     	list.addMouseListener(new MouseAdapter() {
     		public void mouseClicked(MouseEvent e) {
         		if (e.getClickCount() == 2) {
             		int index = list.locationToIndex(e.getPoint());
             		if (index != -1) new TermModelWindow(theModel.terms[index]);
     			}
     		};
     	});
		setVisible(true);
		// System.out.println("Created window for TermListWindow "+ptm);
	}
}
