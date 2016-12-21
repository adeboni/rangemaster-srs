package accufiresystems.rangemaster;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class PlayWave extends Thread {

	private final URL soundFile;

	private static final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb

	public PlayWave(final URL wavfile) {
		soundFile = wavfile;
	}

	@Override
	public void run() {

		AudioInputStream audioInputStream = null;
		try {
			audioInputStream = AudioSystem.getAudioInputStream(soundFile);
		} catch (final UnsupportedAudioFileException e1) {
			e1.printStackTrace();
			return;
		} catch (final IOException e1) {
			e1.printStackTrace();
			return;
		}

		final AudioFormat format = audioInputStream.getFormat();
		SourceDataLine auline = null;
		final DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

		try {
			auline = (SourceDataLine) AudioSystem.getLine(info);
			auline.open(format);
		} catch (final LineUnavailableException e) {
			e.printStackTrace();
			return;
		} catch (final Exception e) {
			e.printStackTrace();
			return;
		}

		auline.start();
		int nBytesRead = 0;
		final byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];

		try {
			while (nBytesRead != -1) {
				nBytesRead = audioInputStream.read(abData, 0, abData.length);
				if (nBytesRead >= 0) {
					auline.write(abData, 0, nBytesRead);
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
			return;
		} finally {
			auline.drain();
			auline.close();
		}

	}
}