/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.declarativa.interprolog.util.IPException;
import com.declarativa.interprolog.util.VariableNode;

/** Represents a Prolog term, as a tree of TermModel objects, each containing a term node and a children list.
 Implements TreeModel, therefore easily supporting display in a JTree.
   It includes the functionality of Prolog's Edinburgh syntax write within toString(); if you wish to reflect
  operator declarations dynamically you should provide a different PrologOperatorsContext object  */
public class TermModel implements Serializable,TreeModel{
	private static final long serialVersionUID = 5148989080944522740L;
	/** public for convenience, but should not be set outside this class */
	public Object node;
	/** public for convenience, but should not be set outside this class;
	children == null means children == new TermModel[0]*/
	public TermModel[] children; 
	/** This should be true only if node=="." or node=="[|]" (on SWI) or node=="[]" and this represents a list term */
	protected boolean hasListFunctor;
	
	private transient Vector<TreeModelListener> treeListeners=null /*new Vector() here doesn't work on deserialization...*/;
	private transient Vector<TermModelListener> termListeners;
	/** Used for hashCode() */
	private transient int hashCodeCache=0;
	/** The TermModel containing this; relevant to define variable scope. For this to contain the root, 
	the root TermModel must have been messaged once with setRoot() */
	public transient TermModel root; 
	
	/** Prolog functor used to construct lists. Although some Prologs (e.g. SWI) may use a different functor, on the Java side
	 * (and because this class is engine agnostic) we'll always use "." to build lists, 
	 * but will tolerate [|] in lists coming from Prolog
	 */
	public static String LIST_FUNCTOR = ".";
	
	static PrologOperatorsContext defaultOperatorContext = new PrologOperatorsContext();
	
	public static ObjectExamplePair example(){
		return new ObjectExamplePair("TermModel",
			new TermModel(new Integer(1),(TermModel[])null,false),
			new TermModel(LIST_FUNCTOR,new TermModel[2],true)
			);
	}
	
	/** Clones by serialization, to keep variable bindings; these should form a closed graph
	within this term, otherwise the new term instance would contain dangling references */
	public Object clone(){
		Object x = null;
		try{
			ByteArrayOutputStream buffer = new ByteArrayOutputStream(100);
			ObjectOutputStream oos = new ObjectOutputStream(buffer);
			oos.writeObject(this); oos.close();
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
			x = ois.readObject();
			ois.close();
		} catch (Exception e){
			throw new IPException("Failed to clone");
		}
		return x;
	}
	
	/** True if both terms are structurally similar with equal nodes. 
	If nonground...variables will be compared as if with == . Probably what you do NOT want to do.*/
	public boolean equals(Object x){
		if (!(x instanceof TermModel)) return false;
		TermModel tx = (TermModel)x;
		if (!(node.equals(tx.node))) return false;
		if (getChildCount()!=tx.getChildCount()) return false;
		for (int c=0; c<getChildCount(); c++)
			if (!(getChild(c).equals(tx.getChild(c)))) return false;
		return true;
	}
	
	public boolean variant(TermModel tx){
		Map<VariableNode,VariableNode> myVars = new HashMap<VariableNode,VariableNode>();
		return variant(tx,myVars);
	}
	
	public boolean variant(TermModel tx,Map<VariableNode,VariableNode> myVars){
		if (node instanceof VariableNode){
			if (!(tx.node instanceof VariableNode)) return false;
			VariableNode myVar = (VariableNode)node;
			VariableNode otherVar = myVars.get(myVar);
			if (otherVar==null) {
				if (myVars.containsValue(tx.node)) return false;
				myVars.put(myVar,(VariableNode)tx.node);
			} else if (!otherVar.equals(tx.node)) return false;		
		} else if (!(node.equals(tx.node))) return false;
		
		if (getChildCount()!=tx.getChildCount()) return false;
		for (int c=0; c<getChildCount(); c++)
			if (!(((TermModel)getChild(c)).variant((TermModel)tx.getChild(c)))) return false;
		return true;
	}
	
