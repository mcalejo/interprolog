/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.gui;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import com.declarativa.interprolog.AbstractPrologEngine;
import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.SubprocessEngine;
import com.declarativa.interprolog.TermModel;
import com.declarativa.interprolog.util.IPException;
import com.xsb.interprolog.NativeEngine;
import com.xsb.interprolog.NativeEngineWindow;

/** 
To try in a Silk console near you:
?-silk:flora(" ipPrologEngine(?E)@_prologall, javaMessage('com.declarativa.interprolog.gui.EngineInspectionWindow',init(?E))@_prologall ");
?-silk:flora(" showPrologEngineInspector(flora_abort('User aborted from inspector window'))@_prologall, flora_set_timeout(repeating(1)@_plg,refreshPrologEngineInspector(?)@_prologall)@_plg(flrerrhandler) ");
?-silk:flora(" (repeat,fail)@_prologall ");
*/
@SuppressWarnings("serial")
public class EngineInspectionWindow extends JFrame{
	JTable memoryTable, tablesTable;
	JLabel currentGoal;
	final PrologEngine engine;
	ListenerWindow listener=null;
	JCheckBox pauseButton;
	boolean paused=false; /** Prolog execution will be paused the next time we refresh */
	boolean aborted=false; /** Flora execution will be aborted the next time we end refreshing */
	JButton query;
	
	public EngineInspectionWindow(PredicateTableModel memory, PrologEngine e /* Java bug!!! ,PredicateTableModel tables*/){
		super("Prolog Engine Inspector");
		engine=e;
		
		currentGoal = new JLabel("    ");
		getContentPane().add("North",currentGoal);

		memoryTable = new JTable(memory);
		JScrollPane scrollerM = new JScrollPane(memoryTable); 
		/*
		tablesTable = new JTable(tables);
		JScrollPane scrollerT = new JScrollPane(tablesTable); 
		Dimension ps = tablesTable.getPreferredScrollableViewportSize();
		tablesTable.setSize(ps.width,ps.height);
		*/
		JPanel center = new JPanel(new GridLayout(1,2));
		center.add(scrollerM); //center.add(scrollerT);
		
		getContentPane().add("Center",center);
		JPanel bottom = new JPanel(new FlowLayout());
		getContentPane().add("South",bottom);
		JButton listenerButton = new JButton("Open Prolog listener");
		bottom.add(listenerButton);
		listenerButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				((JButton)e.getSource()).setEnabled(false);
				if (engine instanceof NativeEngine) listener = new NativeEngineWindow((NativeEngine)engine,true,false);
				else listener = new SubprocessEngineWindow((SubprocessEngine)engine,true,false);
			}
		});
		pauseButton = new JCheckBox("Paused",false);
		pauseButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){	
				paused = ((JCheckBox)e.getSource()).isSelected();
			}
		});
		JButton abortButton = new JButton("Abort");
		abortButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){	
				aborted=true;
			}
		});
		query = new JButton("Show table info");
		bottom.add(query);
		bottom.add(pauseButton);
		bottom.add(abortButton);
		query.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){	
				PredicateTableModel TM = (PredicateTableModel)engine.deterministicGoal("buildTableUsageModel(Model)","[Model]")[0];
				new PredicateTableWindow(TM);
			}
		});
		pack();
		setVisible(true);
	}
	/** Receives some status information about the Prolog engine. Returns false if the user aborts, true otherwise */
	public boolean refresh(final PredicateTableModel memory,final TermModel G /* could use flora_decode_goal_as_atom */ /*, PredicateTableModel tables*/){
		Runnable R = new Runnable(){
			public void run(){
				memoryTable.setModel(memory);
				currentGoal.setText(G.toString());
				//tablesTable.setModel(tables);
			}
		};
		
		if (SwingUtilities.isEventDispatchThread()) throw new IPException("Bad thread!"); //R.run();
		else 
			try{SwingUtilities.invokeAndWait(R);} // blocks here
			catch(Exception e){throw new IPException("Problems in refresh:"+e);}
		/* Now hold here while the pause button is checked, letting Prolog wait; meanwhile, new Prolog goals may be called...*/
		while(paused && !aborted) Thread.yield(); 
		if (aborted){
			aborted=false;
			return false;
		}
		return true;
	}
	/** Try it with ipPrologEngine(E), javaMessage('com.declarativa.interprolog.gui.EngineInspectionWindow',init(E)). */
	public static void init(AbstractPrologEngine engine){
		String VF = engine.getImplementationPeer().visualizationFilename();
		if (engine.getLoadFromJar()) engine.consultFromPackage(VF,ListenerWindow.class,true);
		else engine.consultRelative(VF,ListenerWindow.class);
		engine.teachMoreObjects(ListenerWindow.guiExamples());
	}
	/** Try it with ipPrologEngine(E), javaMessage('com.declarativa.interprolog.gui.EngineInspectionWindow',showIt(E)). */
	public static void showIt(AbstractPrologEngine engine){
		engine.deterministicGoal("showPrologEngineInspector");
	}
}
