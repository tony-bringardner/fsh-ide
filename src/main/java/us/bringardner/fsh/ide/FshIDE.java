/**
 *	Copyright 2024 Tony Bringardner
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. 
 *	You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed 
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for 
 *	the specific language governing permissions and limitations under the License.
 */
package us.bringardner.fsh.ide;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;

import us.bringardner.fsh.parser.FileSourceShLexer;
import us.bringardner.fsh.parser.FileSourceShParser;
import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceChooserDialog;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.fsh.Console;
import us.bringardner.fsh.ConsolePanel;
import us.bringardner.fsh.ConsoleSignal;
import us.bringardner.fsh.DebugContext;
import us.bringardner.fsh.DebugContext.RunState;
import us.bringardner.fsh.FshList;
import us.bringardner.fsh.ShellContext;
import us.bringardner.fsh.ShellContext.LoopControl;
import us.bringardner.fsh.antlr.Argument;
import us.bringardner.fsh.antlr.Compare;
import us.bringardner.fsh.antlr.FileSourceShVisitorImpl;
import us.bringardner.fsh.antlr.statement.LoopStatement.LoopControlException;
import us.bringardner.fsh.job.AbstractJob;
import us.bringardner.fsh.job.ForgroundJob;


public class FshIDE extends JFrame  {

	private enum IdeRunstate {Idel,Running,Debugging}



	private static  List<FshIDE> ideWindows = new ArrayList<>();

	private synchronized static void register(FshIDE p) {
		ideWindows.add(p);
	}

	private synchronized static void close(FshIDE p) {
		boolean approved = false;
		if( p.hasChanged()) {
			if( p.showConfirmDialog() != JOptionPane.OK_OPTION) {
				return;
			} else {
				approved = true;
			}
		}

		if(!approved && ideWindows.size() == 1) {
			Object[] options = { "Exit Application", "No, Don't exit" };

			int ret = JOptionPane.showOptionDialog(p, "Closing this window will cause the application to exit.\nClose anyway?", "Warning",
					JOptionPane.OK_CANCEL_OPTION, 
					JOptionPane.QUESTION_MESSAGE, 
					null, // do not use a custom Icon
					options, // the titles of buttons
					options[0]); // default button title

			if( ret != JOptionPane.OK_OPTION) {
				return;
			}
		}


		ideWindows.remove(p);
		p.dispose();

		if( ideWindows.size() == 0 ) {
			System.exit(0);
		}

	}


	private int showConfirmDialog() {
		Object[] options = { "Discard and continue", "Don't continue" };

		int ret = JOptionPane.showOptionDialog(this, " There are unsaved changes.\nDo you want to discard them?", "Warning",
				JOptionPane.OK_CANCEL_OPTION, 
				JOptionPane.QUESTION_MESSAGE, 
				null,//getOpenScadIcon(), // do not use a custom Icon
				options, // the titles of buttons
				options[0]); // default button title

		return ret;
	}

	/** True if there are no unsaved changes or the user agrees to discard them. */
	private boolean okToDiscard() {
		return !hasChanged() || showConfirmDialog() == JOptionPane.OK_OPTION;
	}

	private boolean hasChanged() {

		String tmp = getCode();
		boolean ret = !original.equals(tmp);

		return ret;
	}


	protected static final Preferences prefs = LegacyPreferences.forPackage(FshIDE.class);

	private List<String> recentFiles = new ArrayList<String>();
	private static final long serialVersionUID = 1L;

	private static final String PREF_RECENT_LIST = "RecentList";

	private static final String KEY_SCREEN_LOCATION = "Location";

	private JPanel contentPane;

	private String original = "";
	private EditorPanel editorPane =new EditorPanel();
	//private JEditorPane editorPane = new JEditorPane();
	private JTextArea logView;

	private FileSource scriptFile;
	private FileSource lastFile;	
	private ExecuteTask currentTask;


	private Rectangle lastSize;

	public static class CompileError {
		public CompileError(int line2, int charPositionInLine, String msg2) {
			line = line2;
			col = charPositionInLine;
			msg = msg2;
		}
		int line;
		int col;
		String code;
		String msg;
	}


	/**
	 * Script line (1-based) + lineAdjust = editor line (0-based). The script run has "#!fsh"
	 * added in front, and when only the selection runs it starts at the selection's line.
	 */
	private volatile int lineAdjust = -2;

