package accufiresystems.rangemaster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;

public class Settings {
	private int DETECT_INTERTIA = 250;
	private int DETECT_INTERVAL = 30;
	private int DETECT_THRESHOLD = 70;
	private double ACCEPT_THRESHOLD_HIGH = 0.008;
	private double ACCEPT_THRESHOLD_LOW = 0.00008;

	private String CAMERA_NAME = null;
	private String CAMERA_RES = null;

	public synchronized int getDetectInertia() {
		return DETECT_INTERTIA;
	}

	public synchronized int getDetectInterval() {
		return DETECT_INTERVAL;
	}

	public synchronized int getDetectThreshold() {
		return DETECT_THRESHOLD;
	}

	public synchronized double getAcceptThresholdHigh() {
		return ACCEPT_THRESHOLD_HIGH;
	}

	public synchronized double getAcceptThresholdLow() {
		return ACCEPT_THRESHOLD_LOW;
	}

	public synchronized String getCameraName() {
		return CAMERA_NAME;
	}

	public synchronized String getCameraRes() {
		return CAMERA_RES;
	}

	public synchronized void setDetectInertia(final int inertia) {
		DETECT_INTERTIA = inertia;
	}

	public synchronized void setDetectInterval(final int interval) {
		DETECT_INTERVAL = interval;
	}

	public synchronized void setDetectThreshold(final int threshold) {
		DETECT_THRESHOLD = threshold;
	}

	public synchronized void setAcceptThresholdHigh(final double high) {
		ACCEPT_THRESHOLD_HIGH = high;
	}

	public synchronized void setAcceptThresholdLow(final double low) {
		ACCEPT_THRESHOLD_LOW = low;
	}

	public synchronized void setCameraName(final String name) {
		CAMERA_NAME = name;
	}

	public synchronized void setCameraRes(final String res) {
		CAMERA_RES = res;
	}

	public void writeToFile() {
		try {
			final String temp = RSRSMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			final File path = new File(temp.substring(0, temp.lastIndexOf('/') + 1) + "config.txt");

			final BufferedWriter output = new BufferedWriter(new FileWriter(path));
			output.write("DETECT_INTERVAL="+getDetectInterval()+"\n");
			output.write("DETECT_INTERTIA="+getDetectInertia()+"\n");
			output.write("DETECT_THRESHOLD="+getDetectThreshold()+"\n");

			final DecimalFormat df = new DecimalFormat("#.#");
			df.setMaximumFractionDigits(12);

			output.write("ACCEPT_THRESHOLD_LOW="+df.format(getAcceptThresholdLow())+"\n");
			output.write("ACCEPT_THRESHOLD_HIGH="+df.format(getAcceptThresholdHigh())+"\n");

			if (getCameraName() != null)
				output.write("CAMERA_NAME="+getCameraName()+"\n");
			if (getCameraRes() != null)
				output.write("CAMERA_RES="+getCameraRes()+"\n");

			output.close();

		} catch (final RuntimeException e) {
		    throw e;
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