	/** Set the node, notifying TermModelListeners */
	public void setNodeValue(Object v){
		if (v!=null) {
			if (!(v instanceof Serializable))
			throw new IPException("TermModel nodes must be Serializable");
			if (!((v instanceof Number)||(v instanceof String)||(v instanceof VariableNode)))
			throw new IPException("Bad TermModel node type");
		}
		boolean changed = !(v==node);
		node=v;
		if (changed) fireTermChanged();
	}
	
	/** Set the child, notifying TermModelListeners */
	public void setChild(int index,TermModel child){
		boolean changed = !(child==children[index]);
		children[index] = child;
		if (changed) fireTermChanged();
	}
	
	/** Set the children array, notifying TermModelListeners */
	public void setChildren(TermModel[] c){
		boolean changed = !(children==c);
		children=c;
		if (changed) fireTermChanged();
	}
	
	public TermModel[] getChildren(){
		return children;
	}
	
	/** Add children to this term node; a new children array is created and old children are copied to it */
	public void addChildren(TermModel[] more){
		if (more.length==0) return;
		TermModel[] newChildren = new TermModel[getChildCount()+more.length];
		for (int i=0;i<getChildCount();i++)
			newChildren[i] = children[i];
		for (int i=getChildCount();i<getChildCount()+more.length;i++)
			newChildren[i] = more[i-getChildCount()];
		setChildren(newChildren);
	}
	
	/** Delete children with specified indexes; a new children array is created and old children are copied to it */
	public void deleteChildren(int[] less){
		if (less.length==0) return;
		TermModel[] newChildren = new TermModel[getChildCount()-less.length];
		int oldIndex=0;
		for (int newIndex=0;newIndex<newChildren.length;newIndex++){
			while (inArray(oldIndex,less)) oldIndex++;
			newChildren[newIndex] = children[oldIndex];
			oldIndex++;
		}
		setChildren(newChildren);
	}
	
	/** Removes the children in array; it finds children to remove by using the equivalent of Prolog's ==. All "matching" terms
	will be removed */
	public void deleteChildren(TermModel[] less){
		if (less.length==0) return;
		// System.out.println(getChildCount()+ "children, less.length=="+less.length);
		// TermModel[] newChildren = new TermModel[getChildCount()-less.length];
		Vector<TermModel> newTemp = new Vector<TermModel>();
		
		for (int oldIndex=0;oldIndex<children.length;oldIndex++){
			if(inArray(children[oldIndex],less)) continue;
			// System.out.println("oldIndex=="+oldIndex+",oldchildren.length=="+children.length+",newIndex=="+newIndex);
			newTemp.addElement(children[oldIndex]);
		}
		
		TermModel[] newChildren = new TermModel[newTemp.size()];
		for(int i=0;i<newChildren.length;i++)
			newChildren[i]=newTemp.elementAt(i);
		setChildren(newChildren);
	}
	
	static boolean inArray(int x,int[] a){
		for(int i=0;i<a.length;i++)
			if (x==a[i]) return true;
		return false;
	}
	
	static boolean inArray(Object x,Object[] a){
		for(int i=0;i<a.length;i++)
			if (x.equals(a[i])) return true;
		return false;
	}
	
	/** Replaces all occurrences of v in this subterm by value, using setNodeValue */
	public void assignToVar(VariableNode v,Object value){
		if (v.equals(node)) {
			setNodeValue(value);
		}
		for (int i=0; i<getChildCount(); i++)
			children[i].assignToVar(v,value);
	}

