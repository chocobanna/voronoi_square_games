import java.awt.*;
import java.util.*;
import javax.swing.*;

public class RPGSwingGame extends JFrame {
    private JTextArea outputArea;
    private JPanel buttonPanel;
    private Player player;
    private Enemy currentEnemy;

    public RPGSwingGame() {
        setTitle("RPG Swing Game");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);

        buttonPanel = new JPanel();
        add(buttonPanel, BorderLayout.SOUTH);

        initializeGame();
    }

    private void initializeGame() {
        String name = JOptionPane.showInputDialog(this, "Enter your name, if you dare:");
        if (name == null || name.trim().isEmpty()) {
            name = "Nameless Hero";
        }
        player = new Player(name, 100, 20);
        appendOutput("Alright " + name + ", your adventure begins!");
        player.addItem(new Item("Health Potion", 50, "Restores 50 HP"));
        player.addItem(new Item("Rusty Sword", 10, "A sword that's seen better days"));

        showMainMenu();
    }

    private void showMainMenu() {
        clearButtons();
        appendOutput("\nWhat do you want to do?");
        JButton battleButton = new JButton("Venture into battle");
        battleButton.addActionListener(e -> startBattle());
        JButton inventoryButton = new JButton("Check inventory");
        inventoryButton.addActionListener(e -> showInventory());
        JButton restButton = new JButton("Rest and recover (end adventure)");
        restButton.addActionListener(e -> endGame());

        buttonPanel.add(battleButton);
        buttonPanel.add(inventoryButton);
        buttonPanel.add(restButton);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private void startBattle() {
        clearButtons();
        currentEnemy = Enemy.getRandomEnemy();
        appendOutput("\nA wild " + currentEnemy.getName() + " appears! Brace yourself.");
        showBattleMenu();
    }

    private void showBattleMenu() {
        clearButtons();
        JButton attackButton = new JButton("Attack");
        attackButton.addActionListener(e -> playerAttack());
        JButton potionButton = new JButton("Use Health Potion");
        potionButton.addActionListener(e -> useHealthPotion());
        JButton fleeButton = new JButton("Attempt to flee");
        fleeButton.addActionListener(e -> attemptFlee());

        buttonPanel.add(attackButton);
        buttonPanel.add(potionButton);
        buttonPanel.add(fleeButton);
        buttonPanel.revalidate();
        buttonPanel.repaint();

        updateBattleStatus();
    }

    private void updateBattleStatus() {
        appendOutput("\n" + player.getName() + " HP: " + player.getHealth() +
                     " | " + currentEnemy.getName() + " HP: " + currentEnemy.getHealth());
    }

    private void playerAttack() {
        int damage = player.attack();
        currentEnemy.takeDamage(damage);
        appendOutput("\nYou strike the " + currentEnemy.getName() + " for " + damage + " damage.");
        if (currentEnemy.isAlive()) {
            enemyTurn();
        } else {
            appendOutput("\nYou've defeated the " + currentEnemy.getName() + ". Not bad for a beginner.");
            player.gainExperience(currentEnemy.getExperienceValue());
            showMainMenu();
        }
    }

    private void enemyTurn() {
        int enemyDamage = currentEnemy.attack();
        player.takeDamage(enemyDamage);
        appendOutput("\nThe " + currentEnemy.getName() + " hits you for " + enemyDamage + " damage.");
        if (!player.isAlive()) {
            appendOutput("\nYou have been defeated by the " + currentEnemy.getName() + ". Expected, really.");
            endGame();
        } else {
            updateBattleStatus();
        }
    }

    private void useHealthPotion() {
        boolean used = false;
        for (int i = 0; i < player.getInventory().size(); i++) {
            Item item = player.getInventory().get(i);
            if (item.getName().equalsIgnoreCase("Health Potion")) {
                player.heal(item.getPower());
                player.getInventory().remove(i);
                appendOutput("\nYou used a health potion to restore your HP.");
                used = true;
                break;
            }
        }
        if (!used) {
            appendOutput("\nNo health potions available. Face the music!");
        }
        enemyTurn();
    }

    private void attemptFlee() {
        if (Math.random() < 0.5) {
            appendOutput("\nYou successfully fled the battle. Cowardly, but effective.");
            showMainMenu();
        } else {
            appendOutput("\nYou failed to flee. Tough luck!");
            enemyTurn();
        }
    }

    private void showInventory() {
        StringBuilder inv = new StringBuilder("\nYour Inventory:\n");
        if (player.getInventory().isEmpty()) {
            inv.append("Your inventory is empty. Shocked?\n");
        } else {
            for (int i = 0; i < player.getInventory().size(); i++) {
                Item item = player.getInventory().get(i);
                inv.append((i + 1) + ". " + item.getName() + " - " + item.getDescription() + "\n");
            }
        }
        appendOutput(inv.toString());
        showMainMenu();
    }

    private void endGame() {
        appendOutput("\nGame Over. Hopefully, you'll do better next time.");
        clearButtons();
    }

    private void clearButtons() {
        buttonPanel.removeAll();
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private void appendOutput(String text) {
        outputArea.append(text + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RPGSwingGame game = new RPGSwingGame();
            game.setVisible(true);
        });
    }

    // Inner classes

    class Player {
        private String name;
        private int maxHealth;
        private int health;
        private int attackPower;
        private int experience;
        private int level;
        private java.util.List<Item> inventory;

        public Player(String name, int health, int attackPower) {
            this.name = name;
            this.maxHealth = health;
            this.health = health;
            this.attackPower = attackPower;
            this.experience = 0;
            this.level = 1;
            this.inventory = new ArrayList<>();
        }

        public int attack() {
            return (int)(Math.random() * attackPower) + 1;
        }

        public void takeDamage(int damage) {
            health -= damage;
            if (health < 0) health = 0;
        }

        public void heal(int amount) {
            health += amount;
            if (health > maxHealth) health = maxHealth;
        }

        public boolean isAlive() {
            return health > 0;
        }

        public void addItem(Item item) {
            inventory.add(item);
            appendOutput("You acquired: " + item.getName() + " (" + item.getDescription() + ")");
        }

        public void gainExperience(int exp) {
            experience += exp;
            appendOutput("You gained " + exp + " EXP.");
            if (experience >= level * 50) {
                levelUp();
            }
        }

        private void levelUp() {
            level++;
            maxHealth += 20;
            attackPower += 5;
            health = maxHealth;
            appendOutput("Congratulations, you've leveled up to Level " + level + "!");
            appendOutput("Your stats have improved. It's about time, right?");
        }

        public int getHealth() {
            return health;
        }

        public String getName() {
            return name;
        }

        public java.util.List<Item> getInventory() {
            return inventory;
        }
    }

    static class Enemy {
        private String name;
        private int health;
        private int attackPower;
        private int experienceValue;

        public Enemy(String name, int health, int attackPower, int experienceValue) {
            this.name = name;
            this.health = health;
            this.attackPower = attackPower;
            this.experienceValue = experienceValue;
        }

        public int attack() {
            return (int)(Math.random() * attackPower) + 1;
        }

        public void takeDamage(int damage) {
            health -= damage;
            if (health < 0) health = 0;
        }

        public boolean isAlive() {
            return health > 0;
        }

        public String getName() {
            return name;
        }

        public int getHealth() {
            return health;
        }

        public int getExperienceValue() {
            return experienceValue;
        }

        public static Enemy getRandomEnemy() {
            Random rand = new Random();
            int choice = rand.nextInt(3);
            switch (choice) {
                case 0:
                    return new Enemy("Goblin", 50, 10, 20);
                case 1:
                    return new Enemy("Skeleton", 60, 12, 25);
                case 2:
                    return new Enemy("Orc", 80, 15, 35);
                default:
                    return new Enemy("Goblin", 50, 10, 20);
            }
        }
    }

    class Item {
        private String name;
        private int power;
        private String description;

        public Item(String name, int power, String description) {
            this.name = name;
            this.power = power;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public int getPower() {
            return power;
        }

        public String getDescription() {
            return description;
        }
    }
}
