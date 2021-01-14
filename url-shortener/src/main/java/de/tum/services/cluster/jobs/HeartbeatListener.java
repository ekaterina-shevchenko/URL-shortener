package de.tum.services.cluster.jobs;

import de.tum.services.PropertiesService;
import de.tum.services.cluster.ClusterMember;
import de.tum.services.cluster.ClusterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

@Slf4j
public class HeartbeatListener implements Runnable{
    private final ClusterService clusterService = ClusterService.getInstance();

    @Override
    public void run() {
        PropertiesService propertiesService = PropertiesService.getInstance();
        try (MulticastSocket socket = new MulticastSocket(
                Integer.parseInt(propertiesService.getAppProperty("cluster.port")))){
            InetAddress group = InetAddress.getByName(propertiesService.getAppProperty("cluster.multicast"));
            socket.joinGroup(group);
            log.info("Heartbeat listener service joined multicast group {} and is listening on port: {}",
                    propertiesService.getAppProperty("cluster.multicast"),
                    propertiesService.getAppProperty("cluster.port"));
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                ClusterMember member =  SerializationUtils.deserialize(packet.getData());
                if (!member.isLocal()) {
                    clusterService.updateClusterMember(member);
                }
            }
        } catch (IOException e){
            log.error("Listening to heartbeat failed", e);
        }
    }

}
