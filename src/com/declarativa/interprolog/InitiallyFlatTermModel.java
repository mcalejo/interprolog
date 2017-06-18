/*
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import com.declarativa.interprolog.util.IPException;
import com.declarativa.interprolog.util.MyStreamTokenizer;
import com.declarativa.interprolog.util.VariableNode;
/** A TermModel specialization so serialization is done faster, based on string representations of the term. On the Prolog side
a specification for this can be obtained with buildInitiallyFlatTermModel(T,M) */
public class InitiallyFlatTermModel extends TermModel{
	private static final long serialVersionUID = 6354585189997233327L;
	private String canonicalTerm;
	private transient boolean stillFlat;
	private transient MyStreamTokenizer ST;
	private transient HashMap<String,Integer> variables; /** Variable names and their numbers - order of the first occurrence */
		
	public InitiallyFlatTermModel(Object n){
		super(n);
	}
	public InitiallyFlatTermModel(){ 
		super(); 
	}
	public InitiallyFlatTermModel(Object n,TermModel[] c,boolean isAList){
		super(n,c,isAList);
	}
	/** Get a TermModel array previously written by Prolog with ipPutTermList(L,FilePath) */
	public static TermModel[] getTermList(File F){
		//long T0 = System.currentTimeMillis();
		//long T1 = 0;
		InitiallyFlatTermModel IFTM = new InitiallyFlatTermModel(".",null,true);
		long Fsize = F.length();
		if (Fsize>Integer.MAX_VALUE)
			throw new IPException("Outrageous term size");
		if (Fsize==0)
			throw new IPException("There is no term to get");
		try{
			byte[] buffer = new byte[(int)Fsize];
			InputStream is = new FileInputStream(F); // Unicode / charset encoding issues here...and below
			is.read(buffer,0,buffer.length);
			is.close();
			
			IFTM.stillFlat=true;
			
			//T1=System.currentTimeMillis();
			IFTM.canonicalTerm = " reading term from "+F;
			// ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
			StringReader sr = new StringReader(escapeDoubledQuotes(new String(buffer))); 
			
			IFTM.inflateListFromStream(sr,(int)Fsize);
			sr.close(); buffer=null;
			//System.out.println("getTermList:"+ (System.currentTimeMillis()-T0));
			//System.out.println("inflate only:"+ (System.currentTimeMillis()-T1));
			return IFTM.children;
		} catch (IOException ex){
			throw new IPException("Could not getTermList:"+ex);
		}		
	}
	
	private void inflateListFromStream(Reader is,int size) throws IOException{
		ST = new MyStreamTokenizer(is);
		prepareForInflate();
				
		int NT = parseFlatList(ST.nextToken(),this,size);
		if (NT!=StreamTokenizer.TT_EOF) 
		throw new IPException("Extra garbage after "+NT+"in "+is);
		inflateWrapup();
	}
	
	private void prepareForInflate(){
		//....parse and build tree... 
		// Term=2+X+[1,2,3,4]+'Y'+zz3+'_bad',browseTerm(Term), buildInitiallyFlatTermModel(Term,_M) ,ipPrologEngine(_E),ipObjectSpec('IPClassObject',Class,['com.declarativa.interprolog.gui.TermModelWindow'],_),javaMessage(Class,_,null,'TermModelWindow',[_M,_E],0,_).
		// System.out.println("canonicalTerm:"+canonicalTerm);
		variables = new HashMap<String,Integer>();
		ST.wordChars(95,95); // "_" can be in words
		ST.wordChars(36,36); // "$" can be in words
	}
	
	private void inflateWrapup(){
		stillFlat=false;
		canonicalTerm=null;
	}
	
