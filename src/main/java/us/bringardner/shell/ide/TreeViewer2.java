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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

/*
 * Copyright (c) 2012-2017 The ANTLR Project. All rights reserved.
 * Use of this file is governed by the BSD 3-clause license that
 * can be found in the LICENSE.txt file in the project root.
 */


import org.abego.treelayout.NodeExtentProvider;
import org.abego.treelayout.TreeForTreeLayout;
import org.abego.treelayout.TreeLayout;
import org.abego.treelayout.util.DefaultConfiguration;
import org.antlr.v4.gui.TreeLayoutAdaptor;
import org.antlr.v4.gui.TreeTextProvider;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.VocabularyImpl;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.Trees;

public class TreeViewer2 extends JComponent {
	private static final long serialVersionUID = 1L;
	
	public enum ShowType {Both,Text,Token};
	
	public static final Color LIGHT_RED = new Color(244, 213, 211);

	public class DefaultTreeTextProvider implements org.antlr.v4.gui.TreeTextProvider {
		private final List<String> ruleNames;
		

		public DefaultTreeTextProvider(List<String> ruleNames) {
			this.ruleNames = ruleNames;
		}

		@Override
		public String getText(Tree node) {
			String ret = Trees.getNodeText(node, ruleNames);
			if ( node instanceof TerminalNode) {
				Token symbol = ((TerminalNode)node).getSymbol();
				if (symbol != null) {
					int type = symbol.getType();
					if( type >=0 ) {
						String tmp = vocabulary.getSymbolicName(type);
						switch (showType) {
						case Both: ret = tmp+":"+ret; break;
						case Text: break;
						case Token: ret = tmp; break;
						default:
							throw new IllegalArgumentException("Unexpected value: " + showType);
						}						
					}					
				}
			}
			return String.valueOf(ret);
		}
	}

	public static class VariableExtentProvide implements NodeExtentProvider<Tree> {
		TreeViewer2 viewer;
		public VariableExtentProvide(TreeViewer2 viewer) {
			this.viewer = viewer;
		}
		@Override
		public double getWidth(Tree tree) {
			FontMetrics fontMetrics = viewer.getFontMetrics(viewer.font);
			String s = viewer.getText(tree);
			int w = fontMetrics.stringWidth(s) + viewer.nodeWidthPadding*2;
			return w;
		}

		@Override
		public double getHeight(Tree tree) {
			FontMetrics fontMetrics = viewer.getFontMetrics(viewer.font);
			int h = fontMetrics.getHeight() + viewer.nodeHeightPadding*2;
			String s = viewer.getText(tree);
			String[] lines = s.split("\n");
			return h * lines.length;
		}
	}

	protected TreeTextProvider treeTextProvider;
	protected TreeLayout<Tree> treeLayout;
	protected java.util.List<Tree> highlightedNodes;

	protected String fontName = "Helvetica"; //Font.SANS_SERIF;
	protected int fontStyle = Font.PLAIN;
	protected int fontSize = 11;
	protected Font font = new Font(fontName, fontStyle, fontSize);

	protected double gapBetweenLevels = 17;
	protected double gapBetweenNodes = 20;
	protected int nodeWidthPadding = 12;// bump to 8 for line & col (12,12) 2;  // added to left/right
	protected int nodeHeightPadding = 0; // added above/below
	protected int arcSize = 0;           // make an arc in node outline?

	protected double scale = 1.0;

	protected Color boxColor = null;     // set to a color to make it draw background

	protected Color highlightedBoxColor = Color.lightGray;
	protected Color borderColor = null;
	protected Color textColor = Color.blue;
	protected ShowType showType = ShowType.Text;
	private List<String> ruleNames = new ArrayList<>();
	public Vocabulary vocabulary = VocabularyImpl.EMPTY_VOCABULARY;

	
	public TreeViewer2() {
		setFont(font);
	}
	
	public TreeViewer2(List<String> ruleNames,Vocabulary vocabulary, Tree tree) {
		this.ruleNames= ruleNames;
		this.vocabulary = vocabulary;
		setRuleNames(ruleNames);
		if ( tree!=null ) {
			setTree(tree);
		}
		setFont(font);
	}
	
	

	
	
	public java.util.List<Tree> getHighlightedNodes() {
		return highlightedNodes;
	}



	public void setHighlightedNodes(java.util.List<Tree> highlightedNodes) {
		this.highlightedNodes = highlightedNodes;
	}



	public int getFontStyle() {
		return fontStyle;
	}



	public void setFontStyle(int fontStyle) {
		this.fontStyle = fontStyle;
	}



	public double getGapBetweenLevels() {
		return gapBetweenLevels;
	}



	public void setGapBetweenLevels(double gapBetweenLevels) {
		this.gapBetweenLevels = gapBetweenLevels;
	}



