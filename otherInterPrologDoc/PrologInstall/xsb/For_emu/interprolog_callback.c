/* 
** Author(s): Miguel Calejo, Vera Pereira
** Contact:   interprolog@declarativa.com, http://www.declarativa.com, http://www.xsb.com
** Copyright (C) XSB Inc., USA, 2001; Declarativa Lda., Portugal, 2013
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/

/* This file provides the implementation for the native methods of NativeEngine.java, 
as well as the XSB Prolog built-in used by InterProlog */
#include <jni.h>
#include "com_xsb_interprolog_NativeEngine.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
// for interrupts:
#include "sig_xsb.h"
#include <signal.h>
// extern int *asynint_ptr;
extern void keyint_proc(int);

/* The following include is necessary to get the macros and routine
   headers */

#include "xsb_config.h"
#include "auxlry.h"
#include "context.h"
#include "cell_xsb.h"
#include "memory_xsb.h"
#include "register.h"
#include "flags_xsb.h"
#include "heap_xsb.h"
#include "subp.h"
#include "cinterf.h"
#include "error_xsb.h"
#include "loader_xsb.h"
#include "cinterf.h"
#include "thread_xsb.h"


JavaVM *theJvm = NULL;
// JNIEnv *theEnv;
jobject theObj;
jboolean debug;
#ifdef MULTI_THREAD
   static th_context *th ;
#endif

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *jvm, void *reserved) {
	theJvm = jvm;
	return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *jvm, void *reserved) {
	int rc = xsb_close(CTXT);
	if (rc != 0) {
		fprintf(stderr, "XSB exited uncleanly!\n");
	}
}


JNIEXPORT jint JNICALL 
Java_com_xsb_interprolog_NativeEngine_put_1bytes
(JNIEnv *env, jobject obj, jbyteArray b, jint size, jint args, jstring jStr) {
	// int head, tail, newVar, newVar2;
	prolog_term head, tail, newVar, newVar2;
	int i = 1;
	int rc;
	
	//long T0 = clock(), T1, T2, T3, T4;
	jbyte *bytes; char *argString;
	
	if (debug==JNI_TRUE) printf("C:Entering put_bytes\n");
	check_glstack_overflow(3, pcreg, (size+4)*2*sizeof(Cell)) ;  /* 2 was 8? dsw*/
	
	bytes = (*env)->GetByteArrayElements(env, b, 0);
	
	//T1 = clock() - T0;
	//printf("C:got bytes array - %d mS",T1);
	
	argString = (char *)((*env)->GetStringUTFChars(env, jStr, 0));
	if (debug==JNI_TRUE) printf("C:will try to call %s\n",argString);
	c2p_functor(CTXTc argString, args, reg_term(CTXTc 1));
	c2p_list(CTXTc p2p_arg(reg_term(CTXTc 1),1));
	if (args == 3) {
		newVar = p2p_arg(reg_term(CTXTc 1),2);
		newVar = p2p_new(CTXT);
		newVar2 = p2p_arg(reg_term(CTXTc 1),3);
		newVar2 = p2p_new(CTXT);
	}
	head = p2p_car(p2p_arg(reg_term(CTXTc 1),1));
	tail = p2p_cdr(p2p_arg(reg_term(CTXTc 1),1));
	if (bytes[0]<0){
		c2p_int(CTXTc (bytes[0]+256), head);
	}else{
		c2p_int(CTXTc bytes[0], head);
	}
	while (i < size) {
		c2p_list(CTXTc tail);
		head = p2p_car(tail);
		tail = p2p_cdr(tail);
		if (bytes[i]<0)
			c2p_int(CTXTc (bytes[i]+256), head);
		else
			c2p_int(CTXTc bytes[i], head);
		i++;
	}
	c2p_nil(CTXTc tail);
	
	//T2 = clock() - T0 ;
	//printf("C:constructed Prolog list - %d mS",T2);

	// theEnv = env;
	theObj = obj;
	rc = xsb_query(CTXT);
	
	//T3 = clock() - T0;
	//printf("C:Returned from Prolog - %d mS",T3);
	
	(*env)->ReleaseByteArrayElements(env, b, bytes, 0);
	(*env)->ReleaseStringUTFChars(env,jStr, argString);
	if (debug==JNI_TRUE) printf("C:leaving put_bytes\n");
	
	//T4 = clock() - T0;
	//printf("C:leaving put_bytes - %d mS\n",T4);
	
	return rc;
}


JNIEXPORT jint JNICALL 
Java_com_xsb_interprolog_NativeEngine_xsb_1close_1query
(JNIEnv *env , jobject obj) {
	return xsb_close_query(CTXT);
}


