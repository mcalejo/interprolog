/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
import java.nio.charset.Charset;
import java.util.Vector;

/** A simple pattern recognizor used in error detection. ASCII characters only. */
public class Recognizer implements OutputListener{
	Vector<RecognizerListener> listeners;
	int nextInPattern; // next byte to recognize
	byte[] bytePattern;
	boolean collectRestOfBuffer;
	byte[] lastBuffer; int lastNbytes;

	public Recognizer(){
		this(null);
	}
	public Recognizer(String pattern){
		this(pattern,false);
	}
	public Recognizer(String pattern,boolean collectRestOfBuffer){
		listeners = new Vector<RecognizerListener>();
		if (pattern==null) bytePattern=new byte[0];
		else bytePattern = pattern.getBytes();
		nextInPattern=0;
		this.collectRestOfBuffer=collectRestOfBuffer;
	}
	
	public int numberListeners(){ return listeners.size(); }

	// OutputListener methods:
	public synchronized void analyseBytes(byte[] buffer,int nbytes,String originStd, Charset charset){
		lastBuffer = buffer; lastNbytes=nbytes;
		if (bytePattern.length==0) 
			fireRecognized(new String(buffer,0,nbytes,charset),originStd);
		else {
			if (nextInPattern>bytePattern.length-1)
				throw new IPException("Inconsistency in Recognizer ("+nextInPattern+","+bytePattern.length+")");
			for(int b=0;b<nbytes;b++) {
				if(buffer[b]==bytePattern[nextInPattern]){
					nextInPattern++;
					if (nextInPattern>=bytePattern.length) {
						nextInPattern = 0;
						if (collectRestOfBuffer && b+1<nbytes) {
							fireRecognized(new String(buffer,b+1,nbytes-(b+1),charset),originStd);
							break;
						}
						else fireRecognized("", originStd);
					}
				}
				else nextInPattern = 0;
			}
		}
	}
	/** Assume there are no network problems, hence unexpected stream end means Prolog process has died unexpectedly */
	public void streamEnded(String originStd){
		//throw new IPException("Unexpected end of stream, Prolog may have died abruptly");
		// handled elsewhere: throw new PrologHaltedException("Unexpected end of stream, Prolog has died without request");
	}
	
	public synchronized void addRecognizerListener(RecognizerListener l){
		listeners.addElement(l);
	}
	public synchronized void removeRecognizerListener(RecognizerListener l){
		listeners.removeElement(l);
	}
	void fireRecognized(String extra, String originStd){
		for (int l=0; l<listeners.size(); l++)
			listeners.elementAt(l).recognized(this,extra,originStd);
	}
	public String toString(){return "Recognizer of "+new String(bytePattern) /* +". Last buffer I got:"+new String(lastBuffer,0,lastNbytes) */;}
}

