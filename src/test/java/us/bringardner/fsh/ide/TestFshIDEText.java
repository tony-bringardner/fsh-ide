package us.bringardner.fsh.ide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import org.junit.jupiter.api.Test;

public class TestFshIDEText {

	@Test
	public void cleanKeepsNonAsciiText() {
		String text = "echo \"café → 日本 ✓\"\n\tls\r\n";
		assertEquals(text, FshIDE.clean(text));
	}

	@Test
	public void cleanDropsInvisibleCharacters() {
		assertEquals("echo hi\n", FshIDE.clean("﻿echo​ hi\u0007\n"));
		assertEquals("echo hi", FshIDE.clean("echo hi"));
		assertNull(FshIDE.clean(null));
	}

	@Test
	public void noArgumentsForABlankField() {
		assertEquals(List.of(), FshIDE.splitArguments(""));
		assertEquals(List.of(), FshIDE.splitArguments("   "));
		assertEquals(List.of(), FshIDE.splitArguments(null));
	}

	@Test
	public void argumentsSplitOnAnyWhiteSpace() {
		assertEquals(List.of("a", "b", "c"), FshIDE.splitArguments("  a  b\tc "));
	}

	private static final Rectangle[] SCREEN = { new Rectangle(0, 0, 1920, 1080) };

	@Test
	public void savedBounds() {
		assertEquals(new Rectangle(10, 20, 800, 600), FshIDE.parseBounds("10,20,800,600", SCREEN));
	}

	@Test
	public void badSavedBoundsAreIgnored() {
		assertNull(FshIDE.parseBounds(null, SCREEN));
		assertNull(FshIDE.parseBounds("", SCREEN));
		assertNull(FshIDE.parseBounds("10,20,800", SCREEN));
		assertNull(FshIDE.parseBounds("10,20,x,600", SCREEN));
		assertNull(FshIDE.parseBounds("10,20,0,0", SCREEN));
		// on a screen that has since been unplugged
		assertNull(FshIDE.parseBounds("3000,20,800,600", SCREEN));
	}

	@Test
	public void statementTextIsTheFirstLine() {
		List<FshIDE.CompileError> errors = new ArrayList<>();
		ParseTree tree = FshIDE.parse("while true\ndo\n  echo x\ndone\n", errors);
		assertEquals(List.of(), errors);
		assertEquals("while true ...", FshIDE.statementText((ParserRuleContext) tree));
	}

	@Test
	public void longStatementTextIsCutShort() {
		String echo = "echo "+"x".repeat(500);
		ParseTree tree = FshIDE.parse(echo, new ArrayList<>());
		String text = FshIDE.statementText((ParserRuleContext) tree);
		assertTrue(text.length() < 200, text);
		assertTrue(text.startsWith("echo xxx") && text.endsWith(" ..."), text);
	}
}
