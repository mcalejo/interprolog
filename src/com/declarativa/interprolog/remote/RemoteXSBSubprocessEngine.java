/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.remote;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.declarativa.interprolog.PrologImplementationPeer;
import com.declarativa.interprolog.SubprocessEngine;
import com.declarativa.interprolog.XSBPeer;
import com.declarativa.interprolog.util.IPException;

public class RemoteXSBSubprocessEngine extends SubprocessEngine{
	String hostname, user;
	/** Used only for PrologServer engines */
	String clienthostaddress=null;
	int port = -1;
	boolean windowsServer;
	protected RemoteProcessProxy remoteProxy=null;
	
	public RemoteXSBSubprocessEngine(String hostname,String user,String xsbPath,String interprologPath,boolean windowsServer){
		this(hostname, user, xsbPath, interprologPath, windowsServer, false);
	}
	/** Assumes ssh connection exists preconfigured to this host, with signatures in place, 
	and XSB Prolog in there with the interprolog prolog file */
	public RemoteXSBSubprocessEngine(String hostname,String user,String xsbPath,String interprologPath,boolean windowsServer,boolean debug){
		super(new String[]{xsbPath},true,debug,false);
		localEngine = false;
		if (windowsServer) nl = "\r\n"; 
		else nl = "\n";
		this.hostname=hostname; this.user=user; this.windowsServer=windowsServer; this.interprologPath=interprologPath;
    	initSubprocess(null);
	}
	public RemoteXSBSubprocessEngine(String hostname,int port){
		this(hostname, port,false);
	}
	/** assumes that hostname has interprolog.jar, and PrologServer.main() executing */
	public RemoteXSBSubprocessEngine(String hostname,int port,boolean debug){
		super(new String[]{"someDummyPath/bin/xsb"},true,debug,false);
		localEngine = false;
		Socket s = null; this.port=port;
		try{
			s = new Socket(hostname,port);
			OutputStream os = s.getOutputStream();
			progressMessage("Sending passPhrase");
			os.write(PrologServer.passPhrase);
			DataOutputStream dos = new DataOutputStream(os);
			dos.writeUTF(PrologServer.createEngineCommand); dos.flush();
			InputStream is = s.getInputStream();
			DataInputStream dis = new DataInputStream(is);
			progressMessage("reading windowsServer");
			windowsServer = dis.readBoolean();
			progressMessage("reading interprologPath");
			interprologPath = dis.readUTF();
			progressMessage("...got "+interprologPath);
			if (!windowsServer) intSocket = new Socket(hostname,dis.readInt());
			clienthostaddress = s.getLocalAddress().getHostAddress();
			/*
			System.out.println("clienthostaddress:"+clienthostaddress);
			System.out.println("s.getInetAddress() :"+s.getInetAddress() );
			System.out.println("s.getRemoteSocketAddress() :"+s.getRemoteSocketAddress() );
			System.out.println("s.getLocalSocketAddress() :"+s.getLocalSocketAddress() );

			for (Enumeration<NetworkInterface> e1 = NetworkInterface.getNetworkInterfaces(); e1.hasMoreElements(); ){
				NetworkInterface NI = e1.nextElement();
				System.out.println(NI);
				for (Enumeration<InetAddress> e2 = NI.getInetAddresses(); e2.hasMoreElements(); ){
					InetAddress ia = e2.nextElement();
					System.out.println("   "+ia);
				}
			}
			*/
			
		} catch (IOException e){
			throw new IPException("Could not create RemoteXSBSubprocessEngine:"+e);
		}
		remoteProxy = new RemoteProcessProxy(s);
		if (windowsServer) nl = "\r\n"; 
		else nl = "\n";
		this.hostname=hostname; this.user=null; 
		mustUseSocketInterrupt = true;
    	initSubprocess(null);
    	
	}
	
	static class RemoteProcessProxy extends Process{
		Socket socket;
		RemoteProcessProxy(Socket s){
			socket=s;
		}
		public void destroy(){
			try{socket.close();} catch(IOException e){System.err.println("Problem closing Prolog proxy:"+e);}
		}
		public int exitValue(){
			throw new IPException("Not supported");
			// might be improved by having the server side send some message before exiting...
		}
		public InputStream getInputStream(){ 
			try{ return socket.getInputStream();}
			catch(IOException e){throw new IPException("unexpected :"+e);}
		}
		/** stderr will be merged with stdout */
		public InputStream getErrorStream(){ return null;}
		public OutputStream getOutputStream(){
			try{ return socket.getOutputStream();}
			catch(IOException e){throw new IPException("unexpected :"+e);}
		}
		/** This implementation assumes the remote process died peacefully if the socket is not connected. And it polls it every 100mS. */
		public int waitFor() throws InterruptedException{
			try{
				while(socket.isConnected())
					Thread.sleep(100);
			} catch(Exception e){
				System.err.println("Unexpected exception waiting for remote Prolog dead:"+e);
				return 1;
			}
			return 0;
		}
	}
	
	protected Process createProcess(String[] prologCommands) throws IOException{
		if (usesRemoteInterPrologServer()) return remoteProxy;
		else { // let's initiate a ssh session, alive during the live of this engine
			if (prologCommands.length!=1)
				throw new IPException("Remote engines require a simple Prolog start command, without args");
			String theCommand;
			if (windowsServer) theCommand = "ssh -T "+ user +"@"+hostname /*+" '"+prologCommands[0]+"'"*/ ;
			else theCommand = "ssh -T "+ user +"@"+hostname+" '"+prologCommands[0]+"'";
			progressMessage("Launching subprocess "+theCommand);
			System.out.print("Executing "+theCommand+"...");
			Process R = Runtime.getRuntime().exec(theCommand);
			System.out.println("created "+R); // this print seems critical!!!
			return R;
        }
    }
    
    /** Used to hack our way into a proper Windows freeSSHd session, without hanging */
    protected void postCreateHack(String[] prologCommands){
    	if (windowsServer && !usesRemoteInterPrologServer()){
    		try{Thread.sleep(1000);} catch(Exception e){throw new IPException("Weird:"+e);}
    		sendAndFlushLn(prologCommands[0]);
    	}
    }
    
    public boolean usesRemoteInterPrologServer(){
    	return remoteProxy!=null;
    }
    
	protected void loadInitialFiles(){
		if (usesRemoteInterPrologServer()) {
			// prologStdin.print(nl); prologStdin.flush();
			try{Thread.sleep(1000);}catch(InterruptedException e){}
			command("writeln(hello)");
			return;
		}
        progressMessage("Loading InterProlog initial file...");
        consultAbsolute(interprologPath);
        waitUntilAvailable();
	}
	
	protected String clientHostname(){
		if (clienthostaddress!=null) return clienthostaddress;
		String hostname = "???";
		try{hostname=InetAddress.getLocalHost().getHostAddress();}
		catch(UnknownHostException e){throw new IPException("Could not find localhost address:"+e);}
		return hostname;
	}
	
    protected PrologImplementationPeer makeImplementationPeer(){
    	return new XSBPeer(this);
    }
    
    // TODO: PrologServer based interrupts missing
	protected String unixInterruptCommand(String PID){
		return "ssh -T "+ user +"@"+hostname+" '/bin/kill -s INT "+PID+"'\n";
	}

	public boolean serverIsWindows(){
		return windowsServer;
	}
	
	public char serverFileSeparatorChar(){
		if (serverIsWindows()) return '\\';
		else return '/';
	}	
	
	public boolean isPrologServerBased(){
		return port != -1;
	}
}