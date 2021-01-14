package de.tum.services.cluster;

import de.tum.services.ConsistentHashingService;
import de.tum.services.PropertiesService;
import de.tum.services.cluster.jobs.ClusterMemberCleaner;
import de.tum.services.cluster.jobs.HTTPHeartbeatSender;
import de.tum.services.cluster.jobs.HeartbeatListener;
import de.tum.services.cluster.jobs.HeartbeatSender;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ClusterService {
    private static final ClusterService INSTANCE = new ClusterService();
    private final ConcurrentMap<String, ClusterMember> instances = new ConcurrentHashMap<>();
    private final Set<String> staticInstances = new HashSet(Arrays.asList(
            PropertiesService.getInstance().getAppProperty("cluster.instances").split(",")));
    private final ConsistentHashingService consistentHashingService = ConsistentHashingService.getInstance();

    private ClusterService() {}

    public void startClusterService() {
        ScheduledExecutorService httpHeartbeatSenderService = Executors.newSingleThreadScheduledExecutor();
        httpHeartbeatSenderService.scheduleAtFixedRate(new HTTPHeartbeatSender(),0,3, TimeUnit.SECONDS);

        ScheduledExecutorService heartbeatSenderService = Executors.newSingleThreadScheduledExecutor();
        heartbeatSenderService.scheduleAtFixedRate(new HeartbeatSender(),0,1,TimeUnit.SECONDS);

        Thread heartbeatListener = new Thread(new HeartbeatListener());
        heartbeatListener.start();

        ScheduledExecutorService clusterMemberCleanerService = Executors.newSingleThreadScheduledExecutor();
        clusterMemberCleanerService.scheduleAtFixedRate(new ClusterMemberCleaner(),10,10,TimeUnit.SECONDS);
    }

    public static ClusterService getInstance() {
        return INSTANCE;
    }

    public Collection<ClusterMember> getClusterMembers(){
        return this.instances.values();
    }

    public void updateClusterMember(ClusterMember member) {
        ClusterMember oldValue = this.instances.put(member.getInstanceId(), member);
        if (oldValue == null) {
            log.info("New instance in the cluster: {}", member);
            consistentHashingService.addNode(member);
        }
    }

    public void removeClusterMember(ClusterMember member) {
        this.instances.remove(member.getInstanceId(), member);
        consistentHashingService.removeNode(member);
        log.info("Instance {} left the cluster", member);
    }

    public void addStaticInstance(String ipPort) {
        staticInstances.add(ipPort);
    }

    public Set<String> getStaticInstances() {
        return new HashSet<String>(staticInstances);
    }
}
