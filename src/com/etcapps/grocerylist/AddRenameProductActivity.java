package com.etcapps.grocerylist;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.etcapps.grocerylist.engine.GroceryListEngine;
import com.etcapps.grocerylist.engine.GroceryListUtils;
import com.etcapps.grocerylist.engine.Product;
import com.etcapps.grocerylist.R;

public class AddRenameProductActivity extends Activity implements TextWatcher {
	
	public static final String EXTRA_KEY_PRODUCT_ID = "com.etcapps.grocerylist.android.ProuductId";
	public static final String EXTRA_KEY_PRODUCT_NAME = "com.etcapps.grocerylist.android.ProuductName";
	
    private EditText mProductNameText;
    private Button mAddRenameButton;
    private Product mProduct;
    private String mNewProductName;
    
    public static void renameProduct(Activity activity, long productId, int requestCode) {
        Intent intent = new Intent(activity, AddRenameProductActivity.class);
        intent.putExtra(EXTRA_KEY_PRODUCT_ID, productId);
        activity.startActivityForResult(intent, requestCode);
    }
    
    public static void addProduct(Activity activity, String productName, int requestCode) {
        Intent intent = new Intent(activity, AddRenameProductActivity.class);
        intent.putExtra(EXTRA_KEY_PRODUCT_NAME, productName);
        activity.startActivityForResult(intent, requestCode);
    }
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readArgumentsFromIntent();
        setupViews();
    }
    
    public void afterTextChanged(Editable e) {
        String listName = e.toString().trim();
        if (listName.length() == 0) {
        	mAddRenameButton.setEnabled(false);
        } else {
        	mAddRenameButton.setEnabled(true);
        }
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // do nothing
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // do nothing        
    }
    
    private void readArgumentsFromIntent() {
    	if(getIntent().hasExtra(EXTRA_KEY_PRODUCT_ID)) {
    		long productId = getIntent().getLongExtra(EXTRA_KEY_PRODUCT_ID, GroceryListUtils.UNKNOWN_ID);
    		mProduct = getEngine().getProduct(productId);
    	} 
        if (mProduct != null) {
        	setTitle(R.string.title_rename_product);
        } else {
        	setTitle(R.string.title_add_product);
        }
        if (getIntent().hasExtra(EXTRA_KEY_PRODUCT_NAME)) {
        	mNewProductName = getIntent().getStringExtra(EXTRA_KEY_PRODUCT_NAME);
        }
    }
    
    private void setupViews() {
        setContentView(R.layout.add_rename_product);
        
    	mAddRenameButton = (Button) findViewById(R.id.button_product_add);
        
    	if(mProduct != null) {
    		mAddRenameButton.setText(R.string.button_rename_grocery_list);
    	}

    	mAddRenameButton.setEnabled(false);
    	mAddRenameButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	String productNameText = mProductNameText.getText().toString();
            	final String productName = productNameText.trim();
            	if (productName.contains(GroceryListUtils.NAME_FORBIDDEN_CHARACTER)) {
            		GroceryListUtils.displayForbiddenCharacterDialog(AddRenameProductActivity.this);
            	} else {
	            	Product sameNameProduct = getEngine().getProduct(productName);
	            	if (mProduct != null) {
	                	if (sameNameProduct != null &&
	                		mProduct.getId() != sameNameProduct.getId()) {
	                		showProductAlreadyExist(productName);
	                	} else {
	                		rename(productName);
	                	}
	            	} else {
	            		if (sameNameProduct != null) {
	            			showProductAlreadyExist(productName);
	            		} else {
	            			add(productName);
	            		}
	            	}
            	}
            }
        });	
    	
        Button cancelButton = (Button) findViewById(R.id.button_product_cancel);
        cancelButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        mProductNameText = (EditText) findViewById(R.id.product_name);
        mProductNameText.addTextChangedListener(this);
        if (mProduct != null) {
    		mProductNameText.setText(mProduct.getName());
        } else if (!TextUtils.isEmpty(mNewProductName)) {
        	mProductNameText.setText(mNewProductName);
        }
    }
    
    private void rename(String productName) {
		mProduct.setName(productName);
		getEngine().updateProduct(mProduct);
    	Intent data = new Intent();
    	data.putExtra(EXTRA_KEY_PRODUCT_ID, mProduct.getId());
    	setResult(Activity.RESULT_OK, data);
    	finish();
    }
    
    private void add(String productName) {
    	Product product = new Product(productName, R.drawable.default_product_icon, true);
    	long productId = getEngine().addProduct(product);
    	Intent data = new Intent();
    	data.putExtra(EXTRA_KEY_PRODUCT_ID, productId);
    	setResult(Activity.RESULT_OK, data);
    	finish();
    }
    
    private void showProductAlreadyExist(String productName) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(AddRenameProductActivity.this);
		dialog.setMessage(getString(R.string.dialog_product_already_exists, productName));
		dialog.setPositiveButton(android.R.string.ok, null);
		dialog.create().show();
    }

    GroceryListEngine getEngine() {
    	return ((GroceryListApplication)getApplication()).getEngine();
    }
}