	/** build this term's tree (children, node and hasListFunctor fields) by parsing its string representation in canonicalTerm  */
	private void inflateFromString() throws IOException{
		if (!stillFlat) return;
		if (canonicalTerm==null) throw new IPException("Inconsistent canonicalTerm");
		ST = new MyStreamTokenizer(new StringReader(escapeDoubledQuotes(canonicalTerm)));
		prepareForInflate();
		int NT = parseTerm(ST.nextToken(),this);
		if (NT!=StreamTokenizer.TT_EOF) 
		throw new IPException("Extra garbage after "+NT+"in "+canonicalTerm);
		inflateWrapup();
	}
	/** transforms quoted doubled quotes - '' - into single quotes represented as an octal escape*/
	static private String escapeDoubledQuotes(String S){
		StringBuffer result = new StringBuffer();
		int L = S.length();
		boolean unquoted=true;
		int i=0;
		try{
			while(i<L){
				if (unquoted){
					if (S.charAt(i)=='\'' && i<L-2 && S.charAt(i+1)=='\'' && S.charAt(i+2)=='\'') {
						unquoted=false;
						result.append(S.charAt(i)); i++;
					} else if (S.charAt(i)=='\'' && i<L-1 && S.charAt(i+1)=='\'') {
						result.append("''"); i=i+2;
					} else {
						if (S.charAt(i)=='\'') unquoted=false;
						result.append(S.charAt(i)); i++;
					} 
				} else {
					if (S.charAt(i)=='\'' && i<L-1 && S.charAt(i+1)=='\'') {
						result.append("\\"); result.append("47"); // doubled quote: ' escape in octal 
						i=i+2;
					} else {
						if (S.charAt(i)=='\'') unquoted=true;
						result.append(S.charAt(i)); i++;
					} 				
				}
			}
		} catch(IndexOutOfBoundsException e){
			throw new IPException("bad quotes in string");
		}
		return result.toString();
	}
	
	@SuppressWarnings("unused")
	/** for testing */
	private void dump(int NT) throws IOException{
		System.out.println("Dumping term tokens. canonicalTerm:"+canonicalTerm);
		while(NT!=StreamTokenizer.TT_EOF){
			//System.out.println("NT="+NT+" Type="+ST.ttype + " string:"+ST.sval+" number:"+ST.nval);
			if (NT==StreamTokenizer.TT_NUMBER){
				System.out.println(ST.nval);
			} else if (NT==StreamTokenizer.TT_WORD && ST.sval.startsWith("_"))
				System.out.println("Variable:"+ST.sval);
			else if (NT==39 && ST.sval.equals(".") ) 
				System.out.println("list operator");
			else if (NT==StreamTokenizer.TT_WORD || NT==39 /* ' */ ) 
				System.out.println("Atom:"+ST.sval);
			else if (NT==91){
				int NT2 = ST.nextToken();
				if (NT2==93) System.out.println("[]");
				else ST.pushBack();
			}
			else if (NT==44 /* , */)
				System.out.println(",");
			else if (NT==41)
				System.out.println(")");
			else if (NT==40)
				System.out.println("(");
			else throw new IPException("Unexpected char in "+canonicalTerm+":"+NT);
			NT = ST.nextToken();
		}
		System.out.println("---end of dump");
	}
	
	private int parseTerm(int NT,TermModel term /* node and children null */) throws IOException{
		boolean consumedSecond = false;	
		if (NT==StreamTokenizer.TT_NUMBER){
			// if ((int)ST.nval == ST.nval) term.node = new Integer((int)ST.nval);
			if (!ST.containsDot()) term.node = new Integer((int)ST.nval);
			else term.node = new Double(ST.nval);
			if (term.node instanceof Double){ // hack to parse exponent
				NT = ST.nextToken(); consumedSecond = true;
				if (NT==StreamTokenizer.TT_WORD && ST.sval.startsWith("E")){
					term.node = Double.valueOf(term.node+ST.sval);
					NT = ST.nextToken(); 
				}
			}
		} else if (NT==StreamTokenizer.TT_WORD && ST.sval.startsWith("_"))
			term.node = new VariableNode(lookupVariable(ST.sval)); 
		else if (NT==StreamTokenizer.TT_WORD || NT==39 /* ' */ ) 
			term.node = ST.sval.intern(); // avoid proliferation of Strings
		else if (NT==39 && ST.sval.equals(".") ) 
			term.node="."; // list functor...
		else if (NT==91){ // [ 
			int NT2 = ST.nextToken();
			if (NT2==93) term.node="[]";
			else { 
				// list starting... this could be refactored with parseFlatList
				term.node="."; term.hasListFunctor=true; 
				ArrayList<TermModel> listItems = new ArrayList<TermModel>();
				NT = parseTermArgs(NT2, listItems);
				if (NT==124 /*|*/) throw new IPException("Open tail lists not admissible here:"+canonicalTerm); 
				// term.children = listItems.toArray(new TermModel[0]);
				term.children = TermModel.makeList(listItems).children;
				if (NT!=93 /*]*/) throw new IPException("Missing ] in "+canonicalTerm);
				return ST.nextToken(); // list can not be a functor for a larger term...
			}
		}		
		else throw new IPException("Unexpected char in "+canonicalTerm+":"+NT);
		if (!consumedSecond) NT = ST.nextToken();
		if (NT==40 /* ( */) {
			ArrayList<TermModel> args = new ArrayList<TermModel>(8); // tuning here...
			NT = parseTermArgs(ST.nextToken(), args);
			term.children = args.toArray(tmPrototype);
			if (NT==41 /* ) */) {
				if (term.node.equals(".")&&term.getChildCount()==2)
					term.hasListFunctor=true;
				return ST.nextToken();
			} else {
				//ST.dump(60);
				throw new IPException("Missing ) in "+canonicalTerm+", found token "+NT+" instead");
			}
		} else {
			if (term.node.equals("[]")) term.hasListFunctor=true;
			return NT;
		}
	}
	static final TermModel[] tmPrototype = new TermModel[0];
	
