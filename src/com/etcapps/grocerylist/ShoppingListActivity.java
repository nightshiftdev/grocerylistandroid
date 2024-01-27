package com.etcapps.grocerylist;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.etcapps.grocerylist.engine.DebugUtils;
import com.etcapps.grocerylist.engine.GroceryListEngine;
import com.etcapps.grocerylist.engine.GroceryListProvider;
import com.etcapps.grocerylist.engine.GroceryListStateTracker;
import com.etcapps.grocerylist.engine.GroceryListUtils;
import com.etcapps.grocerylist.engine.ShareUtils;
import com.etcapps.grocerylist.engine.ShoppingListItem;
import com.etcapps.grocerylist.R;

public class ShoppingListActivity extends ListActivity {
	public static final String TAG = ShoppingListActivity.class.getSimpleName();
	
	public static final String EXTRA_KEY_GROCERY_LIST_NAME = "com.etcapps.grocerylist.android.GroceryListName";
	public static final String EXTRA_KEY_GROCERY_LIST_ID = "com.etcapps.grocerylist.android.GroceryListId";

	private ShoppingListAdapter mShoppingListAdapter;
	private long mGroceryListId;
	private boolean mHidePurchasedItems;
	
	public static void startShopping(Activity activity, String groceryListName, long groceryListId, int requestCode) {
		Intent productSelectorIntent = new Intent(activity, ShoppingListActivity.class);
		productSelectorIntent.putExtra(EXTRA_KEY_GROCERY_LIST_NAME, groceryListName);
		productSelectorIntent.putExtra(ProductSelectorActivity.EXTRA_KEY_GROCERY_LIST_ID, groceryListId);
		productSelectorIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activity.startActivityForResult(productSelectorIntent, requestCode);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shopping_list);
        readArgumentsFromIntent();
        mShoppingListAdapter = new ShoppingListAdapter(this, getEngine(), mGroceryListId);
        setListAdapter(mShoppingListAdapter);
        registerForContextMenu(getListView());
        mHidePurchasedItems = false;
        updateShoppingList(mHidePurchasedItems);
        
