package util;

import java.io.File;
import java.io.FileOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.SourceDataLine;

/**
 * SoundUtil provides audio notification alerts using Java's built-in Sound API and JavaFX Media.
 * It supports looping the alert for 30 seconds or stopping it immediately.
 */
public class SoundUtil {

    private static Object activeAudioClip = null;
    private static Clip activeJavaSoundClip = null;
    private static volatile boolean stopRequested = false;
    private static Thread activeBeepThread = null;

    /**
     * Stops any currently playing alert sound immediately.
     */
    public static synchronized void stopAlert() {
        stopRequested = true;
        if (activeAudioClip != null) {
            try {
                activeAudioClip.getClass().getMethod("stop").invoke(activeAudioClip);
            } catch (Exception e) {
                System.err.println("Failed to stop AudioClip: " + e.getMessage());
            }
            activeAudioClip = null;
        }
        if (activeJavaSoundClip != null) {
            try {
                activeJavaSoundClip.stop();
                activeJavaSoundClip.close();
            } catch (Exception e) {
                System.err.println("Failed to stop Java Sound Clip: " + e.getMessage());
            }
            activeJavaSoundClip = null;
        }
        if (activeBeepThread != null) {
            activeBeepThread.interrupt();
            activeBeepThread = null;
        }
    }

    /**
     * Plays the alert notification audio continuously for 30 seconds in a background thread.
     */
    public static void playAlert() {
        new Thread(() -> {
            File alertFile = new File("resources/alert.wav");
            
            // Ensure resources directory and alert.wav exist
            if (!alertFile.exists()) {
                File resourcesDir = new File("resources");
                if (!resourcesDir.exists()) {
                    resourcesDir.mkdirs();
                }
                generateDefaultAlertWav(alertFile);
            }

            synchronized (SoundUtil.class) {
                stopRequested = false;
            }

            // 1. Try playing using JavaFX AudioClip (via reflection for compilation safety)
            try {
                Class<?> audioClipClass = Class.forName("javafx.scene.media.AudioClip");
                java.lang.reflect.Constructor<?> constructor = audioClipClass.getConstructor(String.class);
                Object audioClip = constructor.newInstance(alertFile.toURI().toString());
                
                int indefiniteValue = -1;
                try {
                    java.lang.reflect.Field indefiniteField = audioClipClass.getField("INDEFINITE");
                    indefiniteValue = indefiniteField.getInt(null);
                } catch (Exception ignored) {}
                
                audioClipClass.getMethod("setCycleCount", int.class).invoke(audioClip, indefiniteValue);
                
                synchronized (SoundUtil.class) {
                    if (stopRequested) return;
                    activeAudioClip = audioClip;
                }

                audioClipClass.getMethod("play").invoke(audioClip);
                System.out.println("Sound notification started playing using JavaFX AudioClip (looped).");
                
                // Keep playing for 30 seconds unless stop is requested
                for (int i = 0; i < 300; i++) {
                    if (stopRequested) break;
                    Thread.sleep(100);
                }

                synchronized (SoundUtil.class) {
                    if (activeAudioClip != null) {
                        audioClipClass.getMethod("stop").invoke(activeAudioClip);
                        activeAudioClip = null;
                    }
                }
                return;
            } catch (ClassNotFoundException e) {
                System.out.println("javafx.scene.media.AudioClip not available in classpath. Falling back to Java Sound API.");
            } catch (Exception e) {
                System.err.println("Failed to play using JavaFX AudioClip: " + e.getMessage() + ". Falling back to Java Sound.");
            }

            // 2. Fallback to Java Sound Clip
            if (alertFile.exists()) {
                try {
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(alertFile);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    
                    synchronized (SoundUtil.class) {
                        if (stopRequested) {
                            clip.close();
                            return;
                        }
                        activeJavaSoundClip = clip;
                    }
                    
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                    clip.start();
                    System.out.println("Sound notification started playing using Java Sound Clip (looped).");
                    
                    // Keep playing for 30 seconds unless stop is requested
                    for (int i = 0; i < 300; i++) {
                        if (stopRequested) break;
                        Thread.sleep(100);
                    }

                    synchronized (SoundUtil.class) {
                        if (activeJavaSoundClip != null) {
                            activeJavaSoundClip.stop();
                            activeJavaSoundClip.close();
                            activeJavaSoundClip = null;
                        }
                    }
                    return;
                } catch (Exception e) {
                    System.err.println("WAV Audio playback failed: " + e.getMessage() + ". Playing fallback beep loop.");
                }
            }
            playSynthBeepLoop();
        }, "SoundUtil-PlaybackThread").start();
    }

