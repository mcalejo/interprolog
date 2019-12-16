/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/

package com.declarativa.interprolog;
import java.util.*;
import com.declarativa.interprolog.util.*;

/** Represents a set of Prolog operator declarations, to provide TermModel with knowledge
for toString(). Includes Prolog implementation-dependent knowledge, which could miggrate to PrologImplementationPeer subclasses*/

public class PrologOperatorsContext {
	Hashtable<String,PrologOperator>  prefixOperators,postfixOperators,infixOperators;
	
	PrologOperator prefixOperator(String node){
		return prefixOperators.get(node);
	}
	PrologOperator postfixOperator(String node){
		return postfixOperators.get(node);
	}
	PrologOperator infixOperator(String node){
		return infixOperators.get(node);
	}
	boolean someOperator(String node){
		return prefixOperator(node)!=null || postfixOperator(node)!=null || infixOperator(node)!=null;
	}
	int prefixPrecedence(String node){
		PrologOperator op = prefixOperators.get(node);
		if (op==null) return 0;
		else return op.precedence;
	}
	int postfixPrecedence(String node){
		PrologOperator op = postfixOperators.get(node);
		if (op==null) return 0;
		else return op.precedence;
	}
	int infixPrecedence(String node){
		PrologOperator op = infixOperators.get(node);
		if (op==null) return 0;
		else return op.precedence;
	}
	
