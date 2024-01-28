package Auditor;

import com.google.gson.Gson;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Auditor {

    private static final int UDP_PORT = 9904;
    private static final String MULTICAST_ADDRESS = "239.255.22.5";
    private static final int TCP_PORT = 2205;
    private final Map<String, MusicianInfo> activeMusicians = new ConcurrentHashMap<>();
    private final Map<String, String> soundToInstrumentMap = Map.of(
        "ti-ta-ti", "piano",
        "pouet", "trumpet",
        "trulu", "flute",
        "gzi-gzi", "violin",
        "boum-boum", "drum"
    );

    public static void main(String[] args) {
        Auditor auditor = new Auditor();
        auditor.startUdpListener();
        auditor.startTcpServer();
    }

    private void startUdpListener() {
        new Thread(() -> {
            try (MulticastSocket socket = new MulticastSocket(UDP_PORT)) {
                InetSocketAddress group_address =  new InetSocketAddress(MULTICAST_ADDRESS, UDP_PORT);
                NetworkInterface netif = NetworkInterface.getByName("eth0");
                socket.joinGroup(group_address, netif);

                while (true) {
                    byte[] buf = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());
                    processReceivedMessage(received);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void processReceivedMessage(String messageJson) {
        Gson gson = new Gson();
        SoundMessage soundMessage = gson.fromJson(messageJson, SoundMessage.class);
        String instrument = soundToInstrumentMap.get(soundMessage.sound);
        if (instrument == null) {
            System.out.println("Unknown sound received: " + soundMessage.sound);
            return;
        }

        MusicianInfo musicianInfo = new MusicianInfo(
            soundMessage.uuid,
            instrument,
            System.currentTimeMillis()
        );
        activeMusicians.put(musicianInfo.uuid, musicianInfo);
        removeInactiveMusicians();
    }

    private void startTcpServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
                while (true) {
                    try (Socket clientSocket = serverSocket.accept();
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                        removeInactiveMusicians();
                        Gson gson = new Gson();
                        String jsonResponse = gson.toJson(new ArrayList<>(activeMusicians.values()));
                        out.println(jsonResponse);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void removeInactiveMusicians() {
        long currentTime = System.currentTimeMillis();
        activeMusicians.entrySet().removeIf(entry -> currentTime - entry.getValue().lastActivity > 5000);
    }
    private static class SoundMessage {
        String uuid;
        String sound;
    }

    private static class MusicianInfo {
        String uuid;
        String instrument;
        long lastActivity;

        MusicianInfo(String uuid, String instrument, long lastActivity){
            this.uuid = uuid;
            this.instrument = instrument;
            this.lastActivity = lastActivity;
        }
    }
}