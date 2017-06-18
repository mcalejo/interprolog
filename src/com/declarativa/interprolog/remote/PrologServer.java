/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.remote;
import com.declarativa.interprolog.*;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.ArrayList;
/**
java -classpath ../interprolog.jar com.declarativa.interprolog.remote.PrologServer 0 MyPath/XSB/bin/xsb
*/
public class PrologServer{
	static final byte[] passPhrase = new byte[]{105,110,116,101,114,112,114,111,108,111,103,33}; // "interprolog!";
	static final String createEngineCommand = "create";
	static final String shutdownServerCommand = "shutdown";
	static final int maxClients = 20;
	int nClients=0;
	static String prologCommand=null;
	static String interprologPath=null;
	static boolean windowsServer=false;
	ArrayList<PrologClient> clients = new ArrayList<PrologClient>();
	
	public static void main(String[] args){
		int port=0,interruptPort = 0; // by default, get us some free port
		System.out.println("--InterProlog Prolog server--");
		if (args.length==1)
			prologCommand = args[0];
		else if (args.length==2){
			prologCommand = args[1];
			port = Integer.parseInt(args[0]);
		}
		else if (args.length==3){
			prologCommand = args[2];
			port = Integer.parseInt(args[0]);
			interruptPort = Integer.parseInt(args[1]);
		} else {
			System.err.println("Arguments are: [PortNumber] [InterruptPortNumber] XSBexecutablePath");
			System.exit(1);
		}
		XSBSubprocessEngine dummy = new XSBSubprocessEngine(new String[]{prologCommand});
		windowsServer = XSBSubprocessEngine.isWindowsOS();
		interprologPath = dummy.getInterprologPath();
		dummy.shutdown();
		new PrologServer(port,interruptPort);
	}

	/** Shutdown the PrologServer at the given location. All its engines should have been shutdown() first */
	public static void shutdown(String hostname,int port){
		try{
			Socket s = new Socket(hostname,port);
			OutputStream os = s.getOutputStream();
			os.write(passPhrase);
			DataOutputStream dos = new DataOutputStream(os);
			dos.writeUTF(shutdownServerCommand); dos.flush();
			dos.close();
			s.close();
		} catch (IOException e){
			System.err.println("Problem shutting down PrologServer at "+hostname+"/"+port+":"+e);
		}
	}
	
	@SuppressWarnings("resource")
	PrologServer(int port,int interruptPort){
		ServerSocket ss = null;
		InetAddress localhost=null;
		try{ 
			ss = new ServerSocket(port);
			localhost = InetAddress.getLocalHost();
		} catch(IOException e){System.err.println(e); System.exit(1);}
		System.out.println("Waiting for Prolog clients at port "+ss.getLocalPort()+" of IP "+ss.getInetAddress());
		System.out.println("Local host is "+localhost);
		System.out.println("Each will launch an instance of "+prologCommand);
		System.out.println("InterProlog file:"+interprologPath);
		
		Runtime.getRuntime().addShutdownHook( new Thread(){
			public void run(){
				for (PrologClient client:clients)
					if (client.prolog!=null)
						client.prolog.destroy();
			}
		});

		while(true) { // might have a termination command...
			try{
				if (nClients>=maxClients) Thread.sleep(1000);
				else{
					Socket cs = ss.accept();
					System.out.println("Accepted client "+nClients);
					PrologClient client = new PrologClient(cs,interruptPort);
					clients.add(client);
					client.start();
				}
			} 
			catch (InterruptedException e){System.err.println("Weird: "+e); }
			catch (IOException e){System.err.println("Socket problem: "+e); }
		}
	}
	
	static final int BUFFER_SIZE=2048;
	
	class PrologClient extends Thread{
		Socket cs, interruptClientSocket;
		int interruptPort;
		InputStream sis,pis;
		OutputStream sos, pos;
		Process prolog = null;
		
