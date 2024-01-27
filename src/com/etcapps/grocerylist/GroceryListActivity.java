package com.etcapps.grocerylist;

import java.util.ArrayList;
import java.util.Date;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.etcapps.grocerylist.engine.GroceryList;
import com.etcapps.grocerylist.engine.GroceryListEngine;
import com.etcapps.grocerylist.engine.GroceryListProvider;
import com.etcapps.grocerylist.engine.GroceryListUtils;
import com.etcapps.grocerylist.engine.Product;
import com.etcapps.grocerylist.engine.ShareUtils;
import com.etcapps.grocerylist.engine.ShoppingListItem;

public class GroceryListActivity extends ListActivity {

	private static final int REQUEST_CODE_GET_GROCERY_LIST_NAME = 0;
	private static final int REQUEST_CODE_CHANGE_GROCERY_LIST_NAME = 1;
	private static final int REQUEST_CODE_SHOP = 2;
	public static final int REQUEST_CODE_EDIT_GROCERY_LIST = 3;
	private static final int REQUEST_CODE_ADD_GROCERY_LIST = 4;
	private static final int REQUEST_CODE_ADD_GROCERY_LIST_BY_VOICE = 5;
	
	private GroceryListAdapter mGroceryListAdapter;
	private int mSortBy;
	private boolean mDescending;
	private Long mVoiceCreatedGroceryListId;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.glists);
        mVoiceCreatedGroceryListId = null;
        mGroceryListAdapter = new GroceryListAdapter(this);
        setListAdapter(mGroceryListAdapter);
        registerForContextMenu(getListView());

        Button newListButton = (Button)findViewById(R.id.button_new_list);
        newListButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View arg0) {
				AddRenameGroceryListActivity.addGroceryList(GroceryListActivity.this, REQUEST_CODE_GET_GROCERY_LIST_NAME);
			}
		});
        
        mDescending = true;
        mSortBy = GroceryListUtils.SORT_GROCERY_LISTS_BY_DATE;
        updateGroceryList(mSortBy, mDescending);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        showGroceryListItems(id);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.grocery_lists_conext_menu, menu);
        AdapterContextMenuInfo listItemInfo = (AdapterContextMenuInfo) menuInfo;
        Cursor c = (Cursor) mGroceryListAdapter.getItem(listItemInfo.position);
        String groceryListName = c.getString(GroceryListProvider.GROCERY_LIST_NAME_COLUMN);
        menu.setHeaderTitle(groceryListName);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo listItemInfo = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor c = (Cursor) mGroceryListAdapter.getItem(listItemInfo.position);
        long itemId = c.getLong(GroceryListProvider.GROCERY_LIST_ID_COLUMN);
    	
        switch (item.getItemId()) {
        case R.id.menu_item_grocery_list_delete:
            deleteGroceryList(itemId);
            return true;
        case R.id.menu_item_grocery_list_share:
            shareGroceryList(itemId);
            return true;
        case R.id.menu_item_grocery_list_change_name:
            renameGroceryList(itemId);
            return true;
        case R.id.menu_item_grocery_list_edit:
            editGroceryList(itemId);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.grocery_lists_options_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.menu_item_sort_grocery_lists_by_date:
    		if (mSortBy == GroceryListUtils.SORT_GROCERY_LISTS_BY_NAME) {
    			mDescending = false;
    		} else {
    			mDescending = !mDescending;
    		}
    		mSortBy = GroceryListUtils.SORT_GROCERY_LISTS_BY_DATE;
    		updateGroceryList(mSortBy, mDescending);
    		return true;
    	case R.id.menu_item_sort_grocery_lists_by_name:
    		if (mSortBy == GroceryListUtils.SORT_GROCERY_LISTS_BY_DATE) {
    			mDescending = false;
    		} else {
    			mDescending = !mDescending;
    		}
    		mSortBy = GroceryListUtils.SORT_GROCERY_LISTS_BY_NAME;
    		updateGroceryList(mSortBy, mDescending);
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch (requestCode) {
    	case REQUEST_CODE_ADD_GROCERY_LIST:
    	case REQUEST_CODE_EDIT_GROCERY_LIST:
    		if (resultCode == RESULT_OK) {
    			if(data.hasExtra(ProductSelectorActivity.EXTRA_KEY_GROCERY_LIST_ID)) {
    				long groceryListId = data.getLongExtra(ProductSelectorActivity.EXTRA_KEY_GROCERY_LIST_ID, GroceryListUtils.UNKNOWN_ID);
    				if (groceryListId != GroceryListUtils.UNKNOWN_ID) {
    					showGroceryListItems(groceryListId);
    					if (data.hasExtra(ProductSelectorActivity.EXTRA_KEY_IS_LIST_DIRTY) &&
    						data.getBooleanExtra(ProductSelectorActivity.EXTRA_KEY_IS_LIST_DIRTY, false)) {
    						updateLastModifiedDate(groceryListId);
    					}
    				}
    			}
    		}
    		break;
    	case REQUEST_CODE_GET_GROCERY_LIST_NAME:
    	case REQUEST_CODE_CHANGE_GROCERY_LIST_NAME:
            if (resultCode == RESULT_OK) {
            	if (data != null) {
            		if (data.hasExtra(AddRenameGroceryListActivity.EXTRA_KEY_GROCERY_LIST_ID) &&
            			data.hasExtra(AddRenameGroceryListActivity.EXTRA_KEY_CREATE_LIST_BY_VOICE_FLAG)) {
                  		mVoiceCreatedGroceryListId = data.getLongExtra(AddRenameGroceryListActivity.EXTRA_KEY_GROCERY_LIST_ID, GroceryListUtils.UNKNOWN_ID);
                      	startVoiceRecognitionActivity();
            		} else if (data.hasExtra(AddRenameGroceryListActivity.EXTRA_KEY_GROCERY_LIST_ID)) {
            			long groceryListId = data.getLongExtra(AddRenameGroceryListActivity.EXTRA_KEY_GROCERY_LIST_ID, 
 							   								   GroceryListUtils.UNKNOWN_ID);
                    	ProductSelectorActivity.startForResult(this, groceryListId, REQUEST_CODE_ADD_GROCERY_LIST);
            		}
            	} 
            	updateGroceryList(mSortBy, mDescending);
            }
    		break;
    	case REQUEST_CODE_ADD_GROCERY_LIST_BY_VOICE:
    		if(resultCode == RESULT_OK) {
                ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                createGroceryListFromVoiceResults(matches);
            }
    		break;
    	case REQUEST_CODE_SHOP:
    		if (resultCode == RESULT_OK) {
    			if (data != null &&
    				data.hasExtra(ShoppingListActivity.EXTRA_KEY_GROCERY_LIST_ID)) {
    				final long groceryListId = data.getLongExtra(ShoppingListActivity.EXTRA_KEY_GROCERY_LIST_ID, 
    													   		 GroceryListUtils.UNKNOWN_ID);
    				if (groceryListId != GroceryListUtils.UNKNOWN_ID) {
    					if (data.hasExtra(ProductSelectorActivity.EXTRA_KEY_IS_LIST_DIRTY) &&
    						data.getBooleanExtra(ProductSelectorActivity.EXTRA_KEY_IS_LIST_DIRTY, false)) {
    						updateLastModifiedDate(groceryListId);
    					}
    					Cursor allItemsPurchased = null;
    					try {
    					allItemsPurchased = getEngine().getAllShoppingListItemsCursor(groceryListId, true);
    					if (!allItemsPurchased.moveToFirst()) {
    						GroceryList groceryList = getEngine().getGroceryList(groceryListId);
	    		    		AlertDialog.Builder confirmDeleteShoppingList = new AlertDialog.Builder(this);
	    		    		confirmDeleteShoppingList.setMessage(getString(R.string.dialog_confirm_remove_shopping_list, groceryList.getName()));
	    		    		confirmDeleteShoppingList.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
								
								public void onClick(DialogInterface arg0, int arg1) {
									GroceryListActivity.this.doDeleteGroceryList(groceryListId);
								}
							});
	    		    		confirmDeleteShoppingList.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
								
								public void onClick(DialogInterface arg0, int arg1) {
									Cursor cursor = null; 
									try {
										cursor = getEngine().getAllShoppingListItemsCursor(groceryListId, false);
										while(cursor.moveToNext()) {
											ShoppingListItem item = getEngine().getShoppingListItem(cursor.getLong(GroceryListProvider.SHOPPING_LIST_ITEM_ID_COLUMN));
											item.setIsPurchased(false);
											getEngine().updateShoppingListItem(item);
										}
									} finally {
										if (cursor != null) {
											cursor.close();
											cursor = null;
										}
									}
								}
							});
	    		    		confirmDeleteShoppingList.create().show();
    					} 
    					} finally {
    						if (allItemsPurchased != null && !allItemsPurchased.isClosed()) {
    							allItemsPurchased.close();
    						}
    					}
    				}		
    			}
    		}
    		break;
    	}
    }

    private void showGroceryListItems(long groceryListId) {
    	GroceryList gl = getEngine().getGroceryList(groceryListId);
    	if (gl != null) {
    		ShoppingListActivity.startShopping(GroceryListActivity.this, gl.getName(), gl.getId(), REQUEST_CODE_SHOP);
    	}
    }

    private void deleteGroceryList(final long groceryListId) {
    	final GroceryList groceryList = getEngine().getGroceryList(groceryListId);
    	AlertDialog.Builder confirmDelete = new AlertDialog.Builder(this);
    	confirmDelete.setTitle(R.string.dialog_confirm_title_warning);
    	confirmDelete.setMessage(getString(R.string.dialog_confirm_grocery_list_delete, groceryList.getName()));
    	confirmDelete.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

    		public void onClick(DialogInterface arg0, int arg1) {
    			doDeleteGroceryList(groceryList.getId());
    		}
    	});
    	confirmDelete.setNegativeButton(android.R.string.cancel, null);
    	confirmDelete.create().show();
    }
    
    private void doDeleteGroceryList(final long groceryListId) {
    	getEngine().removeGroceryListItem(groceryListId);
		getEngine().removeAllShoppingListItemsByGroceryList(groceryListId);
		updateGroceryList(mSortBy, mDescending);
    }

    private void shareGroceryList(long groceryListId) {
    	ShareUtils.shareGroceryList(this, getEngine(), groceryListId);
    }

    private void renameGroceryList(long groceryListId) {
    	GroceryList groceryList = getEngine().getGroceryList(groceryListId);
    	if(groceryList == null) {
    		return;
    	}
    	AddRenameGroceryListActivity.renameGroceryList(this, groceryList.getId(), REQUEST_CODE_CHANGE_GROCERY_LIST_NAME);
    }
    
    private void editGroceryList(long groceryListId) {
    	ProductSelectorActivity.startForResult(this, groceryListId, REQUEST_CODE_EDIT_GROCERY_LIST);
    }
    
    private void updateGroceryList(int sortBy, boolean descending) {
    	Cursor oldCursor = mGroceryListAdapter.getCursor();
    	if (oldCursor != null) {
    		stopManagingCursor(oldCursor);
    	}
    	Cursor cursor = getEngine().getAllGroceryListItemsCursor(sortBy, descending);
        if (cursor == null || cursor.isClosed()) {
            return;
        }
        startManagingCursor(cursor);
        mGroceryListAdapter.changeCursor(cursor);
    }
    
    private void updateLastModifiedDate(long groceryListId) {
		GroceryList groceryList = getEngine().getGroceryList(groceryListId);
		groceryList.setDateModified(new Date(System.currentTimeMillis()));
		getEngine().updateGroceryListItem(groceryList);
    }
    
    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                		RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_hint_create_list));
        startActivityForResult(intent, REQUEST_CODE_ADD_GROCERY_LIST_BY_VOICE);
    }
    
    private void createGroceryListFromVoiceResults(ArrayList<String> voiceResults) {
    	if (mVoiceCreatedGroceryListId != null) {
    		class CreateListFromVoiceRestultsTask extends AsyncTask<Void, Void, Void>{
    			private ArrayList<String> mVoiceResults;
    			private long mGroceryListId;
    			ProgressDialog mProgress;
    			CreateListFromVoiceRestultsTask(ArrayList<String> voiceResults, long groceryListId) {
    				mVoiceResults = voiceResults;
    				mGroceryListId = groceryListId;
    			}
    			
    			@Override
    			protected Void doInBackground(Void... ignore) {
    	    		for (String result : mVoiceResults) {     
    	    			String[] products = result.split(" ");
    	    			for (String productName : products) {
    		    			Product product = getEngine().getProduct(productName);
    		    			if (product != null) {
    		    				getEngine().addShoppingListItem(product.getId(), mGroceryListId, false);
    		    			}
    	    			}
    	    		}
    	    		return null;
    			}
    			
    	        @Override
    	        protected void onPostExecute(Void ignore) {
    	        	mProgress.dismiss();
    	            showGroceryListItems(mGroceryListId);
    	        }
    	        
    	        @Override
    	        protected void onPreExecute() {
    	        	super.onPreExecute();
    	        	mProgress = ProgressDialog.show(GroceryListActivity.this, "", GroceryListActivity.this.getString(R.string.creating_list_by_voice_wait), true);
    	        }
        	}
        	new CreateListFromVoiceRestultsTask(voiceResults, mVoiceCreatedGroceryListId).execute();
    	}
    	mVoiceCreatedGroceryListId = null;
    }
    
    private GroceryListEngine getEngine() {
    	return ((GroceryListApplication)getApplication()).getEngine();
    }
}