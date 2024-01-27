package com.etcapps.grocerylist.engine;

public class Product {

	private String mName;
	private int mResIconId;
	private long mId;
	private int mOriginalPriority;
	private int mPriority;
	private boolean mIsUserCreated;

	public String getName() {
		return mName;
	}
	
	public void setName(String newName) {
		mName = newName;
	}
	
	public int getResIconId() {
		return mResIconId;
	}
	
	public void setResIconId(long iconId) {
		mResIconId = (int)iconId;
	}
	
	public long getId() {
		return mId;
	}
	
	public int getPriority() {
		return mPriority;
	}
	
	public int getIsUserCreated() {
		if (mIsUserCreated) {
			return GroceryListUtils.PRODUCT_USER_CREATED;
		} else {
			return GroceryListUtils.PRODUCT_NOT_USER_CREATED;
		}
	}
	
	public void resetPriority() {
		mPriority = mOriginalPriority;
	}
	
	public void increasePriority(int increaseBy) {
		mPriority += increaseBy;
	}
	
	public Product(String name, int resIconId, boolean isUserCreated) {
		this(name, resIconId, GroceryListUtils.UNKNOWN_ID, 0, isUserCreated);
	}
	
	public Product(String name, int resIconId, long id, int priority, boolean isUserCreated) {
		mName = name;
		mResIconId = resIconId;
		mId = id;
		mOriginalPriority = priority;
		mPriority = mOriginalPriority;
		mIsUserCreated = isUserCreated;
	}
}
