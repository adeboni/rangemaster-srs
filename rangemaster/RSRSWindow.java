package accufiresystems.rangemaster;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.miginfocom.swing.MigLayout;
import webcam.Webcam;
import webcam.WebcamMotionDetector;
import webcam.WebcamMotionEvent;
import webcam.WebcamMotionListener;

public class RSRSWindow extends JFrame {

	private static final long serialVersionUID = -8057300813109148399L;
	private static Webcam webcam;
	private JMenu mnSetResolution;
	private JMenu mnSelectCamera;
	private JToggleButton tglbtnReactionStart;
	private JToggleButton tglbtnAreaStart;
	private JComboBox<String> cboReactionDelay;
	private JComboBox<String> cboAreaDelay;
	private JCheckBox chckbxRepeat;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
	private static ScheduledFuture<?> imageFuture;
	private static ScheduledFuture<?> toneFuture;
	private final Runnable imageUpdater;
	private final Runnable toneTimer;
	private int fpsSetting = 30;
	private double unitMult = 1.0;
	private String unitText = "inches";
	private JDialog loadingDialog;
	private JLabel lblCurrentPlayer;
	private JLabel lblSoloSize;
	private JLabel lblSoloCenter;
	private JLabel lblCompSize;
	private JTextArea tpaneReactionTimes;
	private JTextArea tpaneSoloScores;
	private JTextArea tpaneCompScores;
	private JTextArea tpaneAreaTimes;
	private JPanel lpaneModes;
	private ImagePanel pnlWebcam;
	private JComboBox<String> cboSounds;
	private long toneTime = -1;
	private boolean isCalLength = false;
	private boolean isCalCent = false;
	private final double[] calPoints = new double[4];
	private final double[] calCenter = new double[2];
	private long startTime;

	private static final int SOLO_PRACTICE = 0;
	private static final int COMPETITION = 1;
	private static final int AREA_SHOOTING = 2;
	private static final int REACTION_TIMING = 3;
	private int selectedMode = SOLO_PRACTICE;

	private static final int BRIGHT_SPOT_DETECT = 0;
	private static final int COLOR_DIFF_DETECT = 1;
	private int selectedDetect = BRIGHT_SPOT_DETECT;

	private WebcamMotionDetector detector;
	private final WebcamMotionListener detectionEvent;
	private final Settings settings;
	private final SettingsWindow settingsWindow;

	private int numPlayers = 2;
	private int numShots = 5;
	private int currentPlayer = 1;
	private final List<Double> scores = new ArrayList<Double>();

	private void doCalibration() {
		final int width = pnlWebcam.getImageWidth();
		final int height = pnlWebcam.getImageHeight();

		final Point unscaledStart = pnlWebcam.getCalStart();
		final Point unscaledEnd = pnlWebcam.getCalEnd();
		if (unscaledStart != null && unscaledEnd != null) {
			calPoints[0] = unscaledStart.getX() / width;
			calPoints[1] = unscaledStart.getY() / height;
			calPoints[2] = unscaledEnd.getX() / width;
			calPoints[3] = unscaledEnd.getY() / height;
			isCalLength = true;
		}

		final Point center = pnlWebcam.getTargetCenter();
		if (center != null) {
			calCenter[0] = center.getX() / width;
			calCenter[1] = center.getY() / height;
			isCalCent = true;
		}
	}

	private void resetDetector() {
		imageFuture = scheduler.scheduleAtFixedRate(imageUpdater, 0, 1000 / fpsSetting, TimeUnit.MILLISECONDS);
		detector = new WebcamMotionDetector(webcam, settings, selectedDetect);
		detector.addMotionListener(detectionEvent);
		detector.start();
	}

	private void scheduleTone(final int index) {
		int delay = 0;
		int length = 0;
		switch (index) {
		case 0:
			delay = 2;
			length = 3;
			break;
		case 1:
			delay = 3;
			length = 4;
			break;
		case 2:
			delay = 5;
			length = 5;
			break;
		case 3:
			delay = 10;
			length = 5;
			break;
		default:
			break;
		}

		toneFuture = scheduler.schedule(toneTimer,
				(int)(1000L*delay+1000L*length*Math.random()), TimeUnit.MILLISECONDS);
	}

	private void resetResolutions() {
		mnSetResolution.removeAll();
		final Dimension sizes[] = webcam.getViewSizes();
		final ButtonGroup resGroup = new ButtonGroup();
		for (final Dimension d : sizes) {
			final JRadioButtonMenuItem item = new JRadioButtonMenuItem((int)d.getWidth() + " x " + (int)d.getHeight());
			item.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(final ActionEvent e) {
					closeCamera();
					final String temp[] = item.getText().split(" x ");
					webcam.setViewSize(new Dimension(Integer.parseInt(temp[0]), Integer.parseInt(temp[1])));
					settings.setCameraRes(item.getText());
					settings.writeToFile();
					webcam.open(true);
					resetDetector();
				}
			});
			resGroup.add(item);
			
			if (settings.getCameraRes() != null && 
					settings.getCameraRes().equals((int)d.getWidth() + " x " + (int)d.getHeight())) {
				webcam.setViewSize(d);
			}
			
