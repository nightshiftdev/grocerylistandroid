package com.etcapps.grocerylist;


import java.text.DateFormat;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.etcapps.grocerylist.engine.GroceryListProvider;
import com.etcapps.grocerylist.engine.GroceryListUtils;
import com.etcapps.grocerylist.R;

public class GroceryListAdapter extends CursorAdapter {
	
	public GroceryListAdapter(Context context) {
        super(context, null, true);
	}

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
    	int iconResourceId = cursor.getInt(GroceryListProvider.GROCERY_LIST_RES_ICON_ID_COLUMN);
    	if (iconResourceId == GroceryListUtils.UNKNOWN_ID) {
    		iconResourceId = R.drawable.default_grocery_list_icon;
    	}
    	ImageView listIconView = (ImageView)view.findViewById(R.id.grocery_list_icon);
    	listIconView.setImageResource(iconResourceId);
    	
		TextView listNameView = (TextView)view.findViewById(R.id.grocery_list_name);
		listNameView.setText(cursor.getString(GroceryListProvider.GROCERY_LIST_NAME_COLUMN));
		
		TextView dateModifiedView = (TextView)view.findViewById(R.id.grocery_list_date_modified);
		Date dateModified = new Date(cursor.getLong(GroceryListProvider.GROCERY_LIST_DATE_COLUMN));
		DateFormat dateFormat = DateFormat.getDateTimeInstance();
		dateModifiedView.setText(dateFormat.format(dateModified));
    }
    
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.glists_item, parent, false);
    }
}
