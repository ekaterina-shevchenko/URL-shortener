package de.tum.handlers;

import com.sun.net.httpserver.HttpExchange;
import de.tum.utils.HTTPServerUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ForwardedGenerateAliasHandler extends GenerateAliasHandler{
    @Override
    public HTTPServerResponse handleWithResponse(HttpExchange exchange) {
        HTTPServerResponse serverResponse = new HTTPServerResponse();
        try {
            String url = HTTPServerUtils.readRequestBody(exchange);
            log.info("Remote write request received for url {}", url);
            String alias = super.generateAlias(url);
            serverResponse.setBody(alias);
        } catch (Exception e) {
            serverResponse.setBody("Server error");
            serverResponse.setCode(500);
        }
        return serverResponse;
    }
}