	/** Assuming this and the other terms are structurally similar, replaces variable nodes in this term
	by corresponding nonvar nodes. 	If either a (this) node is non var or a (other) node is var,
	 (this) node is left untouched.*/
	public void assignTermChanges(TermModel other){
            // for cases when this node is not variable (compound structure) and the other is variable
            // loop ahead did not handle it  - other node does not have any children
		if (!nodeIsVar() && other.nodeIsVar()) {
                    return;
		}

		if (nodeIsVar() && !(other.nodeIsVar())) {
			// System.out.println("changing from node "+node+" to "+other.node);
			setNodeValue(other.node);
                        setChildren(other.getChildren());
                        return;
		}
		// Overcautious check, may perhaps be removed later:
		if (!nodeIsVar() && !(other.nodeIsVar()) && !node.equals(other.node))
			throw new IPException("Inconsistent term: "+node+" different from "+other.node);
		for (int i=0;i<getChildCount();i++)
			((TermModel)getChild(i)).assignTermChanges((TermModel)other.getChild(i));
	}

	/** Set the root variables of nodes in this subterm to refer this term node as their root */
	public void setRoot(){
		setRoot(this);
	}
		
	/** Set the root variables of nodes in this subterm to refer r as their root */
	public void setRoot(TermModel r){
		if (root!=null) return; // subtree already set, ignore "new" root
		root=r;
		propagateRoot();
	}
	
	/** This node is its own root */
	public boolean isRoot(){ return root==this;}
	
	protected void propagateRoot(){
		for(int i=0;i<getChildCount();i++)
			children[i].setRoot(root);
	}
	
	/** Return a Node(_,..._) String */
	public String getTemplate(){
		StringBuffer temp = new StringBuffer(node.toString());
		for (int i=0; i<getChildCount(); i++) {
			if (i==0) temp.append("(");
			if (i>0) temp.append(",");
			temp.append("_");
		}
		if (getChildCount()>0) temp.append(")");
		return temp.toString();
	}

	/** Return a node/arity String */
	public String getFunctorArity(){
		return node+"/"+getChildCount();
	}
	
	// Swing TreeModel methods:
	
	/** @see javax.swing.tree.TreeModel */
	public Object getRoot()	{
		return this;
	}
	/** @see javax.swing.tree.TreeModel */
	public Object getChild(Object parent,int index) {
		return ((TermModel)parent).children[index];
	}
	/** @see javax.swing.tree.TreeModel */
    public int getChildCount(Object parent){
    	TermModel[] c = ((TermModel)parent).children;
    	if (c==null) return 0;
		else return c.length;
    }
	/** @see javax.swing.tree.TreeModel */
    public boolean isLeaf(Object node){
    	return (getChildCount(node) == 0);
    }
	/** @see javax.swing.tree.TreeModel */
    public void valueForPathChanged(TreePath path,Object newValue) {
    	throw new RuntimeException("I can not handle term edition!");
    }   
	/** @see javax.swing.tree.TreeModel */
    public int getIndexOfChild(Object parent,Object child){
    	TermModel[] c = ((TermModel)parent).children;
    	if (c==null) return 0;
    	for (int i=0; i<c.length; i++)
    		if (c[i]==child) return i+1;
    	return 0;
    }
	/** @see javax.swing.tree.TreeModel */
	public void addTreeModelListener(TreeModelListener l){
		if (treeListeners==null) treeListeners = new Vector<TreeModelListener>();
		treeListeners.addElement(l);
	}
	/** @see javax.swing.tree.TreeModel */
	public void removeTreeModelListener(TreeModelListener l){
		if (!treeListeners.removeElement(l)) 
		throw new IPException("Bad removal of TermModel listener");
	}
	
	// simpler versions
	public Object getChild(int index) {
		return getChild(this,index);
	}	
    public int getChildCount(){
		return getChildCount(this);
    }
	public boolean isLeaf(){
    	return isLeaf(this);
    }
	
	/** Start notifying listener l of changes to this term. The term is considered changed iff either a node or children
	is set to a different object; a nonreported change is the setting of a same object as node/children, 
	not the setting of an "equals" object */
	public void addTermModelListener(TermModelListener l){
		if (!isRoot())
			throw new IPException("addTermModelListener can be sent to the TermModel root only");
		if (termListeners==null) termListeners = new Vector<TermModelListener>();
		termListeners.addElement(l);
	}
	public void removeTermModelListener(TermModelListener l){
		if (!termListeners.removeElement(l)) 
		throw new IPException("Bad removal of TermModelListener");
	}
	public void fireTermChanged(){
		hashCodeCache = 0;
		if (isRoot()) {
			if (termListeners!=null)
				for (int l=0;l<termListeners.size();l++)
					termListeners.elementAt(l).termChanged(this);
		} else if (root!=null) root.fireTermChanged();
	}
    // two convenience methods for simple node searching
    
