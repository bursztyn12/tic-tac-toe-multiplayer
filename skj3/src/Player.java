//s18911

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Player extends Application {
    private HBox playerGui;
    private VBox listAndId;
    private VBox buttonsSettings;
    private VBox watchOrGame;
    private GridPane ticTacToeBord;
    private Label yourId;
    private Label availableOpponents;
    private Label statusConnection;
    private Label statusGame;
    private List<Label> cellsList = new ArrayList<>();
    private Button logout;
    private Button connect;
    private Button play;
    private Button list;
    private Button restart;
    private Button watch;
    private Button game;
    private TableView<Opponent> playersListTable;
    private TableView<Games> gamesListTable;
    private TableColumn<Opponent, String> playerIp;
    private TableColumn<Opponent, String> playerId;
    private TableColumn<Games, String> players;
    private Socket socketPlayerServer;
    private InputStream getIdFromServer;
    private PrintWriter streamRequestToServer;
    private BufferedReader streamReadListFromServer;
    private int ip;
    private int port;
    private List<Opponent> opponentsList;
    private Label[][] ticTacToeCellArray = new Label[3][3];
    private String sign;
    private String id;
    private boolean isStarting = false;
    private ServerSocket socketServerToPlayer;
    private Socket socketPlayerToGameThread = null;
    private PrintWriter streamPlayerToGameThread;
    private BufferedReader streamGameThreadToPlayer;
    private boolean isMoved = false;
    private volatile boolean isGameOver = false;
    private String[] dataMove;
    private int row;
    private int col;
    private volatile boolean isYourTurn = false;
    private Label yourSign;
    private Label whoStarting;
    private List<Label> gameList = new ArrayList<>();
    private boolean isWatch = false;
    private DatagramSocket socketContests;
    private DatagramPacket packetWithContestsData;
    private List<Games> gamesList = new ArrayList<>();
    private List<String> playersList = new ArrayList<>();
    private List<Thread> watchGames = new ArrayList<>();
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private Map<String, List<String>> playersAndGameStateMap = new LinkedHashMap<>();
    private volatile boolean isPlayingGame = true;

    @Override
    public void start(Stage stage) {
        playerGui = new HBox();
        playerGui.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));

        listAndId = new VBox();
        buttonsSettings = new VBox();
        watchOrGame = new VBox();

        ticTacToeBord = new GridPane();

        playersListTable = new TableView<>();
        playersListTable.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        playersListTable.setPrefSize(150, 400);

        gamesListTable = new TableView<>();
        gamesListTable.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        gamesListTable.setPrefSize(150, 400);

        yourId = new Label("Your ID : -");
        yourId.setFont(Font.font("Verdana", 12));
        yourId.setPrefSize(150, 25);
        yourId.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        listAndId.getChildren().add(0, yourId);

        yourSign = new Label("Sign : -");
        yourSign.setFont(Font.font("Verdana", 12));
        yourSign.setPrefSize(150, 25);
        yourSign.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        listAndId.getChildren().add(1, yourSign);

        whoStarting = new Label("Starter : -");
        whoStarting.setFont(Font.font("Verdana", 12));
        whoStarting.setPrefSize(150, 25);
        whoStarting.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        listAndId.getChildren().add(2, whoStarting);

        availableOpponents = new Label("Available opponents");
        availableOpponents.setFont(Font.font("Verdana", 12));
        availableOpponents.setPrefSize(150, 25);
        availableOpponents.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        listAndId.getChildren().add(3, availableOpponents);

        playerIp = new TableColumn<>("IP");
        playerIp.setPrefWidth(75);
        playerIp.setCellValueFactory(new PropertyValueFactory<>("Ip"));

        playerId = new TableColumn<>("PORT");
        playerId.setPrefWidth(75);
        playerId.setCellValueFactory(new PropertyValueFactory<>("Port"));

        players = new TableColumn<>("CONTESTS");
        players.setPrefWidth(150);
        players.setCellValueFactory(new PropertyValueFactory<>("Players"));
        players.setCellFactory(column -> {
            TableCell<Games, String> cell = new TableCell<Games, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(item);
                }
            };
            cell.setOnMouseClicked(event -> {
                String playersId = cell.getItem();
                for (Games games : gamesList) {
                    if (games.getPlayers().equals(playersId)) {
                        System.out.println("Chosen game : " + playersId + " , state- " + games.getGameState());
                        if (watchGames.size() > 0) {
                            for (Thread thread : watchGames) {
                                thread.interrupt();
                            }
                        }
                        isPlayingGame = true;

                        Platform.runLater(()->{
                            ticTacToeBord.getChildren().removeAll(cellsList);
                            generateList(3);
                            ticTacToeBord.setDisable(true);
                        });

                        Thread monitorGame = new Thread(() -> {
                            watchGame(playersId);
                            gamesListTable.getItems().remove(games);
                            gamesList.remove(games);
                            playersList.remove(playersId);
                            playersAndGameStateMap.remove(playersId);
                        }, playersId);
                        monitorGame.start();
                        watchGames.add(monitorGame);
                    }
                }
            });
            return cell;
        });

        playersListTable.getColumns().addAll(playerIp, playerId);
        gamesListTable.getColumns().add(players);

        listAndId.getChildren().add(4, playersListTable);

        generateList(3);

        list = new Button("LIST");
        list.setPrefSize(265, 100);
        list.setFont(Font.font("Verdana", 12));
        list.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        list.setOnMouseClicked(event -> {
            list();
        });
        buttonsSettings.getChildren().add(0, list);

        play = new Button("PLAY");
        play.setFont(Font.font("Verdana", 12));
        play.setPrefSize(265, 100);
        play.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        play.setOnMouseClicked(event -> {
            play();
        });
        buttonsSettings.getChildren().add(1, play);

        connect = new Button("CONNECT");
        connect.setFont(Font.font("Verdana", 12));
        connect.setPrefSize(265, 100);
        connect.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        connect.setOnMouseClicked(event -> {
            connect();
        });
        buttonsSettings.getChildren().add(2, connect);

        logout = new Button("LOGOUT");
        logout.setFont(Font.font("Verdana", 12));
        logout.setPrefSize(265, 100);
        logout.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        logout.setOnMouseClicked(event -> {
            logout();
        });
        buttonsSettings.getChildren().add(3, logout);

        restart = new Button("RESTART");
        restart.setFont(Font.font("Verdana", 12));
        restart.setPrefSize(265, 66);
        restart.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        restart.setOnMouseClicked(event -> {
            restartTicTacToeBord();
        });
        buttonsSettings.getChildren().add(4, restart);

        watch = new Button("WATCH");
        watch.setFont(Font.font("Verdana", 11));
        watch.setPrefSize(150, 50);
        watch.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        watch.setOnMouseClicked(event -> {
            isWatch = true;
            listAndId.getChildren().remove(playersListTable);
            availableOpponents.setText("Payer1 vs Player2");
            statusGame.setText("GAME STATUS : " + GameStatus.WATCHING);
            listAndId.getChildren().add(4, gamesListTable);
            getContestsFromServer();
        });
        listAndId.getChildren().add(5, watch);

        game = new Button("BACK TO GAME");
        game.setFont(Font.font("Verdana", 11));
        game.setPrefSize(150, 50);
        game.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        game.setOnMouseClicked(event -> {
            if (isWatch) {
                listAndId.getChildren().remove(gamesListTable);
                availableOpponents.setText("Available opponents");
                listAndId.getChildren().add(4, playersListTable);
                statusGame.setText("GAME STATUS : " + GameStatus.OFF);
                generateList(3);
            }
            isWatch = false;
        });
        listAndId.getChildren().add(6, game);

        statusConnection = new Label("CONNECTION STATUS : " + PlayerStatus.NOT_CONNECTED);
        statusConnection.setFont(Font.font("Verdana", 12));
        statusConnection.setPrefSize(265, 66);
        statusConnection.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        buttonsSettings.getChildren().add(5, statusConnection);

        statusGame = new Label("GAME STATUS : GAME OFF");
        statusGame.setFont(Font.font("Verdana", 12));
        statusGame.setPrefSize(265, 67);
        statusGame.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        buttonsSettings.getChildren().add(6, statusGame);

        playerGui.getChildren().add(0, listAndId);
        playerGui.getChildren().add(1, ticTacToeBord);
        playerGui.getChildren().add(2, buttonsSettings);

        stage.setTitle("Tic Tac Toe");
        stage.setScene(new Scene(playerGui, 1015, 600));
        stage.show();
    }

    private void generateList(int x) {
        //ticTacToeBord.setGridLinesVisible(true);
        ticTacToeBord.setDisable(false);
        ticTacToeBord.getChildren().removeAll(cellsList);
        cellsList = new ArrayList<>();
        Label cell;
        for (int row = 0; row < x; row++) {
            for (int col = 0; col < x; col++) {
                cell = new Label();
                cell.setPrefSize(200, 200);
                cell.setBorder(new Border(new BorderStroke(Color.LIGHTSLATEGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
                ticTacToeBord.add(cell, col, row);
                cellsList.add(cell);
                ticTacToeCellArray[row][col] = cell;
            }
        }
        ticTacToeBord.setDisable(true);
    }

    private void setTicTacToeBord() {
        statusGame.setText("GAME STATUS : " + GameStatus.ON);
        try {
            streamPlayerToGameThread = new PrintWriter(socketPlayerToGameThread.getOutputStream(), true);
        } catch (IOException io) {
            System.out.println(io);
        }
        new Thread(this::playerMove).start();
        ticTacToeBord.setOnMouseClicked(event -> {
            row = (int) event.getY() / (600 / 3);
            col = (int) event.getX() / (600 / 3);
            Label label = ticTacToeCellArray[row][col];
            gameList.add(label);
            if (label.getText().equals("")) {
                label.setText(sign);
                label.setAlignment(Pos.CENTER);
                label.setFont(Font.font("Verdana", 130));
            }
            isMoved = true;

            sentMoveToOpponent(row, col);
            statusGame.setText("GAME STATUS : " + GameStatus.OPPONENTS_MOVE);
            ticTacToeBord.setDisable(true);
            isYourTurn = false;
            isMoved = false;

            if (checkRowTicTackToeBoardStatus() || checkColTicTackToeBoardStatus() || checkDiagonalTicTacToe()) {
                System.out.println("game winner");
                ticTacToeBord.setDisable(true);
                isGameOver = true;
            }
            if (checkDraw()) {
                Platform.runLater(() -> {
                    ticTacToeBord.setDisable(true);
                    isGameOver = true;
                });
            }
        });
    }

    public void restartTicTacToeBord() {
        gameList = new ArrayList<>();
        ticTacToeBord.setDisable(false);
        isGameOver = false;
        isYourTurn = false;
        isMoved = false;
        isStarting = false;
        System.out.println("set tic tac toe board again");
        generateList(3);
        try {
            if (socketServerToPlayer != null && streamPlayerToGameThread != null && streamGameThreadToPlayer != null){
                socketServerToPlayer.close();
                streamGameThreadToPlayer.close();
                streamPlayerToGameThread.close();
            }
        } catch (IOException io) {
            System.out.println(io);
        }
    }

    public void playerMove() {
        while (!isGameOver) {
            if (!isYourTurn) {
                System.out.println("ok");
                if (!isMoved) {
                    System.out.println("very ok");
                    dataMove = getMoveFromOpponent();
                    Platform.runLater(() -> {
                        Label label2 = ticTacToeCellArray[Integer.parseInt(dataMove[1])][Integer.parseInt(dataMove[2])];
                        gameList.add(label2);
                        label2.setText(dataMove[0]);
                        label2.setAlignment(Pos.CENTER);
                        label2.setFont(Font.font("Verdana", 130));
                        System.out.println("getting move from opponent " + dataMove[0] + " " + dataMove[1] + " " + dataMove[2]);
                    });
                    isYourTurn = true;
                    ticTacToeBord.setDisable(false);
                    Platform.runLater(() -> {
                        statusGame.setText("GAME STATUS : " + GameStatus.YOUR_MOVE);
                    });
                }
            }

            checkRowTicTackToeBoardStatus();
            checkColTicTackToeBoardStatus();
            checkDiagonalTicTacToe();

            if (checkRowTicTackToeBoardStatus() || checkColTicTackToeBoardStatus() || checkDiagonalTicTacToe() || checkDraw()) {
                System.out.println("game winner");
                Platform.runLater(() -> {
                    ticTacToeBord.setDisable(true);
                    isGameOver = true;
                });
                break;
            }
        }
    }

    public void sentMoveToOpponent(int row, int col) {
        streamPlayerToGameThread.println(sign + ":" + row + ":" + col);
        streamPlayerToGameThread.println("");
    }

    public String[] getMoveFromOpponent() {
        System.out.println("getMoveFromOpponent");
        String read;
        StringBuilder sb = new StringBuilder();
        String[] gameData;

        try {
            while (!(read = streamGameThreadToPlayer.readLine()).isEmpty()) {
                sb.append(read);
            }
        } catch (NullPointerException | IOException ex) {
            System.out.println(ex);

        }

        gameData = sb.toString().split(":");
        return gameData;
    }

    public boolean checkRowTicTackToeBoardStatus() {
        boolean gameOver = false;
        int countX = 0;
        int countO = 0;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (ticTacToeCellArray[row][col].getText().equals("X")) {
                    ++countX;
                } else if (ticTacToeCellArray[row][col].getText().equals("O")) {
                    ++countO;
                }
            }
            if (countX == 3) {
                gameOver = true;
                if (sign.equals("X")) {
                    Platform.runLater(() -> {
                        statusGame.setText("GAME STATUS : " + GameStatus.WON);
                    });
                } else {
                    Platform.runLater(() -> {
                        statusGame.setText("GAME STATUS : " + GameStatus.LOST);
                    });
                }
                break;
            } else if (countO == 3) {
                gameOver = true;
                if (sign.equals("O")) {
                    statusGame.setText("GAME STATUS : " + GameStatus.WON);
                } else {
                    statusGame.setText("GAME STATUS : " + GameStatus.LOST);
                }
                break;
            } else {
                countX = 0;
                countO = 0;
            }
        }
        return gameOver;
    }

    public boolean checkColTicTackToeBoardStatus() {
        boolean gameOver = false;
        int countX = 0;
        int countO = 0;

        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                if (ticTacToeCellArray[row][col].getText().equals("X")) {
                    ++countX;
                } else if (ticTacToeCellArray[row][col].getText().equals("O")) {
                    ++countO;
                }
            }
            if (countX == 3) {
                gameOver = true;
                if (sign.equals("X")) {
                    Platform.runLater(() -> {
                        statusGame.setText("GAME STATUS : " + GameStatus.WON);
                    });
                } else {
                    Platform.runLater(() -> {
                        statusGame.setText("GAME STATUS : " + GameStatus.LOST);
                    });
                }
                break;
            } else if (countO == 3) {
                gameOver = true;
                if (sign.equals("O")) {
                    Platform.runLater(() -> {
                        statusGame.setText("GAME STATUS : " + GameStatus.WON);
                    });
                } else {
                    Platform.runLater(() -> {
                        statusGame.setText("GAME STATUS : " + GameStatus.LOST);
                    });
                }
                break;
            } else {
                countX = 0;
                countO = 0;
            }
        }
        return gameOver;
    }

    public boolean checkDiagonalTicTacToe() {
        boolean gameOver = false;
        int countX1 = 0;
        int countO1 = 0;
        int countX2 = 0;
        int countO2 = 0;
        int diag2 = 2;

        for (int diag = 0; diag < 3; diag++) {
            if (ticTacToeCellArray[diag][diag].getText().equals("X")) {
                ++countX1;
            } else if (ticTacToeCellArray[diag][diag].getText().equals("O")) {
                ++countO1;
            }
            if (ticTacToeCellArray[diag][diag2].getText().equals("X")) {
                ++countX2;
            } else if (ticTacToeCellArray[diag][diag2].getText().equals("O")) {
                ++countO2;
            }
            --diag2;
        }
        if (countX1 == 3 || countX2 == 3) {
            gameOver = true;
            if (sign.equals("X")) {
                Platform.runLater(() -> {
                    statusGame.setText("GAME STATUS : " + GameStatus.WON);
                });
            } else {
                Platform.runLater(() -> {
                    statusGame.setText("GAME STATUS : " + GameStatus.LOST);
                });
            }
        } else if (countO1 == 3 || countO2 == 3) {
            gameOver = true;
            if (sign.equals("O")) {
                Platform.runLater(() -> {
                    statusGame.setText("GAME STATUS : " + GameStatus.WON);
                });
            } else {
                Platform.runLater(() -> {
                    statusGame.setText("GAME STATUS : " + GameStatus.LOST);
                });
            }
        }
        return gameOver;
    }

    public boolean checkDraw() {
        boolean isDraw = false;
        if ((gameList.size() == 9) && (!isGameOver)) {
            isDraw = true;
            Platform.runLater(() -> {
                statusGame.setText("GAME STATUS : " + GameStatus.DRAW);
            });
        }
        return isDraw;
    }

    public void connect() {
        try {
            socketPlayerServer = new Socket("localhost", 9999);
            this.setPort(socketPlayerServer.getPort());
            getIdFromServer = socketPlayerServer.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(socketPlayerServer.getInputStream()));
            String read;
            StringBuilder sb = new StringBuilder();
            while (!(read = br.readLine()).isEmpty()) {
                sb.append(read);
            }
            Platform.runLater(() -> {
                yourId.setText("Your ID : " + sb.toString());
                setYourId(sb.toString());
                System.out.println(sb.toString());
                statusConnection.setText("CONNECTION STATUS : " + PlayerStatus.CONNECTED);
            });
        } catch (IOException ex) {
            System.out.println("error -> connect(105-)  " + ex);
        }
    }

    public void list() {
        try {
            streamRequestToServer = new PrintWriter(socketPlayerServer.getOutputStream(), true);
            streamRequestToServer.println("LIST");
            streamRequestToServer.println("");

            streamReadListFromServer = new BufferedReader(new InputStreamReader(socketPlayerServer.getInputStream()));
            String read;
            StringBuilder sb = new StringBuilder();
            while (!(read = streamReadListFromServer.readLine()).isEmpty()) {
                sb.append(read);
            }
            processPlayersList(sb.toString());
        } catch (IOException ex) {
            System.out.println("error logout() -> 159 " + ex);
        }
    }

    public void processPlayersList(String list) {
        String[] playersList = list.split("X");
        String[] playerIpPort;
        opponentsList = new ArrayList<>();
        Opponent tmpOpponent;
        for (int i = 0; i < playersList.length; i++) {
            playerIpPort = playersList[i].split(":");
            System.out.println("IP " + playerIpPort[0] + ", PORT " + playerIpPort[1]);
            opponentsList.add(new Opponent(playerIpPort[0], playerIpPort[1]));
        }
        addPlayersToTable();
    }

    public void addPlayersToTable() {
        playersListTable.getItems().clear();
        playersListTable.getItems().addAll(opponentsList);
    }

    public void play() {
        try {
            streamRequestToServer = new PrintWriter(socketPlayerServer.getOutputStream(), true);
            streamRequestToServer.println("PLAY");
            streamRequestToServer.println("");

            Platform.runLater(() -> {
                statusGame.setText("GAME STATUS " + GameStatus.SEARCHING_OPPONENT);
            });

            socketServerToPlayer = new ServerSocket(Integer.parseInt(getId()) + 1);

            while (true) {
                socketPlayerToGameThread = socketServerToPlayer.accept();
                if (socketPlayerToGameThread != null) {
                    break;
                }
            }

            streamGameThreadToPlayer = new BufferedReader(new InputStreamReader(socketPlayerToGameThread.getInputStream()));
            String read;
            StringBuilder sb = new StringBuilder();
            while (!(read = streamGameThreadToPlayer.readLine()).isEmpty()) {
                sb.append(read);
            }

            if (sb.toString().equals("FOUND")) {
                Platform.runLater(() -> {
                    statusGame.setText("GAME STATUS: " + GameStatus.OPPONENT_FOUND);
                });
                sb = new StringBuilder();
                while (!(read = streamGameThreadToPlayer.readLine()).isEmpty()) {
                    sb.append(read);
                }
                processDataGameFromServer(sb.toString());
            } else {
                statusGame.setText("GAME STATUS: " + GameStatus.OPPONENT_NOT_FOUND);
            }

        } catch (IOException ex) {
            System.out.println("error sendPlayerMoveToOpponent()-> 346 " + ex);
        }
    }

    public void processDataGameFromServer(String data) {
        String[] signStart = data.split(":");
        sign = signStart[0];
        yourSign.setText("Sign : " + signStart[0]);
        System.out.println("starting game, your game data : " + signStart[0] + " , " + signStart[1]);
        if (signStart[1].equals("1")) {
            isStarting = true;
            isYourTurn = true;
            whoStarting.setText("Starter : YOU");
            if (isStarting) {
                ticTacToeBord.setDisable(false);
            }
        } else {
            whoStarting.setText("Starter : OPPONENT");
        }
        System.out.println("setting tic tac toe board");
        setTicTacToeBord();
    }

    private void getContestsFromServer() {
        System.out.println("start");
        byte[] data = new byte[2046];
        try {
            socketContests = new DatagramSocket(9001);
            packetWithContestsData = new DatagramPacket(data, data.length);
            new Thread(() -> {
                while (true) {
                    try {
                        socketContests.receive(packetWithContestsData);
                        String msg = new String(packetWithContestsData.getData(), 0, packetWithContestsData.getLength());
                        if (msg.equals("null")) {
                            processContests(msg);
                            //isPlayingGame = false;
                            break;
                        } else {
                            processContests(msg);
                            Thread.sleep(100);
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException io) {
            System.out.println(io);
        }
        System.out.println("end");
    }

    private void processContests(String contests) {
        System.out.println("Contests received from from server " + contests);
        List<String> allContests = new ArrayList<>(Arrays.asList(contests.split("N")));
        List<String> moves;
        Games game;
        String[] tmp;
        for (String c : allContests) {
            tmp = c.split("-");
            String[] tmp2 = tmp[1].split(",");
            moves = new ArrayList<>(Arrays.asList(tmp2));
            System.out.println(moves);
            game = new Games(tmp[0], moves);
            if (playersList.contains(game.getPlayers())) {
                for (Games games : gamesList) {
                    synchronized (playersAndGameStateMap) {
                        if (games.getPlayers().equals(game.getPlayers())) {
                            games.setGameState(game.getGameState());
                            System.out.println("Update : " + games);
                            playersAndGameStateMap.computeIfPresent(games.getPlayers(), (k, v) -> v = games.getGameState());
                            playersAndGameStateMap.notifyAll();
                            break;
                        }
                    }
                }
            } else {
                System.out.println("Putting new players " + game);
                gamesList.add(game);
                playersList.add(tmp[0]);
                gamesListTable.getItems().clear();
                gamesListTable.getItems().addAll(gamesList);
                playersAndGameStateMap.put(game.getPlayers(), game.getGameState());
            }
        }
    }

    public void watchGame(String players) {
        System.out.println("watchGame");
        int i = 0;
        List<String> tmp = new ArrayList<>();
        int tmpSize = playersAndGameStateMap.get(players).size();
        String state;

        while (tmp.size() < 9) {
            tmp =  playersAndGameStateMap.get(players);
            if (tmpSize != tmp.size()){
                tmpSize = tmp.size();
                System.out.println("change i work ok");
            }

            state = tmp.get(i);
            String[] tmpCellState = state.split(":");

            if (tmpCellState[0].equals("null")){
                break;
            }else {
                Platform.runLater(() -> {
                    System.out.println(tmpCellState[1] + " " + tmpCellState[2] + " " + tmpCellState[0]);
                    Label cell = ticTacToeCellArray[Integer.parseInt(tmpCellState[1])][Integer.parseInt(tmpCellState[2])];
                    cell.setText(tmpCellState[0]);
                    cell.setAlignment(Pos.CENTER);
                    cell.setFont(Font.font("Verdana", 130));
                });
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                System.out.println(ex);
            }
            if ((i + 1) == playersAndGameStateMap.get(players).size()) {
                try {
                    System.out.println("waiting");
                    System.out.println("before " + tmp);
                    synchronized (playersAndGameStateMap) {
                        playersAndGameStateMap.wait();
                    }
                    tmp = playersAndGameStateMap.get(players);
                    System.out.println("end waiting");
                    System.out.println("after " + tmp);
                    tmpSize = tmp.size();
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }else {
                ++i;
            }
        }
        System.out.println("end");
    }

    public void logout() {
        try {
            streamRequestToServer = new PrintWriter(socketPlayerServer.getOutputStream(), true);
            streamRequestToServer.println("LOGOUT");
            streamRequestToServer.println("");
            playersListTable.getItems().clear();
        } catch (IOException ex) {
            System.out.println("error logout() -> 359 " + ex);
        }
        Platform.runLater(() -> {
            yourId.setText("Your ID : -");
            statusConnection.setText("CONNECTION STATUS : " + PlayerStatus.DISCONNECTED);
        });
    }

    public int getIp() {
        return ip;
    }

    public void setIp(int ip) {
        this.ip = ip;
    }

    public int getPort() {
        System.out.println(port);
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setYourId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
