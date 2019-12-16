/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2016
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import com.declarativa.interprolog.util.IPException;

/** Utilities for caching specific Java state items as Prolog facts */
public class JavaStateCaching {
    private static String CACHING_GOAL = "retractall(ipIsShowing(%s,_)), assert(ipIsShowing(%s,%s))";
    /** To be called from the Prolog side, see ipInitIsShowing/1. Start notifying the Prolog side whenever C.isShowing() changes.
     * NOTE: there should be no pending AWT events when this is called.  */
    public static void initIsShowingCache(final PrologEngine engine,Component C){
    	final int ID = engine.registerJavaObject(C);
    	if (!(engine.deterministicGoal(String.format(CACHING_GOAL, ID, ID, C.isShowing()))))
    		throw new IPException("Could not initialize isShowing caching for "+C);
    	C.addComponentListener(new ComponentAdapter(){
    		public void componentShown(ComponentEvent e){
    			String G = String.format(CACHING_GOAL, ID, ID, true);
    			engine.deterministicGoal(G);
    		}
    		public void componentHidden(ComponentEvent e){
    			String G = String.format(CACHING_GOAL, ID, ID, false);
    			engine.deterministicGoal(G);
    		}
    	});
    }
}
