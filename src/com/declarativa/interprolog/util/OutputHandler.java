/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Vector;

/** An object consuming input from a stream, analyzing it and sending messages to a list 
of OutputListener objects; if a debugStream is provided it gets a copy of all the input read.
If the stream is null, nothing happens. The platform default character set is assumed, unless it is set elsewhere:
@see com.declarativa.interprolog.SubprocessEngine#setCharset(String) */
public class OutputHandler extends Thread {
	InputStream sourceStream;
	PrintStream debugStream;
	Vector<OutputListener> listeners;
	private boolean ignoreStreamEnd;
	String originStd;
	final StringBuffer buffer = new StringBuffer();
	private Charset charset;
	
	public OutputHandler(InputStream s,OutputStream debugStream,String originStd){
		super("OutputHandler for "+originStd);
		charset = Charset.defaultCharset();
		if (s==null) sourceStream = null;
		else if (s instanceof BufferedInputStream) sourceStream = s;
		else sourceStream = new BufferedInputStream(s);
		setDebugStream(debugStream);
		listeners = new Vector<OutputListener>();
		ignoreStreamEnd=false;
		this.originStd=originStd;
	}
	
	public OutputHandler(InputStream s,OutputStream debugStream){
		this(s,debugStream,"An OutputHandler");
	}
	
	public OutputHandler(InputStream s){
		this(s,null);
	}
	
	public synchronized void addOutputListener(OutputListener ol){
		listeners.addElement(ol);
	}
	public synchronized void removeOutputListener(OutputListener ol){
		listeners.removeElement(ol);
	}
	
	public boolean hasListener(OutputListener ol){
		return listeners.contains(ol);
	}
	
	public void run(){
		byte[] buffer = new byte[2048]; // slightly larger buffer to make sure we read quickly...
		if (sourceStream!=null) while(true) {
			try{
				int nchars = sourceStream.read(buffer,0,buffer.length);
				if (nchars==-1){
					fireStreamEnded();
					break;
				} else fireABs(buffer,nchars);
			} catch (IOException ex){ if (!ignoreStreamEnd) throw new IPException("Problem fetching output:"+ex);}
		}
	}
	/** A Prolog subprocess stream has unexpectedly broke */
	synchronized void fireStreamEnded(){
		if (ignoreStreamEnd) return;
		for (int L=0; L<listeners.size(); L++)
			((listeners.elementAt(L))).streamEnded(originStd);
		if(debugStream!=null) 
		debugStream.println("PROLOG "+originStd+" ENDED");
	}
	synchronized void fireABs(byte[] buffer,int nbytes){
		for (int L=0; L<listeners.size(); L++)
			((listeners.elementAt(L))).analyseBytes(buffer,nbytes,originStd,charset);
		if(debugStream!=null) 
		//debugStream.println("PROLOG "+name+":"+new String(buffer,0,nbytes));
			printDebug(new String(buffer,0,nbytes, charset));
	}
	static final int CHARS_TO_FLUSH = 1000;
	/** This attempts to flush the debug stream less often, so console output is not so confuse */
	private void printDebug(String s){
		buffer.append(s);
		if (s.contains(System.getProperty("line.separator")) || buffer.length() > CHARS_TO_FLUSH) {
			// let's flush it
			int nChars = (buffer.length() > CHARS_TO_FLUSH ? CHARS_TO_FLUSH:buffer.length());
			debugStream.println("PROLOG " + originStd +":"+ buffer.substring(0, nChars));
			debugStream.flush();
			buffer.delete(0, nChars);
		}
	}

	
	public void setIgnoreStreamEnd(boolean ignore){
		ignoreStreamEnd=ignore;
	}
	public void setDebugStream(OutputStream debugStream){
		if (debugStream==null || debugStream instanceof PrintStream) this.debugStream=(PrintStream)debugStream;
		else this.debugStream=new PrintStream(debugStream);
	}

	public void setCharset(Charset c) {
		charset = c;
	}

	public Charset getCharset() {
		return charset;
	}
}

