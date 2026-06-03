package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        try (OutputStream os = ex.getResponseBody()) {
            ex.getResponseHeaders().set("Content-Type", CT_JSON);
            ex.sendResponseHeaders(status, 0);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            os.write(bytes);
        }
    }

    protected void sendWithoutBody(HttpExchange ex, int status) throws java.io.IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, -1);
    }
}