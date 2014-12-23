package com.alizinhouse.imagecrawl.channel;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.alizinhouse.imagecrawl.R;
import com.alizinhouse.imagecrawl.data.DataSource;
import com.alizinhouse.imagecrawl.data.DataSourceItem;

public class ChannelDetail extends Activity {
	private ImageView mThumbnail;
	private EditText mName;
	private EditText mDomain;
	private Button saveButton;
	private int mPosition;
	private DataSourceItem mItem;
	private DataSource mDataSourceInstance;

	private String nameString, domainString;
	private boolean nameStringEdited, domainStringEdited;

	private final String defaultName = "New Channel";
	private final String defaultDomain = "http://www.askmen.com/galleries/";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.channel_detail);

		Intent i = getIntent();
		mPosition = i.getIntExtra("position", 0);
		Log.d("ImageCrawl", "item position -> " + mPosition);

		mName = (EditText) findViewById(R.edit.nametext);
		mDomain = (EditText) findViewById(R.edit.domaintext);
		mThumbnail = (ImageView) findViewById(R.edit.thumb);

		mDataSourceInstance = DataSource.getDataSourceInstance(this);

		if (mPosition < 0) {
			mItem = new DataSourceItem(null, -1, defaultName, defaultDomain);
			mName.setText(defaultName);
			mDomain.setText(defaultDomain);
			mThumbnail.setImageBitmap(((BitmapDrawable) this.getResources()
					.getDrawable(R.drawable.ic_gallery)).getBitmap());
		} else {
			mItem = mDataSourceInstance.getItemsData().get(mPosition);
			mName.setText(mItem.getName());
			mDomain.setText(mItem.getDomain());
			mThumbnail.setImageBitmap(mItem.getThumbnail());
		}

		mName.addTextChangedListener(new TextWatcher() {
			private String temp;

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				nameString = s.toString();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				nameStringEdited = false;
				temp = nameString;
			}

			@Override
			public void afterTextChanged(Editable s) {
				nameStringEdited = !nameString.equals(temp);
			}
		});

		mDomain.addTextChangedListener(new TextWatcher() {
			private String temp;

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				domainString = s.toString();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				domainStringEdited = false;
				temp = domainString;
			}

			@Override
			public void afterTextChanged(Editable s) {
				domainStringEdited = !domainString.equals(temp);
			}
		});

		saveButton = (Button) findViewById(R.id.save);
		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (nameStringEdited) {
					mItem.setName(nameString);
				}
				if (domainStringEdited) {
					mItem.setDomain(domainString);
				}

				if (mItem.getId() < 0) {
					mDataSourceInstance.insertDomain(mItem.getName(),
							mItem.getDomain());
				} else {
					mDataSourceInstance.updateDomain(mItem.getId(),
							mItem.getName(), mItem.getDomain());
				}

				setResult(RESULT_OK, null);
				finish();
			}
		});

	}
}
