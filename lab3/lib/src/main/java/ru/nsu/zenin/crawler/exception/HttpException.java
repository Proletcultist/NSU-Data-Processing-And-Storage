package ru.nsu.zenin.crawler.exception;

public class HttpException extends Exception {
    public HttpException(int code) {
        super("HTTP " + code);
    }
}