	PrologOperatorsContext(PrologOperator[] operators){
		int nOperators=0;
		if (operators!=null) nOperators=operators.length;
		prefixOperators = new Hashtable<String,PrologOperator>(nOperators+1);
		postfixOperators = new Hashtable<String,PrologOperator>(nOperators+1);
		infixOperators = new Hashtable<String,PrologOperator>(nOperators+1);
		for (int o=0;o<nOperators;o++){
			int type=operators[o].type;
			Hashtable<String,PrologOperator> placeholder = null;
			if (type==fx||type==fy) 
				placeholder=prefixOperators;
			else if (type==xfx||type==xfy||type==yfx) 
				placeholder=infixOperators;
			else if (type==xf||type==yf)
				placeholder=postfixOperators;
			placeholder.put(operators[o].name,operators[o]);
		}
	}
	PrologOperatorsContext(){
		this(standardCommonOperators);
	}
	static final int xfx=1,xfy=2,yfx=3,fx=4,fy=5,xf=6,yf=7;
	/* ?- xsbOperators(XSB), swiOperators(SWI), length(SWI,SWIN), length(XSB,XSBN), 
	findall(Op,(member(Op,XSB),member(Op,SWI)),L), length(L,LN).
	...
	G=findall(prologOperator(P,T,Name), current_op(P,T,Name), L), 
	javaMessage('com.declarativa.interprolog.SWISubprocessEngine',SWI,'SWISubprocessEngine'), 
	buildTermModel(G,_GM), javaMessage(SWI,_SM,deterministicGoal(_GM)), 
	recoverTermModel(_SM,findall(_,_,Ops)), assert(swiOperators(Ops)).
	
	Or:
	current_op(P,T,Name), write('new PrologOperator('), write(P), write(','), write(T), write(',"'),write(Name), write('"),'), fail.
*/
	static final PrologOperator[] standardCommonOperators={
		new PrologOperator(1200,xfx,":-"),
		new PrologOperator(1200,xfx,"-->"),
		new PrologOperator(1200,fx,":-"),
		new PrologOperator(1200,fx,"?-"),
		new PrologOperator(1150,fx,"dynamic"),
		new PrologOperator(1150,fx,"multifile"),
		new PrologOperator(1100,xfy,";"),
		new PrologOperator(1050,xfy,"->"),
		new PrologOperator(1000,xfy,","),
		new PrologOperator(900,fy,"\\+"),
		new PrologOperator(700,xfx,"="),
		new PrologOperator(700,xfx,"\\="),
		new PrologOperator(700,xfx,"=="),
		new PrologOperator(700,xfx,"\\=="),
		new PrologOperator(700,xfx,"@<"),
		new PrologOperator(700,xfx,"@=<"),
		new PrologOperator(700,xfx,"@>"),
		new PrologOperator(700,xfx,"@>="),
		new PrologOperator(700,xfx,"=.."),
		new PrologOperator(700,xfx,"is"),
		new PrologOperator(700,xfx,"=:="),
		new PrologOperator(700,xfx,"=\\="),
		new PrologOperator(700,xfx,"<"),
		new PrologOperator(700,xfx,"=<"),
		new PrologOperator(700,xfx,">"),
		new PrologOperator(700,xfx,">="),
		new PrologOperator(600,xfy,":"),
		new PrologOperator(500,yfx,"+"),
		new PrologOperator(500,yfx,"-"),
		new PrologOperator(500,yfx,"/\\"),
		new PrologOperator(500,yfx,"\\/"),
		new PrologOperator(500,fx,"+"),
		new PrologOperator(500,fx,"-"),
		new PrologOperator(500,fx,"\\"),
		new PrologOperator(400,yfx,"*"),
		new PrologOperator(400,yfx,"/"),
		new PrologOperator(400,yfx,"//"),
		new PrologOperator(400,yfx,"mod"),
		new PrologOperator(400,yfx,"rem"),
		new PrologOperator(400,yfx,"<<"),
		new PrologOperator(400,yfx,">>"),
		new PrologOperator(200,xfy,"^")
	};
	static final PrologOperator[] standardGNUOperators={
		new PrologOperator(1200,fx,":-"),
		new PrologOperator(1200,xfx,":-"),
		new PrologOperator(700,xfx,"\\="),
		new PrologOperator(700,xfx,"=:="),
		new PrologOperator(700,xfx,"#>="),
		new PrologOperator(700,xfx,"#<#"),
		new PrologOperator(700,xfx,"@>="),
		new PrologOperator(1200,xfx,"-->"),
		new PrologOperator(400,yfx,"mod"),
		new PrologOperator(700,xfx,"#>=#"),
		new PrologOperator(200,xfy,"**"),
		new PrologOperator(400,yfx,"*"),
		new PrologOperator(200,fy,"+"),
		new PrologOperator(500,yfx,"+"),
		new PrologOperator(1000,xfy,","),
		new PrologOperator(200,fy,"-"),
		new PrologOperator(500,yfx,"-"),
		new PrologOperator(400,yfx,"/"),
		new PrologOperator(700,xfx,"=="),
		new PrologOperator(700,xfx,">="),
		new PrologOperator(700,xfx,"#="),
		new PrologOperator(600,xfy,":"),
		new PrologOperator(1100,xfy,";"),
		new PrologOperator(740,xfy,"#\\==>"),
		new PrologOperator(700,xfx,"<"),
		new PrologOperator(700,xfx,"="),
		new PrologOperator(700,xfx,">"),
		new PrologOperator(700,xfx,"is"),
		new PrologOperator(700,xfx,"#=<"),
		new PrologOperator(720,yfx,"#\\/\\"),
		new PrologOperator(700,xfx,"@=<"),
		new PrologOperator(900,fy,"\\+"),
		new PrologOperator(700,xfx,"#=<#"),
		new PrologOperator(200,fy,"\\"),
		new PrologOperator(400,yfx,"rem"),
		new PrologOperator(200,xfy,"^"),
		new PrologOperator(730,yfx,"#\\\\/"),
		new PrologOperator(700,xfx,"#=#"),
		new PrologOperator(500,yfx,"\\/"),
		new PrologOperator(1050,xfy,"->"),
		new PrologOperator(700,xfx,"=\\="),
		new PrologOperator(700,xfx,"#\\="),
		new PrologOperator(400,yfx,">>"),
		new PrologOperator(700,xfx,"#\\=#"),
		new PrologOperator(730,xfy,"##"),
		new PrologOperator(700,xfx,"#>"),
		new PrologOperator(700,xfx,"@>"),
		new PrologOperator(400,yfx,"//"),
		new PrologOperator(750,xfy,"#<=>"),
		new PrologOperator(700,xfx,"#>#"),
		new PrologOperator(750,xfy,"#\\<=>"),
		new PrologOperator(700,xfx,"\\=="),
		new PrologOperator(500,yfx,"/\\"),
		new PrologOperator(710,fy,"#\\"),
		new PrologOperator(740,xfy,"#==>"),
		new PrologOperator(700,xfx,"=.."),
		new PrologOperator(720,yfx,"#/\\"),
		new PrologOperator(400,yfx,"<<"),
		new PrologOperator(730,yfx,"#\\/"),
		new PrologOperator(700,xfx,"=<"),
		new PrologOperator(700,xfx,"#<"),
		new PrologOperator(700,xfx,"@<")
	};

