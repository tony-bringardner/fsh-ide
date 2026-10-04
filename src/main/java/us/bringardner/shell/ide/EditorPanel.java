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
package us.bringardner.shell.ide;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rtextarea.RTextScrollPane;

import us.bringardner.io.filesource.FileSource;
import us.bringardner.shell.ide.BjlShellIDE.CompileError;



public class EditorPanel extends JPanel {

	/* 
	 * Add edit polygon to editor
	 */
	private static final long serialVersionUID = 1L;

	public static final String ACTION_SAVE = "Save";
	public static final String ACTION_OPEN = "Open";
	public static final String ACTION_NEW = "New";
	
	public static interface BreakpointListner {
		void changed();
	}

	private List<BreakpointListner> breakpointListners = new ArrayList<EditorPanel.BreakpointListner>();

	private AutoCompletion autoComplete;
	private int fontHeight;
	private BjlShellIDETextArea editorPane = new BjlShellIDETextArea(200,200) {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void paste() {	
			Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable content = cb.getContents(this);
			try {
				Object df = content.getTransferData(DataFlavor.stringFlavor);
				//System.out.println("df="+df.getClass());
				if (df instanceof String) {
					String str = (String) df;
					byte[] data = str.getBytes();
					StringBuilder buf = new StringBuilder();
					for(byte b : data) {
						if( b >=9 && b <=127) {
							buf.append((char)b);
						}
					}
					if( buf.length() != str.length()) {
						cb.setContents(new StringSelection(buf.toString()),null);
					}
				}

			} catch (UnsupportedFlavorException | IOException e) {
			}
			super.paste();
		}
	};

	private List<String> variables = new ArrayList<>();
	private RTextScrollPane scrollPane;
	