JNIEXPORT jint JNICALL 
Java_com_xsb_interprolog_NativeEngine_xsb_1command_1string
(JNIEnv *env, jobject obj, jstring jCommandString) {
	int rcode;
	char *CommandString = (char *)((*env)->GetStringUTFChars(env, jCommandString, 0));
	// theEnv = env;
	theObj = obj;
	rcode=xsb_command_string(CTXTc CommandString);
	if (rcode==XSB_ERROR) printf("XSB error during realCommand:%s\n",xsb_get_error_message(CTXTc));
	(*env)->ReleaseStringUTFChars(env,jCommandString, CommandString);
	return rcode;
}

JNIEXPORT jint JNICALL 
Java_com_xsb_interprolog_NativeEngine_xsb_1init_1internal
(JNIEnv *env, jobject obj, jstring jXSBPath) {
	int rcode;
	int myargc=2;
	char * myargv[2];
	char *XSBPath = (char *)((*env)->GetStringUTFChars(env, jXSBPath, 0));

	if (debug==JNI_TRUE) printf("Entering Java_com_xsb_interprolog_NativeEngine_xsb_1init_1internal\n");
	myargv[0] = XSBPath;
	myargv[1]="-n";
	
	rcode=xsb_init(myargc,myargv);
	(*env)->ReleaseStringUTFChars(env,jXSBPath, XSBPath);
	if (debug==JNI_TRUE) printf("Exiting Java_com_xsb_interprolog_NativeEngine_xsb_1init_1internal\n");
	return rcode;
}

JNIEXPORT jint JNICALL
Java_com_xsb_interprolog_NativeEngine_xsb_1init_1internal_1arg
(JNIEnv *env, jobject obj, jstring jXSBPath, jobjectArray jXSBParameters) {
        int rcode;
	int i;
	int myargc;
        char ** myargv;
	jstring * parameters;
	char *XSBPath;

	myargc = (*env)->GetArrayLength(env, jXSBParameters) + 2;
       	myargv = (char **) mem_alloc(myargc * sizeof(char *),INTERPROLOG_SPACE);

	XSBPath = (char *)((*env)->GetStringUTFChars(env, jXSBPath, 0));
	if (debug==JNI_TRUE) printf("Entering Java_com_xsb_interprolog_NativeEngine_xsb_1init_1internal\n");
	myargv[0] = XSBPath;
	myargv[1] = "-n";
	
	parameters = (jstring *) mem_alloc((myargc - 2) * sizeof(jstring *),INTERPROLOG_SPACE);
	for (i=0; i < myargc-2; i++) {
          parameters[i] = (jstring)(*env)->GetObjectArrayElement(env, jXSBParameters, i);
	  myargv[i + 2] = (char *)((*env)->GetStringUTFChars(env, parameters[i], 0));
	}

	rcode=xsb_init(myargc,myargv);

	(*env)->ReleaseStringUTFChars(env,jXSBPath, XSBPath);

      	for (i=0; i < myargc-2; i++) {
	  (*env)->ReleaseStringUTFChars(env, parameters[i], myargv[i + 2]);
	  // may be needed ?(*env)->DeleteLocalRef(env, parameters[i]);
	}
	mem_dealloc(parameters,(myargc-2)*sizeof(jstring *),INTERPROLOG_SPACE);
	mem_dealloc(myargv,myargc*sizeof(char *),INTERPROLOG_SPACE);

	if (debug==JNI_TRUE) printf("Exiting Java_com_xsb_interprolog_NativeEngine_xsb_1init_1internal_1arg\n");
	return rcode;
}

JNIEXPORT void JNICALL 
Java_com_xsb_interprolog_NativeEngine_xsb_1interrupt (JNIEnv *env, jobject obj){
	/* Do XSB's "interrupt" thing, by simulating a ctrl-C: */
	// cf. keyint_proc(SIGINT) in subp.c; 
	// *asynint_ptr |= KEYINT_MARK; 
	keyint_proc(SIGINT);
}

