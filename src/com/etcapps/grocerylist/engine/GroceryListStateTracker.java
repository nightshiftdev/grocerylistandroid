package com.etcapps.grocerylist.engine;

import java.util.Collection;
import java.util.HashMap;

public class GroceryListStateTracker {
	private static HashMap<Long, Integer> mGroceryListStateMap = new HashMap<Long, Integer>();
	
    public static boolean isListStateDirty() {
    	boolean isListDirty = false;
    	Collection<Integer> stateValues = mGroceryListStateMap.values();
    	if (stateValues != null &&
    		stateValues.size() > 0) {
    		for (int value : stateValues) {
    			if (value != 0) {
    				isListDirty = true;
    				break;
    			}
    		}
    	}
    	mGroceryListStateMap.clear();
    	return isListDirty;
    }
    
    public static void updateListState(long productId, int stateCount) {
		Integer value = mGroceryListStateMap.get(productId);
		if (value != null) {
			mGroceryListStateMap.put(productId, value + stateCount);
		} else {
			mGroceryListStateMap.put(productId, stateCount);
		}
    }
}
