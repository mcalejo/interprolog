package com.declarativa.interprolog.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/** A fictional file filter to exemplify how it can be called from the logic side in a number of ways.
Copy the following to a Prolog file and consult it:
<PRE>
:- import java/2, java/3 from interprolog.

call_static_method :-
	File1 = '/Users/mc/git/fidji/interprologForJDK/src/com/declarativa/interprolog/xsb/interprolog.P',
	File2 = '/Users/mc/lixo.txt',
	java('com.declarativa.interprolog.examples.FileProcessor',main(array(string,[File1,File2]))).

call_instance_method :- 
	File1 = '/Users/mc/git/fidji/interprologForJDK/src/com/declarativa/interprolog/xsb/interprolog.P',
	File2 = '/Users/mc/lixo4.txt',
	% Construct a FileProcessor object, getting an object reference back.
	% To specify a java String object to fit in those parameters, we use the term string(some_atom):
	java('com.declarativa.interprolog.examples.FileProcessor',FP,'FileProcessor'(string(File1),string(File2))),
	% Optionally we could:
	%  assert(myFP(FP)),
	% ... perhaps do other stuff... and later retrieve the Java object reference:
	% myFP(FP),
	
	java(FP,int(NLines),copyAndCountLines),
	writeln(NLines).
</PRE>
 *  */
public class FileProcessor {
	File input, output;
	public FileProcessor(String inputFileName, String outputFileName){
		this.input = new File(inputFileName);
		this.output = new File(outputFileName);
		if (this.output.exists())
			throw new RuntimeException("I refuse to overwrite "+output+"!");
	}
	
	public int copyAndCountLines() throws IOException{
		int nLines = 0;
		BufferedReader bf = new BufferedReader(new FileReader(input));
		FileWriter writer = new FileWriter(output);
		String line = null;
		while ((line = bf.readLine())!=null){
			nLines ++;
			writer.write(line);
		}
		writer.close(); bf.close();
		return nLines;
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length!=2) {
			System.err.println("Please invoke with arguments InputFile, OutputFile!");
			System.exit(1);
		}
		FileProcessor FP = new FileProcessor(args[0],args[1]);
		System.out.println("Copied file with "+FP.copyAndCountLines()+" lines.");
	}
}
