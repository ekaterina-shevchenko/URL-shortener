package de.tum.services.cluster.jobs;

import de.tum.services.PropertiesService;
import de.tum.services.cluster.ClusterMember;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

public class HeartbeatSender implements Runnable{
    private static final Integer IPV4_SIZE = 4;

    @Override
    public void run() {
        try {
            sendHeartbeat();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendHeartbeat() throws IOException {
        List<NetworkInterface> activeNetworkInterfaces =
                Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                .filter(i -> {
                    try {
                        return i.isUp() && !i.isLoopback();
                    } catch (SocketException e) {
                        return false;
                    }
                }).collect(Collectors.toList());
        for (NetworkInterface activeNetworkInterface : activeNetworkInterfaces) {
            Optional<String> ip = Collections.list(activeNetworkInterface.getInetAddresses())
                    .stream()
                    .filter(inetAddress -> inetAddress.getAddress().length == IPV4_SIZE)
                    .map(InetAddress::getHostAddress)
                    .findAny();
            if (ip.isPresent()) {
                ClusterMember clusterMember = new ClusterMember(ip.get(), new Date().getTime());
                DatagramSocket socket = new DatagramSocket();
                PropertiesService propertiesService = PropertiesService.getInstance();
                InetAddress group = InetAddress.getByName(propertiesService.getAppProperty("cluster.multicast"));
                byte[] buf = SerializationUtils.serialize(clusterMember);
                DatagramPacket packet = new DatagramPacket(buf, buf.length, group,
                        Integer.parseInt(propertiesService.getAppProperty("cluster.port")));
                socket.send(packet);
                socket.close();
            }
        }
    }
}
