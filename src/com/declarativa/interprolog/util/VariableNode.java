/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
import java.io.Serializable;

import com.declarativa.interprolog.ObjectExamplePair;

/** TermModel node for a free Prolog variable. Each free Prolog variable at TermModel construction time
corresponds to a globally unique Integer number, as enforced by ip_inc_var_counter 
in interprolog.P. */
public class VariableNode implements Serializable{
	private static final long serialVersionUID = -3977871843710903016L;
	Integer number;
	public static ObjectExamplePair example(){
		return new ObjectExamplePair("VariableNode",
			new VariableNode(1),
			new VariableNode(2)
			);
	}
	public VariableNode(int n){
		number = new Integer(n);
	}
	
	public String toString(){
		return "Var"+number;
	}
	public boolean equals(Object x){
		return (x.getClass()==getClass()  && number.equals( ((VariableNode)x).number ));
	}
}