	static final PrologOperator[] standardSWIOperators={
		new PrologOperator(400,yfx,"/"),
		new PrologOperator(400,yfx,"rem"),
		new PrologOperator(500,fx,"+"),
		new PrologOperator(500,yfx,"+"),
		new PrologOperator(700,xfx,"\\="),
		new PrologOperator(700,xfx,"<"),
		new PrologOperator(500,fx,"\\"),
		new PrologOperator(1150,fx,"volatile"),
		new PrologOperator(700,xfx,"=<"),
		new PrologOperator(1150,fx,"initialization"),
		new PrologOperator(1050,xfy,"*->"),
		new PrologOperator(400,yfx,"<<"),
		new PrologOperator(200,xfx,"**"),
		new PrologOperator(600,xfy,":"),
		new PrologOperator(700,xfx,"=="),
		new PrologOperator(1150,fx,"multifile"),
		new PrologOperator(1000,xfy,","),
		new PrologOperator(1150,fx,"dynamic"),
		new PrologOperator(500,yfx,"/\\"),
		new PrologOperator(700,xfx,"is"),
		new PrologOperator(1100,xfy,"|"),
		new PrologOperator(1200,xfx,"-->"),
		new PrologOperator(1,fx,"$"),
		new PrologOperator(1150,fx,"module_transparent"),
		new PrologOperator(500,fx,"-"),
		new PrologOperator(500,yfx,"-"),
		new PrologOperator(1100,xfy,";"),
		new PrologOperator(1200,fx,"?-"),
		new PrologOperator(400,yfx,"mod"),
		new PrologOperator(500,yfx,"\\/"),
		new PrologOperator(400,yfx,"xor"),
		new PrologOperator(1200,fx,":-"),
		new PrologOperator(1200,xfx,":-"),
		new PrologOperator(700,xfx,"="),
		new PrologOperator(700,xfx,">="),
		new PrologOperator(1150,fx,"meta_predicate"),
		new PrologOperator(700,xfx,"=.."),
		new PrologOperator(700,xfx,"=:="),
		new PrologOperator(700,xfx,">"),
		new PrologOperator(200,xfy,"^"),
		new PrologOperator(700,xfx,"=\\="),
		new PrologOperator(400,yfx,">>"),
		new PrologOperator(700,xfx,"\\=@="),
		new PrologOperator(400,yfx,"//"),
		new PrologOperator(1150,fx,"discontiguous"),
		new PrologOperator(700,xfx,"@<"),
		new PrologOperator(500,fx,"?"),
		new PrologOperator(700,xfx,"@=<"),
		new PrologOperator(1150,fx,"thread_local"),
		new PrologOperator(900,fy,"\\+"),
		new PrologOperator(700,xfx,"=@="),
		new PrologOperator(1050,xfy,"->"),
		new PrologOperator(700,xfx,"@>"),
		new PrologOperator(700,xfx,"\\=="),
		new PrologOperator(400,yfx,"*"),
		new PrologOperator(700,xfx,"@>=")
	};
	