    private static void playSynthBeepLoop() {
        activeBeepThread = Thread.currentThread();
        long start = System.currentTimeMillis();
        try {
            while (System.currentTimeMillis() - start < 30000 && !stopRequested) {
                generateTone(587, 120); // D5
                if (stopRequested) break;
                Thread.sleep(80);
                if (stopRequested) break;
                generateTone(880, 200); // A5
                if (stopRequested) break;
                Thread.sleep(300);
            }
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            java.awt.Toolkit.getDefaultToolkit().beep();
        } finally {
            activeBeepThread = null;
        }
    }

    private static void generateTone(int hz, int msecs) throws Exception {
        byte[] buf = new byte[8000 * msecs / 1000];
        for (int i = 0; i < buf.length; i++) {
            double angle = i / (8000.0 / hz) * 2.0 * Math.PI;
            // Apply exponential volume decay to make it a pleasant chime
            buf[i] = (byte) (Math.sin(angle) * 120.0 * Math.exp(-i / (double) buf.length));
        }
        AudioFormat af = new AudioFormat(8000f, 8, 1, true, false);
        try (SourceDataLine sdl = AudioSystem.getSourceDataLine(af)) {
            sdl.open(af);
            sdl.start();
            sdl.write(buf, 0, buf.length);
            sdl.drain();
        }
    }

    private static void generateDefaultAlertWav(File file) {
        int sampleRate = 8000;
        double frequency = 660.0; // E5 tone for alert
        double durationSeconds = 0.8;
        int numSamples = (int) (sampleRate * durationSeconds);
        int dataSize = numSamples * 2; // 16-bit = 2 bytes per sample
        
        byte[] header = new byte[44];
        
        // "RIFF"
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        int fileSize = 36 + dataSize;
        header[4] = (byte) (fileSize & 0xff);
        header[5] = (byte) ((fileSize >> 8) & 0xff);
        header[6] = (byte) ((fileSize >> 16) & 0xff);
        header[7] = (byte) ((fileSize >> 24) & 0xff);
        
        // "WAVE"
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        
        // "fmt "
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0; // fmt size = 16
        header[20] = 1; header[21] = 0; // format = 1 (PCM)
        header[22] = 1; header[23] = 0; // channels = 1 (mono)
        
        // Sample rate
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        
        // Byte rate (sampleRate * blockAlign)
        int byteRate = sampleRate * 2;
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        
        header[32] = 2; header[33] = 0; // block align = 2
        header[34] = 16; header[35] = 0; // bits per sample = 16
        
        // "data"
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (dataSize & 0xff);
        header[41] = (byte) ((dataSize >> 8) & 0xff);
        header[42] = (byte) ((dataSize >> 16) & 0xff);
        header[43] = (byte) ((dataSize >> 24) & 0xff);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(header);
            // Write sine wave data with exponential decay
            for (int i = 0; i < numSamples; i++) {
                double angle = 2.0 * Math.PI * i * frequency / sampleRate;
                double decay = Math.exp(-3.0 * i / numSamples); // fade out nicely
                short sample = (short) (Math.sin(angle) * 32767.0 * decay);
                fos.write(sample & 0xff);
                fos.write((sample >> 8) & 0xff);
            }
            System.out.println("Successfully generated default audio alert file at: " + file.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to write default alert.wav: " + e.getMessage());
        }
    }
}
