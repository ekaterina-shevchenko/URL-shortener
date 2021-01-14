package de.tum.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class Dispatcher implements HttpHandler {
    private final HttpHandler resolveAliasHandler = ResolveAliasHandler.getInstance();
    private final Map<String,HttpHandler> postHandlers = new HashMap<>();
    private static final String PATH_DELIMETER = "/";
    private static final String PATH_FORWARDED = "/forwarded";
    private static final String PATH_HEARTBEAT = "/heartbeat";

    public Dispatcher() {
        postHandlers.put(PATH_DELIMETER, new PrimaryGenerateAliasHandler());
        postHandlers.put(PATH_FORWARDED, new ForwardedGenerateAliasHandler());
        postHandlers.put(PATH_HEARTBEAT, new HTTPHeartbeatHandler());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            URI requestURI = exchange.getRequestURI();
            String path = requestURI.getPath();
            if (!path.equals(PATH_HEARTBEAT)) {
                log.info("Received client request {} - {}", exchange.getRequestMethod(), path);
            }
            Optional<String> optionalMapping;
            if (exchange.getRequestMethod().toLowerCase().equals("get")) {
                resolveAliasHandler.handle(exchange);
            } else if (exchange.getRequestMethod().toLowerCase().equals("post")) {
                optionalMapping = postHandlers.keySet().stream().filter(path::equals).findFirst();
                if (optionalMapping.isPresent()) {
                    HttpHandler handler = postHandlers.get(optionalMapping.get());
                    handler.handle(exchange);
                }
            }
        } catch (Exception e) {
            log.error("Error", e);
        }
    }
}
