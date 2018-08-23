import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;

public class GUI extends Frame implements PropertyChangeListener {
    private String playerName;
    private JLabel[] mapGrids;
    private JLabel infoLabel;
    private GameState gameState;

    private void updateInfoLabel(){
        StringBuilder sb = new StringBuilder("<html><body>Scores<br>");
        for (Map.Entry<String, GameState.PlayerState> entry : gameState.playerStates.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue().score).append("<br>");
        }
        sb.append("</body></html>");
        infoLabel.setText(sb.toString());
    }

    private void updateMapGrids() {
        int rows = gameState.N;
        int cols = gameState.N;
        for(int i=0; i<rows; i++) {
            for(int j=0; j<cols; j++) {
                int pos = i*rows + j;
                Color borderColor = Color.black;
                StringBuilder cell = new StringBuilder("<html><body>").append(pos).append("<br>");
                if(gameState.treasurePositions.contains(pos)) {
                    cell.append("*<br>");
                }
                for (Map.Entry<String, GameState.PlayerState> entry : gameState.playerStates.entrySet()) {
                    if(entry.getValue().position == pos) {
                        cell.append(entry.getKey()).append("<br>");
                        if(entry.getKey().equals(playerName)) {
                            borderColor = Color.RED;
                        }
                    }
                }
                cell.append("</body></html>");
                mapGrids[pos].setText(cell.toString());
                mapGrids[pos].setBorder(BorderFactory.createLineBorder(borderColor));
            }
        }
    }

    public GUI (GameState gameState, String playerName) {

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
        mapGrids = new JLabel[rows*cols];
        for(int i=0; i<rows; i++) {
            for (int j = 0; j < cols; j++) {
                mapGrids[i*rows + j] = new JLabel();
                map.add(mapGrids[i*rows + j]);
            }
        }
        updateMapGrids();

        setLayout(new BorderLayout());
        add(map, BorderLayout.CENTER);
        add(legend, BorderLayout.WEST);

        setTitle(playerName);
        setSize(1024, 768);
        setVisible(true);

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