	public double getGapBetweenNodes() {
		return gapBetweenNodes;
	}



	public void setGapBetweenNodes(double gapBetweenNodes) {
		this.gapBetweenNodes = gapBetweenNodes;
	}



	public int getNodeWidthPadding() {
		return nodeWidthPadding;
	}



	public void setNodeWidthPadding(int nodeWidthPadding) {
		this.nodeWidthPadding = nodeWidthPadding;
	}



	public int getNodeHeightPadding() {
		return nodeHeightPadding;
	}



	public void setNodeHeightPadding(int nodeHeightPadding) {
		this.nodeHeightPadding = nodeHeightPadding;
	}



	public ShowType getShowType() {
		return showType;
	}



	public void setShowType(ShowType showType) {
		this.showType = showType;
	}



	public String getFontName() {
		return fontName;
	}



	public int getFontSize() {
		return fontSize;
	}



	public List<String> getRuleNames() {
		return ruleNames;
	}



	private void updatePreferredSize() {
		setPreferredSize(getScaledTreeSize());
		invalidate();
		if (getParent() != null) {
			getParent().validate();
		}
		repaint();
	}

	// ---------------- PAINT -----------------------------------------------

	private boolean useCurvedEdges = false;

	public boolean getUseCurvedEdges() {
		return useCurvedEdges;
	}

	public void setUseCurvedEdges(boolean useCurvedEdges) {
		this.useCurvedEdges = useCurvedEdges;
	}

	protected void paintEdges(Graphics g, Tree parent) {
		if (!getTreeLayout().isLeaf(parent)) {
            BasicStroke stroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND);
            ((Graphics2D)g).setStroke(stroke);

			Rectangle2D.Double parentBounds = getBoundsOfNode(parent);
			double x1 = parentBounds.getCenterX();
			double y1 = parentBounds.getMaxY();
			for (Tree child : getTreeLayout().getChildren(parent)) {
				Rectangle2D.Double childBounds = getBoundsOfNode(child);
				double x2 = childBounds.getCenterX();
				double y2 = childBounds.getMinY();
				if (getUseCurvedEdges()) {
					CubicCurve2D c = new CubicCurve2D.Double();
					double ctrlx1 = x1;
					double ctrly1 = (y1+y2)/2;
					double ctrlx2 = x2;
					double ctrly2 = y1;
					c.setCurve(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
					((Graphics2D) g).draw(c);
				}
				else {
					g.drawLine((int) x1, (int) y1,
							   (int) x2, (int) y2);
				}
				paintEdges(g, child);
			}
		}
	}

	protected void paintBox(Graphics g, Tree tree) {
		Rectangle2D.Double box = getBoundsOfNode(tree);
		// draw the box in the background
		int line = -1;
		int col = -1;
		boolean ruleFailedAndMatchedNothing = false;
		if ( tree instanceof ParserRuleContext ) {
			ParserRuleContext ctx = (ParserRuleContext) tree;
			if( ctx.start!=null ) {
				line = ctx.start.getLine();
				col  = ctx.start.getCharPositionInLine();
			} else if( ctx.stop != null ) {
				line = ctx.stop.getLine();
				col  = ctx.stop.getCharPositionInLine();
			}
			
			ruleFailedAndMatchedNothing = ctx.exception != null && ctx.stop != null && ctx.stop.getTokenIndex() < ctx.start.getTokenIndex();
		}
		if ( isHighlighted(tree) || boxColor!=null || tree instanceof ErrorNode || ruleFailedAndMatchedNothing)
		{
			if ( isHighlighted(tree) ) g.setColor(highlightedBoxColor);
			else if ( tree instanceof ErrorNode || ruleFailedAndMatchedNothing ) g.setColor(LIGHT_RED);
			else g.setColor(boxColor);
			g.fillRoundRect((int) box.x, (int) box.y, (int) box.width - 1,
							(int) box.height - 1, arcSize, arcSize);
		}
		if ( borderColor!=null ) {
            g.setColor(borderColor);
            g.drawRoundRect((int) box.x, (int) box.y, (int) box.width - 1,
                    (int) box.height - 1, arcSize, arcSize);
        }

		// draw the text on top of the box (possibly multiple lines)
		g.setColor(textColor);
		String s = getText(tree);
		String[] lines = s.split("\n");
		FontMetrics m = getFontMetrics(font);
		int x = (int) box.x + arcSize / 2 + nodeWidthPadding;
		if( line>0) {
			x+=4;
		}
		int y = (int) box.y + m.getAscent() + m.getLeading() + 1 + nodeHeightPadding;
		for (int i = 0; i < lines.length; i++) {
			String word = lines[i];
			if( line >=0) {
				word = word+" ("+line+","+col+")";
			}
			text(g, word, x, y);
			y += m.getHeight();
		}
	}

