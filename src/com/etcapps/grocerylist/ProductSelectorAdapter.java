
package com.etcapps.grocerylist;

import android.content.Context;
import android.database.Cursor;
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

public class ProductSelectorAdapter extends CursorAdapter {
	
	GroceryListEngine mEngine;
	long mGroceryListId;
	
	public ProductSelectorAdapter(Context context, GroceryListEngine engine, long groceryListId) {
        super(context, null, true);
        mEngine = engine;
        mGroceryListId = groceryListId;
	}
	
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
    	Product product = mEngine.getProduct(cursor.getLong(GroceryListProvider.PRODUCT_ID_COLUMN));
    	String productNameText = cursor.getString(GroceryListProvider.PRODUCT_NAME_COLUMN);
    	productNameText.trim();
    	String resourceName = productNameText.replace(" ", "_");
    	RoundedImageView productIconView = (RoundedImageView)view.findViewById(R.id.product_icon);
    	int productIconResId = context.getResources().getIdentifier("com.etcapps.grocerylist:drawable/" + resourceName, null, null);
    	if (product.getIsUserCreated() == 1) {
    		productIconResId = product.getResIconId();
    		if (productIconResId <= 0 || productIconResId == R.drawable.default_product_icon) {
    			productIconView.setImageResource(R.drawable.default_product_icon);
    		} else {
    			Icon icon = mEngine.getIcon(productIconResId);
    			if (icon != null && icon.getBitmap() != null) {
    				productIconView.setImageBitmap(icon.getBitmap());
    			} else {
    				productIconView.setImageResource(R.drawable.default_product_icon);
    			}
    		}
    	} else {
    		if (productIconResId == 0) {
    			productIconResId = R.drawable.default_product_icon;
    		}
    		productIconView.setImageResource(productIconResId);
    	}

    	TextView productName = (TextView)view.findViewById(R.id.product_name);
    	productName.setText(productNameText);

    	ShoppingListItem shoppingListItem = mEngine.getShoppingListItem(cursor.getLong(GroceryListProvider.PRODUCT_ID_COLUMN), mGroceryListId);
    	int count = 0;
    	if (shoppingListItem != null) {
    		count = shoppingListItem.getCount();
    	}
    	TextView productCount = (TextView)view.findViewById(R.id.product_count);
    	if (count > 0) {
    		productCount.setVisibility(View.VISIBLE);
    		productCount.setText("" + count);
    	} else {
    		productCount.setVisibility(View.GONE);
    	}
    }
    
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
    	return LayoutInflater.from(parent.getContext()).inflate(R.layout.product_grid_item, null);
    }
}
