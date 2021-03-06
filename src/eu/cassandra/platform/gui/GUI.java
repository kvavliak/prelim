package eu.cassandra.platform.gui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import eu.cassandra.entities.installations.Installation;
import eu.cassandra.platform.Simulation;
import eu.cassandra.platform.utilities.Params;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.*;

/**
 * 
 * @author Konstantinos N. Vavliakis (kvavliak [at] iti [dot] gr)
 *
 */
public class GUI implements Runnable {
	
	// Initialize all swing objects
	private JFrame f = new JFrame("Cassanda GUI"); 
	private JPanel buttonPanel = new JPanel();  
	private JScrollPane buttonScrollPane = new JScrollPane(buttonPanel);
	private JScrollPane graphScrollPane;

	private JTextArea logTextArea = new JTextArea();
	private JScrollPane logTextAreaScrollPane = new JScrollPane(logTextArea);

	private JTextField projectFileField = new JTextField("Select project file");
	private JButton startButton = new JButton("Start");
	private JButton exitButton = new JButton("Exit");
	private JComboBox installationCombo = new JComboBox();
	private ListenInstallationComboBox a;

	// Menu
	private JMenuBar menuBar = new JMenuBar(); // Menubar
	private JMenu menuFile = new JMenu("File"); // File Entry on Menu bar
	private JMenuItem menuItemQuit = new JMenuItem("Quit"); // Quit sub item
	private JMenu menuHelp = new JMenu("Help"); // Help Menu entry
	private JMenuItem menuItemAbout = new JMenuItem("About"); // About Entry
	
	// Graph related variables
	private TimeSeriesCollection dataset;
	
	// Simulation related variables
	private Simulation sim = null;

	public GUI(){
		//redirectSystemStreams();
		a =  new ListenInstallationComboBox();
		installationCombo.setPreferredSize(new Dimension(300, 20));

		startButton.addActionListener(new ListenStartButton());
		exitButton.addActionListener(new ListenExitButton());
		projectFileField.addMouseListener(new ListenProjectFileField());

		logTextAreaScrollPane.setPreferredSize(new Dimension(400, 500));

		projectFileField.setPreferredSize(new Dimension(600, 20));
		projectFileField.setText(Params.SIM_PROPS);
		projectFileField.setEditable(false);

		f.setJMenuBar(menuBar);

		menuFile.add(menuItemQuit);  
		menuHelp.add(menuItemAbout); 
		menuBar.add(menuFile);       
		menuBar.add(menuHelp);


		TimeSeries series = new TimeSeries("");
		dataset = new TimeSeriesCollection(series);
		JFreeChart chart = createChart("Consumption", dataset);
		ChartPanel chartPanel = new ChartPanel(chart); 
		graphScrollPane = new JScrollPane(chartPanel);

		buttonPanel.add(projectFileField);
		buttonPanel.add(startButton);
		buttonPanel.add(installationCombo);
		buttonPanel.add(exitButton);

		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().add(graphScrollPane, BorderLayout.CENTER);
		f.getContentPane().add(buttonScrollPane, BorderLayout.SOUTH);
		f.getContentPane().add(logTextAreaScrollPane, BorderLayout.EAST);

		f.addWindowListener(new ListenCloseWdw());
		menuItemQuit.addActionListener(new ListenMenuQuit());
	}

	public class ListenStartButton implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			sim = new Simulation();
			Thread t = new Thread(GUI.this);
			t.start();
		}
	}

	public void run() {
		startButton.setEnabled(false);	
		installationCombo.setEnabled(false);
		installationCombo.removeActionListener(a);
		installationCombo.removeAllItems();
		sim.setup();
		sim.simulate();
		installationCombo.addItem("All installations");
		for(Installation inst : sim.getInstallations()) {
			installationCombo.addItem(inst.getId());
		}
		installationCombo.addActionListener(a);
		installationCombo.setSelectedIndex(0);
		startButton.setEnabled(true);
		installationCombo.setEnabled(true);
	}

	public JFreeChart createChart(String title, final XYDataset dataset) {
		final JFreeChart chart = ChartFactory.createTimeSeriesChart(
				title, 
				"Time", 
				"Consumption (W)",
				dataset, 
				false, 
				true, 
				true
		);

		DateAxis axis = (DateAxis) chart.getXYPlot().getDomainAxis();
		axis.setTickUnit(new DateTickUnit(DateTickUnitType.DAY,1));
		axis.setTickMarkPosition(DateTickMarkPosition.START);
		axis.setDateFormatOverride(new SimpleDateFormat("dd"));

		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		return chart;
	}

	public class ListenInstallationComboBox implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			TimeSeries series = new TimeSeries("");
			Calendar cal = Calendar.getInstance();
			cal.clear();
			cal.set(Calendar.YEAR, 2012);
			cal.set(Calendar.MONTH, Calendar.JANUARY);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			System.out.println(cal);
			Minute minute = new Minute(cal.getTime());
//			minute.peg(cal);
			double value = 0;
			for(int i = 0; i < sim.getEndTick()-1; i++) {
				if(installationCombo.getSelectedIndex() == 0) {
					value = sim.getRegistry().getValue(i);
				} else {
					Installation inst = sim.getInstallation(installationCombo.getSelectedIndex()-1);
					value = inst.getRegistry().getValue(i);
				}
				minute = new Minute(minute.next().getStart());
				series.add(minute, value);
			}
			if(!dataset.getSeries().isEmpty()) dataset.removeAllSeries();
			dataset.addSeries(series);
		}
	}

	public class ListenProjectFileField  implements MouseListener {
		public void mousePressed(MouseEvent e) {
			JFileChooser fc = new JFileChooser();
			fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
			CassandraProjectFileFilter filter = new CassandraProjectFileFilter();
			fc.setFileFilter(filter);
			int returnVal = fc.showOpenDialog(f);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				System.out.println("You chose to open this file: " + fc.getSelectedFile().getName());
			}
		}

		@Override
		public void mouseClicked(MouseEvent arg0) {

		}

		@Override
		public void mouseEntered(MouseEvent arg0) {

		}

		@Override
		public void mouseExited(MouseEvent arg0) {

		}

		@Override
		public void mouseReleased(MouseEvent arg0) {

		}


	}


	public class ListenExitButton implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	}

	public class ListenMenuQuit implements ActionListener{
		public void actionPerformed(ActionEvent e){
			System.exit(0);         
		}
	}

	public class ListenCloseWdw extends WindowAdapter{
		public void windowClosing(WindowEvent e){
			System.exit(0);         
		}
	}

	public void launchFrame(){
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
		f.setPreferredSize(new Dimension(1600,1000));
		f.pack();
	}

	private void updateTextArea(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				logTextArea.append(text);
			}
		});
	}

	/**
	 * Redirects output streams to the GUI.
	 */
	private void redirectSystemStreams() {
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				updateTextArea(String.valueOf((char) b));
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				updateTextArea(new String(b, off, len));
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};
		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(out, true));
	}


}