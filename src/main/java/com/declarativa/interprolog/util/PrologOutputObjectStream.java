/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
import java.io.*;
/** Vaguely similar to an ObjectOutputStream, but sends the total number of serialized bytes up front,
 so Prolog can keep grammar and socket reads separate without hanging for input. */
public class PrologOutputObjectStream {
	OutputStream os;
	ObjectOutputStream tempObjects;
	ByteArrayOutputStream serializedTemp;
	boolean flushed = false;
	
	public PrologOutputObjectStream(OutputStream os) throws IOException{
		this(os,false);
	}
	
	public PrologOutputObjectStream(OutputStream os, boolean usesEscapeByte) throws IOException{
		this.os=os;
		if (usesEscapeByte) serializedTemp = new MyByteArrayOutputStream(512);
		else serializedTemp = new ByteArrayOutputStream(512); // this might be tuned to a better initial capacity
		tempObjects = new ObjectOutputStream(serializedTemp);
	}
	
	boolean usingEscapeByte(){return serializedTemp instanceof MyByteArrayOutputStream;}
	
	public ObjectOutputStream getObjectStream(){
		return tempObjects;
	}
	
	public void flush() throws IOException{
		tempObjects.close();
       	int count = -1;
       	if (usingEscapeByte()) count = size() - ((MyByteArrayOutputStream)serializedTemp).escapeCount;
       	else count = size();
       	(new DataOutputStream(os)).writeInt(count); // byte count up front...
       	serializedTemp.writeTo(os);
       	//dump(serializedTemp);
       	serializedTemp.close();
       	os.flush(); 
       	flushed=true;
	}
//	public static final int MAX_MESSAGE_LEN = 65535; // Contributed by Chris Rued Nov 28, 2011
	public void writeObject(Object obj) throws IOException{
		if (flushed) throw new Error("A PrologOutputObjectStream can be used only once.");
//        if (size() > MAX_MESSAGE_LEN) {
//            throw new IOException("Output buffer contains more than Interprolog can receive. Consider splitting the work. (size=" + size() + "; max=" + MAX_MESSAGE_LEN +")");
//        }		
		tempObjects.writeObject(obj);
	}
	public int size(){
		return serializedTemp.size();
	}
	/** for testing */
	static void dump(ByteArrayOutputStream baos){
		byte[] bytes = baos.toByteArray();
		System.out.println("--DUMP of ALL bytes:");
		for(int b=0;b<bytes.length;b++)
			System.out.println(bytes[b]);
		System.out.println("--END OF DUMP");
	}
	
	/** A ByteArrayOutputStream which avoids keeping 255 (-1) and uses escape byte 101 */
	static class MyByteArrayOutputStream extends ByteArrayOutputStream{
		int escapeCount=0;
		
		public MyByteArrayOutputStream(int capacity){
			super(capacity);
		}
		public void write(int b){
			if (b==101) {
				super.write(101); super.write(1); escapeCount++;
			} else if (b==-1){
				super.write(101); super.write(2); escapeCount++;
			} else super.write(b);
		}
		/** This method is required otherwise the superclass will add the bytes directly to the buffer without "escaping" them */
		public void write(byte[] b,int off, int len){
			for(int i=0;i<len;i++)
				write(b[off+i]);
		}
	}
}
