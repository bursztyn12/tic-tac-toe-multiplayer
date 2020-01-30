import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PlayerHandler implements Runnable {
    private String playerIpPort;
    private Socket socketServerToPlayer;
    private String opponentIpPort;
    private volatile boolean isLogout = false;
    private String playerAddress;
    private String playerPort;
    private PrintWriter streamIdToPlayer;
    private PrintWriter streamGameDataToPlayer1;
    private PrintWriter streamGameDataToPlayer2;
    private BufferedReader streamRequestFromPlayer;
    private PlayerStatus playerStatus;
    private Socket socketServerToPlayer1;
    private Socket socketServerToPlayer2;
    private BufferedReader streamMoveFromPlayer1;
    private BufferedReader streamMoveFromPlayer2;
    private PrintWriter streamMoveToPlayer1;
    private PrintWriter streamMoveToPlayer2;
    private String[][] ticTacToeBoardArray;
    private int thisPlayerId;
    private List<String> gameList;

    public PlayerHandler(Socket socketServerToPlayer) {
        playerStatus = PlayerStatus.CONNECTED;

        this.socketServerToPlayer = socketServerToPlayer;
        this.thisPlayerId = Server.playerId;

        playerAddress = socketServerToPlayer.getInetAddress().getHostAddress();

        playerPort = String.valueOf(socketServerToPlayer.getPort());

        playerIpPort = playerAddress + ":" + playerPort + "X";

        Server.connectedPlayers.put(String.valueOf(thisPlayerId), playerIpPort);

        try {
            streamRequestFromPlayer = new BufferedReader(new InputStreamReader(socketServerToPlayer.getInputStream()));
        } catch (IOException ex) {
            System.out.println(ex);
        }

        System.out.println(socketServerToPlayer.getInetAddress().getHostAddress() + " , " + socketServerToPlayer.getPort());
    }

    @Override
    public void run() {
        sendIdToPlayer();
        while (playerStatus != PlayerStatus.LOGOUT) {
            System.out.println(Server.connectedPlayers);
            requestsFromPlayer();
            System.out.println("Current " + thisPlayerId + " player status " + playerStatus);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("FINISHING RUN " + getHandlerId());
    }

    public void sendIdToPlayer() {
        try {
            streamIdToPlayer = new PrintWriter(socketServerToPlayer.getOutputStream(), true);
            streamIdToPlayer.println(thisPlayerId);
            streamIdToPlayer.println("");
        } catch (IOException ex) {
            System.out.println("error sendIdToPlayer()-> 57 line " + ex);
        }
    }

    public void requestsFromPlayer() {
        String read;
        StringBuilder sb = new StringBuilder();
        try {
            while ((read = streamRequestFromPlayer.readLine()) != null) {
                sb.append(read);
                if (read.isEmpty()) {
                    String request = sb.toString();
                    sb = new StringBuilder();
                    new Thread(() -> {
                        checkPlayerStatus(request);
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            try {
                streamRequestFromPlayer.close();
            } catch (IOException io) {
                System.out.println(io);
            }
            Thread.currentThread().interrupt();
        }
    }

    public void checkPlayerStatus(String status) {
        switch (status) {
            case "PLAY":
                playerStatus = PlayerStatus.PLAY;
                addPlayerToGame();
                break;
            case "LIST":
                playerStatus = PlayerStatus.LIST;
                sendListToPlayer();
                break;
            case "LOGOUT":
                playerStatus = PlayerStatus.LOGOUT;
                deletePlayer();
                break;
        }
    }

    public void addPlayerToGame() {
        Server.playersList.put(String.valueOf(thisPlayerId), PlayerStatus.NOT_PLAYING);
        chooseOpponent();
    }

    public void chooseOpponent() {
        System.out.println(Server.playersList);
        for (String opponentId : Server.playersList.keySet()) {
            if (!opponentId.equals(String.valueOf(thisPlayerId))) {
                playerStatus = Server.playersList.get(opponentId);
                if (playerStatus != PlayerStatus.PLAYING) {
                    opponentIpPort = Server.connectedPlayers.get(opponentId);
                    Server.playersList.computeIfPresent(opponentId, (k, v) -> v = PlayerStatus.PLAYING);
                    Server.playersList.put(String.valueOf(thisPlayerId), PlayerStatus.PLAYING);

                    String tmpP1v1 = Server.connectedPlayers.get(String.valueOf(thisPlayerId));
                    String[] tmpP1v2 = tmpP1v1.split("X");
                    String[] p1 = tmpP1v2[0].split(":");
                    String[] tmpP2 = opponentIpPort.split("X");
                    String[] p2 = tmpP2[0].split(":");

                    Server.currentGames.put(thisPlayerId + " vs " + opponentId, new ArrayList<>());
                    System.out.println(Server.currentGames);

                    new Thread(() -> {
                        gameList = new ArrayList<>();
                        sendGameDataToPlayerAndOpponent(String.valueOf(thisPlayerId), p1[0], opponentId, p2[0]);
                    }).start();

                    break;
                }
            }
        }
    }

    public void sendGameDataToPlayerAndOpponent(String player1Id, String player1Address, String player2Id, String player2Address) {
        try {
            Thread.sleep(5000);
            System.out.println(player1Id + " " + player1Address);
            System.out.println(player2Id + " " + player2Address);

            socketServerToPlayer1 = new Socket(InetAddress.getByName(player1Address), Integer.parseInt(player1Id) + 1);
            socketServerToPlayer2 = new Socket(InetAddress.getByName(player2Address), Integer.parseInt(player2Id) + 1);

            Thread.sleep(10);

            sendGameDataToPlayer1();
            sendGameDataToPlayer2();

            //Thread.sleep(1000);

            sendMoveFromPlayerToPlayer(player1Id + " vs " + player2Id);

            System.out.println("playerList before " + Server.playersList);

            Server.playersList.remove(player1Id);
            Server.playersList.remove(player2Id);

            System.out.println("playerList after " + Server.playersList);

            try {
                socketServerToPlayer1.close();
                socketServerToPlayer2.close();
                streamGameDataToPlayer1.close();
                streamGameDataToPlayer2.close();
                streamMoveToPlayer1.close();
                streamMoveFromPlayer1.close();
                streamMoveToPlayer2.close();
                streamMoveFromPlayer2.close();
            } catch (IOException io) {
                System.out.println(io);
            }
        } catch (IOException | InterruptedException ex) {
            System.out.println(" 154 " + ex);
        }
    }

    public void sendGameDataToPlayer1() {
        try {
            streamGameDataToPlayer1 = new PrintWriter(socketServerToPlayer1.getOutputStream(), true);
            streamGameDataToPlayer1.println("FOUND");
            streamGameDataToPlayer1.println("");

            Thread.sleep(10);

            streamGameDataToPlayer1.println("X:1");
            streamGameDataToPlayer1.println("");
        } catch (IOException | InterruptedException ex) {
            System.out.println(ex);
        }
    }

    public void sendGameDataToPlayer2() {
        try {
            streamGameDataToPlayer2 = new PrintWriter(socketServerToPlayer2.getOutputStream(), true);
            streamGameDataToPlayer2.println("FOUND");
            streamGameDataToPlayer2.println("");

            Thread.sleep(10);

            streamGameDataToPlayer2.println("O:0");
            streamGameDataToPlayer2.println("");
        } catch (IOException | InterruptedException ex) {
            System.out.println(ex);
        }
    }

    public void sendMoveFromPlayerToPlayer(String players) {
        System.out.println("sendMoveFromPLayerToPlayer");
        ticTacToeBoardArray = new String[3][3];

        try {
            try {
                streamMoveFromPlayer1 = new BufferedReader(new InputStreamReader(socketServerToPlayer1.getInputStream()));
            } catch (IOException io) {
                System.out.println("movefromplayer1");
            }
            try {
                streamMoveToPlayer1 = new PrintWriter(socketServerToPlayer1.getOutputStream(), true);
            } catch (IOException io) {
                System.out.println("streammovetoplayer1");
            }
            try {
                streamMoveFromPlayer2 = new BufferedReader(new InputStreamReader(socketServerToPlayer2.getInputStream()));
            }catch (IOException io){
                System.out.println("movefromplayer2");
            }
            try {
                streamMoveToPlayer2 = new PrintWriter(socketServerToPlayer2.getOutputStream(), true);
            }catch (IOException io){
                System.out.println("movetoplayer2");
            }

            while (true) {

                String read;
                StringBuilder sb = new StringBuilder();

                while (!(read = streamMoveFromPlayer1.readLine()).isEmpty()) {
                    sb.append(read);
                }

                StringBuilder finalSb = sb;
                Server.currentGames.computeIfPresent(players, (k, v) -> {
                    v.add(finalSb.toString());
                    return v;
                });
                System.out.println(Server.currentGames);

                gameList.add(sb.toString());
                System.out.println("player1 move : " + sb.toString());

                if (updatePlayersGameBoardState(sb.toString())) {
                    streamMoveToPlayer2.println(sb.toString());
                    streamMoveToPlayer2.println("");
                    broadcast();
                    break;
                }

                broadcast();

                streamMoveToPlayer2.println(sb.toString());
                streamMoveToPlayer2.println("");

                sb = new StringBuilder();

                while (!(read = streamMoveFromPlayer2.readLine()).isEmpty()) {
                    sb.append(read);
                }

                StringBuilder finalSb1 = sb;
                Server.currentGames.computeIfPresent(players, (k, v) -> {
                    v.add(finalSb1.toString());
                    return v;
                });
                System.out.println(Server.currentGames);

                gameList.add(sb.toString());
                System.out.println("Current games " + Server.currentGames);
                System.out.println("player2 move : " + sb.toString());

                if (updatePlayersGameBoardState(sb.toString())) {
                    streamMoveToPlayer1.println(sb.toString());
                    streamMoveToPlayer1.println("");
                    broadcast();
                    break;
                }

                streamMoveToPlayer1.println(sb.toString());
                streamMoveToPlayer1.println("");

                broadcast();
            }
            endBroadCast(players);
            Server.currentGames.remove(players);
            System.out.println("server status : game over");
        } catch (IOException io) {
            System.out.println(io);
        }
    }

    public boolean updatePlayersGameBoardState(String sentData) {
        boolean isFound = false;
        String[] move = sentData.split(":");
        int row = Integer.parseInt(move[1]);
        int col = Integer.parseInt(move[2]);
        ticTacToeBoardArray[row][col] = move[0];

        if (checkRowStatus() || checkColStatus() || checkDiagonalStatus() || checkDraw()) {
            isFound = true;
        }

        return isFound;
    }

    public boolean checkRowStatus() {
        boolean isFound = false;
        int countX = 0;
        int countO = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (ticTacToeBoardArray[row][col] != null) {
                    if (ticTacToeBoardArray[row][col].equals("X")) {
                        ++countX;
                    } else if (ticTacToeBoardArray[row][col].equals("O")) {
                        ++countO;
                    }
                }
            }
            if (countX == 3) {
                isFound = true;
                System.out.println("found row");
                break;
            } else if (countO == 3) {
                isFound = true;
                System.out.println("found row");
                break;
            } else {
                countX = 0;
                countO = 0;
            }
        }
        return isFound;
    }

    public boolean checkColStatus() {
        boolean isFound = false;
        int countX = 0;
        int countO = 0;
        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                if (ticTacToeBoardArray[row][col] != null) {
                    if (ticTacToeBoardArray[row][col].equals("X")) {
                        ++countX;
                    } else if (ticTacToeBoardArray[row][col].equals("O")) {
                        ++countO;
                    }
                }
            }
            if (countX == 3) {
                isFound = true;
                System.out.println("found col");
                break;
            } else if (countO == 3) {
                isFound = true;
                System.out.println("found col");
                break;
            } else {
                countX = 0;
                countO = 0;
            }
        }
        return isFound;
    }

    public boolean checkDiagonalStatus() {
        boolean isFound = false;
        int countX1 = 0;
        int countO1 = 0;
        int countX2 = 0;
        int countO2 = 0;
        int diag2 = 2;
        for (int diag = 0; diag < 3; diag++) {
            if (ticTacToeBoardArray[diag][diag] != null) {
                if (ticTacToeBoardArray[diag][diag].equals("X")) {
                    ++countX1;
                } else if (ticTacToeBoardArray[diag][diag].equals("O")) {
                    ++countO1;
                }
            }
            if (ticTacToeBoardArray[diag][diag2] != null) {
                if (ticTacToeBoardArray[diag][diag2].equals("X")) {
                    ++countX2;
                } else if (ticTacToeBoardArray[diag][diag2].equals("O")) {
                    ++countO2;
                }
                --diag2;
            }
        }
        if (countX1 == 3) {
            isFound = true;
            System.out.println("found diag");
        } else if (countO1 == 3) {
            isFound = true;
            System.out.println("found diag");
        } else if (countX2 == 3) {
            isFound = true;
            System.out.println("found diag");
        } else if (countO2 == 3) {
            isFound = true;
            System.out.println("found diag");
        }
        return isFound;
    }

    public boolean checkDraw() {
        boolean isDraw = false;
        if (gameList.size() == 9) {
            isDraw = true;
        }
        return isDraw;
    }

    public void sendListToPlayer() {
        String value;
        try {
            streamIdToPlayer = new PrintWriter(socketServerToPlayer.getOutputStream(), true);
            for (String key : Server.connectedPlayers.keySet()) {
                if (!key.equals(String.valueOf(thisPlayerId))) {
                    value = Server.connectedPlayers.get(key);
                    streamIdToPlayer.println(value);
                }
            }
            streamIdToPlayer.println("");
        } catch (IOException ex) {
            System.out.println("error sendListToPlayer()-> 89 line " + ex);
        }
    }

    public void broadcast() {
        System.out.println("sending broadcast");
        Set<String> set = Server.currentGames.keySet();
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        List<String> tmpVal;
        for (String k : set) {
            tmpVal = Server.currentGames.get(k);
            for (String v : tmpVal) {
                sb2.append(v).append(",");
            }
            sb.append(k).append("-").append(sb2.toString()).append("N");
        }
        byte[] buffer = sb.toString().getBytes();
        try {
            Server.packetWithContests = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("255.255.255.255"), 9001);
            Server.socketBroadcastContests.send(Server.packetWithContests);
        } catch (IOException io) {
            System.out.println(io);
        }
    }

    public void endBroadCast(String players){
        Server.currentGames.computeIfPresent(players,(k,v)->{
            v.add("null:null:null");
            return v;
        });

        broadcast();
    }

    public void deletePlayer() {
        Server.connectedPlayers.remove(String.valueOf(thisPlayerId));
        Server.playerId--;
    }

    public int getHandlerId() {
        return thisPlayerId;
    }
}
