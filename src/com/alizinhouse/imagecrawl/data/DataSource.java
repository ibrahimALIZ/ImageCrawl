package com.alizinhouse.imagecrawl.data;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

import com.alizinhouse.imagecrawl.R;

public class DataSource {
	private static DataSource mDataSource;

	private Context mContext;
	private ArrayList<DataSourceItem> mItemsData;
	private DBAdapter db;

	public static DataSource getDataSourceInstance(Context context) {
		if (mDataSource == null)
			mDataSource = new DataSource(context);
		return mDataSource;
	}

	public DataSource(Context context) {
		this.mContext = context;
		mItemsData = new ArrayList<DataSourceItem>();
		db = new DBAdapter(context);
		setupItemsData();
	}

	public ArrayList<DataSourceItem> getItemsData() {
		return mItemsData;
	}

	public void getItemsList() {
		mItemsData.clear();
		setupItemsData();
	}

	private void setupItemsData() {
		try {
			// ---get all titles---
			db.open();
			byte[] blob;
			Bitmap bmp;
			Cursor c = db.getAllDomains();
			if (c.moveToFirst()) {
				do {
					blob = c.getBlob(3);
					bmp = null;
					if (blob != null) {
						bmp = BitmapFactory.decodeByteArray(blob, 0,
								blob.length);
					}
					if (bmp == null) {
						bmp = ((BitmapDrawable) mContext.getResources()
								.getDrawable(R.drawable.ic_gallery))
								.getBitmap();
					}
					DataSourceItem item = new DataSourceItem(bmp, c.getInt(0),
							c.getString(1), c.getString(2));
					mItemsData.add(item);
				} while (c.moveToNext());
			}
			c.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.close();
		}
	}

	public int getDataSourceLength() {
		return mItemsData.size();
	}

	public boolean updateDomain(long rowId, String name, String descrption) {
		try {
			db.open();
			return db.updateDomain(rowId, name, descrption);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			db.close();
		}
	}

	public boolean updateThumbnail(long rowId, Bitmap thumbnail) {
		try {
			db.open();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			thumbnail.compress(CompressFormat.PNG, 0 /* ignored for PNG */, bos);
			byte[] bitmapdata = bos.toByteArray();
			return db.updateThumbnail(rowId, bitmapdata);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			db.close();
		}
	}

	public long insertDomain(String name, String description) {
		try {
			db.open();
			return db.insertDomain(name, description);
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		} finally {
			db.close();
		}

	}

	public boolean deleteDomain(long rowId) {
		try {
			db.open();
			return db.deleteDomain(rowId);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			db.close();
		}
	}
}
