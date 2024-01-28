package Musician;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.google.gson.Gson;

public class Musician {

    private static final String MULTICAST_ADDRESS = "239.255.22.5";
    private static final int PORT = 9904;
    private final UUID uuid;
    private final String instrumentSound;
    private static final Map<String, String> instrumentSounds = new HashMap<>();

    static {
        instrumentSounds.put("piano", "ti-ta-ti");
        instrumentSounds.put("trumpet", "pouet");
        instrumentSounds.put("flute", "trulu");
        instrumentSounds.put("violin", "gzi-gzi");
        instrumentSounds.put("drum", "boum-boum");
    }

    public Musician(String instrument) {
        this.uuid = UUID.randomUUID();
        this.instrumentSound = instrumentSounds.getOrDefault(instrument, "unknown");
    }

    public void play() {
        System.out.println("play");
        try (DatagramSocket socket = new DatagramSocket();){
            //InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            System.out.println("IN");

            while (true) {
                System.out.println("setup");
                String messageJson = new Gson().toJson(new SoundMessage(uuid.toString(), instrumentSound));
                byte[] buf = messageJson.getBytes();
                var dest_address = new InetSocketAddress(MULTICAST_ADDRESS, PORT);
                DatagramPacket packet = new DatagramPacket(buf, buf.length, dest_address);
                System.out.println("send");
                socket.send(packet);
                try {
                    System.out.println("sleep");
                    Thread.sleep(1000); // Sending the datagram every second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Error: Choose an instrument");
            return;
        }
        System.out.println("running program");
        new Musician(args[0]).play();
    }

    private static class SoundMessage {
        private final String uuid;
        private final String sound;

        public SoundMessage(String uuid, String sound) {
            this.uuid = uuid;
            this.sound = sound;
        }
    }
}