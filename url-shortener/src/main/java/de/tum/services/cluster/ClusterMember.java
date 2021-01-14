package de.tum.services.cluster;

import com.github.jaskey.consistenthash.Node;
import de.tum.services.PropertiesService;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.UUID;

@Slf4j
@Getter @Setter
@ToString
public class ClusterMember implements Node, Serializable {
    public static final String INSTANCE_ID = UUID.randomUUID().toString();
    public static final int WEIGHT;

    static  {
        long totalJvmMemory = Runtime.getRuntime().totalMemory();
        long bytesInGigabyte = 1024 * 1024 * 1024;
        WEIGHT = (int) (totalJvmMemory / bytesInGigabyte + 1) * Integer.parseInt(PropertiesService.getInstance().getAppProperty("cluster.weight.multiplier"));
        log.info("Instance with id {} and weight {} has been started", INSTANCE_ID, WEIGHT);
    }

    @ToString.Include
    private final String address;
    @ToString.Include
    private Integer port;
    @ToString.Include
    private final Long currentTime;
    @ToString.Include
    private String instanceId = INSTANCE_ID;
    @ToString.Include
    private int weight = WEIGHT ;

    public boolean isLocal() {
        return instanceId.equals(INSTANCE_ID);
    }

    @Override
    public String getKey() {
        return instanceId;
    }

    public ClusterMember(String address, Long currentTime) {
        this.address = address;
        this.currentTime= currentTime;
        this.port = Integer.parseInt(PropertiesService.getInstance().getAppProperty("server.port"));
    }

}