        Button buttonFinishLater = (Button) findViewById(R.id.button_finish_later);
        buttonFinishLater.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View button) {
				onBackPressed();
			}
		});
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	ShoppingListItem item = getEngine().getShoppingListItem(id);
		TextView shoppingItemView = (TextView)v.findViewById(R.id.shopping_list_item_name);
		if ((shoppingItemView.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) == 0) {
			item.setIsPurchased(true);
			GroceryListStateTracker.updateListState(item.getProductId(), 1);
		} else {
			item.setIsPurchased(false);
			GroceryListStateTracker.updateListState(item.getProductId(), -1);
		}
		getEngine().updateShoppingListItem(item);
		updateShoppingList(mHidePurchasedItems);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	MenuItem item = menu.findItem(R.id.menu_item_show_hide_purchased_items);
    	if (isAnyShoppingListItemPurchased() && !mHidePurchasedItems) {
    		item.setTitle(R.string.menu_item_hide_purchased_items);
    	} else {
    		item.setTitle(R.string.menu_item_show_purchased_items);
    	}	
    	return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.shopping_list_options_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.menu_item_show_hide_purchased_items:
    		if (item.getTitle().equals(getResources().getString(R.string.menu_item_hide_purchased_items))) {
    			mHidePurchasedItems = true;
    		} else {
    			mHidePurchasedItems = false;
    		}
    		updateShoppingList(mHidePurchasedItems);
    		return true;
    	case R.id.menu_item_grocery_list_edit:
    		ProductSelectorActivity.startForResult(this, mGroceryListId, GroceryListActivity.REQUEST_CODE_EDIT_GROCERY_LIST);
    		return true;
    	case R.id.menu_item_grocery_list_share:
    		shareGroceryList();
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.shopping_list_conext_menu, menu);
        AdapterContextMenuInfo listItemInfo = (AdapterContextMenuInfo) menuInfo;
        Cursor c = (Cursor) mShoppingListAdapter.getItem(listItemInfo.position);
        long productId = c.getInt(GroceryListProvider.SHOPPING_LIST_ITEM_PRODUCT_ID_COLUMN);
        menu.setHeaderTitle(getEngine().getProduct(productId).getName());
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo listItemInfo = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor c = (Cursor) mShoppingListAdapter.getItem(listItemInfo.position);
        long itemId = c.getLong(GroceryListProvider.SHOPPING_LIST_ITEM_PRODUCT_ID_COLUMN);

        switch (item.getItemId()) {
        case R.id.menu_item_product_clear_count:
            clearProductCount(itemId);
            return true;
        case R.id.menu_item_product_decrease_count_by_one:
        	decreaseCountByOne(itemId);
        	return true;
         default:
            return super.onContextItemSelected(item);
        }
    }
    
    public void onBackPressed() {
    	Intent data = new Intent();
    	data.putExtra(ProductSelectorActivity.EXTRA_KEY_IS_LIST_DIRTY, GroceryListStateTracker.isListStateDirty());
        data.putExtra(EXTRA_KEY_GROCERY_LIST_ID, mGroceryListId);
        setResult(Activity.RESULT_OK, data);
        finish();
    }
    
    private void readArgumentsFromIntent() {
        String groceryListName = getIntent().getStringExtra(EXTRA_KEY_GROCERY_LIST_NAME);
        if (groceryListName != null) {
        	setTitle(groceryListName);
        } else {
        	if(DebugUtils.printDebug) {
        		Log.w(TAG, "ProductSelectorAcivity is expecting " + EXTRA_KEY_GROCERY_LIST_NAME);
        	}
        }
        mGroceryListId = getIntent().getLongExtra(ProductSelectorActivity.EXTRA_KEY_GROCERY_LIST_ID, GroceryListUtils.UNKNOWN_ID);
        if (mGroceryListId == GroceryListUtils.UNKNOWN_ID) {
        	if(DebugUtils.printDebug) {
        		Log.w(TAG, "Expecting EXTRA_KEY_GROCERY_LIST_ID");
        	}
        	finish();
        }
    }
    
    private void updateShoppingList(boolean hidePurchasedItems) {
    	Cursor oldCursor = mShoppingListAdapter.getCursor();
    	if (oldCursor != null) {
    		stopManagingCursor(oldCursor);
    	}
        Cursor cursor = getEngine().getAllShoppingListItemsCursor(mGroceryListId, hidePurchasedItems);
        if (cursor == null || cursor.isClosed()) {
            return;
        }
        startManagingCursor(cursor);
        mShoppingListAdapter.changeCursor(cursor);
    }
    
    private boolean isAnyShoppingListItemPurchased() {
    	Cursor cursor = getEngine().getAllPurchasedShoppingListItemsCursor(mGroceryListId);
        if (cursor == null || cursor.isClosed() || cursor.getCount() == 0) {
            return false;
        }
        return true;
    }
    
    private void clearProductCount(long productId) {
        ShoppingListItem shoppingListItem = getEngine().getShoppingListItem(productId, mGroceryListId);
        GroceryListStateTracker.updateListState(productId, (-1)*shoppingListItem.getCount());
        getEngine().removeShoppingListItem(productId, mGroceryListId);
        updateShoppingList(mHidePurchasedItems);
    }
    
    private void decreaseCountByOne(long productId) {
        ShoppingListItem shoppingListItem = getEngine().getShoppingListItem(productId, mGroceryListId);
        if (shoppingListItem.getCount() > 1) {
        	shoppingListItem.decreaseCount();
        	getEngine().updateShoppingListItem(shoppingListItem);
        } else {
        	getEngine().removeShoppingListItem(productId, mGroceryListId);
        }
        GroceryListStateTracker.updateListState(productId, -1);
        updateShoppingList(mHidePurchasedItems);
    }
    
    private void shareGroceryList() {
    	ShareUtils.shareGroceryList(this, getEngine(), mGroceryListId);
    }
    
    private GroceryListEngine getEngine() {
    	return ((GroceryListApplication)getApplication()).getEngine();
    }
}
