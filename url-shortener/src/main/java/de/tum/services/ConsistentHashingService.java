package de.tum.services;

import com.github.jaskey.consistenthash.ConsistentHashRouter;
import de.tum.services.cluster.ClusterMember;

import java.util.Collections;
import java.util.Date;

public class ConsistentHashingService {
    private static final ConsistentHashingService INSTANCE = new ConsistentHashingService();
    private final ConsistentHashRouter<ClusterMember> consistentHashRouter;

    private ConsistentHashingService() {
        ClusterMember self = new ClusterMember("localhost",new Date().getTime());
        consistentHashRouter = new ConsistentHashRouter<>(Collections.singleton(self),
                self.getWeight(), String::hashCode);
    }

    public static ConsistentHashingService getInstance() {
        return INSTANCE;
    }

    public void addNode(ClusterMember member) {
        consistentHashRouter.addNode(member, member.getWeight());
    }

    public void removeNode(ClusterMember member) {
        consistentHashRouter.removeNode(member);
    }

    public boolean isOurs(String alias) {
        ClusterMember aliasNode = consistentHashRouter.routeNode(alias);
        return aliasNode.isLocal();
    }

    public ClusterMember getStoringNode(String alias) {
        return consistentHashRouter.routeNode(alias);
    }
}
