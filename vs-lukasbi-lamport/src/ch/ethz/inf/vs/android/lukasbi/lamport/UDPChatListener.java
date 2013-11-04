package ch.ethz.inf.vs.android.lukasbi.lamport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPChatListener implements Runnable {
	
	/**
	 * listener settings
	 */
	private volatile boolean running = false;
	private int clientPort;
	private final int timeout = 5000;
	
	public UDPChatListener (int clientPort) {
		this.clientPort = clientPort;
	}

	@Override
	public void run() {
		running = true;
		while (running) {
			DatagramSocket socket = null;
			
			try {
				// connect to the server
				socket = new DatagramSocket(clientPort);
				socket.setSoTimeout(timeout);
			
				// receive response
				byte[] buffer = new byte[1024];
				DatagramPacket response = new DatagramPacket(buffer, buffer.length);
				socket.receive(response);
			} catch (SocketException e) {
				// socket failure
				e.printStackTrace();
			} catch (UnknownHostException e) {
				// host does not exists
				e.printStackTrace();
			} catch (IOException e) {
				// server not responding
				e.printStackTrace();
			} finally {
				/**
				 * close socket here in the finally clause
				 * this ensures, that the socket is closed always, even
				 * if an exception is thrown.
				 */
				if (socket != null) {
					socket.close();
				}
			}
		}
	}

	/**
	 * getters and setters
	 */
	public boolean isRunning() { return running; }
	public void setRunning(boolean running) { this.running = running; }
}