	public void text(Graphics g, String s, int x, int y) {
//		System.out.println("drawing '"+s+"' @ "+x+","+y);
		s = Utils.escapeWhitespace(s, true);
		g.drawString(s, x, y);
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		if ( treeLayout==null ) {
			return;
		}

		Graphics2D g2 = (Graphics2D)g;
		// anti-alias the lines
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      						RenderingHints.VALUE_ANTIALIAS_ON);

		// Anti-alias the text
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                         	RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

//		AffineTransform at = g2.getTransform();
//        g2.scale(
//            (double) this.getWidth() / 400,
//            (double) this.getHeight() / 400);
//
//		g2.setTransform(at);

		paintEdges(g, getTreeLayout().getRoot());

		// paint the boxes
		for (Tree Tree : treeLayout.getNodeBounds().keySet()) {
			paintBox(g, Tree);
		}
	}

	
	@Override
	protected Graphics getComponentGraphics(Graphics g) {
		Graphics2D g2d=(Graphics2D)g;
		g2d.scale(scale, scale);
		return super.getComponentGraphics(g2d);
	}

	private Dimension getScaledTreeSize() {
		Dimension scaledTreeSize =
			treeLayout.getBounds().getBounds().getSize();
		scaledTreeSize = new Dimension(50+(int)(scaledTreeSize.width*scale),
									   (int)(scaledTreeSize.height*scale));
		return scaledTreeSize;
	}


	
	// ---------------------------------------------------

	private Rectangle2D.Double getBoundsOfNode(Tree node) {
		return treeLayout.getNodeBounds().get(node);
	}

	private String getText(Tree tree) {
		String s = treeTextProvider.getText(tree);
		s = Utils.escapeWhitespace(s, true);
		return s;
	}

	public TreeTextProvider getTreeTextProvider() {
		return treeTextProvider;
	}

	public void setTreeTextProvider(TreeTextProvider treeTextProvider) {
		this.treeTextProvider = treeTextProvider;
	}

	public void setFontSize(int sz) {
		fontSize = sz;
		font = new Font(fontName, fontStyle, fontSize);
	}

	public void setFontName(String name) {
		fontName = name;
		font = new Font(fontName, fontStyle, fontSize);
	}

	private boolean isHighlighted(Tree node) {
		return getHighlightedNodeIndex(node) >= 0;
	}

	private int getHighlightedNodeIndex(Tree node) {
		if ( highlightedNodes==null ) return -1;
		for (int i = 0; i < highlightedNodes.size(); i++) {
			Tree t = highlightedNodes.get(i);
			if ( t == node ) return i;
		}
		return -1;
	}

	@Override
	public Font getFont() {
		return font;
	}

	@Override
	public void setFont(Font font) {
		this.font = font;
	}

	public int getArcSize() {
		return arcSize;
	}

	public void setArcSize(int arcSize) {
		this.arcSize = arcSize;
	}

	public Color getBoxColor() {
		return boxColor;
	}

	public void setBoxColor(Color boxColor) {
		this.boxColor = boxColor;
	}

	public Color getHighlightedBoxColor() {
		return highlightedBoxColor;
	}

	public void setHighlightedBoxColor(Color highlightedBoxColor) {
		this.highlightedBoxColor = highlightedBoxColor;
	}

	public Color getBorderColor() {
		return borderColor;
	}

	public void setBorderColor(Color borderColor) {
		this.borderColor = borderColor;
	}

	public Color getTextColor() {
		return textColor;
	}

	public void setTextColor(Color textColor) {
		this.textColor = textColor;
	}

	public TreeForTreeLayout<Tree> getTreeLayout() {
		return treeLayout.getTree();
	}

	public void setTree(Tree root) {
		if ( root!=null ) {
			boolean useIdentity = true; // compare node identity
			this.treeLayout =
				new TreeLayout<Tree>(getTreeLayoutAdaptor(root),
									 new TreeViewer2.VariableExtentProvide(this),
									 new DefaultConfiguration<Tree>(gapBetweenLevels,
																	gapBetweenNodes),
									 useIdentity);
			// Let the UI display this new AST.
			updatePreferredSize();
		}
		else {
			this.treeLayout = null;
			repaint();
		}
	}

	/** Get an adaptor for root that indicates how to walk ANTLR trees.
	 *  Override to change the adapter from the default of {@link TreeLayoutAdaptor}  */
	public TreeForTreeLayout<Tree> getTreeLayoutAdaptor(Tree root) {
		return new TreeLayoutAdaptor(root);
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		if(scale <= 0) {
			scale = 1;
		}
		this.scale = scale;
		updatePreferredSize();
	}

	public void setRuleNames(List<String> ruleNames) {
		setTreeTextProvider(new DefaultTreeTextProvider(ruleNames));
	}

	
	
}