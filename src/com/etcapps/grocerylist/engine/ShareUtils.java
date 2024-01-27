package com.etcapps.grocerylist.engine;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.Toast;

import com.etcapps.grocerylist.R;

public class ShareUtils {
	
    public static void shareGroceryList(Context context, GroceryListEngine engine, long groceryListId) {
    	try {
    		Intent shareGroceryListIntent = new Intent(android.content.Intent.ACTION_SEND);
    		shareGroceryListIntent.setType("plain/text");
    		GroceryList groceryList =engine.getGroceryList(groceryListId);
    		Cursor shoppingItemCursor =engine.getAllShoppingListItemsCursor(groceryList.getId(), false);
    		StringBuilder products = new StringBuilder();
    		int productIndex = 1;
    		
    		while (shoppingItemCursor.moveToNext()) {
    			ShoppingListItem item =engine.getShoppingListItem(shoppingItemCursor.getLong(GroceryListProvider.SHOPPING_LIST_ITEM_ID_COLUMN));
    			Product product =engine.getProduct(item.getProductId());
    			products.append("" + productIndex + ". " + product.getName() + " " + "x" + item.getCount() + "\n");
    			productIndex++;
    		}
    		
    		if (shoppingItemCursor != null &&
    			!shoppingItemCursor.isClosed()) {
    			shoppingItemCursor.close();
    		}
    		
    		String subject = context.getString(R.string.share_grocery_list_subject, groceryList.getName());
    		String body = context.getString(R.string.share_grocery_list_body, products.toString());
    		shareGroceryListIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
    		shareGroceryListIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
    		context.startActivity(shareGroceryListIntent);
    	} catch (Exception e) {
    		Toast.makeText(context, context.getResources().getString(R.string.error_could_not_share), Toast.LENGTH_LONG).show();
    	}
    }
	
	private ShareUtils() {
	}

}
