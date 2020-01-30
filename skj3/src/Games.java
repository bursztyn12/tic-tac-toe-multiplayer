import java.util.List;

public class Games {
    private String players;
    private List<String> gameState;

    public Games(String players,List<String>gameState){
        this.players = players;
        this.gameState = gameState;
    }

    public String getPlayers() {
        return players;
    }

    public List<String> getGameState() {
        return gameState;
    }

    public void setGameState(List<String> gameState){
        this.gameState = gameState;
    }

    public String toString(){
        return players + " = " + gameState;
    }
}
