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
    private JLabel[] infoLabels;
    private GameState gameState;
    private static final int NUM_FIXED_TOP_ROWS = 2;
    private static final int MAX_ROWS = 20;

    private void updateInfoLabel(){
        infoLabels[0].setText(" PRIMARY: " + gameState.getPrimary().name);
        if(gameState.getSecondary() != null) {
            infoLabels[1].setText(" SECONDARY: " + gameState.getSecondary().name);
        } else {
            infoLabels[1].setText("");
        }
        infoLabels[2].setText("==========");
        int i = NUM_FIXED_TOP_ROWS;
        for (Map.Entry<Player, GameState.PlayerState> entry : gameState.getPlayerStates().entrySet()) {
            infoLabels[++i].setText(entry.getKey().name + " : " + entry.getValue().score);
        }
        while(i < MAX_ROWS - 1) {
            infoLabels[++i].setText("");
        }
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

        Panel legend = new Panel();
        legend.setLayout(new BoxLayout(legend, BoxLayout.PAGE_AXIS));
        //int maxPossibleRows = rows * cols + NUM_FIXED_TOP_ROWS + 1;
        int maxPossibleRows = MAX_ROWS; // for performance reasons
        infoLabels = new JLabel[maxPossibleRows];
        for(int i=0; i<maxPossibleRows; i++) {
            infoLabels[i] = new JLabel();
            infoLabels[i].setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
            legend.add(infoLabels[i]);
        }
        updateInfoLabel();

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
