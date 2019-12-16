/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
import com.declarativa.interprolog.InitiallyFlatTermModel;
import java.io.Serializable;
import java.util.Arrays;
/** Represents a goal from Java, called through deterministicGoal */
public class GoalFromJava implements Serializable{
	private static final long serialVersionUID = 6501437183213553392L;
	int timestamp;
	String G /* includes OVar, RVars*/;
	Object[] objects;
	transient String realGoal;
	public GoalFromJava(int t,String G,String OVar,Object[] objects,String RVars){
		this.objects=objects;
		if (RVars==null) RVars="null";
		this.G="gfj( ( "+G+" ), ( "+OVar+" ), ("+RVars+") )"; timestamp=t; 
		realGoal = G;
		for (int i=0;i<objects.length;i++)
			if (objects[i] instanceof InitiallyFlatTermModel)
				((InitiallyFlatTermModel)objects[i]).deflate();
	}
	public String toString(){return G+"\ntimestamp:"+timestamp+"\n"+Arrays.toString(objects);}
	public String getGoal(){return realGoal;}
}
