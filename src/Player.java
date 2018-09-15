import java.io.Serializable;

public class Player implements Serializable {
    public String name;
    public int port;

    public Player(String name) {
        this.name = name;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString(){
        return name + "@localhost:" + port;
    }

    @Override
    public boolean equals(Object that) {
        if (that == this) {
            return true;
        }
        if (!(that instanceof Player)) {
            return false;
        }
        Player player = (Player) that;
        return player.name.equals(this.name) && player.port == this.port;
    }
}