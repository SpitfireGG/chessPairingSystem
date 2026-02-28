package com.chesspairing.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class HttpUtil {
    private HttpUtil() {
    }

    public static String readBody(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static Map<String, String> parseFormEncoded(String raw) {
        Map<String, String> values = new HashMap<>();
        if (raw == null || raw.isBlank()) {
            return values;
        }

        String[] pairs = raw.split("&");
        for (String pair : pairs) {
            if (pair.isBlank()) {
                continue;
            }
            int equalsIndex = pair.indexOf('=');
            String key;
            String value;
            if (equalsIndex < 0) {
                key = decode(pair);
                value = "";
            } else {
                key = decode(pair.substring(0, equalsIndex));
                value = decode(pair.substring(equalsIndex + 1));
            }
            values.put(key, value);
        }
        return values;
    }

    public static Map<String, String> parseCookies(Headers headers) {
        Map<String, String> cookies = new HashMap<>();
        String cookieHeader = headers.getFirst("Cookie");
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return cookies;
        }

        String[] chunks = cookieHeader.split(";");
        for (String chunk : chunks) {
            String[] kv = chunk.trim().split("=", 2);
            if (kv.length == 2) {
                cookies.put(kv[0].trim(), kv[1].trim());
            }
        }
        return cookies;
    }

    public static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        sendResponse(exchange, statusCode, "application/json; charset=UTF-8", json.getBytes(StandardCharsets.UTF_8));
    }

    public static void sendText(HttpExchange exchange, int statusCode, String text) throws IOException {
        sendResponse(exchange, statusCode, "text/plain; charset=UTF-8", text.getBytes(StandardCharsets.UTF_8));
    }

    public static void sendHtml(HttpExchange exchange, int statusCode, byte[] html) throws IOException {
        sendResponse(exchange, statusCode, "text/html; charset=UTF-8", html);
    }

    public static void sendStatic(HttpExchange exchange, int statusCode, String contentType, byte[] body) throws IOException {
        sendResponse(exchange, statusCode, contentType, body);
    }

    public static void sendMethodNotAllowed(HttpExchange exchange, String allowValue) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowValue);
        sendText(exchange, 405, "Method Not Allowed");
    }

    private static void sendResponse(
        HttpExchange exchange,
        int statusCode,
        String contentType,
        byte[] body
    ) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
