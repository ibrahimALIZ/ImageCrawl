package com.alizinhouse.imagecrawl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alizinhouse.imagecrawl.channel.Channel;
import com.alizinhouse.imagecrawl.channel.ChannelDetail;
import com.alizinhouse.imagecrawl.data.DataSource;
import com.alizinhouse.imagecrawl.data.DataSourceItem;

public class ImageCrawlActivity extends Activity {
	private static int ADD_NEW_CHANNEL_ID = 1;
	private static final String TAG = "ImageCrawler";

	public class ChannelAdapter extends BaseAdapter {

		private Context mContext;
		private LayoutInflater mInflator;
		private DataSource mDataSource;

		public ChannelAdapter(Context c) {
			mContext = c;
			mInflator = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mDataSource = DataSource.getDataSourceInstance(mContext);
		}

		@Override
		public int getCount() {
			return mDataSource.getDataSourceLength();
		}

		@Override
		public Object getItem(int position) {
			return mDataSource.getItemsData().get(position);
		}

		@Override
		public long getItemId(int position) {
			return mDataSource.getItemsData().get(position).getId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
  			ImageView thumbnail;
			TextView name;

			if (convertView == null) {
				convertView = mInflator.inflate(R.layout.list_item_layout,
						parent, false);
			}

			thumbnail = (ImageView) convertView.findViewById(R.list.thumb);
			thumbnail.setImageBitmap(mDataSource.getItemsData().get(position)
					.getThumbnail());
			name = (TextView) convertView.findViewById(R.list.text);
			name.setText(mDataSource.getItemsData().get(position).getName());

			return convertView;
		}
	}

	private ListView mListView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mListView = (ListView) findViewById(R.id.quotes_list);
		mListView.setAdapter(new ChannelAdapter(this));
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				if (isOnline()) {
					Intent i = new Intent(ImageCrawlActivity.this,
							Channel.class);
					i.putExtra("position", position);
					startActivity(i);
  				} else {
					Toast.makeText(getApplicationContext(),
							"Check if you are online!", Toast.LENGTH_SHORT)
							.show();
				}
			}
		});

		registerForContextMenu(mListView);
	}

	public boolean isOnline() {
		try {
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			State mobile = cm.getNetworkInfo(0).getState();
			State wifi = cm.getNetworkInfo(1).getState();
			if (mobile == NetworkInfo.State.CONNECTED
					|| mobile == NetworkInfo.State.CONNECTING) {
				// mobile
				Log.d("widget activity", "mobile online");
				return true;
			} else if (wifi == NetworkInfo.State.CONNECTED
					|| wifi == NetworkInfo.State.CONNECTING) {
				// wifi
				Log.d("widget activity", "wifi online");
				return true;
			} else {
				return false;
			}
		} catch (Exception exception) {
			Log.d("widget activity", "Connectivity Error");
			return false;
		}
	}

	@Override
	protected void onResume() {
	 	super.onResume();
		if (mListView != null) {
			ChannelAdapter adapter = (ChannelAdapter) mListView.getAdapter();
			adapter.notifyDataSetChanged();
		}
	}

	/********* Options Menu *********/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.CATEGORY_ALTERNATIVE, ADD_NEW_CHANNEL_ID, Menu.NONE,
				"Add New Channel").setIcon(R.drawable.ic_menu_add);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == ADD_NEW_CHANNEL_ID) {
			Intent i = new Intent(ImageCrawlActivity.this, ChannelDetail.class);
			i.putExtra("position", -1);
			startActivityForResult(i, 1);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			Intent refresh = new Intent(this, ImageCrawlActivity.class);
			DataSource.getDataSourceInstance(this).getItemsList();
			startActivity(refresh);
			this.finish();
			// ChannelAdapter adapter = (ChannelAdapter) mListView.getAdapter();
			// adapter.notifyDataSetChanged();
			
			// TODO: shutdown crawler session
		}
	}

	/********* Context Menu *********/
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle(getResources().getString(
				R.string.context_menu_title));
		MenuInflater menuInflator = getMenuInflater();
		menuInflator.inflate(R.layout.context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.delete:
			DataSourceItem dataItem = DataSource.getDataSourceInstance(this)
					.getItemsData().get(info.position);
			DataSource.getDataSourceInstance(this).getItemsData()
					.remove(info.position);
			DataSource.getDataSourceInstance(this).deleteDomain(
					dataItem.getId());

			ChannelAdapter adapter = (ChannelAdapter) mListView.getAdapter();
			adapter.notifyDataSetChanged();
			break;
		case R.id.edit:
			Intent i = new Intent(ImageCrawlActivity.this, ChannelDetail.class);
			i.putExtra("position", info.position);
			startActivity(i);
			break;
		default:
			Toast.makeText(getApplicationContext(),
					"You've not clicked on a menu item", item.getItemId())
					.show();
			break;
		}
		return super.onContextItemSelected(item);
	}
}