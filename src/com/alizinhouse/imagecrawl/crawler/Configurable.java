
package com.alizinhouse.imagecrawl.crawler;

/**
 * Several core components of crawler4j extend this class
 * to make them configurable.
 *
 */
public abstract class Configurable {

	protected CrawlConfig config;
	
	protected Configurable(CrawlConfig config) {
		this.config = config;
	}
	
	public CrawlConfig getConfig() {
		return config;
	}
}
