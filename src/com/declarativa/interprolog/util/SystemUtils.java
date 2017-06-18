/*
Author: Chris Rued
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
 */

package com.declarativa.interprolog.util;
import com.declarativa.interprolog.AbstractPrologEngine;
import com.declarativa.interprolog.PrologEngine;

/**
 *
 * @author  crued
 */
public class SystemUtils {
    
    // XXX Perhaps these should be methods in the peer or implementation of
    // PrologEngine???
    
    /**
     * Maximum integer value in any supported Prolog. XSB Prolog uses 28 bits.
     * Unfortunately, it is pointless to intercept writeInt in
     * <CODE>ObjectOutputStream</CODE>, because
     *   <ol>
     *     <li> the method is invoked in several situations, not just to
     *          serialize int values, and</li>
     *     <li> some classes may serialize in very weird ways</li>
     *   </ol>
     * so in general it is not possible to detect int values beyond these
     * boundaries
     */
    public static final int MAX_INT_VALUE = PrologEngine.MAX_INT_VALUE;
    
    /** Minimum integer value.*/
    public static final int MIN_INT_VALUE = PrologEngine.MIN_INT_VALUE;
    
    /** Convenience for newline */
    public final static String nl = System.getProperty("line.separator") ;
    
    /** Private constructor so nobody creates an instance */
    private SystemUtils() { }
    
    public static boolean isWindowsOS(){ 
        return AbstractPrologEngine.isWindowsOS();
    }
    
    public static boolean isMacOS(){ 
        return AbstractPrologEngine.isMacOS();
    }
    
}