JNIEXPORT void JNICALL Java_com_xsb_interprolog_NativeEngine_xsb_1setDebug
  (JNIEnv * env, jobject obj, jboolean d){
  	debug = d;
}

  
// the XSB Prolog built-in used in interprolog.P
// arguments: +length, +byte list, -new byte list
xsbBool interprolog_callback(CTXTdecl) {
	// JNIEnv *env = theEnv;
	JNIEnv *env;
	if (theJvm == NULL) {
		fprintf(stderr, "interprolog_callback: Java Virtual Machine not set!\n");
		return 0;
	}
	(*theJvm)->AttachCurrentThread(theJvm, (void**)&env, NULL);

	jobject obj = theObj;

	jclass cls;
	jmethodID mid;
	int i = 1;
	jsize size, bsize;
	prolog_term newHead, newTail;
	jbyte *b, *nb;
	jbyteArray newBytes, bytes;

	cls = (*env)->GetObjectClass(env, obj);
	if (cls == NULL) {
		printf("Could not find the class!\n");
		return 0;
	}
	//printf("Got the class\n");
	mid = (*env)->GetMethodID(env, cls, "callback", "([B)[B");
	//test: mid = (*env)->GetMethodID(env, cls, "callback", "()V");
	if (mid == NULL) {
		printf("Could not find the method!\n");
		return 0;
	}
	//printf("Got the method\n");
    if (debug==JNI_TRUE) printf("C:interprolog_callback\n");

	bsize = (jsize)p2c_int(reg_term(CTXTc 1));

    if (debug==JNI_TRUE) printf("bsize: %d\n",bsize);

	b = (jbyte *) mem_alloc(bsize,INTERPROLOG_SPACE);
	if (b==NULL){
		printf("Could not alloc b array!\n");
		return 0;
	}
	
	newHead = p2p_car(reg_term(CTXTc 2));
	newTail = p2p_cdr(reg_term(CTXTc 2));
	b[i-1] = (int)p2c_int(newHead);
	if (debug==JNI_TRUE) printf("%d ",b[i-1]);	
	while(is_nil(newTail) == 0) { 
		newHead = p2p_car(newTail);
		b[i] = (int)p2c_int(newHead);
		if (debug==JNI_TRUE) printf("%d ",b[i]);	
		newTail = p2p_cdr(newTail);
		i++;
	}
	bytes = (*env)->NewByteArray(env, bsize);
	if (bytes==NULL){
		printf("Could not create bytes array!\n");
		return 0;
	}
	
	(*env)->SetByteArrayRegion(env, bytes, 0, bsize, b);
	mem_dealloc(b,bsize,INTERPROLOG_SPACE);
	
    if (debug==JNI_TRUE) printf("C:byte array has%d\n",(*env)->GetArrayLength(env, bytes));	
    if (debug==JNI_TRUE) printf("C:messaging Java\n");	
    
	// Calls the method with bytes, expecting the return in newBytes
	// bytes is a good array, newBytes comes back null, exception occurs during this (but can't throw it...)
	// callback method is not initiated..... env refers the same thread (Prolog handler)
	// can a JNI callback when the first native method was in a different thread...? 
	// ...Because callbacks DO work if in the first (main) thread!
	// making all JNI calls NOT on the main thread does NOT fix the problem
	// XSB 337 (12 Ago 2012) works well, XSB March 2013 30 CVS tarball does NOT
	// 
	newBytes = (*env)->CallObjectMethod(env, obj, mid , bytes);
    if (debug==JNI_TRUE) printf("C:returned from Java\n");	
    jthrowable EE = (*env)->ExceptionOccurred(env);
	if (EE){
		printf("Pending exception:\n");
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
		printf("Throwing JNI exception:\n");
		(*env)->Throw(env, EE);
	}

    if (newBytes==NULL){
		printf("\ndid not get back a bytes array!\n");
	}
	size = (*env)->GetArrayLength(env, newBytes);
	nb = (*env)->GetByteArrayElements(env, newBytes, 0);
	check_glstack_overflow(3, pcreg, size*8*sizeof(Cell)) ;

	c2p_list(CTXTc reg_term(CTXTc 3));
	newHead = p2p_car(reg_term(CTXTc 3));
	newTail = p2p_cdr(reg_term(CTXTc 3));
	if (nb[0]<0) {
		c2p_int(CTXTc (nb[0]+256), newHead);
	} else {
		c2p_int(CTXTc nb[0], newHead);
	}
	i = 1;
	if (debug==JNI_TRUE) printf("C:constructing result list\n");	
	while (i < size) {
		c2p_list(CTXTc newTail);
		newHead = p2p_car(newTail);
		newTail = p2p_cdr(newTail);
		if (nb[i]<0) {
			c2p_int(CTXTc (nb[i]+256), newHead);
		} else {
			c2p_int(CTXTc nb[i], newHead);
		}
		i++;
	}
	if (debug==JNI_TRUE) printf("C:almost done building result list\n");	
	(*env)->ReleaseByteArrayElements(env, newBytes, nb, JNI_ABORT);
	c2p_nil(CTXTc newTail);
	if (debug==JNI_TRUE) printf("C:exiting interprolog_callback\n");	
	return 1;
}
