package com.etcapps.grocerylist.engine;

import android.graphics.Bitmap;

public class Icon {
	private long mId;
	private Bitmap mBitmap;
	
	public long getId() {
		return mId;
	}
	
	public Bitmap getBitmap() {
		return mBitmap;
	}
	public void setBitmap(Bitmap bitmap) {
		this.mBitmap = bitmap;
	}
	
	public Icon(long id, Bitmap bitmap) {
		mId = id;
		mBitmap = bitmap;
	}
	
	public Icon(Bitmap bitmap) {
		mId = 0;
		mBitmap = bitmap;
	}
}
