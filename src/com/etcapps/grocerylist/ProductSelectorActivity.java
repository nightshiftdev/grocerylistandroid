package com.etcapps.grocerylist;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.etcapps.grocerylist.engine.DebugUtils;
import com.etcapps.grocerylist.engine.GroceryList;
import com.etcapps.grocerylist.engine.GroceryListEngine;
import com.etcapps.grocerylist.engine.GroceryListProvider;
import com.etcapps.grocerylist.engine.GroceryListStateTracker;
import com.etcapps.grocerylist.engine.GroceryListUtils;
import com.etcapps.grocerylist.engine.Icon;
import com.etcapps.grocerylist.engine.Product;
import com.etcapps.grocerylist.engine.ShoppingListItem;

public class ProductSelectorActivity extends Activity implements TextWatcher {

	public static final String EXTRA_KEY_GROCERY_LIST_ID = "com.etcapps.grocerylist.android.GroceryListId";
	public static final String EXTRA_KEY_IS_LIST_DIRTY = "com.etcapps.grocerylist.android.IsListDirty";

	private static final String TAG = ProductSelectorActivity.class.getSimpleName();

	private static final int REQUEST_CODE_RENAME_PRODUCT = 0;
	private static final int REQUEST_CODE_ADD_PRODUCT = 1;
	private static final int REQUEST_CODE_SEARCH_PRODUCT_BY_VOICE = 2;
	private static final int REQUEST_CODE_CAMERA = 3;
	private static final int REQUEST_CODE_GALLERY = 4;

	private static final float GRID_CELL_SIZE = 55.0f; 

	private GridView mProductGrid;
	private ProductSelectorAdapter mProductAdapter;
	private long mGroceryListId;

	private int mSortBy;
	private boolean mDescending;
	EditText mFilterProducts;
	private ImageButton mButtonClearFindProduct;
	private ImageButton mButtonAddProduct;
	private TextView mNoProductsFound;
	private Uri mCaptureUri;
	private Product mProductForIcon;

