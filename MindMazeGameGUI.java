package org.example;

import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
class Room {
    String name;
    String description;
    boolean isExit;
    boolean isTrap;
    String riddle;
    boolean solved;

    public Room(String name, String desc, boolean isExit, boolean isTrap, String riddle) {
        this.name = name;
        this.description = desc;
        this.isExit = isExit;
        this.isTrap = isTrap;
        this.riddle = riddle;
        this.solved = riddle == null;
    }

    public String toString() {
        return name;
    }
}

public class MindMazeGameGUI extends JFrame {
    private final Graph<Room, DefaultEdge> maze = new SimpleGraph<>(DefaultEdge.class);
    private final List<Room> rooms = new ArrayList<>();
    private final Random rand = new Random();
    private Room currentRoom;
    private final Room exitRoom;
    private int score = 0;

    private final JTextArea roomDesc = new JTextArea();
    private final JPanel optionsPanel = new JPanel();
    private final JTextArea miniMap = new JTextArea();
    private final JLabel scoreLabel = new JLabel();
    private javax.swing.Timer mazeShifter;
    private javax.swing.Timer gameTimer;
    private int timeLeft = 30; // seconds

    public MindMazeGameGUI() {
        setTitle("Escape the Mind Maze");
        setSize(900, 650);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        roomDesc.setFont(new Font("Monospaced", Font.BOLD, 16));
        roomDesc.setEditable(false);
        roomDesc.setBackground(Color.BLACK);
        roomDesc.setForeground(Color.GREEN);
        roomDesc.setMargin(new Insets(15, 15, 15, 15));
        roomDesc.setLineWrap(true);
        roomDesc.setWrapStyleWord(true);

        optionsPanel.setLayout(new GridLayout(0, 1));
        optionsPanel.setBackground(Color.DARK_GRAY);

        miniMap.setEditable(false);
        miniMap.setFont(new Font("Monospaced", Font.PLAIN, 12));
        miniMap.setBackground(Color.BLACK);
        miniMap.setForeground(Color.ORANGE);
        miniMap.setBorder(BorderFactory.createTitledBorder("Mini Map"));

        scoreLabel.setFont(new Font("Arial", Font.BOLD, 16));
        scoreLabel.setForeground(Color.YELLOW);
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.BLACK);
        topPanel.add(scoreLabel, BorderLayout.EAST);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(roomDesc), new JScrollPane(miniMap));
        split.setDividerLocation(550);
        add(split, BorderLayout.CENTER);
        add(optionsPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        generateMaze();
        randomizeConnections();
        currentRoom = rooms.get(0);
        exitRoom = rooms.stream().filter(r -> r.isExit).findFirst().orElse(null);
        displayRoom(currentRoom);

        mazeShifter = new javax.swing.Timer(30000, (ActionEvent e) -> {
            randomizeConnections();
            JOptionPane.showMessageDialog(this, "The maze shifts!");
            displayRoom(currentRoom);
        });
        mazeShifter.start();

        gameTimer = new javax.swing.Timer(1000, (ActionEvent e) -> {
            timeLeft--;
            scoreLabel.setText("Score: " + score + " | Time Left: " + timeLeft + "s");
            if (timeLeft <= 0) {
                JOptionPane.showMessageDialog(this, "Time's up! Game Over.");
                mazeShifter.stop();
                gameTimer.stop();
            }
        });
        gameTimer.start();

        new Thread(this::playSound).start();
    }

    private void generateMaze() {
        String[] names = {
                "Hall of Whispers", "Gravity Sink", "Void Atrium", "Lava Lounge", "Screaming Spiral",
                "Cloud Hallway", "Illusion Room", "Blink Chamber", "Infinity Cube", "Exit Chamber"
        };

        String[] descs = {
                "You hear whispers echoing in reverse.",
                "The floor tilts toward an endless black pit.",
                "The air hums with invisible voices.",
                "Walls drip lava, yet it's cold.",
                "Spirals dance across your eyes.",
                "Everything feels upside down.",
                "The walls breathe slightly.",
                "The lights blink with your heartbeat.",
                "You see infinite versions of yourself.",
                "A soft light glows at the far end."
        };

        String[] riddles = {
                null,
                "What walks on four legs in the morning, two at noon and three in the evening?",
                null,
                null,
                "I speak without a mouth and hear without ears. What am I?",
                null,
                null,
                null,
                null,
                null
        };

        for (int i = 0; i < names.length; i++) {
            boolean isExit = (i == 9);
            boolean isTrap = (i != 0 && rand.nextInt(100) < 20);
            Room r = new Room(names[i], descs[i], isExit, isTrap, riddles[i]);
            maze.addVertex(r);
            rooms.add(r);
        }
    }

    private void randomizeConnections() {
        maze.removeAllEdges(new HashSet<>(maze.edgeSet()));
        for (Room r : rooms) {
            int conn = rand.nextInt(3) + 1;
            for (int i = 0; i < conn; i++) {
                Room other = rooms.get(rand.nextInt(rooms.size()));
                if (!r.equals(other)) {
                    maze.addEdge(r, other);
                }
            }
        }
    }

    private void displayRoom(Room room) {
        optionsPanel.removeAll();
        roomDesc.setForeground(Color.GREEN);
        roomDesc.setText("=== " + room.name + " ===\n" + room.description);

        if (!room.solved && room.riddle != null) {
            String answer = JOptionPane.showInputDialog(this, "Riddle: " + room.riddle);
            if (answer != null) {
                answer = answer.toLowerCase().trim();
                boolean correct = false;

                if (room.riddle.toLowerCase().contains("four legs in the morning")) {
                    correct = answer.contains("human") || answer.contains("man");
                } else if (room.riddle.toLowerCase().contains("speak without a mouth")) {
                    correct = answer.contains("echo");
                }

                if (correct) {
                    room.solved = true;
                    score += 50;
                    JOptionPane.showMessageDialog(this, "Correct! You earned 50 points.");
                } else {
                    JOptionPane.showMessageDialog(this, "Wrong answer! Try again later.");
                    return;
                }
            }
        }

        if (room.isTrap) {
            roomDesc.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this, "You stepped into a TRAP room! Game Over.");
            mazeShifter.stop();
            gameTimer.stop();
            return;
        }

        if (room.isExit) {
            roomDesc.setForeground(Color.CYAN);
            JOptionPane.showMessageDialog(this, "You found the EXIT! You're free!");
            mazeShifter.stop();
            gameTimer.stop();
            return;
        }

        for (DefaultEdge edge : maze.edgesOf(room)) {
            Room target = maze.getEdgeSource(edge).equals(room) ? maze.getEdgeTarget(edge) : maze.getEdgeSource(edge);
            JButton btn = new JButton("Enter: " + target.name);
            btn.setBackground(Color.LIGHT_GRAY);
            btn.addActionListener(e -> {
                currentRoom = target;
                displayRoom(target);
            });
            optionsPanel.add(btn);
        }

        JButton dfsButton = new JButton("Show DFS Path to Exit");
        dfsButton.addActionListener(e -> showDFSPath());
        optionsPanel.add(dfsButton);

        updateMiniMap();
        scoreLabel.setText("Score: " + score + " | Time Left: " + timeLeft + "s");
        optionsPanel.revalidate();
        optionsPanel.repaint();
    }

    // Custom DFS Implementation
    private void showDFSPath() {
        List<Room> path = new ArrayList<>();
        Set<Room> visited = new HashSet<>();
        boolean found = dfsRecursive(currentRoom, visited, path);

        if (found) {
            StringBuilder sb = new StringBuilder("DFS Path to Exit:\n");
            for (Room r : path) {
                sb.append(r.name).append(" -> ");
            }
            sb.setLength(sb.length() - 4); // remove last arrow
            JOptionPane.showMessageDialog(this, sb.toString());
        } else {
            JOptionPane.showMessageDialog(this, "No path to exit found via DFS.");
        }
    }

    private boolean dfsRecursive(Room current, Set<Room> visited, List<Room> path) {
        visited.add(current);
        path.add(current);

        if (current.isExit) {
            return true;
        }

        for (DefaultEdge edge : maze.edgesOf(current)) {
            Room neighbor = maze.getEdgeSource(edge).equals(current) ? maze.getEdgeTarget(edge) : maze.getEdgeSource(edge);
            if (!visited.contains(neighbor)) {
                if (dfsRecursive(neighbor, visited, path)) {
                    return true;
                }
            }
        }

        path.remove(path.size() - 1);
        return false;
    }

    private void updateMiniMap() {
        StringBuilder map = new StringBuilder();
        for (Room r : rooms) {
            map.append(r == currentRoom ? "-> " : "   ");
            map.append(r.name);
            map.append(" | Links: ").append(maze.edgesOf(r).size());
            if (r.isExit) map.append(" [EXIT]");
            if (r.isTrap) map.append(" [TRAP]");
            map.append("\n");
        }
        miniMap.setText(map.toString());
    }

    private void playSound() {
        try {
            File soundFile = new File("C:\\Users\\SHANT\\eclipse-workspace\\Maze Game\\sound\\ambientsound.wav");
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception ex) {
            System.out.println("Sound failed: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MindMazeGameGUI().setVisible(true));
    }
}