package de.tum.handlers;

import com.sun.net.httpserver.HttpExchange;
import de.tum.services.PropertiesService;
import de.tum.services.cluster.ClusterMember;
import de.tum.services.cluster.ClusterService;
import de.tum.utils.HTTPServerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class PrimaryGenerateAliasHandler extends GenerateAliasHandler{
    private final ClusterService clusterService = ClusterService.getInstance();
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            2 * Integer.parseInt(PropertiesService.getInstance().getAppProperty("server.pool.size")));

    public PrimaryGenerateAliasHandler() {
        super();
    }

    @Override
    public HTTPServerResponse handleWithResponse(HttpExchange exchange) {
        HTTPServerResponse serverResponse = new HTTPServerResponse();
        try {
            String url = HTTPServerUtils.readRequestBody(exchange);
            String alias = super.generateAlias(url);
            serverResponse.setBody(alias);
            if (!clusterService.getClusterMembers().isEmpty()) {
                forwardRequest(url, clusterService.getClusterMembers());
            }
        } catch (Exception e) {
            serverResponse.setBody("Server error");
            serverResponse.setCode(500);
        }
        return serverResponse;
    }

    private void forwardRequest(String url, Collection<ClusterMember> members) throws InterruptedException {
        log.info("Remote write requested for nodes {} for url {}", members.stream().map(ClusterMember::toString)
                .reduce((l,r) -> l+ ", " + r).get(), url);
        List<Future> futures = new ArrayList<>();
        for (ClusterMember member : members) {
            futures.add(executorService.submit(new ForwardToNode(url, member)));
        }
        for (Future future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {}
        }
        log.info("Remote write request confirmed for url {}", url);
    }

    @RequiredArgsConstructor
    static class ForwardToNode implements Runnable{
        private final String url;
        private final ClusterMember clusterMember;

        @Override
        public void run() {
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost request = new HttpPost(
                        "http://" + clusterMember.getAddress() + ":" + clusterMember.getPort() + "/forwarded");
                request.setEntity(new StringEntity(url));
                client.execute(request);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
