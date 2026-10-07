/**
 *	Copyright 2026 Tony Bringardner
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
package us.bringardner.fsh.ide.core;

import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;

import us.bringardner.fsh.Console;
import us.bringardner.fsh.ShellContext;
import us.bringardner.fsh.parser.FileSourceShLexer;
import us.bringardner.fsh.parser.FileSourceShParser;

/** Parses scripts with fsh's grammar, for the syntax check and the parse-tree view. */
public class ScriptParser {

	private ScriptParser() {
	}

	/**
	 * The parse tree of code, as fsh would run it (after its preprocessing). Syntax errors
	 * are added to errors, with line numbers counted from code's first line.
	 */
	public static ParseTree parse(String code, List<CompileError> errors) {
		ShellContext ctx = new ShellContext(new Console());
		code = ctx.console.preProcess(code, ctx);

		BaseErrorListener listener = new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, 
					int line,int charPositionInLine, String msg, RecognitionException e) {
				errors.add(new CompileError(line,charPositionInLine,msg));
			}
		};
		FileSourceShLexer lexer = new FileSourceShLexer(CharStreams.fromString(code));
		lexer.removeErrorListeners();
		lexer.addErrorListener(listener);
		FileSourceShParser parser = new FileSourceShParser(new CommonTokenStream(lexer));
		parser.removeErrorListeners();
		parser.addErrorListener(listener);
		return parser.script();
	}
}
