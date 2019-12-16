package com.declarativa.interprolog.util;

import java.util.Iterator;
/**
 * A wrapper for an Iterator, to use a single call - Object next() - to obtain the ennumeration. 
 * Items can NOT be null.
 * E.g. use from Prolog with
 java('com.declarativa.interprolog.util.CompactIterator',MyIt,'CompactIterator'(Iterator)),
 java(MyIt,OneResult,next),
 ( OneResult == null, !, ... ; OneResult = ..., recurse).
	 
 * @author mc
 *
 */
@SuppressWarnings("rawtypes")
public class CompactIterator {
	private Iterator it;
	public CompactIterator(Iterator it){
		this.it = it;
	}
	/** Returns null if no more items */
	public Object next(){
		if (it.hasNext()) return it.next();
		else return null;
	}
}