		PrologClient(Socket cs,int interruptPort){
			this.cs=cs; this.interruptPort=interruptPort;
		}
		public void run(){
			byte[] buffer = new byte[BUFFER_SIZE];
			try{
				nClients++;
				sis = cs.getInputStream();
				byte[] passBuffer = new byte[passPhrase.length];
				System.out.println("reading passPhrase");
				sis.read(passBuffer,0,passBuffer.length);
				if (!Arrays.equals(passBuffer,passPhrase))
					throw new IOException("Bad passPhrase:"+Arrays.toString(passBuffer));
				System.out.println("...got it");
				DataInputStream dis = new DataInputStream(sis);
				String command = dis.readUTF();
				if (!command.equals(createEngineCommand)){
					if (command.equals(shutdownServerCommand)){
						if (nClients>1) 
							System.err.println("Still "+(nClients-1)+" engines on "+PrologServer.this);
						System.exit(0);
					} else System.err.println("Bad PrologServer command:"+command);
					return;
				}
				sos = cs.getOutputStream();
				DataOutputStream dos = new DataOutputStream(sos);
				dos.writeBoolean(windowsServer);
				dos.writeUTF(interprologPath);
				
				if (!windowsServer){ //UNIX server only; Windows XSB Prolog will connect directly to the client engine
					@SuppressWarnings("resource")
					final ServerSocket ctrlcSS = new ServerSocket(interruptPort);
					Runnable interruptHandler = new Runnable(){
						public void run(){
							try{
								interruptClientSocket = ctrlcSS.accept();
								DataInputStream dis = new DataInputStream(interruptClientSocket.getInputStream());
								while(true){
									String interruptCommand = dis.readUTF();
									System.out.println("Got interrupt request:"+interruptCommand);
									Runtime.getRuntime().exec(interruptCommand);
								}
							} catch (EOFException e){
								System.err.println("End of interrupt stream:"+e);
							} catch (IOException e){
								System.err.println("Problem handling interrupts:"+e);
							}
						}
					};
					new Thread(interruptHandler).start();
					dos.writeInt(ctrlcSS.getLocalPort());
				}
				
				ProcessBuilder pb = new ProcessBuilder(prologCommand, "-e", "['"+interprologPath+"'].");
				pb.redirectErrorStream(true); // let's simplify...
				System.out.println("Launching "+prologCommand + " -e \"['"+interprologPath+"'].\"");
				prolog = pb.start();
				pos = prolog.getOutputStream(); pis = prolog.getInputStream();
				Runnable clientHandler = new Runnable(){
					byte[] clientBuffer = new byte[BUFFER_SIZE];
					public void run(){
						try{
							while(true){
								Thread.yield();
								int count = sis.read(clientBuffer);
								if (count==-1) break;
								//byte[] temp = Arrays.copyOfRange(clientBuffer,0,count);
								//System.out.println("READ FROM client: "+Arrays.toString(temp));
								pos.write(clientBuffer,0,count);
								pos.flush();
							}
						} catch (IOException eee){
							System.err.println("clientHandler exception:"+eee);
							System.err.println("killing Prolog subprocess");
							if (prolog!=null) {
								prolog.destroy();
								prolog=null;
							}
						}
					}
				};
				new Thread(clientHandler).start();
				while(true){
					Thread.yield();
					int count = pis.read(buffer);
					if (count==-1) break;
					//byte[] temp = Arrays.copyOfRange(buffer,0,count);
					//System.out.println("READ FROM Prolog: "+Arrays.toString(temp));
					sos.write(buffer,0,count); sos.flush();
				}
			} catch(IOException e){
				if (prolog!=null) {
					prolog.destroy();
					prolog=null;
				}
				try{cs.close();}catch(IOException ee){System.err.println("Attempting to close socket:"+ee);}
				System.err.println("Destroyed subprocess and closed socket:"+e);
				clients.remove(PrologClient.this);
			} finally{ nClients--;}
		}
	}
}