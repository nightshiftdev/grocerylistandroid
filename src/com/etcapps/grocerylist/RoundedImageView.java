package com.etcapps.grocerylist;

import java.lang.reflect.Method;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.etcapps.grocerylist.engine.DebugUtils;

public class RoundedImageView extends ImageView {
	private static final String TAG = RoundedImageView.class.getSimpleName();
	private float mCornerRadius;
	private float mBackgroundPaddingForAlternateStates;
	
	private void CommonInit() {
		Class<? extends View> view = getClass();
        Class<?> partypes[] = new Class[2];
        partypes[0] = Integer.TYPE;
        partypes[1] = Paint.class;
		try {
			Method meth = view.getMethod("setLayerType", partypes);
            Object arglist[] = new Object[2];
            // View.LAYER_TYPE_SOFTWARE = 1;
            arglist[0] = new Integer(1);
            arglist[1] = null;
			meth.invoke(this, arglist);
		} catch (Throwable e) {
			if (DebugUtils.printDebug) {
				Log.i(TAG, "Could not invoke setLayerType", e);
			}
		}
		mCornerRadius = 8.0f;
		mBackgroundPaddingForAlternateStates = 3.0f;
	}
	
	public RoundedImageView(Context context) {
		super(context);
		CommonInit();
	}

	public RoundedImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		CommonInit();
	}

	public RoundedImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		CommonInit();
	}
	
	public void setCornerRadius(float newRadius) {
		mCornerRadius = newRadius;
		this.invalidate(); 
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (mCornerRadius > 0) {
			Path clipPath = new Path();
			int w = this.getWidth();
			int h = this.getHeight();
			clipPath.addRoundRect(new RectF(mBackgroundPaddingForAlternateStates,
					mBackgroundPaddingForAlternateStates,
					w-mBackgroundPaddingForAlternateStates,
					h-mBackgroundPaddingForAlternateStates), 
					mCornerRadius, mCornerRadius, Path.Direction.CW);
			canvas.clipPath(clipPath);
		}
		try {
			super.onDraw(canvas);
		} catch (Exception e) {
			if (DebugUtils.printDebug) {
				Log.w(TAG, "Could not draw bitmap.", e);
			}
		}
	}
}
