package ch.ethz.inf.vs.android.lukasbi.lamport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class UDPChatListener implements Runnable {
	
	/**
	 * listener settings
	 */
	
	private DatagramSocket socket = null;
	private final String DEBUG_TAG = "A3";
	private Handler handler;
	private String server;
	private int server_port;
	private int port;
	
	public UDPChatListener (String mserver, int mserverPort, int mport, Handler handler) {
		this.handler = handler;
		this.server = mserver;
		this.port = mport;
		this.server_port = mserverPort;
	}

	@Override
	public void run() {
		Log.d(DEBUG_TAG, "UDPChatListener started ...");
		
		try {
			SocketAddress inetAddr = new InetSocketAddress(port);
			socket = new DatagramSocket(null);
			socket.setReuseAddress(true);
			socket.bind(inetAddr);

			// loop for incoming messages
			while (!Thread.interrupted()) {
				Log.d("loop", "waiting for packet");
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
}