	public static void startForResult(Activity activity, long groceryListId, int requestCode) {
		Intent productSelectorIntent = new Intent(activity, ProductSelectorActivity.class);
		productSelectorIntent.putExtra(EXTRA_KEY_GROCERY_LIST_ID, groceryListId);
		activity.startActivityForResult(productSelectorIntent, requestCode);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSortBy = GroceryListUtils.SORT_PRODUCTS_BY_NAME;
		mDescending = false;

		setContentView(R.layout.product_grid);
		readArgumentsFromIntent();
		mProductGrid = (GridView)findViewById(R.id.product_grid);
		mProductGrid.setOnItemClickListener( new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				Cursor c = (Cursor) mProductAdapter.getItem(position);
				long productId = c.getLong(GroceryListProvider.PRODUCT_ID_COLUMN);
				boolean cachePriority = false;
				if (mSortBy == GroceryListUtils.SORT_PRODUCTS_BY_POPULARITY) {
					cachePriority = true;
				}
				getEngine().addShoppingListItem(productId, mGroceryListId, cachePriority);
				GroceryListStateTracker.updateListState(productId, 1);
				updateProducts();
			}
		});

		mProductAdapter = new ProductSelectorAdapter(this, getEngine(), mGroceryListId);
		mProductGrid.setAdapter(mProductAdapter);
		registerForContextMenu(mProductGrid);

		mFilterProducts = (EditText)findViewById(R.id.search_field);
		mFilterProducts.addTextChangedListener(this);
		mFilterProducts.setInputType(InputType.TYPE_NULL);
		mFilterProducts.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				mFilterProducts.setInputType(InputType.TYPE_CLASS_TEXT);
			}
		});


		mButtonClearFindProduct = (ImageButton) findViewById(R.id.button_clear_find);
		mButtonClearFindProduct.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				mFilterProducts.setText("");
				updateProducts();
			}
		});

		mButtonAddProduct = (ImageButton) findViewById(R.id.button_add_product);
		mButtonAddProduct.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				String productName = mFilterProducts.getText().toString();
				AddRenameProductActivity.addProduct(ProductSelectorActivity.this, productName, REQUEST_CODE_ADD_PRODUCT);
			}
		});

		ImageButton newListButtonByVoice = (ImageButton) findViewById(R.id.button_new_list_by_voice);
		PackageManager pm = getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		if (activities.size() != 0) {
			newListButtonByVoice.setOnClickListener(new OnClickListener() {

				public void onClick(View arg0) {
					startVoiceRecognitionActivity();
				}
			});
		} else {
			newListButtonByVoice.setVisibility(View.GONE);
		}

		mButtonClearFindProduct.setEnabled(false);
		mButtonAddProduct.setEnabled(false);

		mNoProductsFound = (TextView) findViewById(R.id.no_products);

		updateProducts();
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateProducts();
	}

	@Override
	protected void onPause() {
		super.onPause();
		getEngine().commitProductPriorities();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo listItemInfo = (AdapterContextMenuInfo) menuInfo;
		Cursor productCursor = (Cursor) mProductAdapter.getItem(listItemInfo.position);
		long productId = productCursor.getLong(GroceryListProvider.PRODUCT_ID_COLUMN);
		Product product = getEngine().getProduct(productId);
		int isUserCreated = product.getIsUserCreated();
		int productCount = 0;
		ShoppingListItem shoppingListItem = getEngine().getShoppingListItem(product.getId(), mGroceryListId);
		if (shoppingListItem != null) {
			productCount = shoppingListItem.getCount();
		}

		boolean userCreated = false;
		if (isUserCreated == GroceryListUtils.PRODUCT_USER_CREATED) {
			userCreated = true;
		}
		if (userCreated) {
			if (productCount > 0) {
				getMenuInflater().inflate(R.menu.product_selector_conext_menu_user_created, menu);
			} else {
				getMenuInflater().inflate(R.menu.product_selector_conext_menu_user_created_zero_count, menu);
			}
		} else {
			if (productCount > 0) {
				getMenuInflater().inflate(R.menu.product_selector_conext_menu, menu);
			} else {
				getMenuInflater().inflate(R.menu.product_selector_conext_menu_zero_count, menu);
			}
		}
		menu.setHeaderTitle(product.getName());
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo listItemInfo = (AdapterContextMenuInfo) item.getMenuInfo();
		Cursor c = (Cursor) mProductAdapter.getItem(listItemInfo.position);
		long itemId = c.getLong(GroceryListProvider.GROCERY_LIST_ID_COLUMN);

		switch (item.getItemId()) {
		case R.id.menu_item_product_clear_count:
			clearProductCount(itemId);
			return true;
		case R.id.menu_item_product_decrease_count_by_one:
			decreaseCountByOne(itemId);
			return true;
		case R.id.menu_item_product_rename:
			renameProduct(itemId);
			return true;
		case R.id.menu_item_product_delete:
			deleteProduct(itemId);
			return true;
		case R.id.menu_item_product_change_icon:
			changeIcon(itemId);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.product_selector_options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.menu_item_add_new_product:
			addNewProduct();
			return true;
		case R.id.menu_item_sort_products_by_name:
			getEngine().commitProductPriorities();
			if (mSortBy == GroceryListUtils.SORT_PRODUCTS_BY_POPULARITY) {
				mDescending = false;
			} else {
				mDescending = !mDescending;
			}
			mSortBy = GroceryListUtils.SORT_PRODUCTS_BY_NAME;
			updateProducts(mSortBy, mDescending);
			return true;
		case R.id.menu_item_sort_products_by_popularity:
			getEngine().commitProductPriorities();
			if (mSortBy == GroceryListUtils.SORT_PRODUCTS_BY_NAME) {
				mDescending = true;
			} else {
				mDescending = !mDescending;
			}
			mSortBy = GroceryListUtils.SORT_PRODUCTS_BY_POPULARITY;
			updateProducts(mSortBy, mDescending);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CODE_RENAME_PRODUCT:
		case REQUEST_CODE_ADD_PRODUCT:
			if (resultCode == RESULT_OK) {
				long productId = data.getLongExtra(AddRenameProductActivity.EXTRA_KEY_PRODUCT_ID, GroceryListUtils.UNKNOWN_ID);
				if (productId != GroceryListUtils.UNKNOWN_ID) {
					mFilterProducts.setText(getEngine().getProduct(productId).getName());
				}
			}
			break;
		case REQUEST_CODE_SEARCH_PRODUCT_BY_VOICE:
			if(resultCode == RESULT_OK) {
				ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				searchProductFromVoiceResults(matches);
			}
			break;
		case REQUEST_CODE_CAMERA:
			if (resultCode == RESULT_OK) {
				try {
					String productName = mProductForIcon.getName().replace(" ", "_");
					final String filename = Environment.getExternalStorageDirectory() + File.separator + productName + ".png";
					File file = new File(filename);
					if (file.exists() == false) {
						Bitmap image = (Bitmap)data.getExtras().get("data");
						updateBitmap(image);
					} else {
						try {  
							Bitmap captureBmp = Media.getBitmap(getContentResolver(), Uri.fromFile(file));
							updateBitmap(captureBmp);
						} catch (FileNotFoundException e) {  
							if (DebugUtils.printDebug) {
								Log.e(TAG, "Could not update bitmap from gallery.", e);
							}  
						} catch (IOException e) {  
							if (DebugUtils.printDebug) {
								Log.e(TAG, "Could not update bitmap from gallery.", e);
							}  
						} 
					}
				} catch (Exception e) {
					if (DebugUtils.printDebug) {
						Log.e(TAG, "Could not update bitmap from camera.", e);
					}
				} 
			}
			break;
		case REQUEST_CODE_GALLERY:
			if (resultCode == RESULT_OK) { 
				try {  
					final Uri selectedImage = data.getData();
					Bitmap captureBmp = Media.getBitmap(getContentResolver(), selectedImage);
					updateBitmap(captureBmp);
				} catch (Exception e) {
					if (DebugUtils.printDebug) {
						Log.e(TAG, "Could not update bitmap from gallery.", e);
					}
				}
			}
			break;
		}
	}

	public void onBackPressed() {
		Intent data = new Intent();    	
		data.putExtra(EXTRA_KEY_GROCERY_LIST_ID, mGroceryListId);
		setResult(Activity.RESULT_OK, data);
		finish();
	}

	public void afterTextChanged(Editable e) {
		String filterByName = e.toString().trim();
		if (filterByName.contains(GroceryListUtils.NAME_FORBIDDEN_CHARACTER)) {
			Toast.makeText(ProductSelectorActivity.this, R.string.name_forbidden_character_automatic_removal, Toast.LENGTH_LONG).show();
			filterByName = filterByName.replace(GroceryListUtils.NAME_FORBIDDEN_CHARACTER, "");
			mFilterProducts.setText(filterByName);
			mFilterProducts.setSelection(filterByName.length());
		}
		boolean enableButtons = false;
		if (filterByName.length() == 0) {
			updateProducts(mSortBy, mDescending);
		} else {
			updateProducts(filterByName);
			enableButtons = true;
		}
		mButtonClearFindProduct.setEnabled(enableButtons);
		mButtonAddProduct.setEnabled(enableButtons);
	}

	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// do nothing
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// do nothing        
	}


	private void readArgumentsFromIntent() {
		mGroceryListId = getIntent().getLongExtra(EXTRA_KEY_GROCERY_LIST_ID, GroceryListUtils.UNKNOWN_ID);
		if (mGroceryListId == GroceryListUtils.UNKNOWN_ID) {
			if(DebugUtils.printDebug) {
				Log.w(TAG, "ProductSelectorAcivity is expecting EXTRA_KEY_GROCERY_LIST_ID");
			}
			finish();
		}
		GroceryList groceryList = getEngine().getGroceryList(mGroceryListId);
		setTitle(groceryList.getName());
	}

	private void clearProductCount(long productId) {
		ShoppingListItem shoppingListItem = getEngine().getShoppingListItem(productId, mGroceryListId);
		GroceryListStateTracker.updateListState(productId, (-1)*shoppingListItem.getCount());
		getEngine().removeShoppingListItem(productId, mGroceryListId);
		updateProducts();
	}

	private void decreaseCountByOne(long productId) {
		ShoppingListItem shoppingListItem = getEngine().getShoppingListItem(productId, mGroceryListId);
		if (shoppingListItem.getCount() > 1) {
			shoppingListItem.decreaseCount();
			getEngine().updateShoppingListItem(shoppingListItem);
		} else {
			getEngine().removeShoppingListItem(productId, mGroceryListId);
		}
		GroceryListStateTracker.updateListState(productId, -1);
		updateProducts();
	}

	private void renameProduct(long productId) {
		AddRenameProductActivity.renameProduct(this, productId, REQUEST_CODE_RENAME_PRODUCT);
	}

	private void deleteProduct(final long productId) {
		Product product = getEngine().getProduct(productId);
		AlertDialog.Builder confirmDelete = new AlertDialog.Builder(this);
		confirmDelete.setTitle(R.string.dialog_confirm_title_warning);
		confirmDelete.setMessage(getString(R.string.dialog_confirm_product_delete, product.getName()));
		confirmDelete.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface arg0, int arg1) {
				getEngine().removeProduct(productId);
				getEngine().removeAllShoppingListItemByProductId(productId);
				updateProducts();
			}
		});
		confirmDelete.setNegativeButton(android.R.string.cancel, null);
		confirmDelete.create().show();
	}

	private void changeIcon(final long productId) {
		mProductForIcon = getEngine().getProduct(productId);
		AlertDialog.Builder uploadBuilder = new AlertDialog.Builder(this);
		uploadBuilder.setTitle("");
		uploadBuilder.setMessage(getString(R.string.text_change_product_icon));
		uploadBuilder.setPositiveButton(getString(R.string.btn_gallery), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				launchGallery();
			}
		});
		uploadBuilder.setNegativeButton(getString(R.string.btn_camera), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				launchCamera(mProductForIcon);
			}
		});
		uploadBuilder.show();

	}

	private void addNewProduct() {
		String productName = mFilterProducts.getText().toString();
		if (TextUtils.isEmpty(productName)) {
			AddRenameProductActivity.addProduct(this, null, REQUEST_CODE_ADD_PRODUCT);
		} else {
			AddRenameProductActivity.addProduct(this, productName, REQUEST_CODE_ADD_PRODUCT);
		}
	}

	private GroceryListEngine getEngine() {
		return ((GroceryListApplication)getApplication()).getEngine();
	}

	private void updateProducts() {
		String filterProducts = mFilterProducts.getText().toString();
		if (TextUtils.isEmpty(filterProducts)) {
			updateProducts(mSortBy, mDescending);
		} else {
			updateProducts(filterProducts);
		}
	}

	private void updateProducts(final int sortBy, final boolean descending) {
		class UpdateProductsTask extends AsyncTask<Void, Void, Cursor>{

			@Override
			protected Cursor doInBackground(Void... ignore) {
				Cursor cursor = getEngine().getAllProductsCursor(sortBy, descending);
				return cursor;
			}

			@Override
			protected void onPostExecute(Cursor cursor) {
				if (cursor == null || cursor.isClosed()) {
					mNoProductsFound.setVisibility(View.VISIBLE);
					return;
				} else {
					mNoProductsFound.setVisibility(View.GONE);
				}
				startManagingCursor(cursor);
				mProductAdapter.changeCursor(cursor);
			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
			}
		}
		Cursor oldCursor = mProductAdapter.getCursor();
		stopManagingCursor(oldCursor);
		new UpdateProductsTask().execute();
	}

	private void updateProducts(final String filterBy) {
		class UpdateProductsTask extends AsyncTask<Void, Void, Cursor>{

			@Override
			protected Cursor doInBackground(Void... ignore) {
				Cursor cursor = getEngine().getAllProductsCursor(filterBy);
				return cursor;
			}

			@Override
			protected void onPostExecute(Cursor cursor) {
				if (cursor != null) {
					startManagingCursor(cursor);
					mNoProductsFound.setVisibility(View.GONE);
				} else {
					mNoProductsFound.setVisibility(View.VISIBLE);
				}
				mProductAdapter.changeCursor(cursor);
			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
			}
		}
		Cursor oldCursor = mProductAdapter.getCursor();
		stopManagingCursor(oldCursor);
		new UpdateProductsTask().execute();
	}

	private void startVoiceRecognitionActivity() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_hint_search_for_product));
		startActivityForResult(intent, REQUEST_CODE_SEARCH_PRODUCT_BY_VOICE);
	}

	private void searchProductFromVoiceResults(ArrayList<String> voiceResults) {	
		for (String result : voiceResults) {     
			String[] products = result.split(" ");
			if (products[0] != null) {
				mFilterProducts.setText(products[0]);
				updateProducts();
			}
		}
	}

	private void launchCamera(final Product product) {
		String productName = product.getName().replace(" ", "_");
		mCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + File.separator + productName + ".png"));
		final Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		camera.putExtra(MediaStore.EXTRA_OUTPUT, mCaptureUri);
		startActivityForResult(camera, REQUEST_CODE_CAMERA);
	}

	private void launchGallery() {
		final Intent gallery = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI); 
		startActivityForResult(gallery, REQUEST_CODE_GALLERY); 
	}

	private void updateBitmap(Bitmap bitmap) {
		float origHeight = bitmap.getHeight();
		float origWidth = bitmap.getWidth();
		float scale = 1.0f;
		if (origHeight > origWidth) {
			scale = GRID_CELL_SIZE / origHeight;
		} else {
			scale = GRID_CELL_SIZE / origWidth;
		}
		int height = (int)(origHeight * scale);
		int width = (int)(origWidth * scale);
		if (DebugUtils.printDebug) {
			Log.d("ORIG SIZE W", "" + origWidth);
			Log.d("ORIG SIZE H", "" + origHeight);
			Log.d("SIZE W", "" + width);
			Log.d("SIZE H", "" + height);
			Log.d("SCALE", "" + scale);
		}
		Bitmap scaledBitmap = null;
		try {
			scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
		} catch (Exception e) {
			if (DebugUtils.printDebug) {
				Log.e(TAG, "Could not scale bitmap.", e);
			}
		}

		long iconId = mProductForIcon.getResIconId();
		if (iconId == R.drawable.default_product_icon || iconId <= 0) {
			iconId = GroceryListUtils.UNKNOWN_ID;
		}
		Icon newIcon = new Icon(iconId, scaledBitmap);
		iconId = getEngine().insertOrUpdateIcon(newIcon);
		mProductForIcon.setResIconId(iconId);
		getEngine().updateProduct(mProductForIcon);
		updateProducts();
	}
}
