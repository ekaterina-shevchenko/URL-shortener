package de.tum.handlers;

import com.sun.net.httpserver.HttpExchange;
import de.tum.services.cluster.ClusterMember;
import de.tum.services.cluster.ClusterService;
import de.tum.utils.HTTPServerUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class HTTPHeartbeatHandler extends GeneralHttpHandler {
    private final HTTPServerResponse serverResponse = new HTTPServerResponse();
    private final ClusterService clusterService = ClusterService.getInstance();

    public HTTPHeartbeatHandler() {
        serverResponse.setBody(ClusterMember.INSTANCE_ID + "," + ClusterMember.WEIGHT);
    }

    @Override
    public HTTPServerResponse handleWithResponse(HttpExchange exchange) {
        try {
            String requestBody = HTTPServerUtils.readRequestBody(exchange);
            String address = exchange.getRemoteAddress().getAddress().getHostAddress();
            clusterService.addStaticInstance(address + ":" + requestBody);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return serverResponse;
    }

    @Override
    public boolean shouldLogResponse() {
        return false;
    }
}
