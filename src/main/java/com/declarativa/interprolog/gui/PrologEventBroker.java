/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.gui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;

import com.declarativa.interprolog.AbstractPrologEngine;
import com.declarativa.interprolog.PrologEngine;
/** A PrologEventBroker can listen to ActionEvents, and calls a Prolog goal to handle them.
If a goal is not specified, Event(ID) goals will be called instead, where ID is the reference of 
the event source object in the engine's knownObjects table.
If a JComponent is specified, its tooltip text will be set; if not, the first JComponent sending an event
will have its tooltip text set */

public class PrologEventBroker implements ActionListener {
	PrologEngine engine;
	String goal;
	JComponent component;
	public PrologEventBroker(PrologEngine e,String g){
		engine=e; 
		goal=g;
		component=null;
		maySetTooltipText("Calls an Event(this) goal");
	}
	public PrologEventBroker(PrologEngine e,Object g){
		engine=e; 
		if (g!=null) goal=g.toString();
		component=null;
		maySetTooltipText("Calls an Event(this) goal");
	}
	public PrologEventBroker(PrologEngine e){
		this(e,null);
	}
	void maySetTooltipText(String defaultTip){
		if (component!=null) {
			if (goal!=null)
				component.setToolTipText("Calls "+goal);
			else
				component.setToolTipText(defaultTip);
		}
	}
	public void actionPerformed(ActionEvent e){
		String thisGoal; Object theSource = e.getSource();
		if (component==null) {
			if (theSource instanceof JComponent) component=(JComponent)theSource;
		}
		if (goal!=null) thisGoal=goal;
		else {
			thisGoal=AbstractPrologEngine.shortClassName(e.getClass()) + "("+
				engine.registerJavaObject(theSource) + ")";
		}
		maySetTooltipText("Last called "+thisGoal);
		engine.deterministicGoal(thisGoal);
	}
}
