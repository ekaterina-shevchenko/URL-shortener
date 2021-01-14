package de.tum.services.cluster.jobs;

import de.tum.services.cluster.ClusterMember;
import de.tum.services.cluster.ClusterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class HTTPHeartbeatSender implements Runnable{
    private final ClusterService clusterService = ClusterService.getInstance();
    private final ExecutorService executorService = Executors.newFixedThreadPool(8);

    public HTTPHeartbeatSender() {
        log.info("HTTP heartbeat sender has been initialized");
    }

    @Override
    public void run() {
        try (CloseableHttpClient client = HttpClients.createMinimal()) {
            Set<String> staticInstances = clusterService.getStaticInstances();
            Collection<Future<ClusterMember>> futures = new ArrayList<>();
            for (String staticInstance : staticInstances) {
                futures.add(executorService.submit(new HTTPRequestSender(staticInstance, client)));
            }
            Thread.sleep(3000);
            for (Future<ClusterMember> future : futures) {
                try {
                    ClusterMember newMember = future.get(0, TimeUnit.SECONDS);
                    if (!newMember.isLocal()) {
                        clusterService.updateClusterMember(newMember);
                    }
                } catch (ExecutionException | TimeoutException e) {
                    log.debug("Could not send http heartbeat to instance", e);
                }
            }
        } catch (IOException | InterruptedException e){
            // Shutdown gracefully
        }
    }

    private static class HTTPRequestSender implements Callable<ClusterMember> {
        private final String staticInstance;
        private final CloseableHttpClient client;

        public HTTPRequestSender(String staticInstance, CloseableHttpClient client) {
            this.staticInstance = staticInstance;
            this.client = client;
        }

        @Override
        public ClusterMember call() throws Exception {
            String[] ipPort = staticInstance.split(":");
            HttpPost request = new HttpPost("http://" + staticInstance + "/heartbeat");
            request.setEntity(new StringEntity(ipPort[1]));
            CloseableHttpResponse response = client.execute(request);
            HttpEntity entity =  response.getEntity();
            String responseBody = IOUtils.toString(entity.getContent(), Charsets.UTF_8);
            String[] responseBodyParts = responseBody.split(",");
            ClusterMember member = new ClusterMember(ipPort[0], new Date().getTime());
            member.setPort(Integer.parseInt(ipPort[1]));
            member.setInstanceId(responseBodyParts[0]);
            member.setWeight(Integer.parseInt(responseBodyParts[1]));
            return member;
        }
    }
}
