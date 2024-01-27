package com.etcapps.grocerylist;

import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.etcapps.grocerylist.engine.GroceryList;
import com.etcapps.grocerylist.engine.GroceryListEngine;
import com.etcapps.grocerylist.engine.GroceryListUtils;

public class AddRenameGroceryListActivity extends Activity implements TextWatcher {

	public static final String EXTRA_KEY_GROCERY_LIST_ID = "com.etcapps.grocerylist.android.GroceryListId";
	public static final String EXTRA_KEY_CREATE_LIST_BY_VOICE_FLAG = "com.etcapps.grocerylist.android.CreateByVoiceFlag";
	
    private EditText mGroceryListNameText;
    private Button mAddRenameButton;
    private ImageButton mNewListButtonByVoice;
    private GroceryList mGroceryList;
	
    public static void renameGroceryList(Activity activity, long groceryListId, int requestCode) {
        Intent intent = new Intent(activity, AddRenameGroceryListActivity.class);
        intent.putExtra(EXTRA_KEY_GROCERY_LIST_ID, groceryListId);
        activity.startActivityForResult(intent, requestCode);
    }
    
    public static void addGroceryList(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, AddRenameGroceryListActivity.class);
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
        	mNewListButtonByVoice.setEnabled(false);
        } else {
        	mAddRenameButton.setEnabled(true);
        	mNewListButtonByVoice.setEnabled(true);
        }
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // do nothing
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // do nothing        
    }
    
    private void readArgumentsFromIntent() {
    	if(getIntent().hasExtra(EXTRA_KEY_GROCERY_LIST_ID)) {
    		long groceryListId = getIntent().getLongExtra(EXTRA_KEY_GROCERY_LIST_ID, GroceryListUtils.UNKNOWN_ID);
    		mGroceryList = getEngine().getGroceryList(groceryListId);
    	} 
        if (mGroceryList != null) {
        	setTitle(R.string.title_rename_grocery_list);
        } else {
        	setTitle(R.string.title_add_grocery_list);
        }
    }

    private void setupViews() {
    	setContentView(R.layout.add_rename_grocery_list);

    	mAddRenameButton = (Button) findViewById(R.id.button_grocery_list_add);
    
    	if(mGroceryList != null) {
    		mAddRenameButton.setText(R.string.button_rename_grocery_list);
    	}
    	
    	mAddRenameButton.setEnabled(false);
    	mAddRenameButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	addList(false);
            }
    	});
    	
    	mNewListButtonByVoice = (ImageButton) findViewById(R.id.button_new_list_by_voice);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() != 0 &&
        	mGroceryList == null) {
        	mNewListButtonByVoice.setEnabled(false);
        	mNewListButtonByVoice.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                	addList(true);
                }
        	});
        } else {
        	mNewListButtonByVoice.setVisibility(View.GONE);
        }

        Button cancelButton = (Button) findViewById(R.id.button_grocery_list_cancel);
        cancelButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        mGroceryListNameText = (EditText) findViewById(R.id.grocery_list_name);
        mGroceryListNameText.addTextChangedListener(this);
        if (mGroceryList != null) {
    		mGroceryListNameText.setText(mGroceryList.getName());
        }
    }
    
    private void rename(String groceryListName) {
		mGroceryList.setName(groceryListName);
		mGroceryList.setDateModified(new Date(System.currentTimeMillis()));
		getEngine().updateGroceryListItem(mGroceryList);
		setResult(Activity.RESULT_OK);
    	finish();
    }
    
    private void add(String groceryListName, boolean createByVoice) {
    	GroceryList groceryList = new GroceryList(groceryListName, 
    											  GroceryListUtils.getRandomGroceryListIcon(this), 
    											  new Date(System.currentTimeMillis()));
    	long groceryListId = getEngine().addGroceryListItem(groceryList);
    	Intent data = new Intent();
    	data.putExtra(EXTRA_KEY_GROCERY_LIST_ID, groceryListId);
    	if (createByVoice) {
    		data.putExtra(EXTRA_KEY_CREATE_LIST_BY_VOICE_FLAG, createByVoice);
    	}
    	setResult(Activity.RESULT_OK, data);
    	finish();
    }
    
    private void showProductAlreadyExist(String groceryListName) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(AddRenameGroceryListActivity.this);
		dialog.setMessage(getString(R.string.dialog_grocery_list_already_exists, groceryListName));
		dialog.setPositiveButton(android.R.string.ok, null);
		dialog.create().show();
    }
    
    private void addList(boolean createByVoice) {
    	String groceryListNameText = mGroceryListNameText.getText().toString();
    	final String groceryListName = groceryListNameText.trim();
    	if (groceryListName.contains(GroceryListUtils.NAME_FORBIDDEN_CHARACTER)) {
    		GroceryListUtils.displayForbiddenCharacterDialog(AddRenameGroceryListActivity.this);
    	} else {
        	GroceryList sameNameGroceryList = getEngine().getGroceryList(groceryListName);
        	if (mGroceryList != null) {
            	if (sameNameGroceryList != null &&
            		mGroceryList.getId() != sameNameGroceryList.getId()) {
            		showProductAlreadyExist(groceryListName);
            	} else {
            		rename(groceryListName);
            	}
        	} else {
        		if (sameNameGroceryList != null) {
        			showProductAlreadyExist(groceryListName);
        		} else {
        			add(groceryListName, createByVoice);
        		}
        	}
    	}
    }
    
    GroceryListEngine getEngine() {
    	return ((GroceryListApplication)getApplication()).getEngine();
    }

}
