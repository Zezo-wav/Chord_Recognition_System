/*
 * Main Chord Recognition 
 * Analyzes audio input and detects chord progressions
 */
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

// Java Collections - For storing data
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChordRecognitionApp {
    
    private AudioAnalyzer analyzer;
    private ChordDetector chordDetector;
    private List<String> chordProgression;
    private boolean isRunning;
    
    // Debouncing variables to prevent rapid false detections
    private String lastDetectedChord;
    private int chordStabilityCounter;
    private static final int STABILITY_THRESHOLD = 3; // Chord must be detected 3 times in a row
    
    public ChordRecognitionApp() {
        this.analyzer = new AudioAnalyzer();
        this.chordDetector = new ChordDetector();
        this.chordProgression = new ArrayList<>();
        this.isRunning = false;
        this.lastDetectedChord = null;          
        this.chordStabilityCounter = 0;        
    }
    
    /*
     * Start listening to microphone input
     */
    public void startRealTimeAnalysis() {
        isRunning = true;
        
        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Line not supported");
                return;
            }
            
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            
            System.out.println("Recording started... Play some music!");
            
            byte[] buffer = new byte[4096];
            
            while (isRunning) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                
                if (bytesRead > 0) {
                    // Convert bytes to audio samples
                    float[] audioBuffer = convertBytesToFloats(buffer, bytesRead);
                    
                    // Analyze and detect chord
                    Set<String> detectedNotes = analyzer.detectNotes(audioBuffer);
                    
                    if (!detectedNotes.isEmpty() && detectedNotes.size() >= 1) {
                        String chord = chordDetector.identifyChord(detectedNotes);
                        
                        if (chord != null && !chord.equals("Unknown")) {
                            if (chord.equals(lastDetectedChord)) {
                                chordStabilityCounter++;
                                if (chordStabilityCounter >= STABILITY_THRESHOLD) {
                                    addToProgression(chord);
                                    chordStabilityCounter = 0; 
                                }
                            } else {
                                lastDetectedChord = chord;
                                chordStabilityCounter = 1;
                            }
                        }
                    } else {
                        // No strong signal - reset stability
                        lastDetectedChord = null;
                        chordStabilityCounter = 0;
                    }
                }
            }
            
            line.stop();
            line.close();
            
        } catch (LineUnavailableException e) {
            System.err.println("Error accessing microphone: " + e.getMessage());
        }
    }
    
    /*
     * Analyze audio from a file
     */
    public void analyzeAudioFile(String filePath) {
        System.out.println("Analyzing audio file: " + filePath);
    }
    
    /*
     * Convert byte array to float array for audio processing
     */
    private float[] convertBytesToFloats(byte[] buffer, int length) {
        float[] floatBuffer = new float[length / 2];
        
        for (int i = 0; i < length / 2; i++) {
            // Convert 16-bit PCM to float (-1.0 to 1.0)
            int sample = ((buffer[i * 2] & 0xFF) << 8) | (buffer[i * 2 + 1] & 0xFF);
            if (sample > 32767) sample -= 65536;
            floatBuffer[i] = sample / 32768.0f;
        }
        
        return floatBuffer;
    }
    
    /*
     * Add chord to progression, avoiding consecutive duplicates
     */
    private void addToProgression(String chord) {
        if (chordProgression.isEmpty() || !chordProgression.get(chordProgression.size() - 1).equals(chord)) {
            chordProgression.add(chord);
            System.out.println("Detected: " + chord);
            displayProgression();
        }
    }
    
    /*
     * Display current chord progression
     */
    public void displayProgression() {
        System.out.println("\nCurrent Chord Progression:");
        System.out.println("========================");
        for (int i = 0; i < chordProgression.size(); i++) {
            System.out.print(chordProgression.get(i));
            if (i < chordProgression.size() - 1) {
                System.out.print(" → ");
            }
        }
        System.out.println("\n");
    }
    
    public void stop() {
        isRunning = false;
    }
    
    public List<String> getChordProgression() {
        return new ArrayList<>(chordProgression);
    }
    
    public void clearProgression() {
        chordProgression.clear();
        lastDetectedChord = null;          
        chordStabilityCounter = 0;         
    }
    
    public static void main(String[] args) {
        ChordRecognitionApp app = new ChordRecognitionApp();
        
        System.out.println("=================================");
        System.out.println("  Chord Recognition System v1.0  ");
        System.out.println("=================================\n");
        
        // Start real-time analysis
        Thread analysisThread = new Thread(() -> app.startRealTimeAnalysis());
        analysisThread.start();
        
        // Run for 30 seconds then stop (for demo purposes)
        try {
            Thread.sleep(30000);
            app.stop();
            System.out.println("\n\nFinal Chord Progression:");
            app.displayProgression();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

/* Enhanced AudioAnalyzer with FULL DEBUG OUTPUT
 * Use this temporarily to diagnose the issue
 */
class AudioAnalyzer {
    
    private static final double CONCERT_A = 440.0;
    private static final int SAMPLE_RATE = 44100;
    private static final double FREQUENCY_THRESHOLD = 60.0;
    private static final double MAGNITUDE_THRESHOLD_MULTIPLIER = 3.0;  
    private static final double MINIMUM_SIGNAL_STRENGTH = 40.0;
    
    public Set<String> detectNotes(float[] audioBuffer) {
        Set<String> notes = new HashSet<>();
        
        // Check if signal is strong enough
        double signalStrength = calculateSignalStrength(audioBuffer);
        System.out.print("Signal: " + (int)signalStrength);
        
        if (signalStrength < MINIMUM_SIGNAL_STRENGTH) {
            System.out.println(" → Too weak");
            return notes;
        }
        
        System.out.println(" → PROCESSING");
        
        // Perform FFT to get frequency spectrum
        double[] frequencies = performFFT(audioBuffer);
        
        // Find peaks in frequency spectrum
        List<Double> peakFrequencies = findPeaks(frequencies);
        
        System.out.println("  Peaks found: " + peakFrequencies.size());
        
        // Print the peak frequencies
        if (!peakFrequencies.isEmpty()) {
            System.out.print("  Frequencies: ");
            for (Double freq : peakFrequencies) {
                System.out.print((int)freq.doubleValue() + "Hz ");
            }
            System.out.println();
        }
        
        // Need at least 1 peak (changed from 2 for testing)
        if (peakFrequencies.size() < 1) {
            System.out.println("  → Not enough peaks");
            return notes;
        }
        
        // Convert frequencies to notes
        for (Double freq : peakFrequencies) {
            if (freq > FREQUENCY_THRESHOLD) {
                String note = frequencyToNote(freq);
                if (note != null) {
                    notes.add(note);
                    System.out.println("  " + (int)freq.doubleValue() + "Hz → " + note);
                }
            }
        }
        
        if (!notes.isEmpty()) {
            System.out.println("  ✓ NOTES: " + notes);
        } else {
            System.out.println("  → No valid notes after conversion");
        }
        
        return notes;
    }
    
    private double calculateSignalStrength(float[] audioBuffer) {
        double sum = 0;
        for (float sample : audioBuffer) {
            sum += sample * sample;
        }
        return Math.sqrt(sum / audioBuffer.length) * 1000;
    }
    
    private double[] performFFT(float[] audioBuffer) {
        int n = audioBuffer.length;
        double[] magnitudes = new double[n / 2];
        
        for (int k = 0; k < n / 2; k++) {
            double real = 0;
            double imag = 0;
            
            for (int t = 0; t < n; t++) {
                double angle = 2 * Math.PI * k * t / n;
                real += audioBuffer[t] * Math.cos(angle);
                imag -= audioBuffer[t] * Math.sin(angle);
            }
            
            magnitudes[k] = Math.sqrt(real * real + imag * imag);
        }
        
        return magnitudes;
    }
    
    private List<Double> findPeaks(double[] magnitudes) {
        List<Double> peaks = new ArrayList<>();
        double avgMag = getAverageMagnitude(magnitudes);
        double threshold = avgMag * MAGNITUDE_THRESHOLD_MULTIPLIER;
        
        // Lower absolute minimum
        double absoluteMinimum = 20.0;  // Lowered from 50.0
        threshold = Math.max(threshold, absoluteMinimum);
        
        System.out.println("  Avg mag: " + (int)avgMag + ", Threshold: " + (int)threshold);
        
        for (int i = 5; i < magnitudes.length - 5; i++) {
            if (magnitudes[i] > threshold) {
                boolean isPeak = true;
                
                // Check if it's a local maximum
                for (int j = -2; j <= 2; j++) {
                    if (j != 0 && magnitudes[i + j] > magnitudes[i]) {
                        isPeak = false;
                        break;
                    }
                }
                
                if (isPeak) {
                    double frequency = (double) i * SAMPLE_RATE / (magnitudes.length * 2);
                    peaks.add(frequency);
                }
            }
        }
        
        // Limit to top 8 peaks
        if (peaks.size() > 8) {
            final double[] finalMags = magnitudes;
            peaks.sort((f1, f2) -> {
                int idx1 = (int)(f1 * magnitudes.length * 2 / SAMPLE_RATE);
                int idx2 = (int)(f2 * magnitudes.length * 2 / SAMPLE_RATE);
                if (idx1 >= finalMags.length) idx1 = finalMags.length - 1;
                if (idx2 >= finalMags.length) idx2 = finalMags.length - 1;
                return Double.compare(finalMags[idx2], finalMags[idx1]);
            });
            peaks = peaks.subList(0, 8);
        }
        
        return peaks;
    }
    
    private double getAverageMagnitude(double[] magnitudes) {
        double sum = 0;
        for (double mag : magnitudes) {
            sum += mag;
        }
        return sum / magnitudes.length;
    }
    
    private String frequencyToNote(double frequency) {
        if (frequency < 20 || frequency > 5000) return null;
        
        double semitones = 12 * (Math.log(frequency / CONCERT_A) / Math.log(2));
        int semitonesRounded = (int) Math.round(semitones);
        
        String[] noteNames = {"A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#"};
        
        int noteIndex = ((semitonesRounded % 12) + 12) % 12;
        
        return noteNames[noteIndex];
    }
}

/*
 * Chord Detector - Identifies chords from sets of notes
 */
class ChordDetector {
    
    private Map<String, Set<String>> chordDatabase;
    
    public ChordDetector() {
        initializeChordDatabase();
    }
    
    /*
     * Initialize database of chord patterns
     */
    private void initializeChordDatabase() {
        chordDatabase = new HashMap<>();
        
        // Major chords
        chordDatabase.put("C", new HashSet<>(Arrays.asList("C", "E", "G")));
        chordDatabase.put("D", new HashSet<>(Arrays.asList("D", "F#", "A")));
        chordDatabase.put("E", new HashSet<>(Arrays.asList("E", "G#", "B")));
        chordDatabase.put("F", new HashSet<>(Arrays.asList("F", "A", "C")));
        chordDatabase.put("G", new HashSet<>(Arrays.asList("G", "B", "D")));
        chordDatabase.put("A", new HashSet<>(Arrays.asList("A", "C#", "E")));
        chordDatabase.put("B", new HashSet<>(Arrays.asList("B", "D#", "F#")));
        
        // Minor chords
        chordDatabase.put("Cm", new HashSet<>(Arrays.asList("C", "D#", "G")));
        chordDatabase.put("Dm", new HashSet<>(Arrays.asList("D", "F", "A")));
        chordDatabase.put("Em", new HashSet<>(Arrays.asList("E", "G", "B")));
        chordDatabase.put("Fm", new HashSet<>(Arrays.asList("F", "G#", "C")));
        chordDatabase.put("Gm", new HashSet<>(Arrays.asList("G", "A#", "D")));
        chordDatabase.put("Am", new HashSet<>(Arrays.asList("A", "C", "E")));
        chordDatabase.put("Bm", new HashSet<>(Arrays.asList("B", "D", "F#")));
        
    }
    
    /*
     * Identify chord from detected notes
     */
    public String identifyChord(Set<String> detectedNotes) {
        if (detectedNotes.size() < 2) {
            return "Unknown";
        }
        
        // Find best matching chord
        String bestMatch = null;
        int maxMatches = 0;
        
        for (Map.Entry<String, Set<String>> entry : chordDatabase.entrySet()) {
            Set<String> chordNotes = entry.getValue();
            Set<String> intersection = new HashSet<>(detectedNotes);
            intersection.retainAll(chordNotes);
            
            int matches = intersection.size();
            
            // Require at least 2 matching notes
            if (matches >= 2 && matches > maxMatches) {
                maxMatches = matches;
                bestMatch = entry.getKey();
            }
        }
        
        return bestMatch != null ? bestMatch : "Unknown";
    }
    
    /*
     * Get notes that make up a chord
     */
    public Set<String> getChordNotes(String chordName) {
        return chordDatabase.getOrDefault(chordName, new HashSet<>());
    }
}