			if (webcam.getViewSize().equals(d)) {
				item.setSelected(true);
			}
			mnSetResolution.add(item);

		}
	}

	/** Create the application. */
	public RSRSWindow(final Settings settings) {
		this.settings = settings;
		settingsWindow = new SettingsWindow(settings);

		detectionEvent = new WebcamMotionListener(){
			@Override
			public void motionDetected(final WebcamMotionEvent wme) {

				final Shot shot = new Shot(wme.getXLocation(), wme.getYLocation(), System.nanoTime());
				Shot.shots.add(shot);

				try {
					playSound();
				} catch (final LineUnavailableException e) {
					e.printStackTrace();
				} catch (final IOException e) {
					e.printStackTrace();
				} catch (final UnsupportedAudioFileException e) {
					e.printStackTrace();
				}

				switch (selectedMode) {
				case SOLO_PRACTICE:
					if (isCalLength) {
						lblSoloSize.setText("Group size: " +
								String.format("%.2f", Shot.getGroupSize(calPoints, Shot.shots)*unitMult) + " " + unitText);
					}

					if (isCalCent && isCalLength) {
						lblSoloCenter.setText(String.format("%.2f", Shot.getDistFromCenter(calPoints, calCenter)*unitMult) + " " + unitText + " from center");
					}

					if (Shot.shots.size() == 1) {
						startTime = System.nanoTime();
						tpaneSoloScores.append("1: 0 sec / 0 sec");
					} else {
						tpaneSoloScores.append("\n" + Shot.shots.size() + ": " +
								String.format("%.2f", (shot.getTime() - Shot.shots.get(Shot.shots.size()-2).getTime())/1000000000.0) + " sec / " +
								String.format("%.2f", (shot.getTime() - startTime)/1000000000.0) + " sec");
					}
					break;

				case COMPETITION:

					if (currentPlayer <= numPlayers && isCalLength) {
						if (Shot.shots.size() % numShots == 0) {
							final double groupSize = Shot.getGroupSize(calPoints,
									Shot.shots.subList(Shot.shots.size()-numShots, Shot.shots.size()))*unitMult;
							scores.add(groupSize);
							new PlayWave(getClass().getResource("/resources/beep.wav")).start();
							tpaneCompScores.append("Player " + currentPlayer + ": " +
									String.format("%.2f", groupSize) + " " + unitText + "\n");

							currentPlayer++;

							if (currentPlayer > numPlayers) {
								int index = 0;
								double min = 1e99;
								for (int i = 0; i < numPlayers; i++) {
									if (scores.get(i) < min) {
										min = scores.get(i);
										index = i+1;
									}
								}

								JOptionPane.showMessageDialog(null,
										"Player " + index +
										" wins with a group size of " +
										String.format("%.2f", scores.get(index-1))
										+ " " + unitText,
										"Winner!",
										JOptionPane.INFORMATION_MESSAGE);
							}
						}

						if (currentPlayer <= numPlayers) {
							lblCurrentPlayer.setText("Current Player: " + currentPlayer);

							final double groupSize = Shot.getGroupSize(calPoints,
									Shot.shots.subList(Shot.shots.size()-Shot.shots.size()%numShots,
											Shot.shots.size()))*unitMult;

							lblCompSize.setText("Group size: " + String.format("%.2f", groupSize) + " " +unitText);
						}
					}

					break;

				case REACTION_TIMING:
					if (toneTime != -1) {
						final long time = System.nanoTime();
						tpaneReactionTimes.append(tpaneReactionTimes.getLineCount() + ": " +
								String.format("%.2f", (time - toneTime)/1000000000.0) + " sec\n");

						if (chckbxRepeat.isSelected()) {
							if (toneFuture != null && toneFuture.isDone()) {
								scheduleTone(cboReactionDelay.getSelectedIndex());
							}
						} else {
							if (tglbtnReactionStart.isSelected()) {
								tglbtnReactionStart.doClick();
							}
						}
					}
					break;

				case AREA_SHOOTING:
					if (toneTime != -1) {
						final long time = System.nanoTime();

						boolean hit = false;
						boolean allHit = true;
						for (final Target t : Target.targets) {
							final double r = PointUtils.getDist(shot.getX()*pnlWebcam.getImageWidth(),
									shot.getY()*pnlWebcam.getImageHeight(),
									(t.getX() + t.getR())*pnlWebcam.getImageWidth(),
									t.getY()*pnlWebcam.getImageHeight() + t.getR()*pnlWebcam.getImageWidth());
							if (r < t.getR()*pnlWebcam.getImageWidth()) {
								t.setHit(true);
								hit = true;
								new PlayWave(getClass().getResource("/resources/steel.wav")).start();
							}

							if (!t.isHit()) {
								allHit = false;
							}
						}

						tpaneAreaTimes.append((hit ? "HIT" : "MISS") + ": " +
								String.format("%.2f", (time - toneTime)/1000000000.0) + " sec\n");

						if (tglbtnAreaStart.isSelected() && allHit) {
							tglbtnAreaStart.doClick();
							new PlayWave(getClass().getResource("/resources/beep.wav")).start();
						}
					}
					break;

				default:
					break;
				}

			}

		};

		toneTimer = new Runnable() {
			@Override
			public void run() {
				new PlayWave(getClass().getResource("/resources/beep.wav")).start();
				toneTime = System.nanoTime();
			}
		};

		imageUpdater = new Runnable() {
			@Override
			public void run() {
				pnlWebcam.updateImage(webcam.getImage());
			}
		};
		
		initialize();

		final ButtonGroup camGroup = new ButtonGroup();
		final List<Webcam> cameras = Webcam.getWebcams();
		for (int i = 0; i < cameras.size(); i++) {
			final JRadioButtonMenuItem item = new JRadioButtonMenuItem(cameras.get(i).getName());
			item.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(final ActionEvent arg0) {
					closeCamera();

					int camIndex = 0;
					while (camIndex < cameras.size() && !cameras.get(camIndex).getName().equals(item.getText())) {
						camIndex++;
					}
					webcam = cameras.get(camIndex < cameras.size() ? camIndex : 0);
					webcam.open(true);
					closeCamera();
					resetResolutions();
					webcam.open(true);
					resetDetector();
					settings.setCameraName(webcam.getName());
					settings.writeToFile();
				}
			});
			camGroup.add(item);
			if (i == 0) {
				item.setSelected(true);
			}
			mnSelectCamera.add(item);
		}

		if (!cameras.isEmpty()) {
			int camIndex = 0;
			while (camIndex < cameras.size() && !cameras.get(camIndex).getName().equals(settings.getCameraName())) {
				camIndex++;
			}
			webcam = cameras.get(camIndex < cameras.size() ? camIndex : 0);
			webcam.open(true);
			closeCamera();
			resetResolutions();
			webcam.open(true);
			resetDetector();
			settings.setCameraName(webcam.getName());
			settings.writeToFile();
		} else {
			JOptionPane.showMessageDialog(this, "No camera found.", "Rangemaster SRS", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}

		loadingDialog.setVisible(false);
	}

	/** Initialize the contents of the frame. */
	private void initialize() {

		this.setTitle("Rangemaster SRS");
		this.setBounds(100, 100, 896, 572);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setIconImage(new ImageIcon(getClass().getResource("/resources/icon.png")).getImage());

		final int reply = JOptionPane.showConfirmDialog(this,
				"Please verify that:" +
						"\n\n1. Your firearm(s) is/are unloaded." +
						"\n\n2. Your clips, magazines, speed loaders, or chambers are clear of ammunition." +
						"\n\n3. Remove all ammuniation and unused firearms from the area that you are practicing in." +
						"\n\n4. Treat your firearms as loaded at all times and point them in a safe direction.", "Warning", JOptionPane.OK_CANCEL_OPTION);
		if (reply != JOptionPane.OK_OPTION){
			System.exit(0);
		}

		loadingDialog = (new JOptionPane("Loading...")).createDialog(null, "Rangemaster SRS");
		loadingDialog.setModalityType(ModalityType.MODELESS);
		loadingDialog.setVisible(true);

		final JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);

		final JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		final JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				System.exit(0);
			}
		});

		final JMenuItem mntmSaveImage = new JMenuItem("Save Image");
		mntmSaveImage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				final JFileChooser fileChooser = new JFileChooser(){
					private static final long serialVersionUID = 1L;
					@Override
					public void approveSelection(){
						final File f = getSelectedFile();
						if(f.exists() && getDialogType() == SAVE_DIALOG){
							final int result = JOptionPane.showConfirmDialog(this,"The file exists, overwrite?","Existing file",JOptionPane.YES_NO_CANCEL_OPTION);
							switch(result){
							case JOptionPane.YES_OPTION:
								super.approveSelection();
								return;
							case JOptionPane.NO_OPTION:
								return;
							case JOptionPane.CLOSED_OPTION:
								return;
							case JOptionPane.CANCEL_OPTION:
								cancelSelection();
								return;
							}
						}
						super.approveSelection();
					}
				};

				fileChooser.setDialogTitle("Save target image");

				final FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("png files (*.png)", "png");
				final FileNameExtensionFilter jpegFilter = new FileNameExtensionFilter("jpg files (*.jpg)", "jpg");
				final FileNameExtensionFilter gifFilter = new FileNameExtensionFilter("gif files (*.gif)", "gif");
				final FileNameExtensionFilter bmpFilter = new FileNameExtensionFilter("bmp files (*.bmp)", "bmp");
				fileChooser.addChoosableFileFilter(pngFilter);
				fileChooser.addChoosableFileFilter(jpegFilter);
				fileChooser.addChoosableFileFilter(gifFilter);
				fileChooser.addChoosableFileFilter(bmpFilter);
				fileChooser.setFileFilter(pngFilter);
				fileChooser.setAcceptAllFileFilterUsed(false);

				if (fileChooser.showSaveDialog(mntmSaveImage.getParent()) == JFileChooser.APPROVE_OPTION) {
					final BufferedImage bImg = new BufferedImage(pnlWebcam.getImageWidth(), pnlWebcam.getImageHeight(), BufferedImage.TYPE_INT_RGB);
					final Graphics2D cg = bImg.createGraphics();
					pnlWebcam.paintAll(cg);
					final String ext = fileChooser.getFileFilter().getDescription().split(" ")[0];

					File selection = fileChooser.getSelectedFile();

					if (!selection.getName().endsWith("." + ext)) {
						selection = new File(selection.getAbsolutePath() + "." + ext);
					}

					try {
						if (ImageIO.write(bImg, ext, selection))
						{
							System.out.println("image saved");
						} else {
							System.err.println("image failed to save");
						}
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
		mnFile.add(mntmSaveImage);

		final JMenuItem mntmSaveStats = new JMenuItem("Save Stats");
		mntmSaveStats.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				final JFileChooser fileChooser = new JFileChooser(){
					private static final long serialVersionUID = 1L;
					@Override
					public void approveSelection(){
						final File f = getSelectedFile();
						if(f.exists() && getDialogType() == SAVE_DIALOG){
							final int result = JOptionPane.showConfirmDialog(this,"The file exists, overwrite?","Existing file",JOptionPane.YES_NO_CANCEL_OPTION);
							switch(result){
							case JOptionPane.YES_OPTION:
								super.approveSelection();
								return;
							case JOptionPane.NO_OPTION:
								return;
							case JOptionPane.CLOSED_OPTION:
								return;
							case JOptionPane.CANCEL_OPTION:
								cancelSelection();
								return;
							}
						}
						super.approveSelection();
					}
				};

				fileChooser.setDialogTitle("Save shooting data");

				final FileNameExtensionFilter txtFilter = new FileNameExtensionFilter("Text files (*.txt)", "txt");
				fileChooser.addChoosableFileFilter(txtFilter);
				fileChooser.setFileFilter(txtFilter);
				fileChooser.setAcceptAllFileFilterUsed(false);

				if (fileChooser.showSaveDialog(mntmSaveStats.getParent()) == JFileChooser.APPROVE_OPTION) {
					File selection = fileChooser.getSelectedFile();

					if (!selection.getName().endsWith(".txt")) {
						selection = new File(selection.getAbsolutePath() + ".txt");
					}

					PrintWriter writer;
					try {
						writer = new PrintWriter(selection, "UTF-8");
						switch (selectedMode) {
						case SOLO_PRACTICE:
							writer.println(tpaneSoloScores.getText());
							break;

						case COMPETITION:
							writer.println(tpaneCompScores.getText());
							break;

						case REACTION_TIMING:
							writer.println(tpaneReactionTimes.getText());
							break;

						case AREA_SHOOTING:
							writer.println(tpaneAreaTimes.getText());
							break;

						default:
							break;
						}
						writer.close();
					} catch (final FileNotFoundException e) {
						e.printStackTrace();
					} catch (final UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
		});
		mnFile.add(mntmSaveStats);
		mnFile.addSeparator();
		mnFile.add(mntmExit);


		final JMenu mnCamera = new JMenu("Camera");
		menuBar.add(mnCamera);

		mnSelectCamera = new JMenu("Select Camera");
		mnCamera.add(mnSelectCamera);

		mnSetResolution = new JMenu("Set Resolution");
		mnCamera.add(mnSetResolution);

		final JMenu mnSetFps = new JMenu("Set FPS");
		mnCamera.add(mnSetFps);

		final JRadioButtonMenuItem rdbtnmntmFps60 = new JRadioButtonMenuItem("60 FPS");
		final JRadioButtonMenuItem rdbtnmntmFps45 = new JRadioButtonMenuItem("45 FPS");
		final JRadioButtonMenuItem rdbtnmntmFps30 = new JRadioButtonMenuItem("30 FPS");
		final JRadioButtonMenuItem rdbtnmntmFps15 = new JRadioButtonMenuItem("15 FPS");
		final ButtonGroup fpsGroup = new ButtonGroup();
		fpsGroup.add(rdbtnmntmFps60);
		fpsGroup.add(rdbtnmntmFps45);
		fpsGroup.add(rdbtnmntmFps30);
		fpsGroup.add(rdbtnmntmFps15);

		rdbtnmntmFps60.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				fpsSetting = 60;
				if (imageFuture != null) {
					imageFuture.cancel(false);
				}
				imageFuture = scheduler.scheduleAtFixedRate(imageUpdater, 0, 1000 / fpsSetting, TimeUnit.MILLISECONDS);
			}
		});
		mnSetFps.add(rdbtnmntmFps60);


		rdbtnmntmFps45.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				fpsSetting = 45;
				if (imageFuture != null) {
					imageFuture.cancel(false);
				}
				imageFuture = scheduler.scheduleAtFixedRate(imageUpdater, 0, 1000 / fpsSetting, TimeUnit.MILLISECONDS);
			}
		});
		mnSetFps.add(rdbtnmntmFps45);


		rdbtnmntmFps30.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				fpsSetting = 30;
				if (imageFuture != null) {
					imageFuture.cancel(false);
				}
				imageFuture = scheduler.scheduleAtFixedRate(imageUpdater, 0, 1000 / fpsSetting, TimeUnit.MILLISECONDS);
			}
		});
		rdbtnmntmFps30.setSelected(true);
		mnSetFps.add(rdbtnmntmFps30);


		rdbtnmntmFps15.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				fpsSetting = 15;
				if (imageFuture != null) {
					imageFuture.cancel(false);
				}
				imageFuture = scheduler.scheduleAtFixedRate(imageUpdater, 0, 1000 / fpsSetting, TimeUnit.MILLISECONDS);
			}
		});
		mnSetFps.add(rdbtnmntmFps15);

		final JMenu mnSetDetectionMethod = new JMenu("Set Detection Method");
		mnCamera.add(mnSetDetectionMethod);

		final ButtonGroup settingsGroup = new ButtonGroup();
		final JRadioButtonMenuItem rdbtnmntmBrightSpotDetection = new JRadioButtonMenuItem("Bright spot detection");
		rdbtnmntmBrightSpotDetection.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (selectedDetect != BRIGHT_SPOT_DETECT) {
					selectedDetect = BRIGHT_SPOT_DETECT;
					detector.setDetect(selectedDetect);
				}
			}
		});
		rdbtnmntmBrightSpotDetection.setSelected(true);
		mnSetDetectionMethod.add(rdbtnmntmBrightSpotDetection);

		final JRadioButtonMenuItem rdbtnmntmColorDifferenceDetection = new JRadioButtonMenuItem("Color difference detection");
		rdbtnmntmColorDifferenceDetection.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (selectedDetect != COLOR_DIFF_DETECT) {
					selectedDetect = COLOR_DIFF_DETECT;
					detector.setDetect(selectedDetect);
				}
			}
		});
		mnSetDetectionMethod.add(rdbtnmntmColorDifferenceDetection);
		settingsGroup.add(rdbtnmntmBrightSpotDetection);
		settingsGroup.add(rdbtnmntmColorDifferenceDetection);

		final JMenuItem mntmColorDifferenceSettings = new JMenuItem("Color difference settings");
		mntmColorDifferenceSettings.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				settingsWindow.setVisible(true);
			}
		});
		mnSetDetectionMethod.add(mntmColorDifferenceSettings);

		final JMenu mnUnits = new JMenu("Units");
		menuBar.add(mnUnits);

		final JRadioButtonMenuItem rdbtnmntmMetric = new JRadioButtonMenuItem("Metric");
		final JRadioButtonMenuItem rdbtnmntmImperial = new JRadioButtonMenuItem("Imperial");
		final ButtonGroup unitGroup = new ButtonGroup();
		unitGroup.add(rdbtnmntmMetric);
		unitGroup.add(rdbtnmntmImperial);

		rdbtnmntmMetric.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				unitMult = 2.54;
				unitText = "cm";
			}
		});
		mnUnits.add(rdbtnmntmMetric);


		rdbtnmntmImperial.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				unitMult = 1.0;
				unitText = "inches";
			}
		});
		rdbtnmntmImperial.setSelected(true);
		mnUnits.add(rdbtnmntmImperial);

		final JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		final JMenuItem mntmViewHelp = new JMenuItem("View Help");
		mntmViewHelp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				JOptionPane.showMessageDialog(mntmViewHelp.getParent(),
						"For help, please open the Manual.pdf file included with the software.",
						"Help",
						JOptionPane.QUESTION_MESSAGE);
			}
		});
		mnHelp.add(mntmViewHelp);
		mnHelp.addSeparator();

		final JMenuItem mntmAboutAccufireSystems = new JMenuItem("About Rangemaster SRS");
		mntmAboutAccufireSystems.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				JOptionPane.showMessageDialog(mntmAboutAccufireSystems.getParent(),
						"Rangemaster SRS\n\nVersion 3.0\n\n© 2012 Alex DeBoni\n\nAccufire Systems",
						"About Rangemaster SRS",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		mnHelp.add(mntmAboutAccufireSystems);
		getContentPane().setLayout(new BorderLayout(0, 0));

		final JSplitPane splitPane = new JSplitPane();
		getContentPane().add(splitPane);
		final JButton btnClear = new JButton("Clear Shots");

		final JPanel pnlSettings = new JPanel();
		splitPane.setLeftComponent(pnlSettings);
		pnlSettings.setLayout(new MigLayout("", "[grow]", "[301.00,grow][]"));

		final JPanel pnlModes = new JPanel();
		pnlModes.setBorder(new TitledBorder(null, "Mode", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlSettings.add(pnlModes, "cell 0 0,grow");
		pnlModes.setLayout(new MigLayout("", "[grow]", "[][71.00,grow][][]"));

		final JComboBox<String> cboModes = new JComboBox<String>();
		cboModes.setModel(new DefaultComboBoxModel<String>(new String[] {"Solo Practice", "Competition", "Reaction Timing", "Area Shooting"}));
		cboModes.setSelectedIndex(0);
		pnlModes.add(cboModes, "cell 0 0,growx");

		lpaneModes = new JPanel();
		pnlModes.add(lpaneModes, "cell 0 1,grow");
		lpaneModes.setLayout(new CardLayout(0,0));
		final JPanel pnlSolo = new JPanel();

		pnlSolo.setBounds(0, 0, 157, 270);
		lpaneModes.add(pnlSolo, "Solo Practice");
		pnlSolo.setLayout(new BorderLayout(0, 0));

		final JToggleButton tglbtnCalibrateSolo = new JToggleButton("Calibrate");
		final JToggleButton tglbtnCalibrateComp = new JToggleButton("Calibrate");
		tglbtnCalibrateSolo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (tglbtnCalibrateSolo.isSelected()) {
					tglbtnCalibrateSolo.setText("Finish Calibration");
					tglbtnCalibrateComp.setSelected(true);
					tglbtnCalibrateComp.setText("Finish Calibration");
					pnlWebcam.startCalibrating();
				} else {
					doCalibration();
					tglbtnCalibrateSolo.setText("Calibrate");
					tglbtnCalibrateComp.setSelected(false);
					tglbtnCalibrateComp.setText("Calibrate");
					pnlWebcam.stopCalibrating();
				}
			}
		});
		pnlSolo.add(tglbtnCalibrateSolo, BorderLayout.NORTH);

		final JPanel pnlSoloDetail = new JPanel();
		pnlSolo.add(pnlSoloDetail, BorderLayout.CENTER);
		pnlSoloDetail.setLayout(new BorderLayout(0, 0));

		final JScrollPane spaneSoloScores = new JScrollPane();
		pnlSoloDetail.add(spaneSoloScores, BorderLayout.CENTER);

		tpaneSoloScores = new JTextArea();
		tpaneSoloScores.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		tpaneSoloScores.setEditable(false);
		spaneSoloScores.setViewportView(tpaneSoloScores);

		final JLabel lblSoloScoresHeader = new JLabel("Time from last shot/total time");
		spaneSoloScores.setColumnHeaderView(lblSoloScoresHeader);

		final JPanel pnlGroupSize = new JPanel();
		pnlSoloDetail.add(pnlGroupSize, BorderLayout.NORTH);
		pnlGroupSize.setLayout(new BorderLayout(0, 0));

		lblSoloSize = new JLabel("");
		lblSoloSize.setFont(new Font("Segoe UI", Font.BOLD, 13));
		pnlGroupSize.add(lblSoloSize, BorderLayout.NORTH);

		lblSoloCenter = new JLabel("");
		lblSoloCenter.setFont(new Font("Segoe UI", Font.BOLD, 13));
		pnlGroupSize.add(lblSoloCenter, BorderLayout.SOUTH);

		final JPanel pnlComp = new JPanel();
		pnlComp.setBounds(0, 0, 157, 270);
		lpaneModes.add(pnlComp, "Competition");
		pnlComp.setLayout(new BorderLayout(0, 0));

		tglbtnCalibrateComp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (tglbtnCalibrateComp.isSelected()) {
					tglbtnCalibrateSolo.setText("Finish Calibration");
					tglbtnCalibrateSolo.setSelected(true);
					tglbtnCalibrateComp.setText("Finish Calibration");
					pnlWebcam.startCalibrating();
				} else {
					doCalibration();
					tglbtnCalibrateSolo.setText("Calibrate");
					tglbtnCalibrateSolo.setSelected(false);
					tglbtnCalibrateComp.setText("Calibrate");
					pnlWebcam.stopCalibrating();
				}
			}
		});
		pnlComp.add(tglbtnCalibrateComp, BorderLayout.NORTH);

		final JPanel pnlCompDetail = new JPanel();
		pnlComp.add(pnlCompDetail, BorderLayout.CENTER);
		pnlCompDetail.setLayout(new BorderLayout(0, 0));

		final JSpinner spnPlayerNum = new JSpinner();
		final JSpinner spnPlayerShots = new JSpinner();
		final JButton btnApply = new JButton("Apply");
		btnApply.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				numPlayers = (Integer)spnPlayerNum.getValue();
				numShots = (Integer)spnPlayerShots.getValue();
				pnlWebcam.setCompSettings(numShots, numPlayers);
				btnClear.doClick();
			}
		});
		pnlCompDetail.add(btnApply, BorderLayout.SOUTH);

		final JPanel pnlCompScore = new JPanel();
		pnlCompDetail.add(pnlCompScore, BorderLayout.NORTH);
		pnlCompScore.setLayout(new BorderLayout(0, 0));

		lblCurrentPlayer = new JLabel("Current Player: 1");
		lblCurrentPlayer.setFont(new Font("Segoe UI", Font.BOLD, 13));
		pnlCompScore.add(lblCurrentPlayer, BorderLayout.NORTH);

		lblCompSize = new JLabel("Group Size: 0 inches");
		lblCompSize.setFont(new Font("Segoe UI", Font.BOLD, 13));
		pnlCompScore.add(lblCompSize, BorderLayout.SOUTH);

		final JPanel pnlCompScores = new JPanel();
		pnlCompDetail.add(pnlCompScores, BorderLayout.CENTER);
		pnlCompScores.setLayout(new BorderLayout(0, 0));

		final JScrollPane spaneCompScores = new JScrollPane();
		pnlCompScores.add(spaneCompScores, BorderLayout.CENTER);

		tpaneCompScores = new JTextArea();
		tpaneCompScores.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		tpaneCompScores.setEditable(false);
		spaneCompScores.setViewportView(tpaneCompScores);

		final JPanel pnlCompSettings = new JPanel();
		pnlCompScores.add(pnlCompSettings, BorderLayout.SOUTH);
		pnlCompSettings.setLayout(new BorderLayout(0, 0));

		final JPanel pnlCompPlayers = new JPanel();
		pnlCompSettings.add(pnlCompPlayers, BorderLayout.NORTH);
		pnlCompPlayers.setLayout(new BorderLayout(0, 0));

		final JLabel lblNumberOfPlayers = new JLabel("Number of players:");
		pnlCompPlayers.add(lblNumberOfPlayers, BorderLayout.WEST);

		spnPlayerNum.setModel(new SpinnerNumberModel(2, 2, 100, 1));
		pnlCompPlayers.add(spnPlayerNum, BorderLayout.EAST);

		final JPanel pnlCompShots = new JPanel();
		pnlCompSettings.add(pnlCompShots, BorderLayout.SOUTH);
		pnlCompShots.setLayout(new BorderLayout(0, 0));

		final JLabel lblShotsPerPlayer = new JLabel("Shots per player:");
		pnlCompShots.add(lblShotsPerPlayer, BorderLayout.WEST);

		spnPlayerShots.setModel(new SpinnerNumberModel(5, 1, 100, 1));
		pnlCompShots.add(spnPlayerShots, BorderLayout.EAST);

		final JPanel pnlReaction = new JPanel();
		pnlReaction.setBounds(0, 0, 157, 270);
		lpaneModes.add(pnlReaction, "Reaction Timing");
		pnlReaction.setLayout(new BorderLayout(0, 0));

		final JScrollPane spaneReactionTimes = new JScrollPane();
		pnlReaction.add(spaneReactionTimes, BorderLayout.CENTER);

		tpaneReactionTimes = new JTextArea();
		tpaneReactionTimes.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		tpaneReactionTimes.setEditable(false);
		spaneReactionTimes.setViewportView(tpaneReactionTimes);
		tpaneReactionTimes.setColumns(10);

		final JPanel pnlReactionSettings = new JPanel();
		pnlReaction.add(pnlReactionSettings, BorderLayout.NORTH);
		pnlReactionSettings.setLayout(new BorderLayout(0, 0));

		tglbtnReactionStart = new JToggleButton("Start");
		tglbtnReactionStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				toneTime = -1;

				if (tglbtnReactionStart.isSelected()) {
					Shot.shots.clear();
					tpaneReactionTimes.setText("");
					tglbtnReactionStart.setText("Stop");
					scheduleTone(cboReactionDelay.getSelectedIndex());
				} else {
					tglbtnReactionStart.setText("Start");
					if (toneFuture != null && !toneFuture.isDone()) {
						toneFuture.cancel(false);
					}
				}
			}
		});
		pnlReactionSettings.add(tglbtnReactionStart, BorderLayout.SOUTH);

		final JPanel pnlReactionSettingsDetail = new JPanel();
		pnlReactionSettings.add(pnlReactionSettingsDetail, BorderLayout.NORTH);
		pnlReactionSettingsDetail.setLayout(new BorderLayout(0, 0));

		chckbxRepeat = new JCheckBox("Repeat for each shot");
		pnlReactionSettingsDetail.add(chckbxRepeat, BorderLayout.SOUTH);

		final JPanel pnlReactionDelay = new JPanel();
		pnlReactionSettingsDetail.add(pnlReactionDelay, BorderLayout.NORTH);
		pnlReactionDelay.setLayout(new BorderLayout(0, 0));

		final JLabel lblReactionDelay = new JLabel("Delay:");
		pnlReactionDelay.add(lblReactionDelay, BorderLayout.WEST);

		cboReactionDelay = new JComboBox<String>();
		cboReactionDelay.setModel(new DefaultComboBoxModel<String>(new String[] {"2 to 5 seconds", "3 to 7 seconds", "5 to 10 seconds", "10 to 15 seconds"}));
		pnlReactionDelay.add(cboReactionDelay, BorderLayout.EAST);

		final JPanel pnlArea = new JPanel();
		lpaneModes.add(pnlArea, "Area Shooting");
		pnlArea.setLayout(new BorderLayout(0, 0));

		final JScrollPane spaneAreaTimes = new JScrollPane();
		pnlArea.add(spaneAreaTimes, BorderLayout.CENTER);

		tpaneAreaTimes = new JTextArea();
		tpaneAreaTimes.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		tpaneAreaTimes.setEditable(false);
		spaneAreaTimes.setViewportView(tpaneAreaTimes);

		final JPanel pnlAreaSettings = new JPanel();
		pnlArea.add(pnlAreaSettings, BorderLayout.NORTH);
		pnlAreaSettings.setLayout(new BorderLayout(0, 0));

		tglbtnAreaStart = new JToggleButton("Start");
		tglbtnAreaStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				toneTime = -1;

				if (tglbtnAreaStart.isSelected()) {
					if (Target.targets.size() == 0) {
						JOptionPane.showMessageDialog(null,
								"Please select targets first.",
								"Area Shooting",
								JOptionPane.WARNING_MESSAGE);
						tglbtnAreaStart.setSelected(false);
						return;
					}

					Shot.shots.clear();
					Target.clearHits();
					tpaneAreaTimes.setText("");
					tglbtnAreaStart.setText("Stop");
					scheduleTone(cboAreaDelay.getSelectedIndex());
				} else {
					tglbtnAreaStart.setText("Start");
					if (toneFuture != null && !toneFuture.isDone()) {
						toneFuture.cancel(false);
					}
				}
			}
		});
		pnlAreaSettings.add(tglbtnAreaStart, BorderLayout.SOUTH);

		final JPanel pnlAreaSettingsDetail = new JPanel();
		pnlAreaSettings.add(pnlAreaSettingsDetail, BorderLayout.NORTH);
		pnlAreaSettingsDetail.setLayout(new BorderLayout(0, 0));

		final JToggleButton tglbtnSelectTargets = new JToggleButton("Select Targets");
		tglbtnSelectTargets.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (tglbtnSelectTargets.isSelected()) {
					tglbtnSelectTargets.setText("Finish Selecting Targets");
					pnlWebcam.setTargetSelect(true);
				} else {
					tglbtnSelectTargets.setText("Select Targets");
					pnlWebcam.setTargetSelect(false);
				}
			}
		});
		pnlAreaSettingsDetail.add(tglbtnSelectTargets, BorderLayout.NORTH);

		final JPanel pnlAreaDelay = new JPanel();
		pnlAreaSettingsDetail.add(pnlAreaDelay, BorderLayout.SOUTH);
		pnlAreaDelay.setLayout(new BorderLayout(0, 0));

		final JLabel lblAreaDelay = new JLabel("Delay:");
		pnlAreaDelay.add(lblAreaDelay, BorderLayout.WEST);

		cboAreaDelay = new JComboBox<String>();
		cboAreaDelay.setModel(new DefaultComboBoxModel<String>(new String[] {"2 to 5 seconds", "3 to 7 seconds", "5 to 10 seconds", "10 to 15 seconds"}));
		pnlAreaDelay.add(cboAreaDelay, BorderLayout.EAST);

		btnClear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				Shot.shots.clear();
				Target.clearHits();
				scores.clear();
				tpaneAreaTimes.setText("");
				tpaneCompScores.setText("");
				tpaneSoloScores.setText("");
				tpaneReactionTimes.setText("");
				lblSoloSize.setText("");
				lblSoloCenter.setText("");
				lblCurrentPlayer.setText("Current Player: 1");
				lblCompSize.setText("Group size: 0 " + unitText);

				tglbtnAreaStart.setSelected(false);
				tglbtnReactionStart.setSelected(false);
				tglbtnAreaStart.setText("Start");
				tglbtnReactionStart.setText("Start");

				if (toneFuture != null && !toneFuture.isDone()) {
					toneFuture.cancel(false);
				}
				toneTime = -1;
				currentPlayer = 1;
			}
		});
		pnlModes.add(btnClear, "cell 0 2,growx");

		final JButton btnDeleteLastShot = new JButton("Delete Last Shot");
		btnDeleteLastShot.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (Shot.shots.size() > 0) {
					Shot.shots.remove(Shot.shots.size() - 1);
				}
			}
		});
		pnlModes.add(btnDeleteLastShot, "cell 0 3,growx");

		final JPanel pnlSounds = new JPanel();
		pnlSounds.setBorder(new TitledBorder(null, "Sound", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlSettings.add(pnlSounds, "cell 0 1,grow");
		pnlSounds.setLayout(new MigLayout("", "[grow]", "[]"));

		cboSounds = new JComboBox<String>();
		cboSounds.setModel(new DefaultComboBoxModel<String>(new String[] {"No Sound", "Suppressed", "44 Magnum", "M4A1", "M1 Garand", "Skorpion", "50 BMG Sniper"}));
		cboSounds.setSelectedIndex(2);
		pnlSounds.add(cboSounds, "cell 0 0,growx");

		pnlWebcam = new ImagePanel();
		splitPane.setRightComponent(pnlWebcam);
		pnlWebcam.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(final MouseEvent e) {
				if (pnlWebcam.isCalibrating() && SwingUtilities.isLeftMouseButton(e)){
					pnlWebcam.setCalEnd(e.getPoint());

				} else if (pnlWebcam.isSelectingTarget() && SwingUtilities.isLeftMouseButton(e)){
					pnlWebcam.setTargetEnd(e.getPoint());
				}
			}
		});
		pnlWebcam.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent e) {
				if (pnlWebcam.isCalibrating() && SwingUtilities.isLeftMouseButton(e)) {
					pnlWebcam.setCalStart(e.getPoint());
				} else if (pnlWebcam.isCalibrating() && SwingUtilities.isRightMouseButton(e)) {
					pnlWebcam.setTargetCenter(e.getPoint());

				} else if (pnlWebcam.isSelectingTarget() && SwingUtilities.isLeftMouseButton(e)) {
					pnlWebcam.setTargetStart(e.getPoint());
				} else if (pnlWebcam.isSelectingTarget() && SwingUtilities.isRightMouseButton(e)) {
					if (Target.targets.size() > 0) {
						Target.targets.remove(Target.targets.size() - 1);
					}
				}
			}
			@Override
			public void mouseReleased(final MouseEvent e) {
				if (pnlWebcam.isSelectingTarget() && SwingUtilities.isLeftMouseButton(e)) {
					pnlWebcam.registerTarget();
				}
			}
		});
		pnlWebcam.setDoubleBuffered(true);

		cboModes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				switch (cboModes.getSelectedIndex()) {
				case 0:
					((CardLayout)lpaneModes.getLayout()).show(lpaneModes, "Solo Practice");
					selectedMode = SOLO_PRACTICE;
					break;
				case 1:
					((CardLayout)lpaneModes.getLayout()).show(lpaneModes, "Competition");
					selectedMode = COMPETITION;
					break;
				case 2:
					((CardLayout)lpaneModes.getLayout()).show(lpaneModes, "Reaction Timing");
					selectedMode = REACTION_TIMING;
					break;
				case 3:
					((CardLayout)lpaneModes.getLayout()).show(lpaneModes, "Area Shooting");
					selectedMode = AREA_SHOOTING;
					break;
				default:
					break;
				}

				if (selectedMode == AREA_SHOOTING || selectedMode == REACTION_TIMING) {
					if (tglbtnCalibrateSolo.isSelected()){
						tglbtnCalibrateSolo.doClick();
					}
				}

				pnlWebcam.setMode(selectedMode);
				btnClear.doClick();
			}
		});

	}

	private void playSound() throws LineUnavailableException, IOException, UnsupportedAudioFileException {
		if (cboSounds.getSelectedIndex() == 0) {
			return;
		}

		switch (cboSounds.getSelectedIndex()) {
		case 1:
			new PlayWave(getClass().getResource("/resources/suppressed.wav")).start();
			break;
		case 2:
			new PlayWave(getClass().getResource("/resources/44 Mag "+((new Random()).nextInt(11)+1)+".wav")).start();
			break;
		case 3:
			new PlayWave(getClass().getResource("/resources/m4a1.wav")).start();
			break;
		case 4:
			new PlayWave(getClass().getResource("/resources/m1.wav")).start();
			break;
		case 5:
			new PlayWave(getClass().getResource("/resources/skorpion.wav")).start();
			break;
		case 6:
			new PlayWave(getClass().getResource("/resources/50bmg.wav")).start();
			break;
		default:
			break;
		}

	}

	public void closeCamera() {
		if (imageFuture != null) {
			imageFuture.cancel(false);
		}

		if (webcam != null && webcam.isOpen()) {
			webcam.close();
		}
		
		if (detector != null) {
			detector.stop();
			detector.removeMotionListener(detectionEvent);
		}
	}
}
