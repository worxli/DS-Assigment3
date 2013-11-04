package ch.ethz.inf.vs.android.lukasbi.lamport;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	/**
	 * Server settings
	 */
	private final String serverAddress = "vslab.inf.ethz.ch";
	private final int serverPort = 5000;
	
	/**
	 * UI elements
	 */
	private EditText name, message;
	private Button register, send;
	private TextView chat;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// reference UI elements
		name = (EditText) findViewById(R.id.ipt_name);
		message = (EditText) findViewById(R.id.ipt_message);
		register = (Button) findViewById(R.id.btn_register);
		send = (Button) findViewById(R.id.btn_send);
		chat = (TextView) findViewById(R.id.txt_chat);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
