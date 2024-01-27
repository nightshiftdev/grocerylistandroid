package com.etcapps.grocerylist.engine;

import java.io.ByteArrayOutputStream;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.BaseColumns;
import android.util.Log;

import com.etcapps.grocerylist.R;

public class GroceryListProvider {
	
	private static final String TAG = GroceryListProvider.class.getSimpleName();
	
	private static final String DATABASE_NAME = "groceryList.db";
	private static final int    DATABASE_VERSION = 1;
	private static final String GROCERY_LISTS_TABLE = "GroceryLists";
	private static final String SHOPPING_LIST_ITEMS_TABLE = "ShoppingListItems";
	private static final String PRODUCTS_TABLE = "Products";
	private static final String ICONS_TABLE = "Icons";
	
	// Column names for GroceryLists table
	public static final String GROCERY_LIST_ITEM_KEY_ID = "_id";
	public static final String GROCERY_LIST_ITEM_KEY_NAME = "name";
	public static final String GROCERY_LIST_ITEM_KEY_RES_ICON_ID = "resIconId";
	public static final String GROCERY_LIST_ITEM_KEY_DATE = "dateModified";
	
	public static final int GROCERY_LIST_ID_COLUMN = 0;
	public static final int GROCERY_LIST_NAME_COLUMN = 1;
	public static final int GROCERY_LIST_RES_ICON_ID_COLUMN = 2;
	public static final int GROCERY_LIST_DATE_COLUMN = 3;

	// Column names for ShoppinListItems table
	public static final String SHOPPING_LIST_ITEM_KEY_ID = "_id";
	public static final String SHOPPING_LIST_ITEM_KEY_GROCERY_LIST_ID = "groceryListId";
	public static final String SHOPPING_LIST_ITEM_KEY_PRODUCT_ID = "productId";
	public static final String SHOPPING_LIST_ITEM_KEY_COUNT = "count";
	public static final String SHOPPING_LIST_ITEM_IS_PURCHASED = "isPurchased";
	
	public static final int SHOPPING_LIST_ITEM_ID_COLUMN = 0;
	public static final int SHOPPING_LIST_ITEM_GROCERY_LIST_ID_COLUMN = 1;
	public static final int SHOPPING_LIST_ITEM_PRODUCT_ID_COLUMN = 2;
	public static final int SHOPPING_LIST_ITEM_COUNT_COLUMN = 3;
	public static final int SHOPPING_LIST_ITEM_IS_PURCHASED_COLUMN = 4;
	
	// Column names for Products table
	public static final String PRODUCT_KEY_ID = "_id";
	public static final String PRODUCT_KEY_NAME = "name";
	public static final String PRODUCT_KEY_RES_ICON_ID = "productIconId";
	public static final String PRODUCT_KEY_PRIORITY = "productPriority";
	public static final String PRODUCT_KEY_IS_USER_CREATED = "isUserCreated";
	
	public static final int PRODUCT_ID_COLUMN = 0;
	public static final int PRODUCT_NAME_COLUMN = 1;
	public static final int PRODUCT_RES_ICON_ID_COLUMN = 2;
	public static final int PRODUCT_USER_PRIORITY_COLUMN = 3;
	public static final int PRODUCT_USER_IS_USER_CREATED_COLUMN = 4;

	// Column names for Icons table
	public static final String USER_ICON_KEY_ID = "_id";
	public static final String USER_ICON_KEY_DATA = "data";
	public static final String USER_ICON_KEY_UNIQUE_HASH = "hash";
	
	public static final int USER_ICON_ID_COLUMN = 0;
	public static final int USER_ICON_DATA_COLUMN = 1;
	public static final int USER_UNIQUE_HASH_COLUMN = 2;
	
	private SQLiteDatabase mGroceryListDb;
	private static Context mContext;
	private GroceryListDBHelper mDbHelper;
	
	private static class GroceryListDBHelper extends SQLiteOpenHelper {
		
		private static final String TAG = GroceryListDBHelper.class.getSimpleName();

		private static final String CREATE_GROCERY_LISTS_TABLE = 
			"create table " + GROCERY_LISTS_TABLE + " ("
			+ GROCERY_LIST_ITEM_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ GROCERY_LIST_ITEM_KEY_NAME + " TEXT NOT NULL, "
			+ GROCERY_LIST_ITEM_KEY_RES_ICON_ID + " INTEGER, "
			+ GROCERY_LIST_ITEM_KEY_DATE + " INTEGER );";
		
