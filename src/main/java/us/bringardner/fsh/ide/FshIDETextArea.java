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

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.fife.com.swabunga.spell.engine.SpellDictionary;
import org.fife.com.swabunga.spell.engine.SpellDictionaryHashMap;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.spell.SpellingParser;



public class FshIDETextArea extends RSyntaxTextArea {
	private static final long serialVersionUID = 1L;
	
	public FshIDETextArea() {
		super();
		initMe();
	}

	public FshIDETextArea(int i, int j) {
		super(i, j);
		initMe();
	}

	
	// The English dictionary, read once in the background and shared by every text area
	private static CompletableFuture<SpellDictionary> dictionary;

	private static synchronized CompletableFuture<SpellDictionary> dictionary() {
		if( dictionary == null ) {
			dictionary = CompletableFuture.supplyAsync(()->{
				try(InputStream in = FshIDETextArea.class.getResourceAsStream("/english_dic.zip")) {
					if( in == null ) {
						return null;
					}
					return readEnglishDictionary(in);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			});
		}
		return dictionary;
	}

	private static SpellDictionary readEnglishDictionary(InputStream resource) throws IOException {

		List<String> files = new ArrayList<>();

		for(String s : new String[]{"eng_com.dic", "color.dic", "labeled.dic", "center.dic", "ize.dic","yze.dic" }) {
			files.add(s);
		}

		SpellDictionaryHashMap dict=null;

		ZipInputStream zf = new ZipInputStream(resource);

		try {
			ZipEntry e = zf.getNextEntry();
			while(e != null) {
				if( files.contains(e.getName())) {
					BufferedReader r = new BufferedReader(new InputStreamReader(zf));
					if( dict == null ) {
						dict = new SpellDictionaryHashMap(r);
					} else {
						dict.addDictionary(r);
					}
				}
				e = zf.getNextEntry();
			}
		} finally {
			zf.close();
		}

		return dict;

	}


	private void initMe() {
		 setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
	     setCodeFoldingEnabled(true);
	       
		setLineWrap(true);
		setTabSize(4);
	
		// each text area needs its own parser, but they can share the dictionary
		dictionary().thenAccept(dict->{
			if( dict != null ) {
				SwingUtilities.invokeLater(()->addParser(new SpellingParser(dict)));
			}
		});


		ActionMap aMap = getActionMap();
		InputMap inMap = getInputMap();

		String name = RSyntaxTextAreaEditorKit.rstaCollapseAllFoldsAction;
		Action a = new RSyntaxTextAreaEditorKit.CollapseAllFoldsAction();
		inMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK ),name);
		aMap.put(name,  a);

		name = RSyntaxTextAreaEditorKit.rstaExpandAllFoldsAction;
		a = new RSyntaxTextAreaEditorKit.ExpandAllFoldsAction();
		inMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK ),name);
		aMap.put(name,  a);


		name = RSyntaxTextAreaEditorKit.rstaCollapseAllCommentFoldsAction;
		a = new RSyntaxTextAreaEditorKit.CollapseAllCommentFoldsAction();
		inMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK ),name);
		aMap.put(name,  a);

		
	}

	protected boolean highlightMatching(char start,char end, int pos, int inc,Document doc,boolean select)  {
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
						setCaretPosition(pos);
						if( select ) {
							select(startPos, pos);
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
}