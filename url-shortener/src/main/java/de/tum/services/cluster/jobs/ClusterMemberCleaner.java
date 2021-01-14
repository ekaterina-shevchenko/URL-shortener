package de.tum.services.cluster.jobs;

import de.tum.services.PropertiesService;
import de.tum.services.cluster.ClusterMember;
import de.tum.services.cluster.ClusterService;

import java.util.Collection;
import java.util.Date;

public class ClusterMemberCleaner implements Runnable{
    private final ClusterService clusterService = ClusterService.getInstance();
    private final Integer timeToLive = Integer.parseInt(
            PropertiesService.getInstance().getAppProperty("cluster.timetolive"));

    @Override
    public void run() {
        clusterService.getClusterMembers().stream()
                .filter(this::isExpired)
                .forEach(clusterService::removeClusterMember);
    }

    private boolean isExpired(ClusterMember member) {
        return (member.getCurrentTime() + timeToLive * 1000) < new Date().getTime();
    }
}
