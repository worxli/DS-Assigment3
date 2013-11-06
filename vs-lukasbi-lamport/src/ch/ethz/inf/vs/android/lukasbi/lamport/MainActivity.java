package ch.ethz.inf.vs.android.lukasbi.lamport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	/**
	 * User settings
	 */
	private String nickname = null;
	private int clientPort = 54752;
	public final String DEBUG_TAG = "A3";
	public int id = -1;
	public int lamportTime = -1;
	
	/**
	 * Server settings
	 */
	private final String serverAddress = "vslab.inf.ethz.ch";
	private final int serverPort = 5000;
	private final int timeout = 3000;
	
	/**
	 * UI elements
	 */
	private Spinner name;
	private EditText message;
	private Button register, send;
	//private TextView chat;
	private ListView chat;
	
	ChatAdapter chat_adapter;
	
	/**
	 * Handler for retrieving chat messages from the UDPChatListener thread
	 */
	UDPChatListener udpListener = null;
	Thread chatListener = null;
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String message = msg.getData().getString("message");

			try {
				JSONObject response = new JSONObject(message);
				if(response.has("text")){
					if(response.has("lamport")){
						int lamport = response.getInt("lamport");
						lamportTime = Math.max(lamport,lamportTime);
						appendChat(response);
					} else {
						// Display as toast
						makeToast(response.get("text").toString());
					}
					
				}
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
		}
	};
	
	/**
	 * Worker that register to the server and sets up the listener thread
	 * @author Nico
	 *
	 */
	private class UDPChatWorker extends AsyncTask<String, Void, String> {
		int localport;
		
		@Override
		protected String doInBackground(String... params) {
			String result = null;
			DatagramSocket socket = null;
			
			
			DatagramChannel channel = null;
			SocketAddress myaddress = new InetSocketAddress(clientPort);
			
			
			try {
				// connect to the server
				SocketAddress inetAddr = new InetSocketAddress(clientPort);
				socket = new DatagramSocket(null);
				socket.setReuseAddress(true);
				socket.bind(inetAddr);

				
				socket.setSoTimeout(timeout);
				localport = socket.getLocalPort();
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
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				/**
				 * close socket here in the finally clause
				 * this ensures, that the socket is closed always, even
				 * if an exception is thrown.
				 */
				if (socket != null)
					socket.close();
			}
			
			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
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
		//chat = (TextView) findViewById(R.id.txt_chat);
		chat = (ListView) findViewById(R.id.chat_list);
		
		chat_adapter = new ChatAdapter(getBaseContext());
        chat.setAdapter(chat_adapter);
		
		// fill spinner with data (nicknames)
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.nicknames, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		name.setAdapter(adapter);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregister();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.get_info:
	            get_info();
	            return true;
	        case R.id.get_clients:
	            get_clients();
	            return true;
	        case R.id.unreg:
	            unregister();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	/**
	 * UI listeners
	 * @throws UnknownHostException 
	 */
	public void onRegisterClick (View v) {
			
			if (register.getText().toString().equals(getString(R.string.register))) {
				// register mode
				register();
			} else {
				// unregister mode
				unregister();
			}
	}
	
	/**
	 * internal functions
	 */
	private void unregister () {
		// deregister
		try {
			if(chatListener!=null){
				chatListener.interrupt();
			}
			
			UDPChatWorker worker = new UDPChatWorker();
			JSONObject deregisterRequest = new JSONObject();
			deregisterRequest.put("cmd", "deregister");
			worker.execute(deregisterRequest.toString());
			
			// we have to wait for the response (attention: this is blocking...)
			String response = worker.get(timeout, TimeUnit.MILLISECONDS);
			
			//JSONObject jsonResponse = new JSONObject(response);
			
			register.setText(R.string.register);
			name.setEnabled(true);
			
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void register () {
		
		UDPChatWorker worker = new UDPChatWorker();
		
		try {
			nickname = name.getSelectedItem().toString();
			
			// Set our own port based on selected nickname so we can actually login with multiple different users
			clientPort = clientPort + name.getSelectedItemPosition();
			
			// register to the server
			JSONObject registerRequest = new JSONObject();
		
			registerRequest.put("cmd", "register");
			registerRequest.put("user", nickname);
			//Log.d(DEBUG_TAG, registerRequest.toString());
			worker.execute(registerRequest.toString());

			// we have to wait for the response (attention: this is blocking...)
			String response = worker.get(timeout, TimeUnit.MILLISECONDS);
					
			// there exists already a user with this name?
			JSONObject jsonResponse = new JSONObject(response);
			if (hasError(jsonResponse)) {
				// error occured
				Log.d(DEBUG_TAG, "Error registering: "+jsonResponse.toString());
				makeToast(jsonResponse.getString("error"));
			} else {
				Log.d(DEBUG_TAG, "Login: "+jsonResponse.toString());
				makeToast("Logged in!");
				
				// hide things ...
				register.setText(R.string.unregister);
				name.setEnabled(false);
				
				// get user id
				id = jsonResponse.getInt("index");
				
				// get initial lamport timestamp
				lamportTime = jsonResponse.getInt("init_lamport");
				
				// start chat listener
				chatListener = new Thread(new UDPChatListener(serverAddress, serverPort, clientPort, handler));
				chatListener.start();
				
				send.setEnabled(true);
				message.setEnabled(true);
				
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void get_info() {
		try {
			UDPChatWorker worker = new UDPChatWorker();
			JSONObject deregisterRequest = new JSONObject();
			deregisterRequest.put("cmd", "info");
			worker.execute(deregisterRequest.toString());
			
			// we have to wait for the response (attention: this is blocking...)
			String response = worker.get(timeout, TimeUnit.MILLISECONDS);
			
			// there exists already a user with this name?
			JSONObject jsonResponse = new JSONObject(response);
			
			makeToast(jsonResponse.getString("info"));
		} catch(Exception e) {
			
		}
	}
	
	public void get_clients() {
		try {
			UDPChatWorker worker = new UDPChatWorker();
			JSONObject deregisterRequest = new JSONObject();
			deregisterRequest.put("cmd", "get_clients");
			worker.execute(deregisterRequest.toString());
			
			// we have to wait for the response (attention: this is blocking...)
			String response = worker.get(timeout, TimeUnit.MILLISECONDS);
			
			// there exists already a user with this name?
			JSONObject jsonResponse = new JSONObject(response);
			
			makeToast(jsonResponse.getString("clients"));
		} catch(Exception e) {
			
		}
	}
	
	public void send(View v) {
		String text = message.getText().toString();
		
		//lamport timestamp
		lamportTime = lamportTime+1;
				
		message.setText("");
		
		UDPChatWorker worker = new UDPChatWorker();
		
		try {
			JSONObject sendMsg = new JSONObject();
		
			sendMsg.put("cmd", "message");
			sendMsg.put("text", text);
			sendMsg.put("lamport", lamportTime);
			worker.execute(sendMsg.toString());
			
			// we have to wait for the response (attention: this is blocking...)
			String response = worker.get(timeout, TimeUnit.MILLISECONDS);
			
			appendChat(sendMsg);
					
			// there exists already a user with this name?
			JSONObject jsonResponse = new JSONObject(response);
			if (hasError(jsonResponse)) {
				// error occured
				Log.d(DEBUG_TAG, "Error sending message: "+jsonResponse.toString());
				makeToast(jsonResponse.getString("error"));
			} else {
				Log.d(DEBUG_TAG, "SUCCESS sending: "+jsonResponse.toString());
				//makeToast(jsonResponse.getString("success"));				
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void appendChat (JSONObject msg) {
		String lineSep = System.getProperty("line.separator");
		
		if(msg!=null){
			
			chat_adapter.add_msg(msg);
			chat_adapter.notifyDataSetChanged();
			
			//TODO: don't block on sinlge message
			//if(isDeliverable(text,lamport)){
				//chat_items.add(text);
				//chat_adapter.notifyDataSetChanged();
			//}
			
		}
	}
	
	/*private boolean isDeliverable(String text, int lamport) {
		// TODO Auto-generated method stub
		return true;
	}*/

	public void makeToast (String text) {
		Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
	}
	
	public boolean hasError (JSONObject obj) {
		return obj.has("error");
	}
	
	public String getMessageByTag (String tag) {
		String value = null;
		if (tag.equals("not_registered")) {
			value = getString(R.string.not_registered);
		} else if (tag.equals("already_registered")) {
			value = getString(R.string.server_timeout);
		}
		return value;
	}
}
