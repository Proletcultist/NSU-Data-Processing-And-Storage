package ru.nsu.zenin.crawler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import ru.nsu.zenin.crawler.exception.CrawlerException;
import ru.nsu.zenin.crawler.exception.HttpException;
import ru.nsu.zenin.util.Result;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class WebMessageCrawler {
    private final HttpClient client;
    private final HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public CompletableFuture<Stream<Result<String>>> crawlAsync(URI uri) {
        return crawlAsync(uri, ConcurrentHashMap.newKeySet());
    }

    private CompletableFuture<Stream<Result<String>>> crawlAsync(URI uri, Set<URI> visited) {
        return CompletableFuture.supplyAsync(
                () -> Result
                    .ofTry(() -> get(uri))
                    .map(resp -> Stream.concat(
                            Stream.of(Result.ofValue(resp.message())),
                            resp
                                .successors()
                                .stream()
                                .map(path -> Result.ofTry(() -> new URI(
                                        uri.getScheme(),
                                        uri.getAuthority(),
                                        "/" + path,
                                        null,
                                        null
                                )))
                                .filter(resUri -> !resUri.isOk() || visited.add(resUri.getValue()))
                                .map(resUri -> resUri
                                    .map(u -> crawlAsync(u, visited))
                                    .or(e -> CompletableFuture.completedFuture(Stream.of(Result.ofException(
                                            new CrawlerException(uri, e)
                                    )))))
                                .reduce(CompletableFuture.completedFuture(Stream.empty()), (fst, snd) -> fst.thenCombine(
                                        snd,
                                        Stream::concat
                                ))
                                .join()
                    ))
                    .or(e -> Stream.of(Result.ofException(new CrawlerException(uri, e)))),
                executor
        );
    }

    protected ServerResponse get(URI uri)
            throws HttpException,
            IOException,
            JsonProcessingException,
            InterruptedException {
        HttpRequest req = HttpRequest.newBuilder().uri(uri).GET().build();

        HttpResponse<String> resp = client.send(req, bodyHandler);

        if (resp.statusCode() != 200) {
            throw new HttpException(resp.statusCode());
        }

        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(resp.body(), ServerResponse.class);
    }
}
