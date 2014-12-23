package com.alizinhouse.imagecrawl.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.alizinhouse.imagecrawl.data.DataSourceItem;
import com.alizinhouse.imagecrawl.parser.BinaryParseData;
import com.alizinhouse.imagecrawl.url.WebURL;

/*
 * This class shows how you can crawl images on the web and store them in a
 * folder. This is just for demonstration purposes and doesn't scale for large
 * number of images. For crawling millions of images you would need to store
 * downloaded images in a hierarchy of folders
 */
public class ImageCrawler extends WebCrawler {

	private static List<String> imageList = new ArrayList<String>();
	private static String domain;
	private static DataSourceItem dataSourceItem;

	private static final Pattern filters = Pattern
			.compile(".*(\\.(gif|css|js|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf"
					+ "|rm|smil|wmv|swf|wma|zip|rar|gz))$");

	private static final Pattern imgPatterns = Pattern
			.compile(".*(\\.(bmp|jpe?g|png|tiff?))$");

	public static void update(DataSourceItem item) {
		domain = item.getDomain();
		dataSourceItem = item;
		imageList.clear();
	}

	public static int count() {
		return imageList.size();
	}

	@Override
	public boolean shouldVisit(WebURL url) {
		String href = url.getURL().toLowerCase();
		if (filters.matcher(href).matches()) {
			return false;
		}

		if (imgPatterns.matcher(href).matches()) {
			return true;
		}

		if (href.startsWith(domain))
			return true;

		return false;
	}

	@Override
	public void visit(Page page) {
		String url = page.getWebURL().getURL();

		// We are only interested in processing images
		if (!(page.getParseData() instanceof BinaryParseData)) {
			return;
		}

		if (!imgPatterns.matcher(url).matches()) {
			return;
		}

		// Not interested in very small images
		if (page.getContentData().length < 10 * 1024) {
			return;
		}

		imageList.add(url);
		if (imageList.size() == 1) {
			byte[] blob = page.getContentData();
			Bitmap bmp = BitmapFactory.decodeByteArray(blob, 0, blob.length);
			Bitmap.createScaledBitmap(bmp, 48, 48, true);
			dataSourceItem.setThumbnail(bmp);
			// Log.v("WebCrawler", "Page data set as thumbnail.");
		}

		// Log.v("WebCrawler", "Added " + url + " to the list, list size is "+
		// imageList.size());
		// Log.v("WebCrawler", imageList.toString());
	}

	public static String getImageAt(int index) {
		if (index >= 0 && index < imageList.size()) {
			return imageList.get(index);
		}
		return "";
	}
}