   	public static TreePath findPathForNode(String label,TermModel tree,boolean exactMatch){
		Vector<TermModel> subtrees = new Vector<TermModel>();
		foundPathForNode(label, tree, exactMatch, subtrees);
		if (subtrees.size()==0) return null;
		Object[] path = new Object[subtrees.size()];
		for (int n=0; n<path.length; n++)
			path[n] = subtrees.elementAt(path.length-n-1);
		return new TreePath(path);				
	}
	
	static boolean foundPathForNode(String label,TermModel tree,boolean exactMatch,Vector<TermModel> bag){
		if ((exactMatch?tree.node.equals(label):tree.node.toString().startsWith(label))){
			bag.addElement(tree);
			return true;
		} else{
			for (int c=0;c<tree.getChildCount();c++){
				if (foundPathForNode(label,(TermModel)(tree.getChild(c)),exactMatch,bag)){
					bag.addElement(tree); return true;
				}
			}
			return false;
		}
	}
	
	public TermModel(){}
	
	public TermModel(Object n){this(n,(TermModel[])null);}
	
	public TermModel(Object n,TermModel[] c){
		this(n,c,false);
	}
	
	public TermModel(Object n,boolean isAList){
		this(n,(TermModel[])null,isAList);
	}
	
	public TermModel(Object n,TermModel[] c,boolean isAList){
		children=c;
		node=n; 
		hasListFunctor = isAList;
                
		if ((hasListFunctor) && !node.equals(LIST_FUNCTOR) && !node.equals("[]")) 
		throw new IPException("Inconsistent list functor");
	}
	
	public TermModel(int n){
		this(new Integer(n));
	}
	
	public TermModel(Object n,Vector<TermModel> v){
		node=n; 
		if (v.size()==0) children=null;
		else {
			children = new TermModel[v.size()];
			for(int c=0;c<children.length;c++)
				children[c] = v.elementAt(c);
		}
	}
	
	public void destroy(){
		node = null;
		treeListeners = null; termListeners = null; root = null;
		if (children!=null){
			destroy(children);
			children=null;
		}
	}
	
	public static void destroy(TermModel[] children){
		if (children!=null){
			for (int i=0; i<children.length; i++)
				children[i].destroy();
			children=null;
		}
	}
	
	/** Make a binary (non flat) list */
	public static TermModel makeList(TermModel[] terms){
		if (terms==null || terms.length == 0) return new TermModel(LIST_FUNCTOR,true);
		// now nonrecursive:
		TermModel T = new TermModel(LIST_FUNCTOR,new TermModel[2],true);
		TermModel R = T;
		for (int t=0;t<terms.length;t++){
			T.children[0] = terms[t];
			if (t<terms.length-1) 
				T.children[1] = new TermModel(LIST_FUNCTOR,new TermModel[2],true);
			else 
				T.children[1] = new TermModel("[]",true);
			T = T.children[1];
		}
		return R;
	}
	
	/** Make a binary (non flat) list */
	public static TermModel makeList(ArrayList<TermModel> terms){
		return makeList(new Vector<TermModel>(terms));
	}
	
