package ch.ethz.inf.vs.android.lukasbi.lamport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class UDPChatListener implements Runnable {
	
	/**
	 * listener settings
	 */
	private volatile boolean running = false;
	private DatagramSocket socket = null;
	private final String DEBUG_TAG = "A3";
	private Handler handler;
	
	public UDPChatListener (String server, int port, Handler handler) throws SocketException {
		this.handler = handler;
		socket = new DatagramSocket(null);
		socket.setReuseAddress(true);
		socket.bind(new InetSocketAddress(server, port));
	}

	@Override
	public void run() {
		running = true;
		Log.d(DEBUG_TAG, "UDPChatListener started ...");
		
		try {
			// loop for incoming messages
			while (running) {
				// receive response
				byte[] buffer = new byte[1024];
				DatagramPacket response = new DatagramPacket(buffer, buffer.length);
				socket.receive(response);
				
				// pass it to the handler which sends it to the UI thread
				Message message = handler.obtainMessage();
				Bundle bundle = new Bundle();
				bundle.putString("message", new String(buffer, 0, buffer.length));
				message.setData(bundle);
				handler.sendMessage(message);
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			Log.d(DEBUG_TAG, "UDPChatListener stopped ...");
			/**
			 * close socket here in the finally clause
			 * this ensures, that the socket is closed always, even
			 * if an exception is thrown.
			 */
			if (socket != null)
				socket.close();
		}
	}
	
	/**
	 * getters and setters
	 */
	public boolean isRunning() { return running; }
	public void setRunning(boolean running) { this.running = running; }
	public void stop () { this.running = false; };
}
