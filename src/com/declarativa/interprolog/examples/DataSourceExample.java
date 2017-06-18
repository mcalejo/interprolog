package com.declarativa.interprolog.examples;

import java.util.ArrayList;

import com.declarativa.interprolog.FloraSubprocessEngine;
import com.declarativa.interprolog.TermModel;
/** 
 * An array and an ArrayList of TermModel to feed some tutorial examples
 */
public class DataSourceExample {
	// First, ingredients for access via an iterator
	private static ArrayList<TermModel> buffer = new ArrayList<TermModel>();
	static {
		// build a sample buffer
		buffer.add(new TermModel("one"));
		buffer.add(new TermModel("two"));
		TermModel sum = new TermModel("+",new TermModel[]{new TermModel("one"), new TermModel("two")});
		buffer.add(sum);
		buffer.add(new TermModel(4));
	}
	//  ?- java('com.declarativa.interprolog.examples.DataSourceExample',AL,getBuffer), 
	//     java(AL,It,iterator), java(It,First,next), java(It,boolean(More),hasNext).
	//  Result: First = term(one)   More = 1
	public static ArrayList<TermModel> getBuffer(){
		return buffer;
	}
	
	//second, for direct access to an array
	//  java('com.declarativa.interprolog.examples.DataSourceExample'-bigArray, Second, [1]).
	public static TermModel[] bigArray = buffer.toArray(new TermModel[0]);
	
	// ?- java('com.declarativa.interprolog.examples.DataSourceExample', Array, getBigArray).
	// Array = terms([one,two,one + two,4])
	public static TermModel[] getBigArray(){
		return bigArray;
	}
	// since serializable arrays are returned in whole entirely to the logic side,
	// we need first to get an object reference to it
	// ?- ipPrologEngine(E), 
	//    java('com.declarativa.interprolog.examples.DataSourceExample', ArrayRef, getbigArrayReference(E)), 
	//    java(ArrayRef,Third,[2]).

	public static int getbigArrayReference(FloraSubprocessEngine e){
		return e.registerJavaObject(bigArray);
	}
}