	public static TermModel makeList(Vector<TermModel> terms){
		if (terms==null || terms.size() == 0) return new TermModel(LIST_FUNCTOR,true);
		// return makeList(0,terms);
		// now nonrecursive:
		TermModel T = new TermModel(LIST_FUNCTOR,new TermModel[2],true);
		TermModel R = T;
		for (int t=0;t<terms.size();t++){
			T.children[0] = terms.elementAt(t);
			if (t<terms.size()-1) 
				T.children[1] = new TermModel(LIST_FUNCTOR,new TermModel[2],true);
			else 
				T.children[1] = new TermModel("[]",true);
			T = T.children[1];
		}
		return R;
	}
	/*
	protected static TermModel makeList(int t, TermModel[] terms){
		TermModel[] cc = new TermModel[2];
		cc[0] = terms[t];
		if (t == terms.length - 1) {
			cc[1] = new TermModel("[]",true);
		} else {
			cc[1] = makeList(t+1,terms);
		}
		return new TermModel(LIST_FUNCTOR,cc,true);
	}
	
	protected static TermModel makeList(int t, Vector terms){
		TermModel[] cc = new TermModel[2];
		cc[0] = (TermModel)terms.elementAt(t);
		if (t == terms.size() - 1) {
			cc[1] = new TermModel("[]",true);
		} else {
			cc[1] = makeList(t+1,terms);
		}
		return new TermModel(LIST_FUNCTOR,cc,true);
	}
	*/
	/** Assuming this is a list of numbers, returns a Vector containing one Integer for each number in the list*/
	public Vector<Integer> makeIntegerVector(){
		TermModel[] elements = flatList(this);
		Vector<Integer> iv = new Vector<Integer>();
		for(int i=0;i<elements.length;i++)
			iv.addElement(new Integer(elements[i].intValue()));
		return iv;
	}
	
	/** Returns node object as an int, assuming it is a Number */
	public int intValue(){
		if (! isLeaf() || !(node instanceof Number))
			throw new RuntimeException("intValue() requires a Number leaf, found:"+node);
		if (isLong())
			throw new RuntimeException("for Long nodes use longValue()");
		return ((Number)node).intValue();
	}
	
	/** Returns node object as a long, assuming it is a Number */
	public long longValue(){
		if (! isLeaf() || !(node instanceof Number))
			throw new RuntimeException("longValue() requires a Number leaf");
		return ((Number)node).longValue();
	}

	/** Returns a close imitation of a Prolog's write, following infix/prefix/postfix operator declarations. However it does NOT 
	add the necessary parenthesis according to precedence declarations; if this is a problem use toString(true) instead */
	public String toString(){
		return toString(defaultOperatorContext,false);
		// return node.toString(); // For a plain JTree...
	}
	public String toString(boolean quoted){
		return toString(defaultOperatorContext,quoted);
	}
	public String toString(PrologOperatorsContext ops){
		return toString(ops,false);
	}
	public static boolean quotesAreNeeded(String atom){
		if (atom.length()==0) return true;
		char first = atom.charAt(0);
		return Character.isUpperCase(first) || first=='_' || first =='$';
	}
	/** quoted true: emulates writeq, and also ignores operator precedences, using prefix notation; 
	also ignores list size limits, writing the full list. For efficiency (and laziness...) more atoms 
	are quoted than necessary; if improvements are needed, inspiration for quotesAreNeeded above 
	is to be found in function quotes_are_needed() in io_builtins_xsb.c*/
	// test for example with ?- buildTermModel(a=(b+c)/d,_TM), javaMessage(_TM,string(S),toString).
	public String toString(PrologOperatorsContext ops, boolean quoted){
		if (getChildCount()==0) {
			String NS = node.toString();
			// if (quoted && isAtom() && NS.length()>0 && !Character.isLowerCase(NS.charAt(0))) return "'"+NS+"'";
			if (quoted && isAtom() /* && quotesAreNeeded(NS)*/) return "'"+doubleQuotes(NS)+"'";
			else return NS;
		} 
		if (isList()) 
			return listToString(ops,quoted);
			
		String NS = nodeToString(ops,quoted);
		int nodePrecedence = precedence(ops);
		String R;
		PrologOperatorsContext.PrologOperator op;
		if (children.length==1){
			op = ops.prefixOperator(node.toString());
			if (op!=null&&!quoted) {
				R = node + " " + parenthesize(children[0].precedence(ops)>nodePrecedence, children[0].toString(ops,quoted));
			} else {
				op = ops.postfixOperator(node.toString());
				if (op!=null&&!quoted) {
					R = parenthesize(children[0].precedence(ops)>nodePrecedence, children[0].toString(ops,quoted)) + " "+ node;
				} else{
					R = NS+"("+children[0].toString(ops,quoted)+")";
				}
			}
		} else {
			op = ops.infixOperator(node.toString());
			if (children.length==2 && op!=null &&!quoted) {
				R = parenthesize(children[0].precedence(ops)>nodePrecedence, children[0].toString(ops,quoted)) + node + 
					parenthesize(children[1].precedence(ops)>nodePrecedence, children[1].toString(ops,quoted));
			} else { // children.lenght>=2 || ...
				StringBuffer s= new StringBuffer(NS+"("+children[0].toString(ops,quoted));
				for (int i=1;i<children.length;i++){
					s.append(","+children[i].toString(ops,quoted));
				}
				R = s+")";
			}		
		}
		return R;
	}
	