	private DebugContext debugContext = new DebugContext() {

		public void suspend() {
			setCurrentState(RunState.StepInto);
		};

		public void terminate() {		
			actionDebug(true);
		};

		@Override
		public void resume() {
			setCurrentState(RunState.Running);
		};

		@Override
		public  synchronized boolean isBreakpoint(Point linePt,ShellContext ctx) {
			boolean ret = false;

			if( runState == IdeRunstate.Debugging) {
				int line = linePt.x+lineAdjust;
				if( line >=0) {
					Breakpoint bp = editorPane.getBreakpoint(line);

					ret = bp !=null && bp.isEnabled(true);
					if( ret && bp.isConditional()) {
						try {
							String code =  bp.getCondition();
							code = ctx.console.preProcess(code, ctx);
							Compare c = FileSourceShVisitorImpl.parseCompare(code);
							ret = c.evaluate(ctx);
						} catch (Exception e) {
							// stops here; reported once per run rather than every time it's reached
							if( bp.reportConditionError()) {
								showError(e, "Condition evaluation failed (line "+(line+1)+")");
							}
						}
					}
				}
			}
			return  ret;
		}

		@Override
		public synchronized void before(ParserRuleContext context,ShellContext ctx) {
			if( getCurrentState() == RunState.Terminate) {
				throw new LoopControlException(LoopControl.Break, 100000);
			}
			if(runState == IdeRunstate.Debugging && context != null ) {
				int line = context.start.getLine()+lineAdjust;
				//System.out.println("Before  line="+line+" state="+debugContext.getCurrentState()+" contex="+context.getClass().getSimpleName());

				if( line >=0) {
					Map<String,Object> vars = ctx.getVariables();
					SwingUtilities.invokeLater(()->{				
						//editorPane.setCeretPosotionFromLine(line-1);
						try {
							editorPane.removeAllLineHighlights();
							editorPane.addLineHighlight(line, Color.green);
						} catch (BadLocationException e) {
							showError(e, "Add hilight");
						}
						debugVariablePanel.setContext(editorPane, ctx, vars);
					});				
				}
			}
		}

		@Override
		public synchronized void after(ParserRuleContext context,ShellContext ctx) {
			if(runState == IdeRunstate.Debugging && context != null ) {
				int line = context.start.getLine()+lineAdjust;
				//System.out.println("After  line="+line+" state="+debugContext.getCurrentState()+" contex="+context.getClass().getSimpleName());
				if( line >=0) {
					String msg = ""+(line+1)+": "+context.getText();
					Map<String,Object> vars = ctx.getVariables();
					SwingUtilities.invokeLater(()->{
						editorPane.removeAllLineHighlights();
						debugVariablePanel.setContext(editorPane, ctx, vars);
						logView.append(msg+"\n");
					});
				}

			}

		}


		public void stepOver() {
			setCurrentState(RunState.StepOver);
		};

		public void stepInto() {
			setCurrentState(RunState.StepInto);
		};
	};

