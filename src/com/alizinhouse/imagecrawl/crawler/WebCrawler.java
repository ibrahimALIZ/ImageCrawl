package com.alizinhouse.imagecrawl.crawler;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;

import com.alizinhouse.imagecrawl.fetcher.CustomFetchStatus;
import com.alizinhouse.imagecrawl.fetcher.PageFetchResult;
import com.alizinhouse.imagecrawl.fetcher.PageFetcher;
import com.alizinhouse.imagecrawl.parser.HtmlParseData;
import com.alizinhouse.imagecrawl.parser.ParseData;
import com.alizinhouse.imagecrawl.parser.Parser;
import com.alizinhouse.imagecrawl.robotstxt.RobotstxtServer;
import com.alizinhouse.imagecrawl.url.WebURL;

/**
 * WebCrawler class in the Runnable class that is executed by each crawler
 * thread.
 * 
 */
public class WebCrawler implements Runnable {

	/**
	 * The id associated to the crawler thread running this instance
	 */
	protected int myId;

	/**
	 * The controller instance that has created this crawler thread. This
	 * reference to the controller can be used for getting configurations of the
	 * current crawl or adding new seeds during runtime.
	 */
	protected CrawlController myController;

	/**
	 * The thread within which this crawler instance is running.
	 */
	private Thread myThread;

	/**
	 * The parser that is used by this crawler instance to parse the content of
	 * the fetched pages.
	 */
	private Parser parser;

	/**
	 * The fetcher that is used by this crawler instance to fetch the content of
	 * pages from the web.
	 */
	private PageFetcher pageFetcher;

	/**
	 * The RobotstxtServer instance that is used by this crawler instance to
	 * determine whether the crawler is allowed to crawl the content of each
	 * page.
	 */
	private RobotstxtServer robotstxtServer;

	/**
	 * Is the current crawler instance waiting for new URLs? This field is
	 * mainly used by the controller to detect whether all of the crawler
	 * instances are waiting for new URLs and therefore there is no more work
	 * and crawling can be stopped.
	 */
	private boolean isWaitingForNewURLs;

	/**
	 * Initializes the current instance of the crawler
	 * 
	 * @param myId
	 *            the id of this crawler instance
	 * @param crawlController
	 *            the controller that manages this crawling session
	 */
	public void init(int myId, CrawlController crawlController) {
		this.myId = myId;
		this.pageFetcher = crawlController.getPageFetcher();
		this.robotstxtServer = crawlController.getRobotstxtServer();
		this.parser = new Parser(crawlController.getConfig());
		this.myController = crawlController;
		this.isWaitingForNewURLs = false;
	}

	/**
	 * Get the id of the current crawler instance
	 * 
	 * @return the id of the current crawler instance
	 */
	public int getMyId() {
		return myId;
	}

	public CrawlController getMyController() {
		return myController;
	}

	/**
	 * This function is called just before starting the crawl by this crawler
	 * instance. It can be used for setting up the data structures or
	 * initializations needed by this crawler instance.
	 */
	public void onStart() {
	}

	/**
	 * This function is called just before the termination of the current
	 * crawler instance. It can be used for persisting in-memory data or other
	 * finalization tasks.
	 */
	public void onBeforeExit() {
	}

	/**
	 * This function is called once the header of a page is fetched. It can be
	 * overwritten by sub-classes to perform custom logic for different status
	 * codes. For example, 404 pages can be logged, etc.
	 */
	protected void handlePageStatusCode(WebURL webUrl, int statusCode,
			String statusDescription) {
	}

	/**
	 * The CrawlController instance that has created this crawler instance will
	 * call this function just before terminating this crawler thread. Classes
	 * that extend WebCrawler can override this function to pass their local
	 * data to their controller. The controller then puts these local data in a
	 * List that can then be used for processing the local data of crawlers (if
	 * needed).
	 */
	public Object getMyLocalData() {
		return null;
	}

	public void run() {
		onStart();
		while (true) {
			List<WebURL> assignedURLs = new ArrayList<WebURL>(50);
			isWaitingForNewURLs = true;
			myController.getNextURLs(40, assignedURLs);
			isWaitingForNewURLs = false;
			// Log.v("ImageCrawler", "Assigned URL " + assignedURLs.toString());

			if (assignedURLs.size() == 0) {
				if (myController.isShuttingDown())
					return;
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				for (WebURL curURL : assignedURLs) {
					if (curURL != null) {
						processPage(curURL);
					}
					if (myController.isShuttingDown()) {
						// Log.v("ImageCrawler","Exiting because of controller shutdown.");
						return;
					}
				}
			}
		}
	}

