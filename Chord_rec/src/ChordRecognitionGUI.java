// Swing GUI components
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JSeparator;

// AWT for layouts and components
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

// Java utilities
import java.util.List;

/**
 * GUI Interface for Chord Recognition System
 * 
 * NO EXTERNAL LIBRARIES REQUIRED - Uses only Java Standard Library
 */
public class ChordRecognitionGUI extends JFrame {
    
    private ChordRecognitionApp app;
    private JTextArea progressionDisplay;
    private JLabel currentChordLabel;
    private JButton startButton;
    private JButton stopButton;
    private JButton clearButton;
    private Thread analysisThread;
    
    public ChordRecognitionGUI() {
        app = new ChordRecognitionApp();
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("Chord Recognition System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header Panel
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Current Chord Display
        JPanel chordPanel = createChordDisplayPanel();
        mainPanel.add(chordPanel, BorderLayout.CENTER);
        
        // Progression Display
        JPanel progressionPanel = createProgressionPanel();
        mainPanel.add(progressionPanel, BorderLayout.SOUTH);
        
        // Control Panel
        JPanel controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.EAST);
        
        add(mainPanel);
        
        // Update timer
        Timer updateTimer = new Timer(500, e -> updateDisplay());
        updateTimer.start();
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        JLabel titleLabel = new JLabel("🎵 Chord Recognition System 🎵");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Real-time chord detection from audio input");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.GRAY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(subtitleLabel);
        
        return panel;
    }
    
    private JPanel createChordDisplayPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Current Chord"));
        
        currentChordLabel = new JLabel("---", SwingConstants.CENTER);
        currentChordLabel.setFont(new Font("Arial", Font.BOLD, 72));
        currentChordLabel.setForeground(new Color(0, 100, 200));
        
        panel.add(currentChordLabel, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(0, 200));
        
        return panel;
    }
    
    private JPanel createProgressionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Chord Progression"));
        
        progressionDisplay = new JTextArea(4, 50);
        progressionDisplay.setFont(new Font("Monospaced", Font.PLAIN, 16));
        progressionDisplay.setEditable(false);
        progressionDisplay.setLineWrap(true);
        progressionDisplay.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(progressionDisplay);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Controls"));
        
        startButton = new JButton("▶ Start");
        startButton.setFont(new Font("Arial", Font.BOLD, 16));
        startButton.setBackground(new Color(0, 150, 0));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.setMaximumSize(new Dimension(150, 50));
        
        stopButton = new JButton("⏹ Stop");
        stopButton.setFont(new Font("Arial", Font.BOLD, 16));
        stopButton.setBackground(new Color(200, 0, 0));
        stopButton.setForeground(Color.WHITE);
        stopButton.setFocusPainted(false);
        stopButton.setEnabled(false);
        stopButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        stopButton.setMaximumSize(new Dimension(150, 50));
        
        clearButton = new JButton("🗑 Clear");
        clearButton.setFont(new Font("Arial", Font.PLAIN, 14));
        clearButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearButton.setMaximumSize(new Dimension(150, 40));
        
        // Button actions
        startButton.addActionListener(e -> startAnalysis());
        stopButton.addActionListener(e -> stopAnalysis());
        clearButton.addActionListener(e -> clearProgression());
        
        panel.add(Box.createVerticalStrut(20));
        panel.add(startButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(stopButton);
        panel.add(Box.createVerticalStrut(20));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(20));
        panel.add(clearButton);
        
        return panel;
    }
    
    private void startAnalysis() {
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        
        analysisThread = new Thread(() -> app.startRealTimeAnalysis());
        analysisThread.start();
        
        currentChordLabel.setText("Listening...");
    }
    
    private void stopAnalysis() {
        app.stop();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        currentChordLabel.setText("---");
    }
    
    private void clearProgression() {
        app.clearProgression();
        progressionDisplay.setText("");
        currentChordLabel.setText("---");
    }
    
    private void updateDisplay() {
        List<String> progression = app.getChordProgression();
        
        if (!progression.isEmpty()) {
            // Update current chord
            String currentChord = progression.get(progression.size() - 1);
            currentChordLabel.setText(currentChord);
            
            // Update progression display
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < progression.size(); i++) {
                sb.append(progression.get(i));
                if (i < progression.size() - 1) {
                    sb.append(" → ");
                }
            }
            progressionDisplay.setText(sb.toString());
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChordRecognitionGUI gui = new ChordRecognitionGUI();
            gui.setVisible(true);
        });
    }
}