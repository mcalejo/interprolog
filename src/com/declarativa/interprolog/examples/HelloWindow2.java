/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.examples;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;

import com.declarativa.interprolog.PrologEngine;
/**
Example for Java-side UI construction, with event handler setup on the Prolog side; the Java part knows nothing about event handling. To create the window, in Prolog call:
ipPrologEngine(_Engine), javaMessage('com.declarativa.interprolog.examples.HelloWindow2','HelloWindow2'(_Engine)).

Watch the console window and write down the Text and Button IDs

Then, assuming we have defined in Prolog an event handler predicate, like:
assert( ( greetat(TextID) :- javaMessage( TextID, setText( string('Hello world!')) )) ).

...we can now call:
ipPrologEngine(Engine), buildTermModel(greetat(TextIDFromConsole),TM), 
javaMessage('com.declarativa.interprolog.gui.PrologEventBroker',R,'PrologEventBroker'(Engine,TM)),
javaMessage(ButtonIDFromConsole,addActionListener(R)).

From this point onwards, clicking the "Greet" button will make the message appear in the text field.

*/
@SuppressWarnings("serial")
public class HelloWindow2 extends JFrame{
	public HelloWindow2(PrologEngine pe /* this argument not really needed, cf. comments below */){
		super("Java-Prolog-Java call test2");
		JTextField text = new JTextField(15);
		text.setBorder(BorderFactory.createTitledBorder("text"));
		JButton button = new JButton("Greet");
		Box box = new Box(BoxLayout.Y_AXIS);
		box.add(text); box.add(button);
		getContentPane().add(box);
		setSize(200,100); validate(); setVisible(true);
		// The following 2 lines are not strictly necessary, as long as someone is able to register
		// button and text with an engine of choice, hence the constructor argument above
		// Concluding, pe and these 2 messages below are just for the benefit of dynamically introspecting
		// into 2 graphical objects, telling our engine about them, and letting the Prolog 
		// programmer know their allocated IDs, nothing more
		System.out.println("Button ID:"+pe.registerJavaObject(button));
		System.out.println("Text ID:"+pe.registerJavaObject(text));
	}
}