	private int parseFlatList(int NT,TermModel term,int size) throws IOException{
		if (NT==91){ // [ 
			int NT2 = ST.nextToken();
			if (NT2==93) {
				term.node="[]";
				return ST.nextToken();
			} else { 
				// list starting...
				term.node="."; term.hasListFunctor=true; 
				ArrayList<TermModel> listItems = new ArrayList<TermModel>(size/100); // Tuning here for term size...
				NT = parseTermArgs(NT2, listItems);
				if (NT==124 /*|*/) throw new IPException("Open tail lists not admissible here"); 
				term.children = listItems.toArray(tmPrototype);
				if (NT!=93 /*]*/) throw new IPException("Missing ] in "+canonicalTerm);
				return ST.nextToken(); // list can not be a functor for a larger term...
			}
		} else throw new IPException("Missing [ in "+canonicalTerm); 
	}

	private int parseTermArgs(int NT,ArrayList<TermModel> args) throws IOException{
		TermModel arg = new TermModel(); 
		args.add(arg);
		NT = parseTerm(NT,arg);
		while (NT==44 /* , */) {
			arg = new TermModel(); args.add(arg);
			NT = parseTerm(ST.nextToken(),arg);
		};		
		return NT;
	}

	private int lookupVariable(String varName){
		Integer i = variables.get(varName);
		if (i==null) {
			i = new Integer(variables.size());
			variables.put(varName,i);
		}
		return i.intValue();
	}
	
	/** hack to parse the term and build the TermModel tree after unserializing this object */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{
		in.defaultReadObject(); // why not ... super.readObject...??
		stillFlat=true; // hack
		inflateFromString();
	}

	/** rebuilds field canonicalTerm and nullifies node and children, so that the term looses its tree and
	becomes ready for quicker serialization; unfortunately this method must be invoked explicitly... because 
	writeObject can not do its job (in readObjects fashion, which fortunately dispenses an explicit call to inflate()...): 
	it is messaged by the system after the object graph to be serialized is build... so writeObject
	can NOT nullify node and children temporarily :-( 
	If you wish to keep the original term, reassign
	the term structure with setNode and setChildren AFTER the serialization*/
	public void deflate(){
		if (stillFlat) return;
		if (node!=null) canonicalTerm = toString(true);
		//System.out.println("deflate...:"+canonicalTerm);
		node=null; children=null;
		stillFlat=true;
	}
 	private void writeObject(java.io.ObjectOutputStream out) throws IOException{
		if (!stillFlat) {
			// Somehow the following exception is lost in java.io's handling (?); 
			// This print also gets lost (not flushed perhaps?)
			System.err.println("Crashing: InitiallyFlatTermModel object must receive the message deflate() before being serialized to Prolog");
			System.err.flush();
			throw new IPException("InitiallyFlatTermModel object must receive the message deflate() before being serialized to Prolog");
		}
		out.defaultWriteObject();
 	}

	public static ObjectExamplePair example(){
		//InitiallyFlatTermModel A = new InitiallyFlatTermModel(); 
		InitiallyFlatTermModel A = new InitiallyFlatTermModel(); 
		//A.addChildren(new TermModel[]{new TermModel(1)});
		A.canonicalTerm="a";
		InitiallyFlatTermModel B = new InitiallyFlatTermModel(); 
		//InitiallyFlatTermModel B = new InitiallyFlatTermModel(); 
		//B.addChildren(new TermModel[]{new TermModel(2)});
		B.canonicalTerm="[]"; B.hasListFunctor=true; 
		A.stillFlat=true; // hack to ready this for writeObject above
		B.stillFlat=true;
		return new ObjectExamplePair("InitiallyFlatTermModel",A,B);
	}
	public String toString(){
		if (stillFlat) return "Canonical:("+canonicalTerm+")";
		else return super.toString();
	}
	public boolean isStillFlat(){
		return stillFlat;
	}
	public String getCanonicalTerm(){
		return canonicalTerm;
	}
}