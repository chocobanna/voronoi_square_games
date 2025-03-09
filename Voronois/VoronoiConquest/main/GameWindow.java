package main;

import java.awt.*;
import javax.swing.*;

public class GameWindow extends JFrame {
    public GameWindow(int mapWidth, int mapHeight, int numRegions, int numTeams, TeamControl[] teamControls, double smartRisk) {
        super("Voronoi Conquest - Battle");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create menu bar with "Game" menu.
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");

        JMenuItem newGameItem = new JMenuItem("New Game");
        newGameItem.addActionListener(e -> {
            // Open the main menu window and close this one.
            SwingUtilities.invokeLater(() -> {
                JFrame mainMenuFrame = new JFrame("Voronoi Conquest - Main Menu");
                mainMenuFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                MainMenuPanel menuPanel = new MainMenuPanel(mainMenuFrame);
                mainMenuFrame.getContentPane().add(menuPanel);
                mainMenuFrame.pack();
                mainMenuFrame.setLocationRelativeTo(null);
                mainMenuFrame.setVisible(true);
            });
            dispose();
        });
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        gameMenu.add(newGameItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);
        menuBar.add(gameMenu);
        setJMenuBar(menuBar);

        // Add the GamePanel to the window.
        GamePanel gamePanel = new GamePanel(mapWidth, mapHeight, numRegions, numTeams, teamControls, smartRisk);
        add(gamePanel, BorderLayout.CENTER);
        pack();
    }
}
