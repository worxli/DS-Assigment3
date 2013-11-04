package ch.ethz.inf.vs.android.lukasbi.lamport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	/**
	 * User settings
	 */
	private String nickname = null;
	private int clientPort = 1250;
	public final String DEBUG_TAG = "A3";
	
	/**
	 * Server settings
	 */
	private final String serverAddress = "vslab.inf.ethz.ch";
	private final int serverPort = 5000;
	private final int timeout = 5000;
	
	/**
	 * UI elements
	 */
	private Spinner name;
	private EditText message;
	private Button register, send;
	private TextView chat;
	
	/**
	 * Worker that register to the server and sets the listener thread up
	 * @author Nico
	 *
	 */
	private class UDPChatWorker extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... params) {
			String result = null;
			DatagramSocket socket = null;
			
			try {
				// connect to the server
				socket = new DatagramSocket(clientPort);
				socket.setSoTimeout(timeout);
				InetAddress serverAddr = InetAddress.getByName(serverAddress);
			
				// for each server request
				for (String param : params) {
					DatagramPacket packet = new DatagramPacket(param.getBytes(), param.length(), serverAddr, serverPort);
					
					// send packet
					socket.send(packet);
					
					// receive response
					byte[] buffer = new byte[1024];
					DatagramPacket response = new DatagramPacket(buffer, buffer.length);
					socket.receive(response);
					result = new String(buffer, 0, buffer.length);
				}
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
			
			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			writeChat(result);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// reference UI elements
		name = (Spinner) findViewById(R.id.spnr_name);
		message = (EditText) findViewById(R.id.ipt_message);
		register = (Button) findViewById(R.id.btn_register);
		send = (Button) findViewById(R.id.btn_send);
		chat = (TextView) findViewById(R.id.txt_chat);
		
		// fill spinner with data (nicknames)
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.nicknames, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		name.setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/**
	 * UI listeners
	 */
	public void onRegisterClick (View v) {
		UDPChatWorker worker = new UDPChatWorker();
		try {
			// register mode
			if (register.getText().toString().equals(getString(R.string.register))) {
				nickname = name.getSelectedItem().toString();
				register.setText(R.string.unregister);
				name.setEnabled(false);
				
				// register to the server
				JSONObject registerRequest = new JSONObject();
				registerRequest.put("cmd", "register");
				registerRequest.put("user", nickname);
				Log.d(DEBUG_TAG, registerRequest.toString());
				worker.execute(registerRequest.toString());

				// we have to wait for the response (attention: this is blocking...)
				String jsonResponse = worker.get();
				writeChat(jsonResponse);
				
			} else {
				// unregister mode
				register.setText(R.string.register);
				name.setEnabled(true);
				
				// deregister json object
				JSONObject deregisterRequest = new JSONObject();
				deregisterRequest.put("cmd", "deregister");
				worker.execute(deregisterRequest.toString());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void writeChat (String text) {
		chat.setText(text);
	}
}
