package com.etcapps.grocerylist.engine;

import java.util.Random;

import android.app.AlertDialog;
import android.content.Context;

import com.etcapps.grocerylist.R;

public class GroceryListUtils {
	
	public final static int UNKNOWN_ID = -1;
	
	// products sort options
	public final static int SORT_PRODUCTS_BY_NAME = 0;
	public final static int SORT_PRODUCTS_BY_POPULARITY = 1;
	public final static int SORT_GROCERY_LISTS_BY_NAME = 2;
	public final static int SORT_GROCERY_LISTS_BY_DATE = 3;
	
	public final static int PRODUCT_USER_CREATED = 1;
	public final static int PRODUCT_NOT_USER_CREATED = 0;
	
	public final static String NAME_FORBIDDEN_CHARACTER = "\"";
	
	public static int getRandomGroceryListIcon(Context context) {
		Random randomGenerator = new Random();
		int random = randomGenerator.nextInt(5);
		switch (random) {
		case 0:
			return R.drawable.default_grocery_list_icon_0;
		case 1:
			return R.drawable.default_grocery_list_icon_1;
		case 2:
			return R.drawable.default_grocery_list_icon_2;
		case 3:
			return R.drawable.default_grocery_list_icon_3;
		case 4:
			return R.drawable.default_grocery_list_icon_4;
		default:
			return R.drawable.default_grocery_list_icon;
		}
		
	}
	
	public static void displayForbiddenCharacterDialog(Context context) {
		AlertDialog.Builder forbidenCharDialog = new AlertDialog.Builder(context);
		forbidenCharDialog.setMessage(R.string.name_forbidden_character);
		forbidenCharDialog.setPositiveButton(android.R.string.ok, null);
		forbidenCharDialog.create().show();
	}
	
	private GroceryListUtils() {
	}

}