		private static final String CREATE_SHOPPING_LIST_ITEMS_TABLE = 
			"create table " + SHOPPING_LIST_ITEMS_TABLE + " ("
			+ SHOPPING_LIST_ITEM_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ SHOPPING_LIST_ITEM_KEY_GROCERY_LIST_ID + " INTEGER NOT NULL, "
			+ SHOPPING_LIST_ITEM_KEY_PRODUCT_ID + " INTEGER NOT NULL, " 
			+ SHOPPING_LIST_ITEM_KEY_COUNT + " INTEGER NOT NULL, "
			+ SHOPPING_LIST_ITEM_IS_PURCHASED + " BOOLEAN );";
		
		private static final String CREATE_PRODUCTS_TABLE = 
			"create table " + PRODUCTS_TABLE + " ("
			+ PRODUCT_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ PRODUCT_KEY_NAME + " TEXT NOT NULL, "
			+ PRODUCT_KEY_RES_ICON_ID + " INTEGER NOT NULL, "
			+ PRODUCT_KEY_PRIORITY + " INTEGER NOT NULL, "
			+ PRODUCT_KEY_IS_USER_CREATED + " BOOLEAN NOT NULL );";
		
		private static final String CREATE_ICONS_TABLE = 
			"create table " + ICONS_TABLE + " ("
			+ USER_ICON_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ USER_ICON_KEY_DATA + " BLOB, "
			+ USER_ICON_KEY_UNIQUE_HASH + " BLOB UNIQUE );";
		
		public GroceryListDBHelper(Context context, String name,
								   CursorFactory factory, int version) {
			super(context, name, factory, version);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_GROCERY_LISTS_TABLE);
			db.execSQL(CREATE_SHOPPING_LIST_ITEMS_TABLE);
			db.execSQL(CREATE_PRODUCTS_TABLE);
			db.execSQL(CREATE_ICONS_TABLE);
			populateWithDefaultContent(db);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO: Should probably migrate old content of the database instead destroying it and recreating
			if(DebugUtils.printDebug) {
				Log.w(TAG, "Upgrading from version " + oldVersion + " to " + newVersion + ". Old database content is gone.");
			}
			db.execSQL("DROP TABLE IF EXISTS " + GROCERY_LISTS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + SHOPPING_LIST_ITEMS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + PRODUCTS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + ICONS_TABLE);
			onCreate(db);
		}
		
		private void populateWithDefaultContent(SQLiteDatabase db) {
			String[] productNames = mContext.getResources().getStringArray(R.array.defaultProducts);
			for (String productName : productNames) {
				String resIconName = productName.replace(" ", "_");
				int resourceId = mContext.getResources().getIdentifier(resIconName, "drawable", mContext.getPackageName());
				if (insertDefaultProduct(db, productName, resourceId) <= 0) {
					if(DebugUtils.printDebug) {
						Log.w(TAG, "Could not insert " + productName);
					}
				}
			}
			insertDefaultGroceryLists(db);
		}
		
		private long insertDefaultProduct(SQLiteDatabase db, String name, int resIconId) {
			ContentValues newProduct = new ContentValues();
		    newProduct.put(PRODUCT_KEY_NAME, name);
		    newProduct.put(PRODUCT_KEY_RES_ICON_ID, resIconId);
		    newProduct.put(PRODUCT_KEY_PRIORITY, 0);
		    newProduct.put(PRODUCT_KEY_IS_USER_CREATED, 0);
		    return db.insert(PRODUCTS_TABLE, null, newProduct);
		}
		
		private void insertDefaultGroceryLists(SQLiteDatabase db) {
			String[] groceryListsNames = mContext.getResources().getStringArray(R.array.defaultGroceryLists);
			GroceryList weekly = new GroceryList(groceryListsNames[0], 
												 GroceryListUtils.getRandomGroceryListIcon(mContext), 
												 new Date(System.currentTimeMillis()));
			long groceryListId = insertGroceryList(db, weekly);
			
			populateGroceryList(db, groceryListId, R.array.weeklyItems, R.array.weeklyItemsCount);
			
			
			
			GroceryList casseroles = new GroceryList(groceryListsNames[1], 
					 								 GroceryListUtils.getRandomGroceryListIcon(mContext), 
					 								 new Date(System.currentTimeMillis()));
			groceryListId = insertGroceryList(db, casseroles);

			populateGroceryList(db, groceryListId, R.array.casserolesItems, R.array.casserolesItemsCount);
			
			
			
			GroceryList cheesecake = new GroceryList(groceryListsNames[2], 
					 GroceryListUtils.getRandomGroceryListIcon(mContext), 
					 new Date(System.currentTimeMillis()));
			groceryListId = insertGroceryList(db, cheesecake);
			
			populateGroceryList(db, groceryListId, R.array.bananaBreadItems, R.array.bananaBreadItemsCount);
		}
		