	public String toIndentedString(){
		return toIndentedString(0);
	}
	
	static final String INDENT = "    ";
	static String indentation(int level){
		StringBuilder sb = new StringBuilder();
		for (int L=0; L<level; L++)
			sb.append(INDENT);
		return sb.toString();
	}
	
	public String toIndentedString(int level){
		StringBuilder sb = new StringBuilder(indentation(level)+node.toString());
		for (int i=0; i<getChildCount(); i++){
			if (i==0) sb.append("\n");
			sb.append(children[i].toIndentedString(level+1)+"\n");
		}
		return sb.toString();
	}
	
	public static String parenthesize(boolean yes,String S){
		if (yes) return "("+S+")";
		else return S;
	}

    public static String doubleQuotes(String S){
		StringBuffer R = new StringBuffer();
		for (int i=0; i<S.length(); i++) {
			char C = S.charAt(i);
			if (C == '\'') R.append("''");
			else R.append(C);
		}
		return R.toString();
    }
	
	public static String quoteIfFirstUpper(String S){
		if (!Character.isUpperCase(S.charAt(0))) return S;
		else return "'"+doubleQuotes(S)+"'";
	}
	
	private String nodeToString(PrologOperatorsContext ops, boolean quoted){
		String NS = node.toString();
		if (!quoted) return NS;
		if (ops.someOperator(NS)) return "'"+NS+"'";
		if (NS.contains("'") || NS.charAt(0)=='_') return "'"+doubleQuotes(NS)+"'";
		return NS;
	}
	
	public String toString(PrologEngine engine,boolean quoted){
		return toString(engine.getImplementationPeer().getOperators(),quoted);
	}
	
	public static final int listMaxLength=100;
	
	
	public String listToString(PrologOperatorsContext ops,boolean quoted){
		int i;
		StringBuffer s = new StringBuffer("[");
		TermModel temp = this;
		for( i = 0 ; i < (!quoted?listMaxLength:Integer.MAX_VALUE) ; i++ ){
			s.append(temp.children[0].toString(ops,quoted)); // head
			if (temp.children.length<2) break; // hack to deal with some malformed lists
			temp = temp.children[1];
			if (temp.isListEnd()) break;
			if( ! temp.isList() ) break ; // tail is not a list
			s.append(',') ;
		}
		if( (!quoted && i == listMaxLength) )
			s.append("...");
        else if ( ! temp.isListEnd() ) {
			s.append('|') ;
			s.append(temp.toString(ops,quoted)); 
		}
		return s + "]";
	}
	
	public String flatListToString(PrologOperatorsContext ops,boolean quoted){    
	// now assumes flat representation of lists: list with N elements => children.length==N
		int i;
		StringBuffer s = new StringBuffer("[");
		for( i = 0 ; i<
			( !quoted ? (getChildCount()<listMaxLength?getChildCount():listMaxLength) : getChildCount())
			; i++ ){
			s.append(children[i].toString(ops,quoted)); // add element
			if(i>=children.length-1) break ; 
			s.append(',') ;
		}
		if( i < children.length-1 )
			s.append("...");
		return s + "]";
	}
	
