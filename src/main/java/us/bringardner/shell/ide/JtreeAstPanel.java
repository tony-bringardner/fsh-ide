package us.bringardner.shell.ide;

import java.awt.BorderLayout;
import java.util.Enumeration;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;

import org.antlr.v4.runtime.tree.Tree;

public class JtreeAstPanel extends JPanel {

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
		
		
		
	}
	
	
	/**
	 * Create the panel.
	 */
	public JtreeAstPanel() {
		setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);
		
		JTree tree = new JTree();
		scrollPane.setViewportView(tree);

	}

}
