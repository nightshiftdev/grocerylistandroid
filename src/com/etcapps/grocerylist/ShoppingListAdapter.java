package com.etcapps.grocerylist;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.etcapps.grocerylist.engine.GroceryListEngine;
import com.etcapps.grocerylist.engine.GroceryListProvider;
import com.etcapps.grocerylist.engine.Icon;
import com.etcapps.grocerylist.engine.Product;
import com.etcapps.grocerylist.engine.ShoppingListItem;

public class ShoppingListAdapter extends CursorAdapter {
	
	private GroceryListEngine mEngine;
	private long mGroceryListId;
	
	public ShoppingListAdapter(Context context, GroceryListEngine engine, long groceryListId) {
        super(context, null, true);
        mEngine = engine;
        mGroceryListId = groceryListId;
	}

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
    	long productId = cursor.getLong(GroceryListProvider.SHOPPING_LIST_ITEM_PRODUCT_ID_COLUMN);
    	Product product = mEngine.getProduct(productId);

    	String productNameText = product.getName();
    	productNameText.trim();
    	String resourceName = productNameText.replace(" ", "_");
    	int productIconResId = context.getResources().getIdentifier("com.etcapps.grocerylist:drawable/" + resourceName, null, null);
    	RoundedImageView iconView = (RoundedImageView)view.findViewById(R.id.shopping_product_icon);
    	if (product.getIsUserCreated() == 1) {
    		productIconResId = product.getResIconId();
    		if (productIconResId <= 0 || productIconResId == R.drawable.default_product_icon) {
    			iconView.setImageResource(R.drawable.default_product_icon);
    		} else {
    			Icon icon = mEngine.getIcon(productIconResId);
    			if (icon != null && icon.getBitmap() != null) {
    				iconView.setImageBitmap(icon.getBitmap());
    			} else {
    				iconView.setImageResource(R.drawable.default_product_icon);
    			}
    		}
    	} else {
    		if (productIconResId == 0) {
    			productIconResId = R.drawable.default_product_icon;
    		}
    		iconView.setImageResource(productIconResId);
    	}

    	TextView shoppingItemNameView = (TextView)view.findViewById(R.id.shopping_list_item_name);
    	shoppingItemNameView.setText(product.getName());

    	shoppingItemNameView.setText(product.getName());
    	TextView shoppingItemCountView = (TextView)view.findViewById(R.id.shopping_list_item_count);
    	ShoppingListItem shoppingListItem =  mEngine.getShoppingListItem(product.getId(), 
    			mGroceryListId);
    	shoppingItemCountView.setText("" + shoppingListItem.getCount());

    	if (shoppingListItem.getIsPurchased()) {
    		shoppingItemNameView.setPaintFlags((shoppingItemNameView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG));
    		shoppingItemNameView.setTextColor(Color.GRAY);
    	} else {
    		shoppingItemNameView.setPaintFlags((shoppingItemNameView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG));
    		shoppingItemNameView.setTextColor(Color.WHITE);
    	}
    }
    
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.shopping_list_item, null);
    }
}