		private long insertGroceryList(SQLiteDatabase db, GroceryList groceryList) {
		    ContentValues newGroceryList = new ContentValues();
		    newGroceryList.put(GROCERY_LIST_ITEM_KEY_NAME, groceryList.getName());
		    newGroceryList.put(GROCERY_LIST_ITEM_KEY_RES_ICON_ID, groceryList.getResIconId());
		    newGroceryList.put(GROCERY_LIST_ITEM_KEY_DATE, groceryList.getDateModified().getTime());
		    return db.insert(GROCERY_LISTS_TABLE, null, newGroceryList);
		}
		
		private void populateGroceryList(SQLiteDatabase db, long groceryListId, int shoppingItemsRes, int shoppingItemsCountRes) {
			String[] productNames = mContext.getResources().getStringArray(shoppingItemsRes);
			int[] productCount = mContext.getResources().getIntArray(shoppingItemsCountRes);
			
			
			for (int i = 0; i < productNames.length; i++) {
				int count = 1;
				if(i < productCount.length) {
					count = productCount[i];
				}
				Product product = findProductByName(db, productNames[i]);
				ShoppingListItem item = new ShoppingListItem(GroceryListUtils.UNKNOWN_ID, 
															 product.getId(), 
															 groceryListId, 
															 count, 
															 false);
				insertShoppingListItem(db, item);
			}
		}
		
		private Product findProductByName(SQLiteDatabase db, String productName) {
			Cursor cursor = null;
			Product result = null;
			try {
				cursor = db.query(PRODUCTS_TABLE, 
								  new String[] { PRODUCT_KEY_ID, 
												 PRODUCT_KEY_NAME,
												 PRODUCT_KEY_RES_ICON_ID,
												 PRODUCT_KEY_PRIORITY,
												 PRODUCT_KEY_IS_USER_CREATED }, 
												 PRODUCT_KEY_NAME + "=" + "'" + productName + "'", 
												 null, null, null, null);
				if ((cursor.getCount() != 0) && cursor.moveToFirst()) {
					long id = cursor.getLong(PRODUCT_ID_COLUMN);
					String name = cursor.getString(PRODUCT_NAME_COLUMN);
					int resIconId = cursor.getInt(PRODUCT_RES_ICON_ID_COLUMN);
					int priority = cursor.getInt(PRODUCT_USER_PRIORITY_COLUMN);
					boolean isUserCreated = false;
					int isUserCreatedInt = cursor.getInt(PRODUCT_USER_IS_USER_CREATED_COLUMN);
					if (isUserCreatedInt == GroceryListUtils.PRODUCT_USER_CREATED) {
						isUserCreated = true;
					}
					result = new Product(name, 
										 resIconId, 
										 id, 
										 priority, 
										 isUserCreated);
				}
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
			return result;
		}
		
		public long insertShoppingListItem(SQLiteDatabase db, ShoppingListItem shoppingList) {
		    ContentValues newShoppingList = new ContentValues();
		    newShoppingList.put(SHOPPING_LIST_ITEM_KEY_GROCERY_LIST_ID, shoppingList.getGroceryListId());
		    newShoppingList.put(SHOPPING_LIST_ITEM_KEY_PRODUCT_ID, shoppingList.getProductId());
		    newShoppingList.put(SHOPPING_LIST_ITEM_KEY_COUNT, shoppingList.getCount());
		    newShoppingList.put(SHOPPING_LIST_ITEM_IS_PURCHASED, shoppingList.getIsPurchased());
		    return db.insert(SHOPPING_LIST_ITEMS_TABLE, null, newShoppingList);
		}
	}
	
