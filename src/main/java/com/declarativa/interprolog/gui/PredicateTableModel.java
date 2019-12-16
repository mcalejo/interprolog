/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.gui;
import com.declarativa.interprolog.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.io.Serializable;


public class PredicateTableModel implements Serializable, TableModel{
	private static final long serialVersionUID = -6249547487405575036L;
	final TermModel template;
	final TermModel[] tuples;
	int arity; // should be transient
	String functor; // should be transient
	
	public static ObjectExamplePair example(){
		TermModel[] Atuples = new TermModel[1];
		Atuples[0]=new TermModel("c");
		TermModel[] Xtuples = new TermModel[1];
		Xtuples[0] = new TermModel("a");
		PredicateTableModel ptm1 = new PredicateTableModel(null,Xtuples);
		PredicateTableModel ptm2 = new PredicateTableModel(new TermModel("b",Atuples),null);
		return new ObjectExamplePair("PredicateTableModel",ptm1,ptm2);
	}
	
	public PredicateTableModel(TermModel template,TermModel[] tuples){
		if (template == null && tuples==null) 
			throw new RuntimeException("The PredicateTableModel constructor needs a non-null argument");
		if (template == null && tuples.length==0)
			throw new RuntimeException("The PredicateTableModel constructor needs some tuple");
		if (template!=null) {
			arity=template.getChildCount();
			functor=template.node.toString();
		} else {
			arity=tuples[0].getChildCount();
			functor=tuples[0].node.toString();
		}
		if (tuples==null) tuples = new TermModel[0];
		for (int t=0; t<tuples.length; t++) {
			if (tuples[t]==null)
				throw new RuntimeException("Null tuple in PredicateTableModel tuple "+t);
			if (arity!=tuples[t].getChildCount())
				throw new RuntimeException("Conflicting arity in PredicateTableModel tuple "+t);
		}
		this.template = template;
		this.tuples = tuples;
	}
	
  	public String toString(){
  		return functor /* +"/"+arity*/ +" ("+getRowCount()+" tuples)";
  	}

	// TableModel methods:
	
	// We don't remember listeners because we're immutable
	public void addTableModelListener(TableModelListener l){
	}
	
	public void removeTableModelListener(TableModelListener l){
	}
		
	public Class<Object> getColumnClass(int columnIndex){
		return Object.class;
	}
	
	public boolean isCellEditable(int rowIndex,int columnIndex){
		return false;
	}
	
	public void setValueAt(Object aValue,int rowIndex,int columnIndex){
		throw new Error("PredicateTableModel can not be edited!");
    }
 	public String getColumnName(int columnIndex){
 		if (template==null) return "Arg"+columnIndex;
 		else {
 			return template.children[columnIndex].toString();
 		}
 	}
   	public int getRowCount(){
  		return tuples.length;
  	}
  	public int getColumnCount(){
  		return arity;
  	}
  	public Object getValueAt(int row, int column){
  		return tuples[row].children[column];
  	}
}
