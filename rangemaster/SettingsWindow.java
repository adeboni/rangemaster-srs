package accufiresystems.rangemaster;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

public class SettingsWindow extends JFrame {

	private static final long serialVersionUID = -4846587174033005139L;
	private final JPanel contentPane;
	private final Settings settings;

	public SettingsWindow(final Settings set) {
		settings = set;
		setTitle("Detection Settings");
		setType(Type.UTILITY);
		setBounds(100, 100, 462, 262);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new MigLayout("", "[][73.00][]", "[][][][][][][][][]"));

		final JLabel lblNewLabel_3 = new JLabel("An explanation of these settings can be found in the \"Detection Settings.pdf\" file.");
		contentPane.add(lblNewLabel_3, "cell 0 0 3 1");

		final Component verticalStrut = Box.createVerticalStrut(20);
		contentPane.add(verticalStrut, "cell 0 1");

		final JLabel lblDetInt = new JLabel("Detect Interval");
		contentPane.add(lblDetInt, "cell 0 2");

		final JSpinner spnDetInt = new JSpinner();
		spnDetInt.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent arg0) {
				settings.setDetectInterval((Integer)spnDetInt.getValue());
			}
		});
		spnDetInt.setModel(new SpinnerNumberModel(settings.getDetectInterval(), 0, null, 1));
		contentPane.add(spnDetInt, "cell 1 2,growx");

		final JLabel lblMilliseconds = new JLabel("milliseconds");
		contentPane.add(lblMilliseconds, "cell 2 2");

		final JLabel lblDetIner = new JLabel("Detect Inertia");
		contentPane.add(lblDetIner, "cell 0 3");

		final JSpinner spnDetIner = new JSpinner();
		spnDetIner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				settings.setDetectInertia((Integer)spnDetIner.getValue());
			}
		});
		spnDetIner.setModel(new SpinnerNumberModel(settings.getDetectInertia(), 0, null, 1));
		contentPane.add(spnDetIner, "cell 1 3,growx");

		final JLabel lblMilliseconds_1 = new JLabel("milliseconds");
		contentPane.add(lblMilliseconds_1, "cell 2 3");

		final JLabel lblDetThresh = new JLabel("Detect Threshold");
		contentPane.add(lblDetThresh, "cell 0 4");

		final JSpinner spnDetThresh = new JSpinner();
		spnDetThresh.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				settings.setDetectThreshold((Integer)spnDetThresh.getValue());
			}
		});
		spnDetThresh.setModel(new SpinnerNumberModel(settings.getDetectThreshold(), 0, 255, 1));
		contentPane.add(spnDetThresh, "cell 1 4,growx");

		final JLabel lblAcceptThreshold = new JLabel("Accept Threshold:");
		contentPane.add(lblAcceptThreshold, "cell 0 5");

		final JLabel lblLowerBound = new JLabel("     Lower Bound");
		contentPane.add(lblLowerBound, "cell 0 6");

		final JSpinner spnAccLow = new JSpinner();
		spnAccLow.setModel(new SpinnerNumberModel(new Double(settings.getAcceptThresholdLow()*100.0), new Double(0.0),
				new Double(settings.getAcceptThresholdHigh()*100.0), new Double(0.001)));
		final JSpinner spnAccUpp = new JSpinner();
		spnAccUpp.setModel(new SpinnerNumberModel(new Double(settings.getAcceptThresholdHigh()*100.0),
				new Double(settings.getAcceptThresholdLow()*100.0), new Double(100.0), new Double(0.001)));

		spnAccLow.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				settings.setAcceptThresholdLow((Double)spnAccLow.getValue()/100.0);
				spnAccUpp.setModel(new SpinnerNumberModel((Double)spnAccUpp.getValue(), (Double)spnAccLow.getValue(), new Double(100.0), new Double(0.001)));
			}
		});
		contentPane.add(spnAccLow, "cell 1 6,growx");

		final JLabel label = new JLabel("%");
		contentPane.add(label, "cell 2 6");

		final JLabel lblUpperBound = new JLabel("     Upper Bound");
		contentPane.add(lblUpperBound, "cell 0 7");


		spnAccUpp.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				settings.setAcceptThresholdHigh((Double)spnAccUpp.getValue()/100.0);
				spnAccLow.setModel(new SpinnerNumberModel((Double)spnAccLow.getValue(), new Double(0.0), (Double)spnAccUpp.getValue(), new Double(0.001)));
			}
		});
		contentPane.add(spnAccUpp, "cell 1 7,growx");

		final JLabel label_1 = new JLabel("%");
		contentPane.add(label_1, "cell 2 7");

		final JButton btnSave = new JButton("Save Settings");
		btnSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				settings.writeToFile();
				setVisible(false);
			}
		});

		final JButton btnRevertToDefault = new JButton("Revert to Default");
		btnRevertToDefault.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				settings.setAcceptThresholdHigh(0.008);
				settings.setAcceptThresholdLow(0.00008);
				settings.setDetectInertia(250);
				settings.setDetectInterval(30);
				settings.setDetectThreshold(70);
				spnDetInt.setModel(new SpinnerNumberModel(settings.getDetectInterval(), 0, null, 1));
				spnDetIner.setModel(new SpinnerNumberModel(settings.getDetectInertia(), 0, null, 1));
				spnDetThresh.setModel(new SpinnerNumberModel(settings.getDetectThreshold(), 0, 255, 1));
				spnAccLow.setModel(new SpinnerNumberModel(new Double(settings.getAcceptThresholdLow()*100.0), new Double(0.0),
						new Double(settings.getAcceptThresholdHigh()*100.0), new Double(0.001)));
				spnAccUpp.setModel(new SpinnerNumberModel(new Double(settings.getAcceptThresholdHigh()*100.0),
						new Double(settings.getAcceptThresholdLow()*100.0), new Double(100.0), new Double(0.001)));
			}
		});
		contentPane.add(btnRevertToDefault, "flowx,cell 2 8,alignx right");
		contentPane.add(btnSave, "cell 2 8,alignx right");
	}

}
