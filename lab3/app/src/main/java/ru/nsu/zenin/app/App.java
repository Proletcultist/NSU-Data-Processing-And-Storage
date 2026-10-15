package ru.nsu.zenin.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import ru.nsu.zenin.crawler.exception.HttpException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.time.Duration;
import ru.nsu.zenin.crawler.WebMessageCrawler;
import ru.nsu.zenin.crawler.ServerResponse;

public class App {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Expected crawling start URI as an only argument, got: " + args + " args");
            System.exit(-1);
        }

        URI uri;
        try {
            uri = new URI(args[0]);
        } catch (URISyntaxException e) {
            System.out.println("URI parsing exception: " + e.getMessage());
            System.exit(-1);
            // To convince javac that uri cannot be uninitialized
            return;
        }

        HttpClient client =
                HttpClient
            .newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        WebMessageCrawler crawler = new WebMessageCrawler(client) {
            @Override
            protected ServerResponse get(URI uri)
                    throws HttpException,
                    IOException,
                    JsonProcessingException,
                    InterruptedException {
                System.err.println("Crawling " + uri.toString());
                return super.get(uri);
            }
        };

        crawler
            .crawlAsync(uri)
            .join()
            .filter(res -> {
                if (res.isOk()) {
                    return true;
                } else {
                    System.err.println("Error: " + res.getException().getMessage());
                    return false;
                }
            })
            .map(res -> res.getValue())
            .sorted()
            .forEach(msg -> System.out.println(msg));
    }
}
