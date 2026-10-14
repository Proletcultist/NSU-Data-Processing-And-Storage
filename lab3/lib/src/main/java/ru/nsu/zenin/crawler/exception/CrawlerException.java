package ru.nsu.zenin.crawler.exception;

import java.net.URI;

public class CrawlerException extends Exception {
    public CrawlerException(URI uri, Throwable cause) {
        super("Error while crawling across " + uri + ": " + cause.getMessage());
    }
}