	public boolean isListEnd(){
		return (isLeaf() && isList()/*node.equals("[]")*/);
	}
	
	/** May be an empty list */
	public boolean isList(){
		// return (children.length==2 && node.equals(LIST_FUNCTOR));
		return hasListFunctor && (node.equals(LIST_FUNCTOR) || node.equals("[]") || node.equals("[|]"));
	}
	
	public boolean isAtom(){
		return (isLeaf() && node instanceof String);
	}
	
	public boolean isNumber(){
		return (isLeaf() && node instanceof Number);
	}
	
	public boolean isInteger(){
		return (isLeaf() && node instanceof Integer);
	}
	
	public boolean isLong(){
		return (isLeaf() && node instanceof Long);
	}
	
	public boolean isVar(){
		return (isLeaf() && nodeIsVar());
	}
	
	public boolean nodeIsVar(){
		return (node instanceof VariableNode);
	}
	
	/** Flattens this list into a new TermModel array, but not completely: 
	the result may still contain lists */
	public TermModel[] flatList(){
		return flatList(this);
	}
	
	/** Flattens a list into a TermModel array, but not completely: 
	the result may still contain lists */
	public static TermModel[] flatList(TermModel list){
		Vector<TermModel> temp = new Vector<TermModel>();
		flatList(list,temp);
		TermModel[] result = new TermModel[temp.size()];
		for (int i=0;i<result.length;i++)
			result[i] = temp.elementAt(i);
		return result;
	}
	static void flatList(TermModel x,Vector<TermModel> bag){
        while (x != null) {
            if (x.isListEnd()) break; // out of while
		if (x.isList()) {
			bag.addElement(x.children[0]);
                x = x.children[1];
		} else throw new IPException("Not a well formed list:"+x);
	}
    }
	
	public static Hashtable<String,Object> props2Hashtable(TermModel[] terms){
		Hashtable<String,Object> result = new Hashtable<String,Object>();
		for (int t=0;t<terms.length;t++){
			TermModel term = terms[t];
			if (term.isLeaf()) result.put(term.node.toString(),term.node);
			else if ((!term.node.equals("=") || term.getChildCount()!=2)) 
				throw new RuntimeException("bad proplist");
			else result.put(term.children[0].toString(),term.children[1]);
		}
		return result;
	}

	/** If two terms, each with nonrepeated variables, are unifiable, this method returns true. So this is NOT a unification test.
	Doesn't change the terms. */
    public boolean unifies(TermModel term){
		if (term == null) return false;
		if(this.isVar() || term.isVar()) return true;
        if (!(node.equals(term.node))) return false;
        if (getChildCount()!=term.getChildCount()) return false;
        for (int c=0; c<getChildCount(); c++)
            if (!(((TermModel)getChild(c)).unifies((TermModel)term.getChild(c)))) return false;
        return true;
    }
    
    /** Returns an hashCode for the result of toString() */
    public int hashCode(){
    	if (hashCodeCache==0){
    		//hashCodeCache = toString().hashCode();
    		StringBuilder SB = new StringBuilder(node.toString() + getChildCount());
    		for (int c=0; c<getChildCount(); c++){
    			TermModel C = (TermModel)getChild(c);
    			String nodeS = (C.node instanceof VariableNode?"var":C.node.toString());
    			SB.append(nodeS+C.getChildCount());
    		}
    		hashCodeCache = SB.toString().hashCode();
    	}
    	return hashCodeCache;
    }
    
    public int precedence(PrologOperatorsContext context){
    	if (!(node instanceof String)) return 0;
    	String snode = (String)node;
    	int P = 0;
    	int nChildren = getChildCount();
    	if (nChildren==0) 
    		P=0;
    	else if (nChildren==1){
    		P = context.prefixPrecedence(snode);
    		if (P==0) P = context.postfixPrecedence(snode);
    	} else if (nChildren==2){
    		P = context.infixPrecedence(snode);
    	}
    	return P;
    }
}


