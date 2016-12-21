package accufiresystems.rangemaster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class RSRSMain {
	
	private static final int TRIAL_LENGTH = 30;

	public static void main(final String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (final Exception e) {
			System.err.println("Something went wrong when setting look and feel.");
			e.printStackTrace();
		}

		final Settings settings = new Settings();

		try {
			final String temp = RSRSMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			final File path = new File(temp.substring(0, temp.lastIndexOf('/') + 1) + "config.txt");
			if (path.exists()) {
				final BufferedReader br = new BufferedReader(new FileReader(path));
				String line;
				while ((line = br.readLine()) != null) {
					final String split[] = line.replace(',','.').split("=");
					System.out.println("Loaded: " + split[0] + " = " + split[1]);
					if (split[0].equals("DETECT_INTERVAL")) {
						settings.setDetectInterval(Integer.parseInt(split[1]));
					} else if (split[0].equals("DETECT_INTERTIA")) {
						settings.setDetectInertia(Integer.parseInt(split[1]));
					} else if (split[0].equals("DETECT_THRESHOLD")) {
						settings.setDetectThreshold(Integer.parseInt(split[1]));
					} else if (split[0].equals("ACCEPT_THRESHOLD_LOW")) {
						settings.setAcceptThresholdLow(Double.parseDouble(split[1]));
					} else if (split[0].equals("ACCEPT_THRESHOLD_HIGH")) {
						settings.setAcceptThresholdHigh(Double.parseDouble(split[1]));
					} else if (split[0].equals("CAMERA_NAME")) {
						settings.setCameraName(split[1]);
					} else if (split[0].equals("CAMERA_RES")) {
						settings.setCameraRes(split[1]);
					}
					
				}
				br.close();
			}
		} catch (final RuntimeException e) {
		    throw e;
		} catch (final Exception e) {
			e.printStackTrace();
		}

		final RSRSWindow window = new RSRSWindow(settings);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				window.setVisible(true);
			}
		});

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				window.closeCamera();
			}
		});
	}

}
