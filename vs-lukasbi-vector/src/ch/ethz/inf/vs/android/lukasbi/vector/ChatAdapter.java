package ch.ethz.inf.vs.android.lukasbi.vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import ch.ethz.inf.vs.android.lukasbi.vector.R;

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
		Collections.sort(chat_items, new VectorClockComparator());
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
	
	public class VectorClockComparator implements Comparator<JSONObject> {
		@Override
		/**
	     * o1 is before o2 iff both conditions are met:
	     * - each process's clock is less-than-or-equal-to its own clock in other; and
	     * - there is at least one process's clock which is strictly less-than its
	     *   own clock in o2
	     */
	    public int compare(JSONObject o1, JSONObject o2) {
			int isBefore = 0;
			
			// Since we are not guaranteed that both objects have the same indexes computer intersection and compare based on that
			HashMap<String, Integer> vector1 = MainActivity.vectorClockFromJSON(o1);
			HashMap<String, Integer> vector2 = MainActivity.vectorClockFromJSON(o2);
			HashMap<String, Integer> intersection = new HashMap<String, Integer>(vector1);
			intersection.keySet().retainAll(vector2.keySet());
			
			for (String key : intersection.keySet()) {
				int val1 = vector1.get(key);
				int val2 = vector2.get(key);
				
				if (val1 > val2)
					return 1;
				else if (val1 < val2)
					isBefore = -1;
				
			}
			
			return isBefore;
		}
	}
}
