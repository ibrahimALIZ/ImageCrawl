package com.alizinhouse.imagecrawl.data;

import android.graphics.Bitmap;

public class DataSourceItem {
	private Bitmap mThumbnail;
	private String mName;
	private String mDomain;
	private int mId;

	public Bitmap getThumbnail() {
		return mThumbnail;
	}

	public int getId() {
		return mId;
	}

	public void setId(int mId) {
		this.mId = mId;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		this.mName = name;
	}

	public String getDomain() {
		return mDomain;
	}

	public void setDomain(String domain) {
		this.mDomain = domain;
	}

	public void setThumbnail(Bitmap thumbnail) {
		this.mThumbnail = thumbnail;
	}

	public DataSourceItem() {
		mName = "New Channel";
	}

	public DataSourceItem(Bitmap thumbnail, int id, String name, String domain) {
		if (name == null || domain == null)
			throw new IllegalArgumentException();
		mThumbnail = thumbnail;
		mId = id;
		mName = name;
		mDomain = domain;
	}
}
