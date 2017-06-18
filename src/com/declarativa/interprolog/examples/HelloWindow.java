/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.examples;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;

import com.declarativa.interprolog.PrologEngine;
/**
From Prolog in a InterProlog listener window:
1) assert( (greetat(TextID) :- javaMessage( TextID, setText(string('Hello world!')) )) ).
2) call this to create the window:
ipPrologEngine(Engine), javaMessage('com.declarativa.interprolog.examples.HelloWindow','HelloWindow'(Engine)).
*/
@SuppressWarnings("serial")
public class HelloWindow extends JFrame{
	PrologEngine myEngine;
	public HelloWindow(PrologEngine pe){
		super("Java-Prolog-Java call example");
		myEngine = pe;
		JTextField text = new JTextField(15);
		final Object fieldObject = myEngine.makeInvisible(text);
		text.setBorder(BorderFactory.createTitledBorder("text"));
		JButton button = new JButton("Greet");
		Box box = new Box(BoxLayout.Y_AXIS);
		box.add(text); box.add(button);
		getContentPane().add(box);
		setSize(200,100); setVisible(true);
		
		button.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					myEngine.deterministicGoal("greetat(Obj)","[Obj]",new Object[]{fieldObject});
				}
			});
	}
}
