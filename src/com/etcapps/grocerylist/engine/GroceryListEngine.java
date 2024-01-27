package com.etcapps.grocerylist.engine;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class GroceryListEngine {
	private static final String TAG = GroceryListEngine.class.getSimpleName();
	
	private GroceryListProvider mDbProvider;
	private ArrayList<Product> mPriorityCache;
	
	public GroceryListEngine(Context context, GroceryListProvider db) {
		mDbProvider = db;
	}
	
	public long addGroceryListItem(GroceryList groceryList) {
		return mDbProvider.insertGroceryListItem(groceryList);
	}
	
	public void removeGroceryListItem(long groceryListId) {
		if (!mDbProvider.removeGroceryListItem(groceryListId)) {
			if(DebugUtils.printDebug) {
				Log.w(TAG, "Grocery list with id " + groceryListId + " could not be removed");
			}
		}
	}
	
	public Cursor getAllGroceryListItemsCursor(int sortBy, boolean descending) {
		return mDbProvider.getAllGroceryListItemsCursor(sortBy, descending);
	}
	
	public GroceryList getGroceryList(long groceryListId) {
		return mDbProvider.getGroceryListItem(groceryListId);
	}
	
	public GroceryList getGroceryList(String groceryListName) {
		return mDbProvider.getGroceryListItem(groceryListName);
	}
	
	public void updateGroceryListItem(GroceryList groceryList) {
		if (mDbProvider.updateGroceryListItem(groceryList.getId(), groceryList)) {
			if(DebugUtils.printDebug) {
				Log.w(TAG, "GroceryList id:" + groceryList.getId() + " name:" + groceryList.getName() + " could not be updated");
			}	
		}
	}
	
	public long addProduct(Product product) {
		return mDbProvider.insertProduct(product);
	}
	
	public Cursor getAllProductsCursor(int sortBy, boolean descending) {
		return mDbProvider.getAllProductsCursor(sortBy, descending);
	}

	public Cursor getAllProductsCursor(String filterBy) {
		return mDbProvider.getAllProductsCursor(filterBy);
	}
	
	public Product getProduct(long productId) {
		return mDbProvider.getProduct(productId);
	}
	
	public Product getProduct(String productName) {
		return mDbProvider.getProduct(productName);
	}
	
	public void updateProduct(Product product) {
		if (mDbProvider.updateProduct(product)) {
			if(DebugUtils.printDebug) {
				Log.w(TAG, "Product id:" + product.getId() + " name:" + product.getName() + " could not be updated");
			}
		}
	}
	
	public void removeProduct(long productId) {
		if (!mDbProvider.removeProduct(productId)) {
			if(DebugUtils.printDebug) {
				Log.w(TAG, "Product with id " + productId + " could not be removed");
			}
		}
	}
	
	public long addShoppingListItem(long productId, long groceryListId, boolean cacheProductProductPriority) {
		Cursor c = mDbProvider.getAllShoppingListItemsCursor(productId, groceryListId, false);
		
		Product product = getProduct(productId);
		if (product != null) {
			if (!cacheProductProductPriority) {
				product.increasePriority(1);
				updateProduct(product);
			} else {
				if (mPriorityCache == null) {
					mPriorityCache = new ArrayList<Product>();
				}
				
				Product item = findProductInCache(productId);
				if (item == null) {
					item = getProduct(productId);
				}
				item.increasePriority(1);
				mPriorityCache.add(item);
			}
		}
		
		if ((c.getCount() > 0) && c.moveToFirst()) {
			long id = c.getLong(GroceryListProvider.SHOPPING_LIST_ITEM_ID_COLUMN);
			c.close();
			ShoppingListItem item = getShoppingListItem(id);
			item.increaseCount();
			mDbProvider.updateShoppingListItem(item);
			return id;
		} else {
			c.close();
			ShoppingListItem item = new ShoppingListItem(GroceryListUtils.UNKNOWN_ID, productId, groceryListId);
			return mDbProvider.insertShoppingListItem(item);
		}
	}
	
	public void commitProductPriorities() {
		if (mPriorityCache != null &&
			mPriorityCache.size() > 0) {
			for(Product item : mPriorityCache) {
				updateProduct(item);
			}
			mPriorityCache = null;
		}
	}
	
	private Product findProductInCache(long productId) {
		for(Product item : mPriorityCache) {
			if (item.getId() == productId) {
				return item;
			}
		}
		return null;
	}
	
	public void updateShoppingListItem(ShoppingListItem shoppingListItem) {
		if (mDbProvider.updateShoppingListItem(shoppingListItem)) {
			if(DebugUtils.printDebug) {
				Log.w(TAG, "ShoppingList id:" + shoppingListItem.getId() + " productId:" + shoppingListItem.getProductId() + " could not be updated");
			}
		}
	}
	
	public void removeShoppingListItem(long shoppingListRowIndex) {
		if (!mDbProvider.removeShoppingListItem(shoppingListRowIndex)) {
			if(DebugUtils.printDebug) {
				Log.w(TAG, "Shopping list item id " + shoppingListRowIndex + "could not be removed");
			}
		}
	}
	
	public void removeAllShoppingListItemsByGroceryList(long groceryListId) {
		if (!mDbProvider.removeAllShoppingListItemsByGroceryList(groceryListId)) {
			if(DebugUtils.printDebug) {
				Log.w(TAG, "Shopping lists with grocery list id " + groceryListId + "could not be removed");
			}
		}
	}
	
	public void removeShoppingListItem(long productId, long groceryListId) {
		if (!mDbProvider.removeShoppingListItem(productId, groceryListId)) {
			if(DebugUtils.printDebug) {
				Log.w(TAG, "Shopping list item id could not be removed");
			}
		}
	}
	
	public void removeAllShoppingListItemByProductId(long productId) {
		if (!mDbProvider.removeAllShoppingListItemByProductId(productId)) {
			if(DebugUtils.printDebug) {
				Log.w(TAG, "Shopping list item id could not be removed");
			}
		}
	}
	
	public Cursor getAllShoppingListItemsCursor(long groceryListId, boolean hidePurchasedItems) {
		return mDbProvider.getAllShoppingListItemsCursor(GroceryListUtils.UNKNOWN_ID, groceryListId, hidePurchasedItems);
	}
	
	public Cursor getAllPurchasedShoppingListItemsCursor(long groceryListId) {
		return mDbProvider.getAllPurchasedShoppingListItemsCursor(groceryListId);
	}
	
	public ShoppingListItem getShoppingListItem(long shoppingListId) {
		return mDbProvider.getShoppingListItem(shoppingListId);
	}
	
	public ShoppingListItem getShoppingListItem(long productId, long groceryListId) {
		return mDbProvider.getShoppingListItem(productId, groceryListId);
	}
	
	public Icon getIcon(long iconId) {
		return mDbProvider.getIcon(iconId);
	}
	
	public long insertOrUpdateIcon(Icon icon) {
		return mDbProvider.insertOrUpdateIcon(icon);
	}
}

