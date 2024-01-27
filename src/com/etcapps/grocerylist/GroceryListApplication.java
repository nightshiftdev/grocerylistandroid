package com.etcapps.grocerylist;

import android.app.Application;

import com.etcapps.grocerylist.engine.GroceryListEngine;
import com.etcapps.grocerylist.engine.GroceryListProvider;

public class GroceryListApplication extends Application {

	private GroceryListProvider mDb;
	private GroceryListEngine mEngine;
	
	@Override
	public void onCreate() {
		super.onCreate();
		mDb = new GroceryListProvider(this);
		mDb.open();
		mEngine = new GroceryListEngine(this, mDb);
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		mDb.close();
	}
	
	public GroceryListEngine getEngine() {
		return mEngine;
	}
}
