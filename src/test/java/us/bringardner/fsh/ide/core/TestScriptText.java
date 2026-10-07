package us.bringardner.fsh.ide.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

public class TestScriptText {

	@Test
	public void cleanKeepsNonAsciiText() {
		String text = "echo \"café → 日本 ✓\"\n\tls\r\n";
		assertEquals(text, ScriptText.clean(text));
	}

	@Test
	public void cleanDropsInvisibleCharacters() {
		assertEquals("echo hi\n", ScriptText.clean("﻿echo​ hi\u0007\n"));
		assertEquals("echo hi", ScriptText.clean("echo hi"));
		assertNull(ScriptText.clean(null));
	}

	@Test
	public void noArgumentsForABlankField() {
		assertEquals(List.of(), ScriptText.splitArguments(""));
		assertEquals(List.of(), ScriptText.splitArguments("   "));
		assertEquals(List.of(), ScriptText.splitArguments(null));
	}

	@Test
	public void argumentsSplitOnAnyWhiteSpace() {
		assertEquals(List.of("a", "b", "c"), ScriptText.splitArguments("  a  b\tc "));
	}

	@Test
	public void statementTextIsTheFirstLine() {
		List<CompileError> errors = new ArrayList<>();
		ParseTree tree = ScriptParser.parse("while true\ndo\n  echo x\ndone\n", errors);
		assertEquals(List.of(), errors);
		assertEquals("while true ...", ScriptText.statementText((ParserRuleContext) tree));
	}

	@Test
	public void longStatementTextIsCutShort() {
		String echo = "echo "+"x".repeat(500);
		ParseTree tree = ScriptParser.parse(echo, new ArrayList<>());
		String text = ScriptText.statementText((ParserRuleContext) tree);
		assertTrue(text.length() < 200, text);
		assertTrue(text.startsWith("echo xxx") && text.endsWith(" ..."), text);
	}

}
