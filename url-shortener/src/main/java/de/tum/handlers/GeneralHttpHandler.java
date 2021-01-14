package de.tum.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.tum.utils.HTTPServerUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public abstract class GeneralHttpHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        HTTPServerResponse serverResponse = handleWithResponse(httpExchange);
        HTTPServerUtils.writeResponse(httpExchange, serverResponse.getCode(), serverResponse.getBody());
        if (shouldLogResponse()) {
            log.info("Sent response to client/communicator-node {} - {}", serverResponse.getCode(), serverResponse.getBody());
        }
    }

    public abstract HTTPServerResponse handleWithResponse(HttpExchange exchange);

    public boolean shouldLogResponse() {
        return true;
    }
}