	/**
	 * Classes that extends WebCrawler can overwrite this function to tell the
	 * crawler whether the given url should be crawled or not. The following
	 * implementation indicates that all urls should be included in the crawl.
	 * 
	 * @param url
	 *            the url which we are interested to know whether it should be
	 *            included in the crawl or not.
	 * @return if the url should be included in the crawl it returns true,
	 *         otherwise false is returned.
	 */
	public boolean shouldVisit(WebURL url) {
		return true;
	}

	/**
	 * Classes that extends WebCrawler can overwrite this function to process
	 * the content of the fetched and parsed page.
	 * 
	 * @param page
	 *            the page object that is just fetched and parsed.
	 */
	public void visit(Page page) {
	}

	private void processPage(WebURL curURL) {
		if (curURL == null) {
			return;
		}
		PageFetchResult fetchResult = null;
		try {
			fetchResult = pageFetcher.fetchHeader(curURL);
			int statusCode = fetchResult.getStatusCode();
			handlePageStatusCode(curURL, statusCode,
					CustomFetchStatus.getStatusDescription(statusCode));
			if (statusCode != HttpStatus.SC_OK) {
				if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY
						|| statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
					if (myController.getConfig().isFollowRedirects()) {
						String movedToUrl = fetchResult.getMovedToUrl();
						if (movedToUrl == null) {
							return;
						}
						WebURL webURL = new WebURL();
						webURL.setURL(movedToUrl);
						webURL.setParentDocid(curURL.getParentDocid());
						webURL.setParentUrl(curURL.getParentUrl());
						webURL.setDepth(curURL.getDepth());
						webURL.setDocid(-1);
						if (shouldVisit(webURL)
								&& robotstxtServer.allows(webURL)) {
							webURL.setDocid(myController.getNewDocId());
							myController.schedule(webURL);
						}
					}
				} else if (fetchResult.getStatusCode() == CustomFetchStatus.PageTooBig) {
					// Log.v("WebCrawler",
					// "Skipping a page which was bigger than max allowed size: "
					// + curURL.getURL());
				}
				return;
			}

			if (!curURL.getURL().equals(fetchResult.getFetchedUrl())) {
				curURL.setURL(fetchResult.getFetchedUrl());
				curURL.setDocid(myController.getNewDocId());
			}

			Page page = new Page(curURL);
			int docid = curURL.getDocid();
			if (fetchResult.fetchContent(page)
					&& parser.parse(page, curURL.getURL())) {
				ParseData parseData = page.getParseData();
				if (parseData instanceof HtmlParseData) {
					HtmlParseData htmlParseData = (HtmlParseData) parseData;

					List<WebURL> toSchedule = new ArrayList<WebURL>();
					int maxCrawlDepth = myController.getConfig()
							.getMaxDepthOfCrawling();
					for (WebURL webURL : htmlParseData.getOutgoingUrls()) {
						webURL.setParentDocid(docid);
						webURL.setParentUrl(curURL.getURL());
						webURL.setDocid(-1);
						webURL.setDepth((short) (curURL.getDepth() + 1));
						if (maxCrawlDepth == -1
								|| curURL.getDepth() < maxCrawlDepth) {
							if (shouldVisit(webURL)
									&& robotstxtServer.allows(webURL)) {
								webURL.setDocid(myController.getNewDocId());
								toSchedule.add(webURL);
							}
						}

					}
					myController.scheduleAll(toSchedule);
				}
				visit(page);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Log.v("WebCrawler", e.getMessage() + ", while processing: "+
			// curURL.getURL());
		} finally {
			if (fetchResult != null) {
				fetchResult.discardContentIfNotConsumed();
			}
		}
	}

	public Thread getThread() {
		return myThread;
	}

	public void setThread(Thread myThread) {
		this.myThread = myThread;
	}

	public boolean isNotWaitingForNewURLs() {
		return !isWaitingForNewURLs;
	}

}