	static final PrologOperator[] standardXSBOperators={
		//current_op(P,T,Name), write(prologOperator(P,T,Name)), nl, fail. ...
		new PrologOperator(1200,xfx,":-"),
		new PrologOperator(1200,xfx,"-->"),
		new PrologOperator(1200,fx,":-"),
		new PrologOperator(1200,fx,"?-"),
		new PrologOperator(1198,xfx,"::-"),
		new PrologOperator(1150,fx,"hilog"),
		new PrologOperator(1150,fx,"dynamic"),
		new PrologOperator(1150,fx,"multifile"),
		new PrologOperator(1100,xfy,";"),
		new PrologOperator(1100,fx,"table"),
		new PrologOperator(1100,fx,"use_variant_tabling"),
		new PrologOperator(1100,fx,"use_subsumptive_tabling"),
		new PrologOperator(1100,fx,"edb"),
		new PrologOperator(1100,fy,"index"),
		new PrologOperator(1100,fy,"ti"),
		new PrologOperator(1100,fy,"ti_off"),
		new PrologOperator(1100,fx,"mode"),
		new PrologOperator(1100,fx,"export"),
		new PrologOperator(1100,fx,"parallel"),
		new PrologOperator(1100,fx,"local"),
		new PrologOperator(1100,fx,"foreign_pred"),
		new PrologOperator(1100,fx,"compile_command"),
		new PrologOperator(1100,fx,"attribute"),
		new PrologOperator(1050,fy,"import"),
		new PrologOperator(1050,xfx,"from"),
		new PrologOperator(1050,xfy,"->"),
		new PrologOperator(1000,xfy,","),
		new PrologOperator(900,fy,"not"),
		new PrologOperator(900,fy,"\\+"),
		new PrologOperator(900,fy,"spy"),
		new PrologOperator(900,fy,"nospy"),
		new PrologOperator(700,xfx,"="),
		new PrologOperator(700,xfx,"\\="),
		new PrologOperator(700,xfx,"=="),
		new PrologOperator(700,xfx,"\\=="),
		new PrologOperator(700,xfx,"@<"),
		new PrologOperator(700,xfx,"@=<"),
		new PrologOperator(700,xfx,"@>"),
		new PrologOperator(700,xfx,"@>="),
		new PrologOperator(700,xfx,"=.."),
		new PrologOperator(700,xfx,"^=.."),
		new PrologOperator(700,xfx,"is"),
		new PrologOperator(700,xfx,"=:="),
		new PrologOperator(700,xfx,"=\\="),
		new PrologOperator(700,xfx,"<"),
		new PrologOperator(700,xfx,"=<"),
		new PrologOperator(700,xfx,">"),
		new PrologOperator(700,xfx,">="),
		new PrologOperator(661,xfy,"."),
		new PrologOperator(600,xfy,":"),
		new PrologOperator(500,yfx,"+"),
		new PrologOperator(500,yfx,"-"),
		new PrologOperator(500,yfx,"/\\"),
		new PrologOperator(500,yfx,"\\/"),
		new PrologOperator(500,fx,"+"),
		new PrologOperator(500,fx,"-"),
		new PrologOperator(500,fx,"\\"),
		new PrologOperator(400,yfx,"*"),
		new PrologOperator(400,yfx,"/"),
		new PrologOperator(400,yfx,"//"),
		new PrologOperator(400,yfx,"mod"),
		new PrologOperator(400,yfx,"rem"),
		new PrologOperator(400,yfx,"<<"),
		new PrologOperator(400,yfx,">>"),
		new PrologOperator(400,yfx,"\\"),
		new PrologOperator(200,xfy,"^")
		};
	
	public static class PrologOperator{
		final int precedence;
		final int type;
		final String name;
		PrologOperator(int p,int t,String n){
			if (t<xfx||t>yf||p<1||p>1200||n==null)
				throw new IPException("Bad arguments in PrologOperator constructor");
			precedence=p; type=t; name=n;
		}
	}
}
