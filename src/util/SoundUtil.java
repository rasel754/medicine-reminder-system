package util;

import java.io.File;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.SourceDataLine;

/**
 * SoundUtil provides audio notification alerts using Java's built-in Sound API.
 * It will play resources/alert.wav, and falls back to synth tones on failure.
 */
public class SoundUtil {

    /**
     * Plays the alert notification audio in a background thread.
     */
    public static void playAlert() {
        new Thread(() -> {
            File alertFile = new File("resources/alert.wav");
            if (!alertFile.exists()) {
                alertFile = new File("medicine-reminder-system/resources/alert.wav");
            }

            if (alertFile.exists()) {
                try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(alertFile)) {
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    clip.start();
                    // Block briefly to prevent resource disposal before playback begins
                    Thread.sleep(clip.getMicrosecondLength() / 1000 + 100);
                    return;
                } catch (Exception e) {
                    System.err.println("WAV Audio playback failed: " + e.getMessage() + ". Playing fallback beep.");
                }
            }
            playSynthBeep();
        }, "SoundUtil-PlaybackThread").start();
    }

    private static void playSynthBeep() {
        try {
            // Generate a pleasing double-beep using synthetic audio lines
            generateTone(587, 120); // D5
            Thread.sleep(80);
            generateTone(880, 200); // A5
        } catch (Exception e) {
            java.awt.Toolkit.getDefaultToolkit().beep();
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
}
