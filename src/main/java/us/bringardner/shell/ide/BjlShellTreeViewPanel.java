package us.bringardner.shell.ide;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Enumeration;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.TreeNode;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Tree;

import us.bringardner.filesource.sh.FileSourceShLexer;
import us.bringardner.filesource.sh.FileSourceShParser;
import us.bringardner.shell.ide.TreeViewer2.ShowType;

public class BjlShellTreeViewPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	public static class TreeTreeNode implements TreeNode {
		TreeTreeNode parent;
		org.antlr.v4.runtime.tree.Tree tree;
		
		public TreeTreeNode(org.antlr.v4.runtime.tree.Tree tree) {
			this.tree = tree;
		}
		
		private TreeTreeNode(org.antlr.v4.runtime.tree.Tree tree,TreeTreeNode parent) {
			this.tree = tree;
			this.parent = parent;
		}
		
		
		@Override
		public TreeNode getChildAt(int childIndex) {
			return new TreeTreeNode( tree.getChild(childIndex),this);
		}
		
		@Override
		public int getChildCount() {
			return tree.getChildCount();
		}
		
		@Override
		public TreeNode getParent() {
			return parent;
		}
		@Override
		public int getIndex(TreeNode node) {
			if (node instanceof TreeTreeNode) {
				TreeTreeNode tn = (TreeTreeNode) node;
				for(int idx=0; idx < tree.getChildCount(); idx++ ) {
					Tree kid = tree.getChild(idx);
					if( kid.equals(tn.tree)) {
						return idx;
					}
				}
			}
			return -1;
		}
		
		@Override
		public boolean getAllowsChildren() {
			return tree.getChildCount()>0;
		}
		
		@Override
		public boolean isLeaf() {
			return tree.getChildCount()==0;
		}
		
		@Override
		public Enumeration<? extends TreeNode> children() {
			
			return new Enumeration<TreeNode>() {
				int pos=0;
				
				@Override
				public boolean hasMoreElements() {
					return pos< tree.getChildCount();
				}

				@Override
				public TreeNode nextElement() {
					return new TreeTreeNode(tree.getChild(pos++),TreeTreeNode.this);
				}
			};
		}
		
		public String toString() {
			if (tree instanceof ParserRuleContext) {
				ParserRuleContext ctx = (ParserRuleContext) tree;
				return ctx.getText();
			}
			return "pl="+tree.getPayload().toString();
		}
		
		
	}
	
	private JTextArea errorTextArea;
	private TreeViewer2 treeView;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	private JRadioButton showTextRadioButton;
	private JRadioButton showTokenRadioButton;
	private JRadioButton showBothRadioButton;
	

	static {

	}

	public Parser getParser(String code)  {
		errorTextArea.setText("");
		Parser parser = null;
		try {
			Constructor<? extends Lexer> con = FileSourceShLexer.class.getConstructor(CharStream.class);
			Lexer lexer = con.newInstance(CharStreams.fromString(code));
			org.antlr.v4.runtime.TokenStream tokenStream = new CommonTokenStream((TokenSource) lexer);
			Constructor<? extends Parser> par = FileSourceShParser.class.getConstructor(org.antlr.v4.runtime.TokenStream.class);
			parser = par.newInstance(tokenStream);
			lexer.addErrorListener(new BaseErrorListener() {
				@Override
				public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,int charPositionInLine, String msg, RecognitionException e) {
					appendToError(e.toString());
				}
				@Override
				public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex,int prediction, ATNConfigSet configs) {
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
		} catch (Exception e) {
			appendToError(e.getMessage());
		}
		return parser;

	}

	protected void appendToError(String msg) {
		System.out.println("append "+msg);
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
		String tmp = errorTextArea.getText()+"\n"+buf;

		errorTextArea.setText(tmp.trim());


	}

	/**
	 * Create the panel.
	 */
	public BjlShellTreeViewPanel()  {

		setLayout(new BorderLayout(0, 0));

		JScrollPane northScrollPane = new JScrollPane();
		add(northScrollPane, BorderLayout.NORTH);

		JPanel northPanel = new JPanel();
		northScrollPane.setViewportView(northPanel);
		
		showTextRadioButton = new JRadioButton("Show Text");
		showTextRadioButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionShowTextOrTokenChanged();
			}
		});
		showTextRadioButton.setSelected(true);
		buttonGroup.add(showTextRadioButton);
		northPanel.add(showTextRadioButton);
		
		showTokenRadioButton = new JRadioButton("Show Token");
		showTokenRadioButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionShowTextOrTokenChanged();
			}
		});
		buttonGroup.add(showTokenRadioButton);
		northPanel.add(showTokenRadioButton);
		
		showBothRadioButton = new JRadioButton("Show Both");
		showBothRadioButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionShowTextOrTokenChanged();
			}
		});
		buttonGroup.add(showBothRadioButton);
		northPanel.add(showBothRadioButton);

		JScrollPane southScrollPane = new JScrollPane();
		add(southScrollPane, BorderLayout.SOUTH);

		JPanel southPanel = new JPanel();
		southScrollPane.setViewportView(southPanel);

		JPanel centerPanel = new JPanel();
		add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BorderLayout(0, 0));

		JSlider slider = new JSlider();
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				double val = slider.getValue()/100.0;
				treeView.setScale(1+val);
			}
		});
		centerPanel.add(slider, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane();
		centerPanel.add(scrollPane, BorderLayout.CENTER);
		JScrollPane scrollPane_1 = new JScrollPane();
		centerPanel.add(scrollPane_1, BorderLayout.SOUTH);

		errorTextArea = new JTextArea();
		scrollPane_1.setViewportView(errorTextArea);
		
		Parser p = getParser("");
		String [] names = p.getRuleNames();
		
		treeView = new TreeViewer2(Arrays.asList(names),p.getVocabulary(),null);
		scrollPane.setViewportView(treeView);
		
		JScrollPane scrollPane_2 = new JScrollPane();
		centerPanel.add(scrollPane_2, BorderLayout.WEST);
		
		


	}
	
	protected void actionShowTextOrTokenChanged() {
		if( showTextRadioButton.isSelected()) {
			treeView.setShowType(ShowType.Text);
		} else if( showTokenRadioButton.isSelected()) {
			treeView.setShowType(ShowType.Token);
		} else {
			treeView.setShowType(ShowType.Both);
		}
		setTree((ParseTree) treeView.getTreeLayout().getRoot(),null);
		
	}

	

	public void setTree(ParseTree tree,String errors) {
		treeView.setTree(tree);
		if( errors !=null) {
			errorTextArea.setText(errors);
		}
		
	}

	
}