	public GroceryListProvider(Context context) {
		mContext = context;
		mDbHelper = new GroceryListDBHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public void close() {
		mGroceryListDb.close();
	}
	
	public void open() throws SQLiteException {
		try {
			mGroceryListDb = mDbHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			if(DebugUtils.printDebug) {
				Log.w(TAG, "Could not open writable database", e);
			}
			mGroceryListDb = mDbHelper.getReadableDatabase();
		}
	}
	
	public long insertGroceryListItem(GroceryList groceryList) {
	    ContentValues newGroceryList = new ContentValues();
	    newGroceryList.put(GROCERY_LIST_ITEM_KEY_NAME, groceryList.getName());
	    newGroceryList.put(GROCERY_LIST_ITEM_KEY_RES_ICON_ID, groceryList.getResIconId());
	    newGroceryList.put(GROCERY_LIST_ITEM_KEY_DATE, groceryList.getDateModified().getTime());
	    return mGroceryListDb.insert(GROCERY_LISTS_TABLE, null, newGroceryList);
	}
	
	public boolean updateGroceryListItem(long groceryListRowIndex, GroceryList groceryList) {
	    ContentValues updateGroceryList = new ContentValues();
	    updateGroceryList.put(GROCERY_LIST_ITEM_KEY_NAME, groceryList.getName());
	    updateGroceryList.put(GROCERY_LIST_ITEM_KEY_RES_ICON_ID, groceryList.getResIconId());
	    updateGroceryList.put(GROCERY_LIST_ITEM_KEY_DATE, groceryList.getDateModified().getTime());
	    return mGroceryListDb.update(GROCERY_LISTS_TABLE, 
	    							 updateGroceryList, 
	    							 GROCERY_LIST_ITEM_KEY_ID + "=" + groceryListRowIndex, 
	    							 null) > 0;
	}
	
	public boolean removeGroceryListItem(long groceryListRowIndex) {
		return mGroceryListDb.delete(GROCERY_LISTS_TABLE, 
									 GROCERY_LIST_ITEM_KEY_ID + "=" + groceryListRowIndex, 
									 null) > 0;
	}
	
	public Cursor getAllGroceryListItemsCursor(int sortBy, boolean descending) {
		String orderBy = GROCERY_LIST_ITEM_KEY_DATE;
		switch (sortBy) {
		case GroceryListUtils.SORT_GROCERY_LISTS_BY_DATE:
			orderBy = GROCERY_LIST_ITEM_KEY_DATE;
			break;
		case GroceryListUtils.SORT_GROCERY_LISTS_BY_NAME:
			orderBy = GROCERY_LIST_ITEM_KEY_NAME;
			break;
		}
		if (descending) {
			orderBy = orderBy + " DESC";
		}
		return mGroceryListDb.query(GROCERY_LISTS_TABLE, 
									new String[] { GROCERY_LIST_ITEM_KEY_ID, 
												   GROCERY_LIST_ITEM_KEY_NAME, 
												   GROCERY_LIST_ITEM_KEY_RES_ICON_ID,
												   GROCERY_LIST_ITEM_KEY_DATE }, 
									null, null, null, null, orderBy);
	}
	 
	public Cursor setCursorToGroceryListItem(long groceryListRowIndex) {
		Cursor result = mGroceryListDb.query(true, 
											 GROCERY_LISTS_TABLE, 
											 new String[] { GROCERY_LIST_ITEM_KEY_ID, 
															GROCERY_LIST_ITEM_KEY_NAME,
															GROCERY_LIST_ITEM_KEY_RES_ICON_ID,
															GROCERY_LIST_ITEM_KEY_DATE },
											 GROCERY_LIST_ITEM_KEY_ID + "=" + groceryListRowIndex, 
											 null, null, null, null, null);

		if (result != null &&
			((result.getCount() == 0) || !result.moveToFirst())) {
			result.close();
			result = null;
		}
		return result;
	}

	public GroceryList getGroceryListItem(long groceryListRowIndex) {
		Cursor cursor = null;
		GroceryList result = null;
		try {
			cursor = mGroceryListDb.query(true, 
										  GROCERY_LISTS_TABLE, 
										  new String[] { GROCERY_LIST_ITEM_KEY_ID, 
										  				 GROCERY_LIST_ITEM_KEY_NAME,
										  				 GROCERY_LIST_ITEM_KEY_RES_ICON_ID,
										  				 GROCERY_LIST_ITEM_KEY_DATE },
										  				 GROCERY_LIST_ITEM_KEY_ID + "=" + groceryListRowIndex, 
										  null, null, null, null, null);
			if ((cursor.getCount() != 0) && cursor.moveToFirst()) {
				String name = cursor.getString(GROCERY_LIST_NAME_COLUMN);
				int resIconId = cursor.getInt(GROCERY_LIST_RES_ICON_ID_COLUMN);
				long millis = cursor.getLong(GROCERY_LIST_DATE_COLUMN); 
				result = new GroceryList(name, resIconId, groceryListRowIndex, new Date(millis));
			} 
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;  
	}

	public GroceryList getGroceryListItem(String groceryListName) {
		Cursor cursor = null;
		GroceryList result = null;
		try {
			cursor = mGroceryListDb.query(GROCERY_LISTS_TABLE, 
										  new String[] { GROCERY_LIST_ITEM_KEY_ID, 
										  				 GROCERY_LIST_ITEM_KEY_NAME,
										  				 GROCERY_LIST_ITEM_KEY_RES_ICON_ID,
										  				 GROCERY_LIST_ITEM_KEY_DATE },
										  				 GROCERY_LIST_ITEM_KEY_NAME + " LIKE " + "\"" + groceryListName + "\"", 
										  null, null, null, null, null);
			if ((cursor.getCount() != 0) && cursor.moveToFirst()) {
				long id =  cursor.getLong(GROCERY_LIST_ID_COLUMN);
				String name = cursor.getString(GROCERY_LIST_NAME_COLUMN);
				int resIconId = cursor.getInt(GROCERY_LIST_RES_ICON_ID_COLUMN);
				long millis = cursor.getLong(GROCERY_LIST_DATE_COLUMN); 
				result = new GroceryList(name, resIconId, id, new Date(millis));
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;  
	}
	
	public long insertShoppingListItem(ShoppingListItem shoppingList) {
	    ContentValues newShoppingList = new ContentValues();
	    newShoppingList.put(SHOPPING_LIST_ITEM_KEY_GROCERY_LIST_ID, shoppingList.getGroceryListId());
	    newShoppingList.put(SHOPPING_LIST_ITEM_KEY_PRODUCT_ID, shoppingList.getProductId());
	    newShoppingList.put(SHOPPING_LIST_ITEM_KEY_COUNT, shoppingList.getCount());
	    newShoppingList.put(SHOPPING_LIST_ITEM_IS_PURCHASED, shoppingList.getIsPurchased());
	    return mGroceryListDb.insert(SHOPPING_LIST_ITEMS_TABLE, null, newShoppingList);
	}
	
	public boolean updateShoppingListItem(ShoppingListItem shoppingList) {
		ContentValues updateShoppingList = new ContentValues();
		updateShoppingList.put(SHOPPING_LIST_ITEM_KEY_GROCERY_LIST_ID, shoppingList.getGroceryListId());
		updateShoppingList.put(SHOPPING_LIST_ITEM_KEY_PRODUCT_ID, shoppingList.getProductId());
		updateShoppingList.put(SHOPPING_LIST_ITEM_KEY_COUNT, shoppingList.getCount());
		updateShoppingList.put(SHOPPING_LIST_ITEM_IS_PURCHASED, shoppingList.getIsPurchased());
		return mGroceryListDb.update(SHOPPING_LIST_ITEMS_TABLE, 
									 updateShoppingList, 
									 SHOPPING_LIST_ITEM_KEY_ID + "=" + shoppingList.getId(), 
									 null) > 0;
	}
	
	public boolean removeShoppingListItem(long shoppingListRowIndex) {
		return mGroceryListDb.delete(SHOPPING_LIST_ITEMS_TABLE, 
									 SHOPPING_LIST_ITEM_KEY_ID + "=" + shoppingListRowIndex, 
									 null) > 0;
	}
	
	public boolean removeShoppingListItem(long productId, long groceryListId) {
		return mGroceryListDb.delete(SHOPPING_LIST_ITEMS_TABLE, 
									 SHOPPING_LIST_ITEM_KEY_PRODUCT_ID + "=" + productId +
									 " AND " + SHOPPING_LIST_ITEM_KEY_GROCERY_LIST_ID + "=" + groceryListId,
									 null) > 0;
	}
	
	public boolean removeAllShoppingListItemsByGroceryList(long groceryListId) {
		return mGroceryListDb.delete(SHOPPING_LIST_ITEMS_TABLE, 
									 SHOPPING_LIST_ITEM_KEY_GROCERY_LIST_ID + "=" + groceryListId, 
									 null) > 0;
	}
	
	public boolean removeAllShoppingListItemByProductId(long productId) {
		return mGroceryListDb.delete(SHOPPING_LIST_ITEMS_TABLE, 
									 SHOPPING_LIST_ITEM_KEY_PRODUCT_ID + "=" + productId,
									 null) > 0;
	}
	
	public Cursor getAllShoppingListItemsCursor(long productId, long groceryListId, boolean hidePurchasedItems) {
		String  selection = null;
		
		String hideString = "";
		if (hidePurchasedItems) {
			hideString = " AND " + SHOPPING_LIST_ITEM_IS_PURCHASED + " = \'0\'";
		}
	
		if (productId == GroceryListUtils.UNKNOWN_ID) {
			selection = SHOPPING_LIST_ITEM_KEY_GROCERY_LIST_ID + "=" + groceryListId + hideString;
		} else {
			selection = SHOPPING_LIST_ITEM_KEY_PRODUCT_ID + "=" + productId + " AND " + SHOPPING_LIST_ITEM_KEY_GROCERY_LIST_ID + "=" + groceryListId;
		}
		return mGroceryListDb.query(true,
									SHOPPING_LIST_ITEMS_TABLE, 
									new String[] { SHOPPING_LIST_ITEM_KEY_ID,  
												   SHOPPING_LIST_ITEM_KEY_GROCERY_LIST_ID,
												   SHOPPING_LIST_ITEM_KEY_PRODUCT_ID,
												   SHOPPING_LIST_ITEM_KEY_COUNT, 
												   SHOPPING_LIST_ITEM_IS_PURCHASED }, 
									selection, null, null, null, null, null);
	}
	
	public Cursor getAllPurchasedShoppingListItemsCursor(long groceryListId) {
		String  selection = SHOPPING_LIST_ITEM_KEY_GROCERY_LIST_ID + "=" + groceryListId + " AND " + SHOPPING_LIST_ITEM_IS_PURCHASED + " = \'1\'";

		return mGroceryListDb.query(true,
									SHOPPING_LIST_ITEMS_TABLE, 
									new String[] { SHOPPING_LIST_ITEM_KEY_ID,  
												   SHOPPING_LIST_ITEM_KEY_GROCERY_LIST_ID,
												   SHOPPING_LIST_ITEM_KEY_PRODUCT_ID,
												   SHOPPING_LIST_ITEM_KEY_COUNT, 
												   SHOPPING_LIST_ITEM_IS_PURCHASED }, 
									selection, null, null, null, null, null);
	}
	 
	public Cursor setCursorToShoppingListItem(long shoppingListRowIndex) {
		Cursor result = mGroceryListDb.query(true, 
											 SHOPPING_LIST_ITEMS_TABLE, 
												new String[] { SHOPPING_LIST_ITEM_KEY_ID,  
													   		   SHOPPING_LIST_ITEM_KEY_GROCERY_LIST_ID,
													   		   SHOPPING_LIST_ITEM_KEY_PRODUCT_ID,
															   SHOPPING_LIST_ITEM_KEY_COUNT, 
															   SHOPPING_LIST_ITEM_IS_PURCHASED },
											 SHOPPING_LIST_ITEM_KEY_ID + "=" + shoppingListRowIndex, 
											 null, null, null, null, null);

		if (result != null &&
			((result.getCount() == 0) || !result.moveToFirst())) {
			result.close();
			result = null;
		}
		return result;
	}
	
	public ShoppingListItem getShoppingListItem(long productId, long groceryListId) {
		Cursor cursor = null;
		ShoppingListItem result = null;
		try {
			String selection = SHOPPING_LIST_ITEM_KEY_PRODUCT_ID + "=" + productId + " AND " + SHOPPING_LIST_ITEM_KEY_GROCERY_LIST_ID + "=" + groceryListId;
			cursor = mGroceryListDb.query(true,
										  SHOPPING_LIST_ITEMS_TABLE, 
										  new String[] { SHOPPING_LIST_ITEM_KEY_ID,  
														 SHOPPING_LIST_ITEM_KEY_GROCERY_LIST_ID,
														 SHOPPING_LIST_ITEM_KEY_PRODUCT_ID,
														 SHOPPING_LIST_ITEM_KEY_COUNT, 
														 SHOPPING_LIST_ITEM_IS_PURCHASED }, 
										  selection, null, null, null, null, null);
			if ((cursor.getCount() != 0) && cursor.moveToFirst()) {
				long id = cursor.getLong(SHOPPING_LIST_ITEM_ID_COLUMN);
				int count = cursor.getInt(SHOPPING_LIST_ITEM_COUNT_COLUMN);
				boolean isPurchased = false;
				int isPurchasedInt = cursor.getInt(SHOPPING_LIST_ITEM_IS_PURCHASED_COLUMN);
				if (isPurchasedInt > 0) {
					isPurchased = true;
				}
				result = new ShoppingListItem(id, 
											  productId, 
											  groceryListId, 
											  count, 
											  isPurchased);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;  
	}
	
	public ShoppingListItem getShoppingListItem(long shoppingListId) {
		Cursor cursor = null;
		ShoppingListItem result = null;
		try {
			String selection = SHOPPING_LIST_ITEM_KEY_ID + "=" + shoppingListId;
			cursor = mGroceryListDb.query(true,
										  SHOPPING_LIST_ITEMS_TABLE, 
										  new String[] { SHOPPING_LIST_ITEM_KEY_ID,  
														 SHOPPING_LIST_ITEM_KEY_GROCERY_LIST_ID,
														 SHOPPING_LIST_ITEM_KEY_PRODUCT_ID,
														 SHOPPING_LIST_ITEM_KEY_COUNT, 
														 SHOPPING_LIST_ITEM_IS_PURCHASED }, 
										  selection, null, null, null, null, null);
			if ((cursor.getCount() != 0) && cursor.moveToFirst()) {
				long productId = cursor.getLong(SHOPPING_LIST_ITEM_PRODUCT_ID_COLUMN);
				long groceryListId = cursor.getLong(SHOPPING_LIST_ITEM_GROCERY_LIST_ID_COLUMN);
				int count = cursor.getInt(SHOPPING_LIST_ITEM_COUNT_COLUMN);
				boolean isPurchased = false;
				int isPurchasedInt = cursor.getInt(SHOPPING_LIST_ITEM_IS_PURCHASED_COLUMN);
				if (isPurchasedInt > 0) {
					isPurchased = true;
				}
				result = new ShoppingListItem(shoppingListId, 
						productId, 
						groceryListId, 
						count, 
						isPurchased);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;  
	}
	
	public long insertProduct(Product product) {
	    ContentValues newProduct = new ContentValues();
	    newProduct.put(PRODUCT_KEY_NAME, product.getName());
	    newProduct.put(PRODUCT_KEY_RES_ICON_ID, product.getResIconId());
	    newProduct.put(PRODUCT_KEY_PRIORITY, product.getPriority());
	    newProduct.put(PRODUCT_KEY_IS_USER_CREATED, product.getIsUserCreated());
	    return mGroceryListDb.insert(PRODUCTS_TABLE, null, newProduct);
	}
	
	public boolean updateProduct(Product product) {
	    ContentValues updateProduct = new ContentValues();
	    updateProduct.put(PRODUCT_KEY_NAME, product.getName());
	    updateProduct.put(PRODUCT_KEY_RES_ICON_ID, product.getResIconId());
	    updateProduct.put(PRODUCT_KEY_PRIORITY, product.getPriority());
	    updateProduct.put(PRODUCT_KEY_IS_USER_CREATED, product.getIsUserCreated());
	    return mGroceryListDb.update(PRODUCTS_TABLE, 
	    							 updateProduct, 
	    							 PRODUCT_KEY_ID + "=" + product.getId(), 
	    							 null) > 0;
	}
	
	public boolean removeProduct(long productRowIndex) {
		return mGroceryListDb.delete(PRODUCTS_TABLE, 
									 PRODUCT_KEY_ID + "=" + productRowIndex, 
									 null) > 0;
	}
	
	public Cursor getAllProductsCursor(int sortBy, boolean descending) {
		String orderBy = PRODUCT_KEY_NAME;
		switch (sortBy) {
		case GroceryListUtils.SORT_PRODUCTS_BY_NAME:
			if (descending) {
				orderBy = PRODUCT_KEY_NAME + " DESC";
			}
			break;
		case GroceryListUtils.SORT_PRODUCTS_BY_POPULARITY:
			orderBy = PRODUCT_KEY_PRIORITY;
			if (descending) {
				orderBy = PRODUCT_KEY_PRIORITY + " DESC";
			}
			break;
		}
		Cursor cursor = mGroceryListDb.query(PRODUCTS_TABLE, 
											 new String[] { PRODUCT_KEY_ID, 
															PRODUCT_KEY_NAME,
															PRODUCT_KEY_RES_ICON_ID,
															PRODUCT_KEY_PRIORITY,
															PRODUCT_KEY_IS_USER_CREATED }, 
											 null, null, null, null, orderBy);
		if (cursor != null && 
			((cursor.getCount() == 0) || !cursor.moveToFirst())) {
			cursor.close();
			cursor = null;
		}
		return cursor;
	}
	
	public Cursor getAllProductsCursor(String filterBy) {
		String whereSelection = PRODUCT_KEY_NAME +" LIKE " + "\"%" + filterBy + "%\"";
		Cursor cursor = mGroceryListDb.query(PRODUCTS_TABLE, 
											 new String[] { PRODUCT_KEY_ID, 
															PRODUCT_KEY_NAME,
															PRODUCT_KEY_RES_ICON_ID,
															PRODUCT_KEY_IS_USER_CREATED }, 
											 whereSelection, null, null, null, null);
		if (cursor != null &&
			((cursor.getCount() == 0) || !cursor.moveToFirst())) {
			cursor.close();
			cursor = null;
		}
		return cursor;
	}
	 
	public Cursor setCursorToProduct(long productRowIndex) { 
		Cursor result = mGroceryListDb.query(true, 
											 PRODUCTS_TABLE, 
											 new String[] { PRODUCT_KEY_ID, 
											 			    PRODUCT_KEY_NAME,
														    PRODUCT_KEY_RES_ICON_ID,
														    PRODUCT_KEY_PRIORITY,
														    PRODUCT_KEY_IS_USER_CREATED },
														    PRODUCT_KEY_ID + "=" + productRowIndex, 
											 null, null, null, null, null);
		if (result != null && 
			((result.getCount() == 0) || !result.moveToFirst())) {
			result.close();
			result = null;
		}
		return result;
	}

	public Product getProduct(long productRowIndex) {
		Cursor cursor = null;
		Product result = null;
		try {
			cursor = mGroceryListDb.query(true, 
										  PRODUCTS_TABLE, 
										  new String[] { PRODUCT_KEY_ID, 
														 PRODUCT_KEY_NAME,
														 PRODUCT_KEY_RES_ICON_ID, 
														 PRODUCT_KEY_PRIORITY,
														 PRODUCT_KEY_IS_USER_CREATED },
														 PRODUCT_KEY_ID + "=" + productRowIndex, 
										 null, null, null, null, null);
			if ((cursor.getCount() != 0) && cursor.moveToFirst()) {
				long productId = cursor.getLong(PRODUCT_ID_COLUMN);
				String name = cursor.getString(PRODUCT_NAME_COLUMN);
				int resIconId = cursor.getInt(PRODUCT_RES_ICON_ID_COLUMN);
				int priority = cursor.getInt(PRODUCT_USER_PRIORITY_COLUMN);
				int isUserCreated = cursor.getInt(PRODUCT_USER_IS_USER_CREATED_COLUMN);
				boolean userCreated = false;
				if (isUserCreated == GroceryListUtils.PRODUCT_USER_CREATED) {
					userCreated = true;
				}
				result = new Product(name, resIconId, productId, priority, userCreated);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;  
	}
	
	public Product getProduct(String productName) {
		Cursor cursor = null;
		Product result = null;
		try {
			cursor = mGroceryListDb.query(true, 
										  PRODUCTS_TABLE, 
										  new String[] { PRODUCT_KEY_ID, 
														 PRODUCT_KEY_NAME,
														 PRODUCT_KEY_RES_ICON_ID, 
														 PRODUCT_KEY_PRIORITY,
														 PRODUCT_KEY_IS_USER_CREATED },
														 PRODUCT_KEY_NAME + " LIKE " + "\"" + productName + "\"", 
										 null, null, null, null, null);
			if ((cursor.getCount() != 0) && cursor.moveToFirst()) {
				long productId = cursor.getLong(PRODUCT_ID_COLUMN);
				String name = cursor.getString(PRODUCT_NAME_COLUMN);
				int resIconId = cursor.getInt(PRODUCT_RES_ICON_ID_COLUMN);
				int priority = cursor.getInt(PRODUCT_USER_PRIORITY_COLUMN);
				int isUserCreated = cursor.getInt(PRODUCT_USER_IS_USER_CREATED_COLUMN);
				boolean userCreated = false;
				if (isUserCreated == GroceryListUtils.PRODUCT_USER_CREATED) {
					userCreated = true;
				}
				result = new Product(name, resIconId, productId, priority, userCreated);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;  
	}
	
	public Icon getIcon(long iconRowIndex) {
		Cursor cursor = null;
		Icon result = null;
		try {
			cursor = mGroceryListDb.query(true, 
										  ICONS_TABLE, 
										  new String[] { USER_ICON_KEY_ID, 
														 USER_ICON_KEY_DATA,
														 USER_ICON_KEY_UNIQUE_HASH,  },
														 USER_ICON_KEY_ID + "=" + iconRowIndex, 
										 null, null, null, null, null);
			if ((cursor.getCount() != 0) && cursor.moveToFirst()) {
				long iconId = cursor.getLong(USER_ICON_ID_COLUMN);
				byte[] bb = cursor.getBlob(USER_ICON_DATA_COLUMN);
				Bitmap bitmap = null;
				try {
					bitmap = BitmapFactory.decodeByteArray(bb, 0, bb.length);
				} catch (Exception e) {
					if(DebugUtils.printDebug) {
						Log.e(TAG, "Could not get icon id " + iconRowIndex + " ", e);
					}
				}
				result = new Icon(iconId, bitmap);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;  
	}
	
	
	public long insertOrUpdateIcon(Icon icon) {
	    ContentValues iconContentValues = new ContentValues();
	    Bitmap bitmap = icon.getBitmap();
	    long resultIconId = icon.getId();
	    if (bitmap != null) {
	    	ByteArrayOutputStream stream = new ByteArrayOutputStream();
	    	bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
	    	byte[] byteArray = stream.toByteArray();
	    	if (icon.getId() != GroceryListUtils.UNKNOWN_ID) {
	    		iconContentValues.put(USER_ICON_KEY_ID, icon.getId());
	    	} 
	    	iconContentValues.put(USER_ICON_KEY_DATA, byteArray);
	    	try {
	    		resultIconId = mGroceryListDb.insertOrThrow(ICONS_TABLE, null, iconContentValues);
			} catch (SQLException e) {
				if(mGroceryListDb.isOpen()){
					int numOfRowsAffected = mGroceryListDb.update(ICONS_TABLE, iconContentValues,
							BaseColumns._ID + "=?", new String[] { Long.toString(iconContentValues
									.getAsLong(BaseColumns._ID)) });
					if (numOfRowsAffected == 0) {
						if(DebugUtils.printDebug) {
							Log.e(TAG, "Could not save icon.", e);
						}
					}
				}
			}
	    }
	    return resultIconId;
	}
}
