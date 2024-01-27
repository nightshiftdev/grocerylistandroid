package com.etcapps.grocerylist.engine;

import java.util.Date;

public class GroceryList { 
	
	private String mName;
	private int mResIconId;
	private long mId;
	private Date mDateModified;
	
	public String getName() {
		return mName;
	}
	
	public void setName(String newName) {
		mName = newName;
	}
	
	public long getResIconId() {
		return mResIconId;
	}
	
	public long getId() {
		return mId;
	}
	
	public Date getDateModified() {
		return mDateModified;
	}
	
	public void setDateModified(Date dateModified) {
		mDateModified = dateModified;
	}

	public GroceryList(String name, Date dateModified) {
		this(name, GroceryListUtils.UNKNOWN_ID, GroceryListUtils.UNKNOWN_ID, dateModified);
	}
	
	public GroceryList(String name, int resIconId, Date dateModified) {
		this(name, resIconId, GroceryListUtils.UNKNOWN_ID, dateModified);
	}
	
	public GroceryList(String name, int resIconId, long id, Date dateModified) {
		mName = name;
		mResIconId = resIconId;
		mId = id;
		mDateModified = dateModified;
	}
}
