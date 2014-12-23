package com.alizinhouse.imagecrawl.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;

import com.alizinhouse.imagecrawl.crawler.Configurable;
import com.alizinhouse.imagecrawl.crawler.CrawlConfig;
import com.alizinhouse.imagecrawl.crawler.Page;
import com.alizinhouse.imagecrawl.url.URLCanonicalizer;
import com.alizinhouse.imagecrawl.url.WebURL;
import com.alizinhouse.imagecrawl.util.Util;


public class Parser extends Configurable {

	private HtmlParser htmlParser;
	private ParseContext parseContext;

	public Parser(CrawlConfig config) {
		super(config);
		htmlParser = new HtmlParser();
		parseContext = new ParseContext();
	}

	public boolean parse(Page page, String contextURL) {

		if (Util.hasBinaryContent(page.getContentType())) {
			if (!config.isIncludeBinaryContentInCrawling()) {
				return false;
			} else {
				page.setParseData(BinaryParseData.getInstance());
				return true;
			}
		} else if (Util.hasPlainTextContent(page.getContentType())) {
			try {
				TextParseData parseData = new TextParseData();
				parseData.setTextContent(new String(page.getContentData(), page
						.getContentCharset()));
				page.setParseData(parseData);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}

		Metadata metadata = new Metadata();
		HtmlContentHandler contentHandler = new HtmlContentHandler();
		InputStream inputStream = null;
		try {
			inputStream = new ByteArrayInputStream(page.getContentData());
			htmlParser.parse(inputStream, contentHandler, metadata,
					parseContext);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (page.getContentCharset() == null) {
			page.setContentCharset(metadata.get("Content-Encoding"));
		}

		HtmlParseData parseData = new HtmlParseData();
		parseData.setText(contentHandler.getBodyText().trim());
		parseData.setTitle(metadata.get(Metadata.TITLE));

		List<WebURL> outgoingUrls = new ArrayList<WebURL>();

		String baseURL = contentHandler.getBaseUrl();
		if (baseURL != null) {
			contextURL = baseURL;
		}

		int urlCount = 0;
		for (ExtractedUrlAnchorPair urlAnchorPair : contentHandler
				.getOutgoingUrls()) {
			String href = urlAnchorPair.getHref();
			href = href.trim();
			if (href.length() == 0) {
				continue;
			}
			String hrefWithoutProtocol = href.toLowerCase();
			if (href.startsWith("http://")) {
				hrefWithoutProtocol = href.substring(7);
			}
			if (!hrefWithoutProtocol.contains("javascript:")
					&& !hrefWithoutProtocol.contains("@")) {
				String url = URLCanonicalizer.getCanonicalURL(href, contextURL);
				if (url != null) {
					WebURL webURL = new WebURL();
					webURL.setURL(url);
					webURL.setAnchor(urlAnchorPair.getAnchor());
					outgoingUrls.add(webURL);
					urlCount++;
					if (urlCount > config.getMaxOutgoingLinksToFollow()) {
						break;
					}
				}
			}
		}

		parseData.setOutgoingUrls(outgoingUrls);

		/*try {
			if (page.getContentCharset() == null) {
				parseData.setHtml(new String(page.getContentData()));
			} else {
				parseData.setHtml(new String(page.getContentData(), page
						.getContentCharset()));
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}*/

		page.setParseData(parseData);
		return true;

	}

}