	private FileSource scriptDir;
	private Icon breakpointIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(BjlShellIDE.class.getResource("/img/eclipse_brkp_obj.png")));
	private Icon errorIcon      = new ImageIcon(Toolkit.getDefaultToolkit().getImage(BjlShellIDE.class.getResource("/img/eclipse_err_obj.png")));

	Map<Integer,Breakpoint> breakpoints = new TreeMap<>();
	Map<Integer,BjlShellIDE.CompileError> compileErrs = new TreeMap<>();
	

	private JPanel iconPanel;

	private JTextArea lineNumberTextArea;


	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {




		EditorPanel frame = new EditorPanel();

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public Object addLineHighlight(int line,Color color) throws BadLocationException {
		
		return editorPane.addLineHighlight(line, color);
	}
	public void removeLineHighlight(Object id) {
		editorPane.removeLineHighlight(id);
	}
	public void removeAllLineHighlights() {
		editorPane.removeAllLineHighlights();
	}
	
	public void addBreapointListner(BreakpointListner l) {
		breakpointListners.add(l);
	}

	public boolean removeBreapointListner(BreakpointListner l) {
		return breakpointListners.remove(l);
	}

	public void clearAllBreeakpointLisners() {
		breakpointListners.clear();
	}


	@Override
	public void setBackground(Color bg) {
		super.setBackground(bg);
		if( editorPane != null) {
			editorPane.setBackground(bg);
		}		
	}


	public Map<Integer, Breakpoint> getBreakpoints() {
		
		return breakpoints;
	}

	/**
	 * Create the frame.
	 */
	public EditorPanel() {
		editorPane.setAutoscrolls(true);
		editorPane.setLineWrap(true);
		editorPane.setCodeFoldingEnabled(true);

		setLayout(new BorderLayout(0, 0));

		scrollPane = new RTextScrollPane(editorPane);
		scrollPane.setIconRowHeaderEnabled(false);
		scrollPane.setLineNumbersEnabled(true);
		scrollPane.setFoldIndicatorEnabled(true);
		scrollPane.setWheelScrollingEnabled(true);
		scrollPane.setAutoscrolls(true);
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setBorder(null);
		splitPane.setEnabled(false);
		splitPane.setDividerSize(0);
		scrollPane.setRowHeaderView(splitPane);

		lineNumberTextArea = new JTextArea();
		lineNumberTextArea.setBorder(null);
		lineNumberTextArea.setFocusable(false);
		lineNumberTextArea.setText("1\n2\n3\n4");
		splitPane.setRightComponent(lineNumberTextArea);

		iconPanel = new JPanel() {
			private static final long serialVersionUID = 1L;
			

			
			
			@Override
			public void paint(Graphics g) {				
				g.setColor(Color.white);
				g.fillRect(0, 0, getWidth(), getHeight());				
				Font f = editorPane.getFont();
				FontMetrics fm = g.getFontMetrics(f);
				fontHeight = fm.getHeight();
				for(Breakpoint b : breakpoints.values()) {
					breakpointIcon.paintIcon(iconPanel, g, 0, fontHeight*(b.getLine()));	
				}
				for(CompileError err :compileErrs.values()) {
					errorIcon.paintIcon(iconPanel, g, 0, fontHeight*(err.line-1));
				}
			}	
		};
		iconPanel.setBackground(new Color(238, 238, 238));
		iconPanel.setFocusable(false);
		iconPanel.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mousePressed(MouseEvent e) {
				
				Point p = e.getPoint();
				int line = (p.y/fontHeight);
				if( e.getButton() == MouseEvent.BUTTON3) {
					Breakpoint bp = breakpoints.get(line);
					if( bp != null) {
						BreakpointPropertiesDialog d = new BreakpointPropertiesDialog();
						p = e.getLocationOnScreen();
						d.showDialog(bp, iconPanel, p.x, p.y);
						if( d.isDelete()) {
							breakpoints.remove(bp.getLine());
							updateUI();
						}

					}
				} else
				if( compileErrs.containsKey(line+1) ) {
					CompileError ce = compileErrs.get(line+1);
					JOptionPane.showMessageDialog(iconPanel, ce.msg, "Compile Error: ", JOptionPane.ERROR_MESSAGE);
					
				} else if(breakpoints.remove(line) == null) {
					String code = "";
					try {
						int start = editorPane.getLineStartOffset(line);
						int end = editorPane.getLineEndOffset(line);
						code = editorPane.getText(start, end-start).trim();
					} catch (BadLocationException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					if( !code.isEmpty()) {
						Breakpoint bp = new Breakpoint();
						bp.setCode(code);
						bp.setLine(line);
						breakpoints.put(line,bp);
					}
				}
				iconPanel.updateUI();
				for(BreakpointListner bpl : breakpointListners) {
					bpl.changed();
				}
				
			}
		});
		iconPanel.setPreferredSize(new Dimension(18, 0));
		iconPanel.setBorder(null);
		splitPane.setLeftComponent(iconPanel);
		iconPanel.setLayout(null);
		
		scrollPane.setRowHeaderView(splitPane);
		
		
		add(scrollPane, BorderLayout.CENTER);
		createAutoComplete();
		

		editorPane.addKeyListener(new KeyAdapter() {
			private FindDialog findDialog;

			@Override

			public void keyPressed(KeyEvent e) {
				int c = e.getKeyCode();
				//System.out.println("Edtor.keyPressed c="+((int)c));
				boolean keep = true;
				if( e.isControlDown() || e.isMetaDown()) {
					// P = 16,  F = 6 ,   S = 19
					if( c == KeyEvent.VK_F) {// 6 == F
						if( findDialog == null ) {
							findDialog = new FindDialog();
						}
						if( !findDialog.isVisible()) {
							findDialog.showDialog(editorPane);
						}
						keep = false;
					} else if( c == KeyEvent.VK_G ) {
						GoToDialog dialog = new GoToDialog();
						Point loc3 = editorPane.getLocationOnScreen();
						Point loc = editorPane.getCaret().getMagicCaretPosition();
						if( loc != null ) {
							loc3.x+=loc.x;
							loc3.y+=loc.y;
						}

						dialog.setLocation(loc3);
						int line = dialog.getLine();
						if( line >0) {
							try {
								editorPane.setCaretPosition(editorPane.getLineStartOffset(line-1));
							} catch (BadLocationException e1) {
							}
						}
					} else if( c == KeyEvent.VK_N ) {
						actionNew();
						keep = false;
					} else if( c == KeyEvent.VK_P ) { // P
						keep = false;
						Document doc = editorPane.getDocument();
						int dot = editorPane.getCaretPosition();
						String left = null;
						String right = null;
						int ch = -1;
						try {right = doc.getText(dot, 1);	ch = right.charAt(0);} catch (BadLocationException e1) {			}
						if( ch != '{' && ch != '}') {
							try {left = doc.getText(--dot, 1);	ch = left.charAt(0);} catch (BadLocationException e1) {			}
						}
						switch (ch) {
						case '{':
							highlightMatching((char)ch, '}', dot+1, 1, doc,e.isShiftDown());
							break;
						case '}':
							highlightMatching((char)ch, '{', dot-1, -1, doc,e.isShiftDown());
							break;

						}
					} else if( c == KeyEvent.VK_O ) { // O
						actionOpen();
						keep = false;
					} else if( c == KeyEvent.VK_S ) { // S
						actionSave();
						keep = false;
					} else if( c == KeyEvent.VK_I ) { // I
						actionFormat();
						keep = false;
					} 
				} else if(e.isAltDown()) {
					if( c == KeyEvent.VK_F) {// 102 == F
						findDialog.find();
						keep = false;
					}
				} else if( c == KeyEvent.VK_DELETE) {
					// removes char in front
					int pos = editorPane.getCaretPosition();
					try {
						String ch = editorPane.getDocument().getText(pos, 1);
						//System.out.println("have delete ch="+((int)ch.charAt(0)));
						if( (int)ch.charAt(0)=='\n') {
							int line = editorPane.getLineOfOffset(pos);
							for(Breakpoint bp : breakpoints.values()) {
								int line2 = bp.getLine();
								if( line2 >= line) {
									bp.setLine(line2-1);
								}
							}
							iconPanel.updateUI();
						}
					} catch (BadLocationException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}



				} else if( c == KeyEvent.VK_BACK_SPACE) {
					// removes char behind
					int pos = editorPane.getCaretPosition();
					try {
						String ch = editorPane.getDocument().getText(pos-1, 1);
						if( (int)ch.charAt(0)=='\n') {
							int line = editorPane.getLineOfOffset(pos);
							for(Breakpoint bp : breakpoints.values()) {
								int line2 = bp.getLine();
								if( line2 >= line) {
									bp.setLine(line2-1);
								}
							}
							iconPanel.updateUI();
						}
							
					} catch (BadLocationException e1) {
					}

					//System.out.println("have backspace");
				} else if( c == KeyEvent.VK_ENTER ) {
					// insert nl at pos
					int pos = editorPane.getCaretPosition();
					for(Breakpoint bp : breakpoints.values()) {
						int pos2 =  getLineStartOffset(bp.getLine());
						if( pos2>=pos) {
							bp.setLine(bp.getLine()+1);
						}
					}
					
					for(BreakpointListner l : breakpointListners) {
						l.changed();
					}
					StringBuilder b = new StringBuilder();
					for(int i=0;i<= editorPane.getLineCount(); i++) {
						b.append(""+(i+1));
						b.append('\n');
					}
					lineNumberTextArea.setText(b.toString());
					iconPanel.updateUI();
				}
				if( keep ) {
					super.keyPressed(e);
				} else {
					e.consume();
				}
			}

		
		});

		
		
		lineNumberTextArea.setFont(editorPane.getFont());
		
	}


	
	
	

	private void actionNew() {
		firePropertyChange(ACTION_NEW, true, false);
	}





	private void actionFormat() {
		String text = editorPane.getSelectedText();
		if( text != null && !text.isEmpty()) {
			int start = editorPane.getSelectionStart();
			int end   = editorPane.getSelectionEnd();

			Document doc = editorPane.getDocument();
			//System.out.println("dco class="+doc.getClass().getName());
			try {
				doc.remove(start, end-start);
				doc.insertString(start, CodeFormatter.format(text), null);
			} catch (BadLocationException e) {
				logError(e, "insert formatted code");
			}			
		}
	}




	public void createAutoComplete()  {

		DefaultCompletionProvider provider = new DefaultCompletionProvider(){
			@Override
			protected boolean isValidChar(char ch) {
				return Character.isLetterOrDigit(ch) || ch=='_'  || ch=='.' || ch=='#' || ch=='$' ;
			}
		};

		
		Configuration config = null;
		try {
			config = Configuration.getInstance();	
		} catch (Throwable e) {
			System.out.println("Configuration.getInstance e="+e);
			e.printStackTrace();
			System.exit(0);
		}
		
		
		for(Template t : config.getTemplates()) {
			t.addCompetion(provider);
		}


		for(String var : variables) {
			provider.addCompletion(new ShorthandCompletion(provider, var+"-variable", var));
		}

		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if(autoComplete == null ) {
					autoComplete = new AutoCompletion(provider); 
					autoComplete.setAutoCompleteEnabled(true);
					autoComplete.setParameterAssistanceEnabled(true);
					autoComplete.install(editorPane);
				} else {
					autoComplete.setCompletionProvider(provider);
				}
			}
		});

	}

	private boolean highlightMatching(char start,char end, int pos, int inc,Document doc,boolean select)  {
		boolean ret = false;
		int cnt = 1;
		int len= doc.getLength();
		int startPos = pos;
		try {
			while( pos >=0 && pos < len) {
				char c = doc.getText(pos, 1).charAt(0);
				if( c == start) {
					cnt++;
				} else if( c == end) {
					if( --cnt == 0 ) {
						editorPane.setCaretPosition(pos);
						if( select ) {
							editorPane.select(startPos, pos);
						}
						return true;
					}
				}
				pos+=inc;
			}
		} catch(BadLocationException e) {

		}
		return ret;
	}



	public void logError(Throwable error, String title) {
		//	error.printStackTrace();
		if(SwingUtilities.isEventDispatchThread()) {
			JOptionPane.showMessageDialog(this, error.toString(), title, JOptionPane.ERROR_MESSAGE);
		} else {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					JOptionPane.showMessageDialog(EditorPanel.this, error.toString(), title, JOptionPane.ERROR_MESSAGE);
				}
			});
		}
	}






	private void actionSave() {
		firePropertyChange(ACTION_SAVE, true, false);

	}


	private void actionOpen() {
		firePropertyChange(ACTION_OPEN, true, false);

	}


	public void setText(String string, FileSource scriptFile) {
		editorPane.setText(string);
		editorPane.setCaretPosition(0);
		editorPane.removeAllLineHighlights();
		breakpoints.clear();
		iconPanel.updateUI();		
		scriptDir = scriptFile;
		try {
			if( scriptDir != null && scriptDir.isFile()) {
				scriptDir = scriptDir.getParentFile();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		for(BreakpointListner l : breakpointListners) {
			l.changed();
		}
	}

	public String getText() {
		return editorPane.getText();
	}

	public String getSelectedText() {
		String ret = editorPane.getSelectedText();
		return ret;
	}

	public void appendMenu(JMenuItem item) {
		JPopupMenu m =editorPane.getPopupMenu();
		m.add(item);				
	}

	
	public int getLineStartOffset(int line) {
		try {
			return editorPane.getLineStartOffset(line);
		} catch (BadLocationException e) {
		}
		return -1;
	}


	public void setCeretPosotionFromLine(int line) {
		try {			
			int offset = editorPane.getLineStartOffset(line);
			editorPane.setCaretPosition(offset);
		} catch (BadLocationException e) {
		}		
	}

	public void setHighlightCurrentLine(boolean b) {
		editorPane.setHighlightCurrentLine(b);
	}

	

	public Color getCurrentLineHighlightColor() {
		return editorPane.getCurrentLineHighlightColor();
	}

	public void setCeretPosotionFromOffset(int offset) {
		editorPane.setCaretPosition(offset);
	}

	public void setTabSize(int size) {		
		editorPane.setTabSize(size);		
	}
	
	public int getTabSize() {
		return editorPane.getTabSize();
	}
	
	@Override
	public void setFont(Font font) {		
		super.setFont(font);
		if( editorPane != null ) {
			editorPane.setFont(font);
			iconPanel.setFont(font);
			lineNumberTextArea.setFont(font);
		}
	}
	
	public void setLineWrap(boolean b) {
		editorPane.setLineWrap(b);
	}
	
	public boolean isLineWrap() {
		return editorPane.getLineWrap();
	}
	
	public SyntaxScheme getSyntaxScheme() {
		return editorPane.getSyntaxScheme();
	}
	
	public void setSyntaxScheme(SyntaxScheme scheme) {
		editorPane.setSyntaxScheme(scheme);
	}

	public void addErrorMarker(CompileError error) {
		
		compileErrs.put(error.line, error);
		iconPanel.updateUI();
	}

	public void clearErrorMarkers() {
		compileErrs.clear();
		iconPanel.updateUI();

		
	}
}
