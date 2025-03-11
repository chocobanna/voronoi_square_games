package newrealm.diagram;

import javax.swing.JComponent;  // Explicitly use Swing’s JComponent
import javax.swing.Timer;         // Explicitly use Swing’s Timer
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;       // Use java.util.List
import java.util.List;
import java.util.Random;


public class CivilizationOverlay extends JComponent {
    private List<Settlement> settlements;
    private Timer timer;
    private int mapWidth, mapHeight;
    private Random rand;
    
    public CivilizationOverlay(int mapWidth, int mapHeight) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        setOpaque(false);
        settlements = new ArrayList<>();
        rand = new Random();
        // Start with a single civilization seed at the center.
        settlements.add(new Settlement(mapWidth / 2.0, mapHeight / 2.0, 10));
        
        // Every 5 seconds, update the civilization.
        timer = new Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateCivilization();
                repaint();
            }
        });
        timer.start();
    }
    
    private void updateCivilization() {
        // Each settlement grows; if large enough, it may spawn a new settlement.
        List<Settlement> newSettlements = new ArrayList<>();
        for (Settlement s : settlements) {
            s.radius += 5;
            if (s.radius >= 50 && rand.nextDouble() < 0.5) { // 50% chance
                double angle = rand.nextDouble() * 2 * Math.PI;
                double newX = s.x + s.radius * Math.cos(angle);
                double newY = s.y + s.radius * Math.sin(angle);
                // Clamp to map boundaries.
                newX = Math.max(0, Math.min(mapWidth, newX));
                newY = Math.max(0, Math.min(mapHeight, newY));
                newSettlements.add(new Settlement(newX, newY, 10));
                s.radius -= 10; // Optionally reduce parent's size.
            }
        }
        settlements.addAll(newSettlements);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        // Draw settlements as golden circles.
        g2.setColor(new Color(255, 215, 0));
        for (Settlement s : settlements) {
            int r = (int) s.radius;
            int x = (int) (s.x - r);
            int y = (int) (s.y - r);
            g2.fillOval(x, y, r * 2, r * 2);
        }
        g2.dispose();
    }
    
    private static class Settlement {
        double x, y, radius;
        public Settlement(double x, double y, double radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
    }
}
