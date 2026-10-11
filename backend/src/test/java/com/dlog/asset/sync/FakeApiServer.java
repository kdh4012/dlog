package com.dlog.asset.sync;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/** 외부 API 응답을 흉내 내는 로컬 HTTP 서버. 경로별 핸들러가 (상태코드, 본문)을 돌려준다 */
public class FakeApiServer implements AutoCloseable {

    public record Response(int status, String body) {
        public static Response ok(String body) {
            return new Response(200, body);
        }
    }

    private final HttpServer server;
    /** 받은 요청 기록: "METHOD path?query" */
    public final List<String> requests = new CopyOnWriteArrayList<>();

    public FakeApiServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.start();
    }

    public FakeApiServer route(String path, Function<HttpExchange, Response> handler) {
        server.createContext(path, exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            requests.add(exchange.getRequestMethod() + " " + path + (query == null ? "" : "?" + query));
            Response response = handler.apply(exchange);
            byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(response.status(), bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        return this;
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public long count(String prefix) {
        return requests.stream().filter(r -> r.startsWith(prefix)).count();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
