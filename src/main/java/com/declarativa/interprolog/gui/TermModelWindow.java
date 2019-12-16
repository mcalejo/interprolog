/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.gui;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;

/** Displays a Prolog term, using a JTree
*/
@SuppressWarnings("serial")
public class TermModelWindow extends JFrame{
	TermModel model; PrologEngine engine;
	public TermModelWindow(TermModel tm){
		this(tm,null);
	}
	public TermModelWindow(TermModel tm,PrologEngine e){
		super("A term");
		this.model=tm; this.engine=e;
		final JTree termTree = new JTree(tm);
		termTree.setCellRenderer(new TermTreeCellRenderer());
		getContentPane().add("Center",new JScrollPane(termTree));
		setSize(200,200);
		/*
		if (engine!=null){
			JMenuBar mb; 
			mb = new JMenuBar(); setJMenuBar(mb);
			JMenu testMenu = new JMenu("Tests"); mb.add(testMenu);
			JMenuItem test = new JMenuItem("Assert foobar(T)..."); testMenu.add(test);
			test.addActionListener(new ActionListener(){
				Object[] objects = {model};
				public void actionPerformed(ActionEvent e){
					System.out.println("Trying to recover and assert...");
					Object[] bindings = engine.deterministicGoal("assert(foobar(TM))", "TM", objects, null);
					System.out.println("success=" + (bindings!=null));
				}
			});
		
		}*/
		MouseListener ml = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				TreePath selPath = termTree.getPathForLocation(e.getX(), e.getY());
				if (selPath!=null) 
					System.out.println("Node class:"+((TermModel)selPath.getLastPathComponent()).node.getClass());
			 }
		};
		termTree.addMouseListener(ml);
		setVisible(true);
	}
}

/** Adapted from: 
	http://developer.javasoft.com/developer/onlineTraining/swing2/exercises/TreeRender/Solution/TreeRender.java
*/
@SuppressWarnings("serial")
class TermTreeCellRenderer extends JLabel implements TreeCellRenderer {

    // Create instance variable to save selected state
    private boolean selected;

    public Component  getTreeCellRendererComponent(JTree tree,
    	Object value, boolean selected, boolean expanded,boolean leaf, int row, boolean hasFocus) {

		if (!(value instanceof TermModel))
			throw new RuntimeException("getTreeCellRendererComponent demands a TermModel");
			
		TermModel term = (TermModel)value;
      	// Save selected state
      	this.selected = selected;		
      	if (expanded) {
        	setText(term.node.toString());
      	} else {
        	setText(term.toString());
      	}
      	doLayout(); // Make sure our size accomodates current text ??
      	return this;
   	}

    public void paint (Graphics g) {

      	// Change background color based on selected state
        Color background = (selected ? Color.lightGray : Color.white);
        g.setColor(background);

      	g.fillRect (getX(), getY(), getWidth(), getHeight());
      	super.paint (g);
   	}

}
