package com.alizinhouse.imagecrawl.channel;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import com.alizinhouse.imagecrawl.R;
import com.alizinhouse.imagecrawl.crawler.CrawlConfig;
import com.alizinhouse.imagecrawl.crawler.CrawlController;
import com.alizinhouse.imagecrawl.crawler.ImageCrawler;
import com.alizinhouse.imagecrawl.data.DataSource;
import com.alizinhouse.imagecrawl.data.DataSourceItem;
import com.alizinhouse.imagecrawl.fetcher.PageFetcher;
import com.alizinhouse.imagecrawl.robotstxt.RobotstxtConfig;
import com.alizinhouse.imagecrawl.robotstxt.RobotstxtServer;

public class Channel extends Activity {
	private WebView mWImageView;
	private Button nextButton, backButton, stopButton;
	private TextView footer, address;

	private int mPosition = -1;
	private DataSourceItem mItem;
	private DataSource mDataSourceInstance;
	private ProgressDialog pd;
	private ProgressThread progressThread;

	private CrawlController controller;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.channel);

		Intent i = getIntent();
		int channelIndex = i.getIntExtra("position", 0);

		mWImageView = (WebView) findViewById(R.id.wimage);
		mWImageView.getSettings().setBuiltInZoomControls(true);
		mWImageView.getSettings().setUseWideViewPort(true);
		mWImageView.getSettings().setJavaScriptEnabled(false);

		final Activity activity = this;
		mWImageView.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				activity.setProgress(progress * 1000);
			}
		});

		mWImageView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				pd = ProgressDialog.show(Channel.this, "",
						"Loading. Please wait...", true);
				view.loadUrl(url);
				return true;
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				pd.dismiss();
				if (mPosition == 0) {
					mDataSourceInstance.updateThumbnail(mItem.getId(),
							mItem.getThumbnail());
				}
				address.setText(ImageCrawler.getImageAt(mPosition));
			}
		});

		mDataSourceInstance = DataSource.getDataSourceInstance(this);
		mItem = mDataSourceInstance.getItemsData().get(channelIndex);

		// Log.v("ImageCrawl",
		// "Domain :" + mItem.getDomain() + ", Name :" + mItem.getName());
		startCrawlWith(mItem);

		pd = ProgressDialog.show(Channel.this, "", "Loading. Please wait...",
				true, true);
		pd.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				stopCrawl();
			}
		});

		nextButton = (Button) findViewById(R.id.next);
		backButton = (Button) findViewById(R.id.back);
		stopButton = (Button) findViewById(R.id.stop);

		nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (mPosition < ImageCrawler.count() - 1) {
					mPosition++;
					// Log.v("ImageCrawl", ImageCrawler.getImageAt(mPosition));
					mWImageView.loadUrl(ImageCrawler.getImageAt(mPosition));

					updateFooter(mPosition, ImageCrawler.count());
				}

			}
		});

		backButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (mPosition > 0) {
					mPosition--;
					// Log.v("ImageCrawl", ImageCrawler.getImageAt(mPosition));
					mWImageView.loadUrl(ImageCrawler.getImageAt(mPosition));
					updateFooter(mPosition, ImageCrawler.count());
				}
			}
		});

		stopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				stopCrawl();
			}
		});

		footer = (TextView) findViewById(R.id.footer);
		address = (TextView) findViewById(R.id.address);

		progressThread = new ProgressThread(handler);
		progressThread.start();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		stopCrawl();
	}

	private void stopCrawl() {
		if (controller != null) {
			controller.Shutdown();
			controller.getCrawlersLocalData().clear();
		}
		if (progressThread != null) {
			progressThread.setState(ProgressThread.STATE_DONE);
		}
	}

	private void startCrawlWith(DataSourceItem item) {
		CrawlConfig config = new CrawlConfig();

		// config.setCrawlStorageFolder(Environment.getExternalStorageDirectory() + "/alizinhouse");
		// config.setMaxDepthOfCrawling(2);
		// config.setMaxOutgoingLinksToFollow(5);
		// config.setMaxPagesToFetch(50);

		/*
		 * Since images are binary content, we need to set this parameter to
		 * true to make sure they are included in the crawl.
		 */
		config.setIncludeBinaryContentInCrawling(true);
		// config.setResumableCrawling(true);

		PageFetcher pageFetcher = new PageFetcher(config);
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		robotstxtConfig.setEnabled(false);
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig,
				pageFetcher);

		try {
			controller = new CrawlController(config, pageFetcher,
					robotstxtServer);
			String domain = item.getDomain();
			if (domain.indexOf("http://") < 0) {
				item.setDomain("http://" + domain);
			}

			controller.addSeed(item.getDomain(), item.getId());
			ImageCrawler.update(item);
			controller.startNonBlocking(ImageCrawler.class, 4);

			// Send the shutdown request and then wait for finishing
			// controller.Shutdown();
			// controller.waitUntilFinish();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Define the Handler that receives messages from the thread and update the
	// progress
	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			int total = ImageCrawler.count();
			if (mPosition < 0 && total > 0) {
				mPosition++;
				mWImageView.loadUrl(ImageCrawler.getImageAt(mPosition));
			}
			updateFooter(mPosition, total);
		}
	};

	private void updateFooter(int pos, int total) {
		footer.setText((pos + 1) + "/" + total + " Total " + total
				+ " images found.");
	}

	/** Nested class that performs progress calculations (counting) */
	private class ProgressThread extends Thread {
		Handler mHandler;
		final static int STATE_DONE = 0;
		final static int STATE_RUNNING = 1;
		int mState;
		int total;

		ProgressThread(Handler h) {
			mHandler = h;
		}

		public void run() {
			mState = STATE_RUNNING;
			total = 0;
			while (mState == STATE_RUNNING) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Log.e("ERROR", "Thread Interrupted");
				}
				Message msg = mHandler.obtainMessage();
				msg.arg1 = total;
				mHandler.sendMessage(msg);
				total++;
			}
		}

		/*
		 * sets the current state for the thread, used to stop the thread
		 */
		public void setState(int state) {
			mState = state;
		}
	}
}
