package ch.ethz.inf.vs.android.lukasbi.lamport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.hardware.Sensor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ChatAdapter extends BaseAdapter {
	
	private LayoutInflater mInflater;
	private List<JSONObject> chat_items;
	
	public ChatAdapter (Context context) {
		//Cache a reference to avoid looking it up on every getView() call
		this.mInflater = LayoutInflater.from(context);
		this.chat_items = new ArrayList<JSONObject>();
	}
	
	@Override
	public View getView (int position, View convertView, ViewGroup parent) {
		//If there's no recycled view, inflate one and tag each of the views
        //you'll want to modify later
        if (convertView == null) {
            convertView = mInflater.inflate (R.layout.list_item, parent, false);

            //This assumes layout/row_left.xml includes a TextView with an id of "textview"
            convertView.setTag (R.id.list_item_text, convertView.findViewById(R.id.list_item_text));
        }

        //Retrieve the tagged view, get the item for that position, and
        //update the text
        String text = null;
        try {
        	text = chat_items.get(position).getString("text");
        } catch (JSONException e) {
        	text = "Oh no no, bad exception!";
        }
        TextView labelView = (TextView) convertView.getTag(R.id.list_item_text);
        String textItem = text;
        labelView.setText(textItem);
        
        return convertView;
	}
	
	public void add_msg(JSONObject msg) {
		this.chat_items.add(msg);
		Collections.sort(chat_items, new LamportComparator());
	}
	
	@Override
	public int getCount() {
		return chat_items.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public class LamportComparator implements Comparator<JSONObject> {
		@Override
	    public int compare(JSONObject o1, JSONObject o2) {
			int lamport1, lamport2;
			try {
				lamport1 = o1.getInt("lamport");
				lamport2 = o2.getInt("lamport");
				return lamport1 > lamport2 ? +1 : lamport1 < lamport2 ? -1 : 0;
			} catch (JSONException e) {
				// Shit happens...
				e.printStackTrace();
				return 0;
			}
	    }
	}
}
