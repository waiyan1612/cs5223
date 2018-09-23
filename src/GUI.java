import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;


public class GUI extends JFrame implements PropertyChangeListener {
    private String playerName;
    private JLabel[][] mapGrids;
    private JLabel infoLabel;
    private GameState gameState;

    private void updateInfoLabel(){
        StringBuilder sb = new StringBuilder();
        sb.append("Primary: ").append(gameState.getPrimary().name).append(";");
        if(gameState.getSecondary() != null) {
            sb.append("Secondary: ").append(gameState.getSecondary().name).append(";");
        }
        sb.append("Scores: ");
        for (Map.Entry<Player, GameState.PlayerState> entry : gameState.getPlayerStates().entrySet()) {
            sb.append(entry.getKey().name).append(" : ").append(entry.getValue().score).append(";");
        }
    	infoLabel.setText(sb.toString());
    }

    private void updateMapGrids() {
        int rows = gameState.N;
        int cols = gameState.N;
        for(int i=0; i<rows; i++) {
            for(int j=0; j<cols; j++) {
                int pos = i*rows + j;
                Color backgroundColor = Color.white;
                StringBuilder cell = new StringBuilder();
                if(gameState.getTreasurePositions().contains(pos)) {
                    backgroundColor = Color.yellow;
                }
                for (Map.Entry<Player, GameState.PlayerState> entry : gameState.getPlayerStates().entrySet()) {
                    if(entry.getValue().position == pos) {
                    	cell.append(entry.getKey().name);
                        if(entry.getKey().name.equals(playerName)) {
                        	backgroundColor = Color.green;
                        }
                        // can break because we cannot have two players in the same cell
                        break;
                    }
                }
            	mapGrids[i][j].setText(cell.toString());
                mapGrids[i][j].setBackground(backgroundColor);
            }
        }
    }

    public GUI (GameState gameState, String playerName) {
        setVisible(true);
        int rows = gameState.N;
        int cols = gameState.N;
        this.playerName = playerName;
        this.gameState = gameState;

        // Info
        Panel legend = new Panel(new FlowLayout());
        infoLabel = new JLabel();
        updateInfoLabel();
        infoLabel.setBorder(BorderFactory.createLineBorder(Color.black));
        infoLabel.setSize(400, 768);
        legend.add(infoLabel);

        // Map
        Panel map = new Panel(new GridLayout(rows, cols));
        mapGrids = new JLabel[rows][cols];
        for(int i=0; i<rows; i++) {
            for (int j = 0; j < cols; j++) {
            	mapGrids[i][j] = new JLabel();
            	mapGrids[i][j].setOpaque(true);
                mapGrids[i][j].setBorder(BorderFactory.createLineBorder(Color.black));
            	map.add(mapGrids[i][j]);
            }
        }
        updateMapGrids();
        setLayout(new BorderLayout());
        add(map, BorderLayout.CENTER);
        add(legend, BorderLayout.WEST);
        setTitle(playerName);
        setSize(600, 400);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Terminating ...");
                System.exit(0);
            }
        });
    }

    public void propertyChange(PropertyChangeEvent event) {
        gameState = (GameState) event.getNewValue();
        updateInfoLabel();
        updateMapGrids();
    }
}
