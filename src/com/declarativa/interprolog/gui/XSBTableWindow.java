/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.gui;
import java.util.*;
import java.text.*;

import javax.swing.*;

/** Shows a XSB Prolog table
*/
@SuppressWarnings("serial")
public class XSBTableWindow extends JFrame{
	public XSBTableWindow(XSBTableModel m, Object[] columnTitles){
		super();
		//System.out.println("Creating XSBTableWindow for "+m);
		getContentPane().add("Center",new TermTreePane(m,columnTitles));
		String time = DateFormat.getTimeInstance().format(new Date());
		setTitle(m.node.toString()+": table at "+time);
		getContentPane().add("North",new JLabel("state was "+m.state/*,SwingConstants.CENTER*/));
		setSize(300,200);
		setVisible(true);
	}
	
}
