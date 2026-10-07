package us.bringardner.fsh.ide;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.PlainDocument;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTree;

import us.bringardner.fsh.parser.ExprParser;
import us.bringardner.fsh.parser.FileSourceShLexer;
import us.bringardner.fsh.parser.FileSourceShParser;
import us.bringardner.fsh.parser.FileSourceShPreProcessorBraceLexer;
import us.bringardner.fsh.parser.FileSourceShPreProcessorBraceParser;
import us.bringardner.fsh.parser.FileSourceShPreProcessorLexer;
import us.bringardner.fsh.parser.FileSourceShPreProcessorParser;
import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceChooserDialog;
import us.bringardner.parley.files.RecentFileMenu;
import us.bringardner.fsh.Console;
import us.bringardner.fsh.ShellContext;
import us.bringardner.fsh.ide.TreeViewer.ShowType;

public class TreeViewFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;

	private JEditorPane originalCodeEditorPane;
	private JPanel treePanel;
	private String originalCode = "";
	private String code = "";
	private JCheckBox myTreeCheckBox;
	private JCheckBox showPositionCheckBox;
	private JTextPane treeTextPane;
	private FileSource file;
	private JTextPane colTextPane;
	private JTextPane rowTextPane;
	private JTextPane errorTextPane;
	private RecentFileMenu recent;
	private JComboBox<String> rulesComboBox;
	private JRadioButton showBothRadioButton;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	private JRadioButton showTextRadioButton;
	private JRadioButton showTokenRadioButton;
	private JButton openDialogButton;
	private JSlider scaleSlider;
	protected TreeViewer viewer;
	private JSplitPane codeSplitPane;
	Class<? extends Lexer>  lexerClass = FileSourceShLexer.class;
	Class<? extends Parser>  parserClass=FileSourceShParser.class;
	private JScrollPane processedCodeScrollPane;
	private JEditorPane processedCodeEditorPane;
	private JPanel ppRowPanel;
	private JTextPane processedCodeRowTextPane;
	private JCheckBox useOriginalCheckBox;
	private JMenuBar menuBar;
	private JComboBox<String> parserClassComboBox;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					TreeViewFrame frame = new TreeViewFrame();
					frame.enableRecentMenu();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	protected void enableRecentMenu() {
		//menuBar.add(recent);

	}

	/**
	 * Create the frame.
	 * @throws IOException 
	 */
	public TreeViewFrame() throws IOException {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1581, 824);

		menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File",true);
		JMenuItem item = new JMenuItem("Open");
		fileMenu.add(item);
		item.addActionListener((e)->{
			actionOpen();
		});

		recent = new RecentFileMenu(getClass());
		recent.addActionListener((e)->{
			Object source =  e.getSource();
			if (source instanceof FileSource) {
				file = (FileSource) source;
				actionReload();	
			} else {
				JOptionPane.showMessageDialog(this, "Did not get a FileSOurce as source of event.", source.getClass().getName(), JOptionPane.ERROR_MESSAGE);
			}

		});
		fileMenu.add(recent);

		item = new JMenuItem("Save");
		fileMenu.add(item);
		item.addActionListener((e)->{
			actionSave();
		});


		item = new JMenuItem("Save As");
		fileMenu.add(item);
		item.addActionListener((e)->{
			actionSaveAs();
		});

		item = new JMenuItem("New");
		fileMenu.add(item);
		item.addActionListener((e)->{
			actionNew();
		});


		item = new JMenuItem("Revert");
		fileMenu.add(item);
		item.addActionListener((e)->{
			actionReload();
		});


		item = new JMenuItem("Exit");
		fileMenu.add(item);
		item.addActionListener((e)->{
			actionExit();
		});

		
		menuBar.add(fileMenu);


		setJMenuBar(menuBar);


		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.1);
		contentPane.add(splitPane, BorderLayout.CENTER);

		JSplitPane splitPane_1 = new JSplitPane();
		splitPane_1.setResizeWeight(0.5);
		splitPane_1.setOrientation(JSplitPane.VERTICAL_SPLIT);
		splitPane.setRightComponent(splitPane_1);

		JScrollPane scrollPane_1 = new JScrollPane();
		splitPane_1.setLeftComponent(scrollPane_1);

		treePanel = new JPanel();
		scrollPane_1.setViewportView(treePanel);
		treePanel.setLayout(new BorderLayout(0, 0));

		errorTextPane = new JTextPane();
		errorTextPane.setEnabled(false);
		treePanel.add(errorTextPane, BorderLayout.SOUTH);

		scaleSlider = new JSlider(JSlider.HORIZONTAL, -999, 1000, 1);
		treePanel.add(scaleSlider, BorderLayout.NORTH);


		scaleSlider.addChangeListener(
				new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent e) {
						if( viewer !=null ) {
							int v = scaleSlider.getValue();
							viewer.setScale(v / 1000.0 + 1.0);
						}
					}
				}
				);
		JScrollPane scrollPane_2 = new JScrollPane();
		splitPane_1.setRightComponent(scrollPane_2);

		treeTextPane = new JTextPane();
		scrollPane_2.setViewportView(treeTextPane);

		JScrollPane originalCodeScrollPane = new JScrollPane();


		originalCodeEditorPane = new JEditorPane();
		originalCodeEditorPane.getDocument().putProperty(PlainDocument.tabSizeAttribute, 2);
		originalCodeEditorPane.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				changed();

			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				changed();

			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				changed();

			}
			private void changed() {
				String text1 = originalCodeEditorPane.getText();
				String []lines = text1.split("\n");
				StringBuilder buf = new StringBuilder();
				int idx = 0;
				for (; idx < lines.length; idx++) {
					buf.append(""+(idx+1)+"\n");
				}
				buf.append(""+(idx+1)+"\n");
				rowTextPane.setText(buf.toString());
				Console console = new Console();
				ShellContext ctx = new ShellContext(console);
				ctx.enterCommand();
				String ppc = console.preProcess(text1,ctx);
				ctx.exitCommand();
				processedCodeEditorPane.setText(ppc);				
			}

		});

		originalCodeEditorPane.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				String newcode = originalCodeEditorPane.getText();
				if(!code.equals(newcode)) {
					actionCodeChanged();					
				}
			}
		});
		originalCodeScrollPane.setViewportView(originalCodeEditorPane);

		JPanel rowHeaderPanel = new JPanel();
		FlowLayout flowLayout_2 = (FlowLayout) rowHeaderPanel.getLayout();
		flowLayout_2.setVgap(0);
		originalCodeScrollPane.setRowHeaderView(rowHeaderPanel);

		rowTextPane = new JTextPane();
		rowTextPane.setText("1\n2\n3\n4\n");
		rowHeaderPanel.add(rowTextPane);

		JPanel colHeaderPanel = new JPanel();
		FlowLayout flowLayout_1 = (FlowLayout) colHeaderPanel.getLayout();
		flowLayout_1.setAlignment(FlowLayout.LEFT);
		originalCodeScrollPane.setColumnHeaderView(colHeaderPanel);

		colTextPane = new JTextPane();
		colHeaderPanel.add(colTextPane);

		JPanel controlPanel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) controlPanel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		contentPane.add(controlPanel, BorderLayout.NORTH);

		myTreeCheckBox = new JCheckBox("MyTree");
		myTreeCheckBox.setSelected(true);
		myTreeCheckBox.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				actionCodeChanged();
			}
		});
		controlPanel.add(myTreeCheckBox);
		String [] tmp1 = Arrays.copyOf(FileSourceShParser.ruleNames, FileSourceShParser.ruleNames.length);
		Arrays.sort(tmp1);

		rulesComboBox = new JComboBox<String>(tmp1);
		rulesComboBox.setSelectedItem("script");
		rulesComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionCodeChanged();
			}
		});

		parserClassComboBox = new JComboBox<>();
		parserClassComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionParserClassChanged();
			}
		});
		parserClassComboBox.setModel(new DefaultComboBoxModel<String>(new String[] {"FSH", "String PP","Brace PP","Expr"}));
		parserClassComboBox.setSelectedIndex(0);
		controlPanel.add(parserClassComboBox);
		controlPanel.add(rulesComboBox);
		
		showPositionCheckBox = new JCheckBox("Show Position");
		showPositionCheckBox.setSelected(true);
		showPositionCheckBox.addActionListener(e->actionCodeChanged());
		
		controlPanel.add(showPositionCheckBox);
		
		showBothRadioButton = new JRadioButton("Both");
		showBothRadioButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionCodeChanged();
			}
		});
		buttonGroup.add(showBothRadioButton);

		controlPanel.add(showBothRadioButton);

		showTextRadioButton = new JRadioButton("Show Text");
		showTextRadioButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionCodeChanged();
			}
		});
		showTextRadioButton.setSelected(true);
		buttonGroup.add(showTextRadioButton);
		controlPanel.add(showTextRadioButton);

		showTokenRadioButton = new JRadioButton("Show Token");
		showTokenRadioButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionCodeChanged();
			}
		});
		buttonGroup.add(showTokenRadioButton);
		controlPanel.add(showTokenRadioButton);

		openDialogButton = new JButton("Open Dialog");
		openDialogButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionOpenDialog();
			}
		});
		controlPanel.add(openDialogButton);

		useOriginalCheckBox = new JCheckBox("Use Original");
		useOriginalCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionCodeChanged();
			}
		});
		useOriginalCheckBox.setSelected(false);
		controlPanel.add(useOriginalCheckBox);

		codeSplitPane = new JSplitPane();
		codeSplitPane.setResizeWeight(0.5);
		codeSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		//contentPane.add(codeSplitPane, BorderLayout.WEST);
		codeSplitPane.setLeftComponent(originalCodeScrollPane);
		splitPane.setLeftComponent(codeSplitPane);

		processedCodeScrollPane = new JScrollPane();
		codeSplitPane.setRightComponent(processedCodeScrollPane);

		processedCodeEditorPane = new JEditorPane();
		processedCodeEditorPane.getDocument().putProperty(PlainDocument.tabSizeAttribute, 2);
		processedCodeScrollPane.setViewportView(processedCodeEditorPane);
		originalCodeEditorPane.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				changed();

			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				changed();

			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				changed();

			}
			private void changed() {
				String text1 = processedCodeEditorPane.getText();
				String []lines = text1.split("\n");
				StringBuilder buf = new StringBuilder();
				int idx = 0;
				for (; idx < lines.length; idx++) {
					buf.append(""+(idx+1)+"\n");
				}
				buf.append(""+(idx+1)+"\n");
				processedCodeRowTextPane.setText(buf.toString());								
			}

		});

		ppRowPanel = new JPanel();
		processedCodeScrollPane.setRowHeaderView(ppRowPanel);

		processedCodeRowTextPane = new JTextPane();
		processedCodeRowTextPane.setText("1\n2\n3\n");
		ppRowPanel.add(processedCodeRowTextPane);

		new Thread(()->{
			while(true) {
				try {
					Thread.sleep(100) ;
				} catch (InterruptedException e1) {
				}
				String tmp = "";
				if( file!=null) {
					if( !code.equals(originalCode)) {
						tmp = "*";
					}
					tmp += file.getAbsolutePath();
				}
				final String newTitle = tmp;

				if( !getTitle().equals(tmp)) {
					SwingUtilities.invokeLater(()->setTitle(newTitle));
				}

			}
		}).start();
	}

	protected void actionParserClassChanged() {
		String start = null;
		if(ppRowPanel!=null) {
			if( parserClassComboBox.getSelectedIndex()==0) {
				lexerClass = FileSourceShLexer.class;
				parserClass= FileSourceShParser.class;
				start = "script";
			} else if( parserClassComboBox.getSelectedIndex()==1){
				lexerClass = FileSourceShPreProcessorLexer.class;
				parserClass= FileSourceShPreProcessorParser.class;
				start = "ppcode";
			}  else if( parserClassComboBox.getSelectedIndex()==2){
				lexerClass = FileSourceShPreProcessorBraceLexer.class;
				parserClass= FileSourceShPreProcessorBraceParser.class;
				start = "expansion_list";
			}else {
				lexerClass = FileSourceShLexer.class;
				parserClass= ExprParser.class;
				start = "equation";
			}
			
			try {
				Parser p = getParser("");
				String [] names = p.getRuleNames();
				String [] tmp1 = Arrays.copyOf(names, names.length);
				Arrays.sort(tmp1);
				rulesComboBox.setModel(new DefaultComboBoxModel<>(names));
				rulesComboBox.setSelectedItem(start);
			} catch (Exception e) {
				showError(e);
			}
		}
	}

	protected void actionOpenDialog() {
		Component tmp = treePanel.getComponent(0);
		if (tmp instanceof TreeViewer) {
			TreeViewer tv = (TreeViewer) tmp;
			TreeViewer tv2 = new TreeViewer(tv.getRuleNames(),tv.vocabulary, tv.getTree().getRoot());
			tv2.open();
		}
	}

	protected void actionCodeChanged() {
		if( useOriginalCheckBox.isSelected()) {
			updateTree(originalCodeEditorPane.getText());
		} else {
			updateTree(processedCodeEditorPane.getText());
		}
	}

	public Parser getParser(String code) throws Exception {
		errorTextPane.setText("");
		Constructor<? extends Lexer> con = lexerClass.getConstructor(CharStream.class);
		Lexer lexer = con.newInstance(CharStreams.fromString(code));
		org.antlr.v4.runtime.TokenStream tokenStream = new CommonTokenStream((TokenSource) lexer);
		Constructor<? extends Parser> par = parserClass.getConstructor(org.antlr.v4.runtime.TokenStream.class);
		Parser parser = par.newInstance(tokenStream);
		lexer.addErrorListener(new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,int charPositionInLine, String msg, RecognitionException e) {
				appendToError(e.toString());
			}
			@Override
			public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex,int prediction, ATNConfigSet configs) {
				System.out.println("reportContextSensitivity ");
				super.reportContextSensitivity(recognizer, dfa, startIndex, stopIndex, prediction, configs);
			}

		});

		parser.addErrorListener(new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,int charPositionInLine, String msg, RecognitionException e) {
				appendToError(msg);
			}
			@Override
			public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
					int prediction, ATNConfigSet configs) {
				super.reportContextSensitivity(recognizer, dfa, startIndex, stopIndex, prediction, configs);
			}

		});

		return parser;

	}

	protected void appendToError(String msg) {
		StringBuilder buf = new StringBuilder();
		String tmp1 = msg;
		while(tmp1.length()>80) {
			buf.append(tmp1.substring(0, 80));
			buf.append("\n");
			tmp1 = tmp1.substring(80);
		}
		if( !tmp1.isEmpty()) {
			buf.append(tmp1.substring(0));
		}
		buf.append("\n");
		String tmp = errorTextPane.getText()+"\n"+buf;

		errorTextPane.setText(tmp.trim());

		
	}

	public void updateTree(String code) {
		try {
			Parser parser = getParser(code);


			
			String[] names = parser.getRuleNames();
			String start = rulesComboBox.getSelectedItem().toString();

			Method f = parserClass.getMethod(start,new Class<?>[0]);
			ParseTree tree = (ParseTree) f.invoke(parser);

			treePanel.removeAll();
			if( myTreeCheckBox.isSelected()) {
				viewer = new TreeViewer(Arrays.asList(names),parser.getVocabulary(), tree);
				viewer.setShowPosition(showPositionCheckBox.isSelected());
				
				if( showBothRadioButton.isSelected()) {
					viewer.setShowType(ShowType.Both);
				} else if( showTextRadioButton.isSelected()) {
					viewer.setShowType(ShowType.Text);
				} else {
					viewer.setShowType(ShowType.Token);				
				}

				treePanel.add(viewer, BorderLayout.CENTER);					
			} else {
				org.antlr.v4.gui.TreeViewer t  = new org.antlr.v4.gui.TreeViewer(Arrays.asList(names), tree);

				treePanel.add(t, BorderLayout.CENTER);
			}
			treePanel.add(errorTextPane, BorderLayout.SOUTH);
			treePanel.add(scaleSlider, BorderLayout.NORTH);
			treePanel.updateUI();

		} catch (Exception e) {
			showError(e);
		}
	}

	
	public void setCode(String code) {
		originalCodeEditorPane.setText(code);
		actionCodeChanged();
	}

	protected void actionExit() {
		System.exit(0);

	}

	protected void actionSaveAs() {
		FileSourceChooserDialog fileChooser = new FileSourceChooserDialog();

		try {
			if( file != null ) {
				fileChooser.setCurrentDirectory(file.getParentFile());
			}

			int i= fileChooser.showSaveDialog(contentPane);
			if( i == JFileChooser.APPROVE_OPTION) {
				FileSource tmp = fileChooser.getSelectedFile();
				if (tmp == null) {
					return;
				}

				String code = originalCodeEditorPane.getText();
				try {
					writeStringToFile(tmp, code);
					file = tmp;			
					setTitle(file.getName());
					recent.addRecent(tmp);
				} catch (IOException e) {
					showError(e);
				}
			}
		} catch (IOException e) {
			showError(e);
		}
	}

	private void writeStringToFile(FileSource tmp, String code2) throws IOException {
		try(OutputStream out = tmp.getOutputStream()){
			out.write(code2.getBytes());
			originalCode = code2;
		}		
	}

	protected void actionSave() {
		try {
			if( file == null || !file.exists()) {
				actionSaveAs();
			} else {
				String code = originalCodeEditorPane.getText();
				writeStringToFile(file, code);

			}
		} catch (Exception e) {
			showError(e);
		}
	}

	private void showError(Throwable e) {
		JOptionPane.showMessageDialog(this, e);

	}

	protected void actionNew() {
		file = null;
		setTitle("");
		setCode("");
	}

	protected void actionReload() {


		try {
			if( file != null && file.exists()) {
				String tmp = readFileToString(file);
				setCode(tmp);
			}
		} catch (IOException e) {
			showError(e);
		}

	}

	private String readFileToString(FileSource file2) throws IOException {
		String ret = null;
		try(InputStream in = file2.getInputStream()) {
			ret = new String(in.readAllBytes());
		}
		return ret;
	}

	protected void actionOpen() {

		try {

			FileSourceChooserDialog fileChooser = new FileSourceChooserDialog();
			if( file != null ) {
				fileChooser.setCurrentDirectory(file.getParentFile());
			}

			if( fileChooser.showOpenDialog(contentPane) == JFileChooser.APPROVE_OPTION) {
				FileSource tmp = fileChooser.getSelectedFile();
				if( tmp !=null ) {
					String vd = readFileToString(tmp);
					file = tmp;
					originalCode = vd;
					setCode(vd);
					setTitle(tmp.getAbsolutePath());
					recent.addRecent(tmp);

				}
			}		
		} catch (IOException e) {
			showError(e);
		}

	}
}
