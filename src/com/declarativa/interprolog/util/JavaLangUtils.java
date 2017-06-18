/*
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
 */
package com.declarativa.interprolog.util;
import com.declarativa.interprolog.AbstractPrologEngine;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 *
 * @author  crued
 */
public class JavaLangUtils {

    /** Private constructor so no one tries to create this */
    private JavaLangUtils() {
    }

    /**
     * An utility building on the functionality of getMethod(), to provide the
     * javaMessage predicate with method argument polymorphism. If the type
     * signatures do not match exactly, searches all method signatures to see if
     * their arguments are type-compatible.
     */
    public static Method findMethod(Class<?> targetClass, String name, Class<?>[] formalArguments) throws NoSuchMethodException {
    	return AbstractPrologEngine.findMethod(targetClass, name,formalArguments);
    }

    /**
     * Similar to findMethod(), but for constructors rather than regular methods
     */
    public static Constructor<?> findConstructor(Class<?> targetClass, Class<?>[] formalArguments) throws NoSuchMethodException {
    	return AbstractPrologEngine.findConstructor(targetClass, formalArguments);
    }

    /**
     * It is OK to assign an expression typed right to a variable typed left.
     * Delegates on isAssignableFrom
     */
    public static boolean assignableType(Class<?> left, Class<?> right) {
    	return AbstractPrologEngine.assignableType(left,right);
    }

    /**
     * Returns just the name of the class, with no package information.
     * That is, if <CODE>foo.bar.Mumble</CODE> were the class passed in, the
     * string <CODE>"Mumble"</CODE> would be returned.  Similarly, if the class
     * that is passed in is <CODE>a.b.Class$InnerClass</CODE> the string
     * returned would be <CODE>InnerClass</CODE>.
     */
    public static String shortClassName(Class<?> c) {
    	return AbstractPrologEngine.shortClassName(c);
    }
}