	public static void fixWindowsIcons(final List<? extends java.awt.Image> iconImages) {
		PropertyChangeListener l = new PropertyChangeListener() {

			private Window prevActiveWindow;

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				final Window o = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
				if (o != null && prevActiveWindow != o) {
					prevActiveWindow = o;
					List<java.awt.Image> windowIcons = o.getIconImages();
					if (windowIcons == null || windowIcons.size() == 0) {
						o.setIconImages(iconImages);
					}
				}
			}
		};
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("activeWindow", l); //$NON-NLS-1$
	}

	/**
	 * Launch the application.
	 */
	public static void main(String [] args) {
		EventQueue.invokeLater(()->openWindow());
	}

	/** The saved window bounds, or null if missing, unreadable or not on any screen now. */
	static Rectangle parseBounds(String text, Rectangle[] screens) {
		if( text == null ) {
			return null;
		}
		String parts[] = text.split("[,]");
		if( parts.length != 4 ) {
			return null;
		}
		Rectangle ret;
		try {
			ret = new Rectangle(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
					Integer.parseInt(parts[2].trim()), Integer.parseInt(parts[3].trim()));
		} catch (NumberFormatException e) {
			return null;
		}
		if( ret.width < 100 || ret.height < 100 ) {
			return null;
		}
		// the title bar must be on a screen, or the window can't be moved
		Rectangle title = new Rectangle(ret.x, ret.y, ret.width, 30);
		for(Rectangle s : screens) {
			if( s.intersects(title)) {
				return ret;
			}
		}
		return null;
	}

	private static Rectangle[] screenBounds() {
		GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		Rectangle[] ret = new Rectangle[devices.length];
		for(int idx=0; idx < devices.length; idx++) {
			ret[idx] = devices[idx].getDefaultConfiguration().getBounds();
		}
		return ret;
	}

	private static void openWindow() {

		ArrayList<java.awt.Image> imageList = new ArrayList<java.awt.Image>();

		String sizes[] = {"100x100","64x64","25x25","16x16"};
		for(String s : sizes) {			
			java.awt.Image image = Toolkit.getDefaultToolkit().getImage(FshIDE.class.getResource("/img/Globe"+s+".png"));
			imageList.add(image);
		}


		fixWindowsIcons(imageList);
		if(Taskbar.isTaskbarSupported()){
			Taskbar bar = Taskbar.getTaskbar();
			bar.setIconImage(imageList.get(0));
		}
		FshIDE frame = new FshIDE();
		frame.setIconImages(imageList);

		if( ideWindows.size() > 0 ) {
			FshIDE last = ideWindows.get(ideWindows.size()-1);
			Rectangle b = last.getBounds();
			b.x+=50;
			b.y+=50;
			frame.lastSize = b;
			frame.setBounds(b);			
		} else {
			Rectangle b = parseBounds(prefs.get(KEY_SCREEN_LOCATION, null), screenBounds());
			if( b != null ) {
				frame.setBounds(b);
			}
		}

		register(frame);
		frame.setVisible(true);
	}

	public class ExecutableCode {
		String allCode;
		String selectedCode;
		// editor line (0-based) the selection starts on
		int selectedLine;
	}


	public class ExecuteTask implements Runnable {
		ExecutableCode code = null;
		// redirect files opened for this run, closed when it ends
		private final List<java.io.Closeable> opened = new ArrayList<>();
		private boolean canceled = false;
		private Console console;
		private Thread thread;
		private AbstractJob job;

		public ExecuteTask(ExecutableCode code) {
			this.code = code;
		}

		private boolean runSelection() {
			return useSelectedCode && code.selectedCode!=null;
		}

		private boolean useSelectedCode;

		private String getCodeToRun() {
			String codeToRun;
			if(runSelection()) {
				codeToRun=code.selectedCode;
			} else {
				codeToRun= code.allCode;
			}

			codeToRun = "#!fsh\n"+codeToRun;

			return codeToRun;
		}

		private Console getConsoleToRun() throws IOException {
			console = new Console();
			console.setStdOut(outputTextArea.getStdOut());
			console.setStdErr(outputTextArea.getStdErr());
			// console will use NativeKeyboardReader
			console.setStdIn(outputTextArea.getStdIn());

			if(!stdIn.isEmpty()) {
				FileSource file1 = FileSourceFactory.getDefaultFactory().createFileSource(stdIn);
				InputStream in = file1.getInputStream();
				opened.add(in);
				console.setStdIn(in);
			}
			if(!stdOut.isEmpty()) {
				FileSource file1 = FileSourceFactory.getDefaultFactory().createFileSource(stdOut);
				PrintStream out = new PrintStream(file1.getOutputStream(), true, StandardCharsets.UTF_8);
				opened.add(out);
				console.setStdOut(out);
			}
			if(!stdErr.isEmpty()) {
				FileSource file1 = FileSourceFactory.getDefaultFactory().createFileSource(stdErr);
				PrintStream err = new PrintStream(file1.getOutputStream(), true, StandardCharsets.UTF_8);
				opened.add(err);
				console.setStdErr(err);
			}
			if( debugContext !=null ) {
				debugContext.setCurrentState(RunState.Running);
				console.setDebugContext(debugContext);
			}

			return console;
		}

		// read from the fields on the EDT when the task is made
		private String arguments;
		private String stdIn;
		private String stdOut;
		private String stdErr;

		private void closeRedirects() {
			for(java.io.Closeable c : opened) {
				try {
					c.close();
				} catch (IOException e) {
					showError(e, "Can't close redirect file");
				}
			}
			opened.clear();
		}

		@Override 
		public void run()  {			
			try {
				execute();
			} finally {
				closeRedirects();
			}
		}

		private void execute()  {			

			String codeToRun = getCodeToRun().trim();
			if( !codeToRun.isEmpty()) {
				Console console = null;						
				try {				
					console = getConsoleToRun();
				} catch (Exception e) {

					showError(e, "");
				}

				if( console != null ) {
					FshList args = new FshList();

					args.add(new Argument( scriptFile!=null?scriptFile.getAbsolutePath():"fsh"));
					for (String a : splitArguments(arguments)) {
						args.add( new Argument(a));
					}

					console.setPositionalParameters(true, args);
					ShellContext sc = new ShellContext(console);

					job = new ForgroundJob(sc, getCodeToRun());
					job.start();

					while(!job.hasStarted()) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
						}
					}
					while(job.isRunning()) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
						}
					}
					int exitCode = job.getExitCode();
					SwingUtilities.invokeLater(()->logView.append("exitCode = "+exitCode+"\n"));
				}

				SwingUtilities.invokeLater(()->{
					stopTask();	
				});
			}
		}

		public synchronized boolean isRunning () {
			boolean ret = job !=null && job.isRunning();
			return ret;
		}

		public synchronized boolean isCancelled() {
			return canceled;
		}

		public synchronized  void cancel() {

			canceled = true;
			if( job !=null ) {
				job.handleSignal(ConsoleSignal.Kill);
			}

			thread.interrupt();
			if( debugContext != null ) {
				debugContext.setCurrentState(RunState.Terminate);
				editorPane.removeAllLineHighlights();
			}
		}

	};

	private List<String> readRecentList() {
		List<String> ret = new ArrayList<String>();
		String tmp = prefs.get(PREF_RECENT_LIST, null);
		if( tmp != null ) {
			for(String name : tmp.split("\n")) {
				ret.add(name);
			}
		}

		return ret;
	}

	private void saveRecentList(List<String> list) {
		StringBuffer buf = new StringBuffer();
		for(String name : list) {
			buf.append(name);
			buf.append('\n');
		}
		prefs.put(PREF_RECENT_LIST, buf.toString());
		try {
			prefs.flush();
		} catch (Exception e) {
			showError(e,"Can't save preferences");
		}
	}



	JMenu openRecentMenu = new JMenu("Open Recent");
	private IdeRunstate runState = IdeRunstate.Idel;

	private JButton executeButton;

	private JCheckBoxMenuItem useSelectedCodeCheckItem;

	private JCheckBoxMenuItem autoExecuteCheckItem;

	private JCheckBoxMenuItem profileCheckItem;

	private Component horizontalStrut;

	private DebugControlPanel debugControlPanel;

	private JButton debugButton;

	private DebugVariablePanel debugVariablePanel;

	private JCheckBox showDebugViewCheckBox;

	private JSplitPane debugSplitPane;

	private JPanel debugPanel;

	private ConsolePanel outputTextArea;

	private JSplitPane centerSplitPane;

	private void buildRecentMenu() {

		openRecentMenu.removeAll();
		List<Integer> obsolet = new ArrayList<Integer>();


		for(int idx=0,sz=recentFiles.size(); idx<sz;idx++) {
			String name = recentFiles.get(idx);
			FileSource tmp;
			try {
				tmp = FileSourceFactory.getDefaultFactory().createFileSource(name);
				if( !tmp.exists()) {
					obsolet.add(0, idx);
				} else {
					JMenuItem item = new JMenuItem(name);
					openRecentMenu.add(item);
					item.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							if( !okToDiscard()) {
								return;
							}
							FileSource tmp=null;
							try {
								tmp = FileSourceFactory.getDefaultFactory().createFileSource(name);
								setCode(readText(tmp), autoExecuteCheckItem.isSelected(), tmp);
							} catch (IOException e1) {
								showError(e1, name);
								return;
							}
							addRecent(scriptFile);
						}
					});	
				}
			} catch (IOException e) {
				// leave it in the list; it may be on a file system that's unavailable now
				JMenuItem item = new JMenuItem(name);
				item.setEnabled(false);
				openRecentMenu.add(item);
			}

		}
		for(int idx : obsolet) {
			recentFiles.remove(idx);
		}
		if( obsolet.size()>0) {
			saveRecentList(recentFiles);
		}
		if( scriptFile !=null ) {
			setTitle(scriptFile.getName());
		} else {
			setTitle("");
		}


	}


	/**
	 * Create the frame.
	 */
	public FshIDE() {
		editorPane.setHighlightCurrentLine(false);

		recentFiles = readRecentList();		

		setBounds(100, 100, 1580, 1000);		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BorderLayout(0, 0));

		contentPane.add(controlPanel, BorderLayout.NORTH);
		JPanel menuPanel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) menuPanel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		controlPanel.add(menuPanel, BorderLayout.SOUTH);

		JMenuBar menuBar = new JMenuBar();
		controlPanel.add(menuBar, BorderLayout.NORTH);

		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);

		JMenuItem mntmNewMenuItem = new JMenuItem("Open");
		mntmNewMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionOpen();
			}
		});

		JMenuItem mntmNewMenuItem_7 = new JMenuItem("Preferences");
		mntmNewMenuItem_7.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionPreferences();
			}
		});
		fileMenu.add(mntmNewMenuItem_7);
		fileMenu.add(mntmNewMenuItem);
		fileMenu.add(openRecentMenu);

		JMenuItem newFileMenu = new JMenuItem("New File");
		newFileMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionNew();
			}
		});
		fileMenu.add(newFileMenu);

		JMenuItem newWindowMenu = new JMenuItem("New Window");
		newWindowMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionNewWindow();
			}
		});
		fileMenu.add(newWindowMenu);

		JMenuItem mntmNewMenuItem_1 = new JMenuItem("Save");
		mntmNewMenuItem_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionSave();
			}
		});
		fileMenu.add(mntmNewMenuItem_1);

		buildRecentMenu();

		JMenuItem mntmNewMenuItem_2 = new JMenuItem("Save As");
		mntmNewMenuItem_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionSaveAs();
			}
		});
		fileMenu.add(mntmNewMenuItem_2);


		JMenuItem mntmNewMenuItem_3 = new JMenuItem("Reload");
		mntmNewMenuItem_3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionReload();
			}
		});
		fileMenu.add(mntmNewMenuItem_3);


		JMenuItem item = new JMenuItem("Reload");
		item.addActionListener((e)-> actionReload());
		editorPane.appendMenu(item);


		item = new JMenuItem("Breakpoint");
		//item.addActionListener((e)-> actionBreakpoint());
		editorPane.appendMenu(item);


		JMenu mnNewMenu_1 = new JMenu("Import");
		fileMenu.add(mnNewMenu_1);



		JMenu mnNewMenu_2 = new JMenu("Export");
		fileMenu.add(mnNewMenu_2);



		fileMenu.add(new JSeparator());



		JMenuItem mntmNewMenuItem_6 = new JMenuItem("Exit");
		mntmNewMenuItem_6.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionExit();
			}
		});
		fileMenu.add(mntmNewMenuItem_6);

		JMenu mnNewMenu = new JMenu("Debug");
		menuBar.add(mnNewMenu);


		useSelectedCodeCheckItem = new JCheckBoxMenuItem("Use selected code");
		useSelectedCodeCheckItem.setSelected(true);
		mnNewMenu.add(useSelectedCodeCheckItem);


		autoExecuteCheckItem = new JCheckBoxMenuItem("Execute on load");
		autoExecuteCheckItem.setSelected(false);
		mnNewMenu.add(autoExecuteCheckItem);

		profileCheckItem = new JCheckBoxMenuItem("Profile");
		mnNewMenu.add(profileCheckItem);






		JButton btnHelp = new JButton("");
		menuBar.add(btnHelp);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionHelp();
			}
		});
		btnHelp.setPreferredSize(new Dimension(40, 40));
		//btnHelp.setIcon(new ImageIcon(getClass().getResource("/HelpBlack.png")));
		btnHelp.setToolTipText("Help");

		showDebugViewCheckBox = new JCheckBox("Debug View");
		showDebugViewCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionShowDebugView();
			}
		});
		menuPanel.add(showDebugViewCheckBox);

		JPanel executePanel = new JPanel();
		menuPanel.add(executePanel);
		executePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		horizontalStrut = Box.createHorizontalStrut(40);
		executePanel.add(horizontalStrut);


		debugButton = new JButton("");
		debugButton.setIcon(new ImageIcon(FshIDE.class.getResource("/img/eclipe_debug_view.png")));
		executePanel.add(debugButton);
		debugButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionDebug(true);
			}
		});


		executeButton = new JButton("");
		executeButton.setSelectedIcon(new ImageIcon(FshIDE.class.getResource("/img/eclipe_delete_config@2x.png")));
		executeButton.setIcon(new ImageIcon(FshIDE.class.getResource("/img/eclipe_run_exc.png")));
		executePanel.add(executeButton);

		scriptArgPanel = new JPanel();
		menuPanel.add(scriptArgPanel);

		argumentsTextField = new JTextField();
		scriptArgPanel.add(argumentsTextField);
		argumentsTextField.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Script Arguments", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		argumentsTextField.setColumns(30);

		JPanel panel = new JPanel();
		scriptArgPanel.add(panel);
		panel.setBorder(new TitledBorder(null, "stdin", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));

		JButton btnNewButton = new JButton("");
		btnNewButton.setIcon(new ImageIcon(FshIDE.class.getResource("/img/redirect_in.png")));
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionBrowse(stdInTextField);
			}
		});
		panel.add(btnNewButton);

		stdInTextField = new JTextField();
		panel.add(stdInTextField);
		stdInTextField.setColumns(10);

		JPanel panel_1 = new JPanel();
		scriptArgPanel.add(panel_1);
		panel_1.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "stdout", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));

		JButton btnNewButton_1 = new JButton("");
		btnNewButton_1.setIcon(new ImageIcon(FshIDE.class.getResource("/img/redirect_out.png")));
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionBrowse(stdOutTextField);
			}
		});
		panel_1.add(btnNewButton_1);

		stdOutTextField = new JTextField();
		panel_1.add(stdOutTextField);
		stdOutTextField.setColumns(10);

		JPanel panel_1_1 = new JPanel();
		scriptArgPanel.add(panel_1_1);
		panel_1_1.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "stderr", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));

		JButton btnNewButton_1_1 = new JButton("");
		btnNewButton_1_1.setIcon(new ImageIcon(FshIDE.class.getResource("/img/redirect_out.png")));
		btnNewButton_1_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionBrowse(stdErrTextField);
			}
		});
		panel_1_1.add(btnNewButton_1_1);

		stdErrTextField = new JTextField();
		stdErrTextField.setColumns(10);
		panel_1_1.add(stdErrTextField);

		debugControlPanel = new DebugControlPanel(debugContext);
		menuPanel.add(debugControlPanel);
		debugControlPanel.setVisible(false);
		FlowLayout flowLayout_1 = (FlowLayout) debugControlPanel.getLayout();
		flowLayout_1.setAlignment(FlowLayout.CENTER);
		executeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionExecute(true);
			}
		});



		// macOS: the screen-top menu bar. Elsewhere that action isn't supported
		// (setDefaultMenuBar throws), so the menu goes on the window instead.
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_MENU_BAR)) {
			Desktop.getDesktop().setDefaultMenuBar(menuBar);
		} else {
			setJMenuBar(menuBar);
		}


		debugPanel = new JPanel();
		debugPanel.setLayout(new BorderLayout(0, 0));

		JSplitPane editSplitPane = new JSplitPane();
		editSplitPane.setResizeWeight(0.6);
		editSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

		debugSplitPane = new JSplitPane();
		debugSplitPane.setVisible(false);
		debugSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		debugSplitPane.setOneTouchExpandable(true);
		debugSplitPane.setResizeWeight(0.6);

		centerSplitPane = new JSplitPane();
		centerSplitPane.setResizeWeight(0.7);

		contentPane.add(centerSplitPane, BorderLayout.CENTER);
		JScrollPane stdViewScrollPane = new JScrollPane();
		outputTextArea = new ConsolePanel();		
		stdViewScrollPane.setViewportView(outputTextArea);



		editSplitPane.setLeftComponent(editorPane);
		editSplitPane.setRightComponent(stdViewScrollPane);

		centerSplitPane.setLeftComponent(editSplitPane);
		centerSplitPane.setRightComponent(debugSplitPane);

		original = "";				
		editorPane.setText(original,null);


		debugVariablePanel  = new DebugVariablePanel();
		debugVariablePanel.setContext(editorPane, null);
		editorPane.addBreapointListner(()->{			
			debugVariablePanel.refreshBreakpoints();
		});

		debugPanel.add(debugVariablePanel, BorderLayout.CENTER);
		JScrollPane logViewScrollPane = new JScrollPane();
		logView = new JTextArea();
		logView.setText("");
		logViewScrollPane.setViewportView(logView);

		debugSplitPane.setRightComponent(logViewScrollPane);
		debugSplitPane.setLeftComponent(debugPanel);		
		debugSplitPane.setResizeWeight(.6);
		editorPane.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				String name = evt.getPropertyName();
				if( EditorPanel.ACTION_NEW.equals(name)) {
					actionNew();
				} else if( EditorPanel.ACTION_OPEN.equals(name)) {
					actionOpen();
				} else if( EditorPanel.ACTION_SAVE.equals(name)) {
					actionSave();
				} else if( EditorPanel.ACTION_NEW.equals(name)) {
					actionNew();
				}

			}
		});

		editorPane.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				String name = evt.getPropertyName();
				if( EditorPanel.ACTION_NEW.equals(name)) {
					actionNew();
				} else if( EditorPanel.ACTION_SAVE.equals(name)) {
					actionSave();
				} else if( EditorPanel.ACTION_OPEN.equals(name)) {
					actionOpen();
				}

			}
		});

		addComponentListener(new ComponentAdapter( ) {
			public void componentResized(ComponentEvent ev) {
				updatePrefs(ev);
			}

			private void updatePrefs(ComponentEvent ev) {
				Rectangle b = getBounds();
				if(lastSize == null || !lastSize.equals(b)) {
					lastSize = b;
					String val = String.format("%d,%d,%d,%d", b.x,b.y,b.width,b.height);
					//System.out.println("resize ev="+val);
					prefs.put(KEY_SCREEN_LOCATION, val);
					try {
						prefs.flush();
					} catch (BackingStoreException e) {
						showError(e,"Can't save preferences");
					}
				}
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				updatePrefs(e);
			};
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close(FshIDE.this);
			}
		});

		new Thread(()->{

			String lastCode = null;
			while(true) {
				try {
					// not trimmed: error line numbers must match the editor's
					String code = getExecutableCode().allCode;
					if(!code.equals(lastCode)) {

						lastCode = code;
						if( !code.isBlank()) {
							ShellContext ctx = new ShellContext(new Console());
							code = ctx.console.preProcess(code, ctx);

							List<CompileError> errors = new ArrayList<>();
							FileSourceShLexer lexer = new FileSourceShLexer(CharStreams.fromString(code));
							lexer.removeErrorListeners();
							lexer.addErrorListener(new BaseErrorListener() {
								@Override
								public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, 
										int line,int charPositionInLine, String msg, RecognitionException e) {
									errors.add(new CompileError(line,charPositionInLine,msg));
								}
							});
							FileSourceShParser parser = new FileSourceShParser(new CommonTokenStream(lexer));
							parser.removeErrorListeners();
							parser.addErrorListener(new BaseErrorListener() {
								@Override
								public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, 
										int line,int charPositionInLine, String msg, RecognitionException e) {
									errors.add(new CompileError(line,charPositionInLine,msg));
								}
							});

							ParseTree tree = parser.script();



							List<CompileError> compileErors = errors;
							SwingUtilities.invokeLater(()->{
								StringBuilder buf = new StringBuilder();
								editorPane.clearErrorMarkers();
								editorPane.removeAllLineHighlights();
								for(CompileError e : compileErors) {
									buf.append(""+e.line+","+e.col+" "+e.msg+"\n");
									editorPane.addErrorMarker(e);									
								}
								debugVariablePanel.updateTree(tree,buf.toString());
							});
						}	
					};
					Thread.sleep(100);
				} catch (Throwable e) {
				}
			}
		},"FshIDE Treeview updater").start();

		new Thread(()->{

			while(!isDisplayable()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
				}					
			}

			while(isDisplayable()) {
				boolean changed = hasChanged();
				String title = scriptFile == null ?"":scriptFile.getName();

				if( changed ) {
					final String nm = "* "+title;
					SwingUtilities.invokeLater(()-> setTitle(nm));
				} else {
					final String nm = title;
					SwingUtilities.invokeLater(()-> setTitle(nm));
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
				}

			}
			System.out.println("Exit update title thread");
		},"FshIDE Title manager").start();

		if( recentFiles != null && recentFiles.size()>0) {
			String path = recentFiles.get(0);
			if( path !=null && !path.isEmpty()) {
				String fName = path;

				final String name = fName;
				SwingUtilities.invokeLater(()->{
					try {
						FileSource file = FileSourceFactory.getDefaultFactory().createFileSource(name);
						setCode (readText(file),autoExecuteCheckItem.isSelected(), file);

					} catch (IOException ex) {
						showError(ex, "Can't read last edited file = "+name);
					}
				});
			}
		}


		createAutoComplete();
	}



	protected void actionBrowse(JTextField fld) {
		try {

			String current = fld.getText().trim();
			FileSourceChooserDialog fc = new FileSourceChooserDialog();
			if( !current.isEmpty()) {
				FileSource file = FileSourceFactory.getDefaultFactory().createFileSource(current);
				fc.setSelectedFile(file);
			}
			if(fc.showOpenDialog(this) == FileSourceChooserDialog.APPROVE_OPTION) {
				FileSource file = fc.getSelectedFile();
				fld.setText(file.getAbsolutePath());
			}
		} catch (Exception e) {
			showError(e, "");
		}

	}

	protected void actionShowDebugView() {
		if( runState == IdeRunstate.Idel) {
			boolean b = showDebugViewCheckBox.isSelected();
			if( b ) {
				debugVariablePanel.setContext(editorPane, null);
			}
			debugSplitPane.setVisible(b);		
			centerSplitPane.resetToPreferredSizes();
		}
	}

	protected void actionDebug(boolean b) {

		switch (runState) {
		case Idel:
			debugButton.setVisible(false);
			executeButton.setVisible(false);
			editorPane.setHighlightCurrentLine(false);
			debugControlPanel.setVisible(true);
			debugSplitPane.setVisible(true);
			centerSplitPane.resetToPreferredSizes();
			startTask(IdeRunstate.Debugging);

			break;
		case Debugging:

			stopTask();
			break;

		default:
			throw new IllegalArgumentException("Unexpected runState in actionDebug: " + runState);
		}


	}




	protected void actionNewWindow() {
		openWindow();

	}


	protected void actionPreferences() {


	}

	protected void actionHelp() {
		try {
		} catch (Exception e) {
			showError(e,"Can't open help file");
		}

	}

	protected void actionNew() {
		if( !okToDiscard()) {
			return;
		}
		if( scriptFile != null) {
			lastFile = scriptFile;
		}
		scriptFile = null;

		setCode("",false, null);

	}

	public void showError(Throwable e1,String msg) {
		if( SwingUtilities.isEventDispatchThread()) {
			String err = "";
			if( e1 != null ) {
				err = e1.toString();
			}
			JOptionPane.showMessageDialog(contentPane, err,msg, JOptionPane.ERROR_MESSAGE);
		} else {
			SwingUtilities.invokeLater(()->showError(e1, msg));
		}		

	}

	private void showWarning(String msg) {
		if( SwingUtilities.isEventDispatchThread()) {
			JOptionPane.showMessageDialog(contentPane, "", msg, JOptionPane.WARNING_MESSAGE);
		} else {
			SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(contentPane, "", msg, JOptionPane.WARNING_MESSAGE));
		}
	}

	private ExecutableCode getExecutableCode() {
		ExecutableCode ret = new ExecutableCode();
		ret.allCode = clean(editorPane.getText());

		ret.selectedCode = clean(editorPane.getSelectedText());
		if( ret.selectedCode != null ) {
			ret.selectedLine = editorPane.getSelectionStartLine();
		}
		return ret;
	}

	/**
	 * text without the invisible characters that come with text copied from web pages and
	 * documents and break scripts: control characters other than tab, newline and carriage
	 * return, zero-width spaces and byte order marks. A non-breaking space becomes a space.
	 * Other characters, including non-ASCII ones, are kept. null stays null.
	 */
	static String clean(String text) {
		if( text == null ) {
			return null;
		}
		StringBuilder ret = new StringBuilder(text.length());
		for(int idx=0,sz=text.length(); idx < sz; idx++) {
			char c = text.charAt(idx);
			if( c == '\u00a0') {
				ret.append(' ');
			} else if( c == '\t' || c == '\n' || c == '\r'
					|| (c >= ' ' && c != 0x7f && !(c >= 0x80 && c < 0xa0)
					&& c != '\u200b' && c != '\u200c' && c != '\u200d' && c != '\ufeff')) {
				ret.append(c);
			}
		}
		return ret.toString();
	}

	/** Script arguments separated by white space; none for a blank string. */
	static List<String> splitArguments(String text) {
		List<String> ret = new ArrayList<>();
		if( text != null ) {
			for(String a : text.trim().split("\\s+")) {
				if( !a.isEmpty()) {
					ret.add(a);
				}
			}
		}
		return ret;
	}

	private static final String SCRIPT_ARGS="#BjlIdeScriptArgs=";
	private static final String SCRIPT_IN  ="#BjlIdeScriptIn=";
	private static final String SCRIPT_OUT ="#BjlIdeScriptOut=";
	private static final String SCRIPT_ERR ="#BjlIdeScriptErr=";

	protected String getCode() {		

		StringBuilder buf = new StringBuilder();
		String args = argumentsTextField.getText().trim();

		if(!args.isEmpty()) {
			buf.append(SCRIPT_ARGS+args+"\n");
		}

		args = stdInTextField.getText().trim();		
		if(!args.isEmpty()) {
			buf.append(SCRIPT_IN+args+"\n");
		}
		args = stdOutTextField.getText().trim();		
		if(!args.isEmpty()) {
			buf.append(SCRIPT_OUT+args+"\n");
		}

		args = stdErrTextField.getText().trim();		
		if(!args.isEmpty()) {
			buf.append(SCRIPT_ERR+args+"\n");
		}

		String ret = buf+editorPane.getText();

		return ret;
	}


	int count = 0;

	// -Xss4m
	private long stackSize=((1024*1024));
	private JTextField argumentsTextField;
	private JTextField stdInTextField;
	private JTextField stdOutTextField;
	private JTextField stdErrTextField;
	private JPanel scriptArgPanel;
	protected void actionExecute(boolean logError) {

		switch (runState) {
		case Idel:				
			startTask(IdeRunstate.Running);
			break;
		case Running:
			stopTask();
			break;

		default:
			throw new IllegalArgumentException("Unexpected runState in actionExecute: " + runState);
		}

	}

	private void stopTask () {
		if( SwingUtilities.isEventDispatchThread()) {
			ExecuteTask task = currentTask;
			if(task !=null && task.isRunning() ) {
				task.cancel();


				new Thread(()->{
					int cnt = 0;
					do {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
						}
						if( ++cnt > 100) {
							SwingUtilities.invokeLater(()->{
								outputTextArea.getStdOut().println("Waiting for task to complete\n");
							});

							task.thread.interrupt();
							cnt=0;
						}
					} while(task.isRunning());

					// only if a new run hasn't started meanwhile
					SwingUtilities.invokeLater(()->{
						if( currentTask == task ) {
							currentTask = null;
						}
					});

				}).start();
			}

			scriptArgPanel.setVisible(true);
			executeButton.setIcon(new ImageIcon(FshIDE.class.getResource("/img/eclipe_run_exc.png")));		
			horizontalStrut.setVisible(true);
			debugButton.setVisible(true);
			executeButton.setVisible(true);
			debugControlPanel.setVisible(false);
			runState = IdeRunstate.Idel;
			editorPane.setHighlightCurrentLine(false);
			actionShowDebugView();
		} else {
			SwingUtilities.invokeLater(()->stopTask());
		}
	}

	private void startTask(IdeRunstate state) {

		if( SwingUtilities.isEventDispatchThread()) {
			for(Breakpoint bp: editorPane.getBreakpoints().values()) {
				bp.reset();
			}

			ExecutableCode code = getExecutableCode();
			if( code.allCode == null || code.allCode.isEmpty()) {
				return;
			}

			executeButton.setIcon(new ImageIcon(FshIDE.class.getResource("/img/Stop.png")));

			scriptArgPanel.setVisible(false);
			logView.setText("");
			outputTextArea.clear();
			if( state == IdeRunstate.Debugging) {
				debugControlPanel.setVisible(true);
			}
			currentTask = new ExecuteTask(code);
			currentTask.useSelectedCode = useSelectedCodeCheckItem.isSelected();
			currentTask.arguments = argumentsTextField.getText();
			currentTask.stdIn = stdInTextField.getText().trim();
			currentTask.stdOut = stdOutTextField.getText().trim();
			currentTask.stdErr = stdErrTextField.getText().trim();
			lineAdjust = -2 + (currentTask.runSelection() ? code.selectedLine : 0);

			Thread th = new Thread(null,currentTask,"ExecuteThread",stackSize);
			th.setDaemon(true);
			th.setName("Execute thread "+(++count));
			currentTask.thread = th;
			runState = state;
			th.start();
		} else {
			SwingUtilities.invokeLater(()->startTask(state));
		}
	}



	protected void actionExit() {
		close(this);
	}

	protected void actionReload() {
		FileSource file = scriptFile != null ? scriptFile : lastFile;
		if( file == null || !okToDiscard()) {
			return;
		}
		try {
			if( file.exists()) {
				setCode(readText(file),autoExecuteCheckItem.isSelected(), file);
			}			
		} catch (IOException e) {
			showError(e, "Can't read file");			
		}

	}

	private static String readText(FileSource file) throws IOException {
		try(InputStream in = file.getInputStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/** Shows code from file (null for a new script). */
	protected void setCode(String code,boolean execute, FileSource file) {
		scriptFile = file;
		argumentsTextField.setText("");
		stdInTextField.setText("");
		stdOutTextField.setText("");
		stdErrTextField.setText("");
		String [] lines = code.split("\n");
		StringBuilder buf = new StringBuilder();
		for(String line:lines) {
			if( line.startsWith(SCRIPT_ARGS)) {
				argumentsTextField.setText(line.substring(SCRIPT_ARGS.length()));
			} else if( line.startsWith(SCRIPT_IN)) {
				stdInTextField.setText(line.substring(SCRIPT_IN.length()));
			} else if( line.startsWith(SCRIPT_OUT)) {
				stdOutTextField.setText(line.substring(SCRIPT_OUT.length()));
			} else if( line.startsWith(SCRIPT_ERR)) {
				stdErrTextField.setText(line.substring(SCRIPT_ERR.length()));
			} else  {
				buf.append(line+"\n");
			}
		}

		editorPane.setText(buf.toString(),scriptFile);
		// what saving it now would write, so it doesn't count as changed
		original = getCode();
	}

	protected void actionSave() {

		final String code = getCode();
		if( code.isEmpty()) {
			showWarning("Nothing to save");
			return;
		}

		try {
			if( scriptFile == null) {
				actionSaveAs();
			} else {
				write(scriptFile, code);
				original = code;
				addRecent(scriptFile);
			}
		} catch (IOException e) {
			showError(e, "Can't save");
		}
	}

	private void write(FileSource file, String code) throws IOException {
		try (OutputStream out = file.getOutputStream()){
			out.write(clean(code).getBytes(StandardCharsets.UTF_8));
		}
	}

	protected void actionSaveAs() {
		final String code = getCode();
		if( code.isEmpty()) {
			showWarning("Nothing to save");
			return;
		}
		FileSourceChooserDialog fc = new FileSourceChooserDialog();
		if( lastFile==null) {
			lastFile = scriptFile;
		}
		if( lastFile!=null ) {
			try {
				fc.setCurrentDirectory(lastFile.getParentFile());
			} catch (IOException e) {				
			}
		}

		if(fc.showSaveDialog(null) == FileSourceChooserDialog.APPROVE_OPTION) {
			FileSource previous = scriptFile;
			scriptFile = fc.getSelectedFile();
			actionSave();
			if( hasChanged()) {
				// not saved: keep the file the editor came from
				scriptFile = previous;
			}
		}




	}

	private void addRecent(FileSource file2) {
		String path = file2.getAbsolutePath();

		int idx = recentFiles.indexOf(path);
		if( idx >=0 ) {
			recentFiles.remove(idx);
		}
		recentFiles.add(0, path);

		int mx = Configuration.getInstance().getMaxRecent();

		while(recentFiles.size()>mx) {
			recentFiles.remove(mx);
		}
		saveRecentList(recentFiles);

		SwingUtilities.invokeLater(()->{
			buildRecentMenu();
		});


	}

	protected void actionOpen() {
		if( !okToDiscard()) {
			return;
		}
		FileSourceChooserDialog fc = new FileSourceChooserDialog();
		if( lastFile==null) {
			lastFile = scriptFile;
		}
		if( lastFile!=null ) {
			try {
				fc.setCurrentDirectory(lastFile.getParentFile());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if(fc.showOpenDialog(null) == FileSourceChooserDialog.APPROVE_OPTION) {
			FileSource file = fc.getSelectedFile();
			try {
				setCode(readText(file), false, file);
				addRecent(file);
			} catch (IOException e) {
				showError(e, "Can't read "+file.getAbsolutePath());
			}
		}


	}





	public void createAutoComplete() {
		editorPane.createAutoComplete();

	}



}
