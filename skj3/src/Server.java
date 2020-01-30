import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Server {
    private ServerSocket serverSocket;
    private Socket socketServerPlayer;
    static Map<String, String> connectedPlayers = new LinkedHashMap<>();
    static Map<String, PlayerStatus> playersList = new LinkedHashMap<>();
    static Map<String, List<String>> currentGames = new LinkedHashMap<>();
    static int gameId;
    static int playerId = 0;
    private List<Integer> availablePlayerId = new ArrayList<>();
    static DatagramSocket socketBroadcastContests;
    static DatagramPacket packetWithContests;

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            System.out.println("error Server.class -> constructor " + ex);
        }
    }

    public void serverOn() {
        try {
            socketBroadcastContests = new DatagramSocket(9000);
            socketBroadcastContests.setBroadcast(true);
        }catch (IOException io){
            System.out.println("udp socket");
        }
        System.out.println("*******SERVER IS RUNNING*************");
        while (true) {
            try {
                socketServerPlayer = serverSocket.accept();
                if (availablePlayerId.contains(playerId)){
                    while (!availablePlayerId.contains(playerId)){
                        ++playerId;
                    }
                }
                System.out.println("Player " + socketServerPlayer + " us connected to server");
                new Thread(new PlayerHandler(socketServerPlayer)).start();
                ++playerId;
            } catch (IOException ex) {
                System.out.println("error serverOn() -> 18 " + ex);
            }
        }
    }

    public static void main(String[]args){
        new Server(9999).serverOn();
    }
}
