package com.etcapps.grocerylist.engine;


public class ShoppingListItem { 

	private long mId;
	private long mProductId;
	private long mGroceryListId;
	private int mCount;
	private boolean mIsPurchased;
	
	public long getId() {
		return mId;
	}
	
    public long getProductId() {
    	return mProductId;
    }
    
    public long getGroceryListId() {
    	return mGroceryListId;
    }
    
    public int getCount() {
    	return mCount;
    }
    
    public boolean getIsPurchased() {
    	return mIsPurchased;
    }
    
    public void setIsPurchased(boolean isPurchased) {
    	mIsPurchased = isPurchased;
    }
    
    public void increaseCount() {
    	mCount++;
    }
    
    public void decreaseCount() {
    	if (mCount > 0) {
    		mCount--;
    	}	
    }
    
	public ShoppingListItem(long id, long productId, long groceryListId) {
		mId = id;
		mProductId = productId;
		mGroceryListId = groceryListId;
		mCount = 1;
		mIsPurchased = false;
	}
	
	public ShoppingListItem(long id, long productId, long groceryListId, int count, boolean isPurchased) {
		mId = id;
		mProductId = productId;
		mGroceryListId = groceryListId;
		mCount = count;
		mIsPurchased = isPurchased;
	}
}

