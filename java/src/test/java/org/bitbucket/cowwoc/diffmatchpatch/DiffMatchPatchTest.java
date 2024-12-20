/*
 * Diff Match and Patch -- Test harness
 * Copyright 2018 The diff-match-patch Authors.
 * https://github.com/google/diff-match-patch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bitbucket.cowwoc.diffmatchpatch;

import junit.framework.TestCase;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Diff;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.LinesToCharsResult;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Patch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DiffMatchPatchTest extends TestCase
{
	private DiffMatchPatch dmp = new DiffMatchPatch();
	private final DiffMatchPatch.Operation DELETE = DiffMatchPatch.Operation.DELETE;
	private final DiffMatchPatch.Operation EQUAL = DiffMatchPatch.Operation.EQUAL;
	private final DiffMatchPatch.Operation INSERT = DiffMatchPatch.Operation.INSERT;

	//  DIFF TEST FUNCTIONS
	public void testDiffCommonPrefix()
	{
		// Detect any common prefix.
		assertEquals("diffCommonPrefix: Null case.", 0, dmp.diffCommonPrefix("abc", "xyz"));

		assertEquals("diffCmmonPrefix: Non-null case.", 4, dmp.diffCommonPrefix("1234abcdef", "1234xyz"));

		assertEquals("diffCommonPrefix: Whole case.", 4, dmp.diffCommonPrefix("1234", "1234xyz"));
	}

	public void testDiffCommonSuffix()
	{
		// Detect any common suffix.
		assertEquals("diffCommonSuffix: Null case.", 0, dmp.diffCommonSuffix("abc", "xyz"));

		assertEquals("diffCommonSuffix: Non-null case.", 4, dmp.diffCommonSuffix("abcdef1234", "xyz1234"));

		assertEquals("diffCommonSuffix: Whole case.", 4, dmp.diffCommonSuffix("1234", "xyz1234"));
	}

	public void testDiffCommonOverlap()
	{
		// Detect any suffix/prefix overlap.
		assertEquals("diffCommonOverlap: Null case.", 0, dmp.diffCommonOverlap("", "abcd"));

		assertEquals("diffCommonOverlap: Whole case.", 3, dmp.diffCommonOverlap("abc", "abcd"));

		assertEquals("diffCommonOverlap: No overlap.", 0, dmp.diffCommonOverlap("123456", "abcd"));

		assertEquals("diffCommonOverlap: Overlap.", 3, dmp.diffCommonOverlap("123456xxx", "xxxabcd"));

		// Some overly clever languages (C#) may treat ligatures as equal to their
		// component letters.  E.g. U+FB01 == 'fi'
		assertEquals("diffCommonOverlap: Unicode.", 0, dmp.diffCommonOverlap("fi", "\ufb01i"));
	}

	public void testDiffHalfmatch()
	{
		// Detect a halfmatch.
		dmp.diffTimeout = 1;
		assertNull("diffHalfMatch: No match #1.", dmp.diffHalfMatch("1234567890", "abcdef"));

		assertNull("diffHalfMatch: No match #2.", dmp.diffHalfMatch("12345", "23"));

		assertArrayEquals("diffHalfMatch: Single Match #1.", new String[]
			{
				"12", "90", "a", "z", "345678"
			}, dmp.diffHalfMatch("1234567890", "a345678z"));

		assertArrayEquals("diffHalfMatch: Single Match #2.", new String[]
			{
				"a", "z", "12", "90", "345678"
			}, dmp.diffHalfMatch("a345678z", "1234567890"));

		assertArrayEquals("diffHalfMatch: Single Match #3.", new String[]
			{
				"abc", "z", "1234", "0", "56789"
			}, dmp.diffHalfMatch("abc56789z", "1234567890"));

		assertArrayEquals("diffHalfMatch: Single Match #4.", new String[]
			{
				"a", "xyz", "1", "7890", "23456"
			}, dmp.diffHalfMatch("a23456xyz", "1234567890"));

		assertArrayEquals("diffHalfMatch: Multiple Matches #1.", new String[]
			{
				"12123", "123121", "a", "z", "1234123451234"
			}, dmp.diffHalfMatch("121231234123451234123121", "a1234123451234z"));

		assertArrayEquals("diffHalfMatch: Multiple Matches #2.", new String[]
			{
				"", "-=-=-=-=-=", "x", "", "x-=-=-=-=-=-=-="
			}, dmp.diffHalfMatch("x-=-=-=-=-=-=-=-=-=-=-=-=", "xx-=-=-=-=-=-=-="));

		assertArrayEquals("diffHalfMatch: Multiple Matches #3.", new String[]
			{
				"-=-=-=-=-=", "", "", "y", "-=-=-=-=-=-=-=y"
			}, dmp.diffHalfMatch("-=-=-=-=-=-=-=-=-=-=-=-=y", "-=-=-=-=-=-=-=yy"));

		// Optimal diff would be -q+x=H-i+e=lloHe+Hu=llo-Hew+y not -qHillo+x=HelloHe-w+Hulloy
		assertArrayEquals("diffHalfMatch: Non-optimal halfmatch.", new String[]
			{
				"qHillo", "w", "x", "Hulloy", "HelloHe"
			}, dmp.diffHalfMatch("qHilloHelloHew", "xHelloHeHulloy"));

		dmp.diffTimeout = 0;
		assertNull("diffHalfMatch: Optimal no halfmatch.", dmp.diffHalfMatch("qHilloHelloHew", "xHelloHeHulloy"));
	}

	public void testDiffLinesToChars()
	{
		// Convert lines down to characters.
		ArrayList<String> tmpVector = new ArrayList<>();
		tmpVector.add("");
		tmpVector.add("alpha\n");
		tmpVector.add("beta\n");
		assertLinesToCharsResultEquals("diffLinesToChars: Shared lines.", new LinesToCharsResult("\u0001\u0002\u0001",
			"\u0002\u0001\u0002", tmpVector), dmp.diffLinesToChars("alpha\nbeta\nalpha\n", "beta\nalpha\nbeta\n"));

		tmpVector.clear();
		tmpVector.add("");
		tmpVector.add("alpha\r\n");
		tmpVector.add("beta\r\n");
		tmpVector.add("\r\n");
		assertLinesToCharsResultEquals("diffLinesToChars: Empty string and blank lines.", new LinesToCharsResult("",
			"\u0001\u0002\u0003\u0003", tmpVector), dmp.diffLinesToChars("", "alpha\r\nbeta\r\n\r\n\r\n"));

		tmpVector.clear();
		tmpVector.add("");
		tmpVector.add("a");
		tmpVector.add("b");
		assertLinesToCharsResultEquals("diffLinesToChars: No linebreaks.", new LinesToCharsResult("\u0001", "\u0002",
			tmpVector), dmp.diffLinesToChars("a", "b"));

		// More than 256 to reveal any 8-bit limitations.
		int n = 300;
		tmpVector.clear();
		StringBuilder lineList = new StringBuilder();
		StringBuilder charList = new StringBuilder();
		for (int i = 1; i < n + 1; i++)
		{
			tmpVector.add(i + "\n");
			lineList.append(i).append("\n");
			charList.append(String.valueOf((char) i));
		}
		assertEquals("Test initialization fail #1.", n, tmpVector.size());
		String lines = lineList.toString();
		String chars = charList.toString();
		assertEquals("Test initialization fail #2.", n, chars.length());
		tmpVector.add(0, "");
		assertLinesToCharsResultEquals("diffLinesToChars: More than 256.", new LinesToCharsResult(chars, "", tmpVector),
			dmp.diffLinesToChars(lines, ""));
	}

	public void testDiffCharsToLines()
	{
		// First check that Diff equality works.
		assertTrue("diffCharsToLines: Equality #1.", new Diff(EQUAL, "a").equals(new Diff(EQUAL, "a")));

		assertEquals("diffCharsToLines: Equality #2.", new Diff(EQUAL, "a"), new Diff(EQUAL, "a"));

		// Convert chars up to lines.
		LinkedList<Diff> diffs = diffList(new Diff(EQUAL, "\u0001\u0002\u0001"), new Diff(INSERT, "\u0002\u0001\u0002"));
		ArrayList<String> tmpVector = new ArrayList<>();
		tmpVector.add("");
		tmpVector.add("alpha\n");
		tmpVector.add("beta\n");
		dmp.diffCharsToLines(diffs, tmpVector);
		assertEquals("diffCharsToLines: Shared lines.", diffList(new Diff(EQUAL, "alpha\nbeta\nalpha\n"), new Diff(INSERT,
			"beta\nalpha\nbeta\n")), diffs);

		// More than 256 to reveal any 8-bit limitations.
		int n = 300;
		tmpVector.clear();
		StringBuilder lineList = new StringBuilder();
		StringBuilder charList = new StringBuilder();
		for (int i = 1; i < n + 1; i++)
		{
			tmpVector.add(i + "\n");
			lineList.append(i).append("\n");
			charList.append(((char) i));
		}
		assertEquals("Test initialization fail #3.", n, tmpVector.size());
		String lines = lineList.toString();
		String chars = charList.toString();
		assertEquals("Test initialization fail #4.", n, chars.length());
		tmpVector.add(0, "");
		diffs = diffList(new Diff(DELETE, chars));
		dmp.diffCharsToLines(diffs, tmpVector);
		assertEquals("diffCharsToLines: More than 256.", diffList(new Diff(DELETE, lines)), diffs);

		// More than 65536 to verify any 16-bit limitation.
		lineList = new StringBuilder();
		for (int i = 0; i < 66000; i++)
		{
			lineList.append(i + "\n");
		}
		chars = lineList.toString();
		LinesToCharsResult results = dmp.diffLinesToChars(chars, "");
		diffs = diffList(new Diff(INSERT, results.chars1));
		dmp.diffCharsToLines(diffs, results.lineArray);
		assertEquals("diff_charsToLines: More than 65536.", chars, diffs.getFirst().text);
	}

	public void testDiffCleanupMerge()
	{
		// Cleanup a messy diff.
		LinkedList<Diff> diffs = diffList();
		dmp.diffCleanupMerge(diffs);
		assertEquals("diffCleanupMerge: Null case.", diffList(), diffs);

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "b"), new Diff(INSERT, "c"));
		dmp.diffCleanupMerge(diffs);
		assertEquals("diffCleanupMerge: No change case.", diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "b"), new Diff(
			INSERT, "c")), diffs);

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(EQUAL, "b"), new Diff(EQUAL, "c"));
		dmp.diffCleanupMerge(diffs);
		assertEquals("diffCleanupMerge: Merge equalities.", diffList(new Diff(EQUAL, "abc")), diffs);

		diffs = diffList(new Diff(DELETE, "a"), new Diff(DELETE, "b"), new Diff(DELETE, "c"));
		dmp.diffCleanupMerge(diffs);
		assertEquals("diffCleanupMerge: Merge deletions.", diffList(new Diff(DELETE, "abc")), diffs);

		diffs = diffList(new Diff(INSERT, "a"), new Diff(INSERT, "b"), new Diff(INSERT, "c"));
		dmp.diffCleanupMerge(diffs);
		assertEquals("diffCleanupMerge: Merge insertions.", diffList(new Diff(INSERT, "abc")), diffs);

		diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "b"), new Diff(DELETE, "c"), new Diff(INSERT, "d"),
			new Diff(EQUAL, "e"), new Diff(EQUAL, "f"));
		dmp.diffCleanupMerge(diffs);
		assertEquals("diffCleanupMerge: Merge interweave.", diffList(new Diff(DELETE, "ac"), new Diff(INSERT, "bd"),
			new Diff(EQUAL, "ef")), diffs);

		diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "abc"), new Diff(DELETE, "dc"));
		dmp.diffCleanupMerge(diffs);
		assertEquals("diffCleanupMerge: Prefix and suffix detection.", diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "d"),
			new Diff(INSERT, "b"), new Diff(EQUAL, "c")), diffs);

		diffs = diffList(new Diff(EQUAL, "x"), new Diff(DELETE, "a"), new Diff(INSERT, "abc"), new Diff(DELETE, "dc"),
			new Diff(EQUAL, "y"));
		dmp.diffCleanupMerge(diffs);
		assertEquals("diffCleanupMerge: Prefix and suffix detection with equalities.", diffList(new Diff(EQUAL, "xa"),
			new Diff(DELETE, "d"), new Diff(INSERT, "b"), new Diff(EQUAL, "cy")), diffs);

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(INSERT, "ba"), new Diff(EQUAL, "c"));
		dmp.diffCleanupMerge(diffs);
		assertEquals("diffCleanupMerge: Slide edit left.", diffList(new Diff(INSERT, "ab"), new Diff(EQUAL, "ac")), diffs);

		diffs = diffList(new Diff(EQUAL, "c"), new Diff(INSERT, "ab"), new Diff(EQUAL, "a"));
		dmp.diffCleanupMerge(diffs);
		assertEquals("diffCleanupMerge: Slide edit right.", diffList(new Diff(EQUAL, "ca"), new Diff(INSERT, "ba")), diffs);

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "b"), new Diff(EQUAL, "c"), new Diff(DELETE, "ac"),
			new Diff(EQUAL, "x"));
		dmp.diffCleanupMerge(diffs);
		assertEquals("diffCleanupMerge: Slide edit left recursive.", diffList(new Diff(DELETE, "abc"),
			new Diff(EQUAL, "acx")), diffs);

		diffs = diffList(new Diff(EQUAL, "x"), new Diff(DELETE, "ca"), new Diff(EQUAL, "c"), new Diff(DELETE, "b"),
			new Diff(EQUAL, "a"));
		dmp.diffCleanupMerge(diffs);
		assertEquals("diffCleanupMerge: Slide edit right recursive.", diffList(new Diff(EQUAL, "xca"), new Diff(DELETE,
			"cba")), diffs);

		diffs = diffList(new Diff(DELETE, "b"), new Diff(INSERT, "ab"), new Diff(EQUAL, "c"));
		dmp.diffCleanupMerge(diffs);
		assertEquals("diff_cleanupMerge: Empty merge.", diffList(new Diff(INSERT, "a"), new Diff(EQUAL, "bc")), diffs);

		diffs = diffList(new Diff(EQUAL, ""), new Diff(INSERT, "a"), new Diff(EQUAL, "b"));
		dmp.diffCleanupMerge(diffs);
		assertEquals("diff_cleanupMerge: Empty equality.", diffList(new Diff(INSERT, "a"), new Diff(EQUAL, "b")), diffs);
	}

	public void testDiffCleanupSemanticLossless()
	{
		// Slide diffs to match logical boundaries.
		LinkedList<Diff> diffs = diffList();
		dmp.diffCleanupSemanticLossless(diffs);
		assertEquals("diffCleanupSemanticLossless: Null case.", diffList(), diffs);

		diffs = diffList(new Diff(EQUAL, "AAA\r\n\r\nBBB"), new Diff(INSERT, "\r\nDDD\r\n\r\nBBB"), new Diff(EQUAL,
			"\r\nEEE"));
		dmp.diffCleanupSemanticLossless(diffs);
		assertEquals("diffCleanupSemanticLossless: Blank lines.", diffList(new Diff(EQUAL, "AAA\r\n\r\n"), new Diff(INSERT,
			"BBB\r\nDDD\r\n\r\n"), new Diff(EQUAL, "BBB\r\nEEE")), diffs);

		diffs = diffList(new Diff(EQUAL, "AAA\r\nBBB"), new Diff(INSERT, " DDD\r\nBBB"), new Diff(EQUAL, " EEE"));
		dmp.diffCleanupSemanticLossless(diffs);
		assertEquals("diffCleanupSemanticLossless: Line boundaries.", diffList(new Diff(EQUAL, "AAA\r\n"), new Diff(INSERT,
			"BBB DDD\r\n"), new Diff(EQUAL, "BBB EEE")), diffs);

		diffs = diffList(new Diff(EQUAL, "The c"), new Diff(INSERT, "ow and the c"), new Diff(EQUAL, "at."));
		dmp.diffCleanupSemanticLossless(diffs);
		assertEquals("diffCleanupSemanticLossless: Word boundaries.", diffList(new Diff(EQUAL, "The "), new Diff(INSERT,
			"cow and the "), new Diff(EQUAL, "cat.")), diffs);

		diffs = diffList(new Diff(EQUAL, "The-c"), new Diff(INSERT, "ow-and-the-c"), new Diff(EQUAL, "at."));
		dmp.diffCleanupSemanticLossless(diffs);
		assertEquals("diffCleanupSemanticLossless: Alphanumeric boundaries.", diffList(new Diff(EQUAL, "The-"), new Diff(
			INSERT, "cow-and-the-"), new Diff(EQUAL, "cat.")), diffs);

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "a"), new Diff(EQUAL, "ax"));
		dmp.diffCleanupSemanticLossless(diffs);
		assertEquals("diffCleanupSemanticLossless: Hitting the start.", diffList(new Diff(DELETE, "a"), new Diff(EQUAL,
			"aax")), diffs);

		diffs = diffList(new Diff(EQUAL, "xa"), new Diff(DELETE, "a"), new Diff(EQUAL, "a"));
		dmp.diffCleanupSemanticLossless(diffs);
		assertEquals("diffCleanupSemanticLossless: Hitting the end.",
			diffList(new Diff(EQUAL, "xaa"), new Diff(DELETE, "a")), diffs);

		diffs = diffList(new Diff(EQUAL, "The xxx. The "), new Diff(INSERT, "zzz. The "), new Diff(EQUAL, "yyy."));
		dmp.diffCleanupSemanticLossless(diffs);
		assertEquals("diffCleanupSemanticLossless: Sentence boundaries.", diffList(new Diff(EQUAL, "The xxx."), new Diff(
			INSERT, " The zzz."), new Diff(EQUAL, " The yyy.")), diffs);
	}

	public void testDiffCleanupSemantic()
	{
		// Cleanup semantically trivial equalities.
		LinkedList<Diff> diffs = diffList();
		dmp.diffCleanupSemantic(diffs);
		assertEquals("diffCleanupSemantic: Null case.", diffList(), diffs);

		diffs = diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "cd"), new Diff(EQUAL, "12"), new Diff(DELETE, "e"));
		dmp.diffCleanupSemantic(diffs);
		assertEquals("diffCleanupSemantic: No elimination #1.", diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "cd"),
			new Diff(EQUAL, "12"), new Diff(DELETE, "e")), diffs);

		diffs
			= diffList(new Diff(DELETE, "abc"), new Diff(INSERT, "ABC"), new Diff(EQUAL, "1234"), new Diff(DELETE, "wxyz"));
		dmp.diffCleanupSemantic(diffs);
		assertEquals("diffCleanupSemantic: No elimination #2.", diffList(new Diff(DELETE, "abc"), new Diff(INSERT, "ABC"),
			new Diff(EQUAL, "1234"), new Diff(DELETE, "wxyz")), diffs);

		diffs = diffList(new Diff(DELETE, "a"), new Diff(EQUAL, "b"), new Diff(DELETE, "c"));
		dmp.diffCleanupSemantic(diffs);
		assertEquals("diffCleanupSemantic: Simple elimination.", diffList(new Diff(DELETE, "abc"), new Diff(INSERT, "b")),
			diffs);

		diffs = diffList(new Diff(DELETE, "ab"), new Diff(EQUAL, "cd"), new Diff(DELETE, "e"), new Diff(EQUAL, "f"),
			new Diff(INSERT, "g"));
		dmp.diffCleanupSemantic(diffs);
		assertEquals("diffCleanupSemantic: Backpass elimination.", diffList(new Diff(DELETE, "abcdef"), new Diff(INSERT,
			"cdfg")), diffs);

		diffs = diffList(new Diff(INSERT, "1"), new Diff(EQUAL, "A"), new Diff(DELETE, "B"), new Diff(INSERT, "2"),
			new Diff(EQUAL, "_"), new Diff(INSERT, "1"), new Diff(EQUAL, "A"), new Diff(DELETE, "B"), new Diff(INSERT, "2"));
		dmp.diffCleanupSemantic(diffs);
		assertEquals("diffCleanupSemantic: Multiple elimination.", diffList(new Diff(DELETE, "AB_AB"), new Diff(INSERT,
			"1A2_1A2")), diffs);

		diffs = diffList(new Diff(EQUAL, "The c"), new Diff(DELETE, "ow and the c"), new Diff(EQUAL, "at."));
		dmp.diffCleanupSemantic(diffs);
		assertEquals("diffCleanupSemantic: Word boundaries.", diffList(new Diff(EQUAL, "The "), new Diff(DELETE,
			"cow and the "), new Diff(EQUAL, "cat.")), diffs);

		diffs = diffList(new Diff(DELETE, "abcxx"), new Diff(INSERT, "xxdef"));
		dmp.diffCleanupSemantic(diffs);
		assertEquals("diffCleanupSemantic: No overlap elimination.", diffList(new Diff(DELETE, "abcxx"), new Diff(INSERT,
			"xxdef")), diffs);

		diffs = diffList(new Diff(DELETE, "abcxxx"), new Diff(INSERT, "xxxdef"));
		dmp.diffCleanupSemantic(diffs);
		assertEquals("diffCleanupSemantic: Overlap elimination.", diffList(new Diff(DELETE, "abc"), new Diff(EQUAL, "xxx"),
			new Diff(INSERT, "def")), diffs);

		diffs = diffList(new Diff(DELETE, "xxxabc"), new Diff(INSERT, "defxxx"));
		dmp.diffCleanupSemantic(diffs);
		assertEquals("diffCleanupSemantic: Reverse overlap elimination.", diffList(new Diff(INSERT, "def"), new Diff(EQUAL,
			"xxx"), new Diff(DELETE, "abc")), diffs);

		diffs = diffList(new Diff(DELETE, "abcd1212"), new Diff(INSERT, "1212efghi"), new Diff(EQUAL, "----"), new Diff(
			DELETE, "A3"), new Diff(INSERT, "3BC"));
		dmp.diffCleanupSemantic(diffs);
		assertEquals("diffCleanupSemantic: Two overlap eliminations.", diffList(new Diff(DELETE, "abcd"), new Diff(EQUAL,
				"1212"), new Diff(INSERT, "efghi"), new Diff(EQUAL, "----"), new Diff(DELETE, "A"), new Diff(EQUAL, "3"),
			new Diff(INSERT, "BC")), diffs);
	}

	public void testDiffCleanupEfficiency()
	{
		// Cleanup operationally trivial equalities.
		dmp.diffEditCost = 4;
		LinkedList<Diff> diffs = diffList();
		dmp.diffCleanupEfficiency(diffs);
		assertEquals("diffCleanupEfficiency: Null case.", diffList(), diffs);

		diffs = diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "12"), new Diff(EQUAL, "wxyz"), new Diff(DELETE, "cd"),
			new Diff(INSERT, "34"));
		dmp.diffCleanupEfficiency(diffs);
		assertEquals("diffCleanupEfficiency: No elimination.", diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "12"),
			new Diff(EQUAL, "wxyz"), new Diff(DELETE, "cd"), new Diff(INSERT, "34")), diffs);

		diffs = diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "12"), new Diff(EQUAL, "xyz"), new Diff(DELETE, "cd"),
			new Diff(INSERT, "34"));
		dmp.diffCleanupEfficiency(diffs);
		assertEquals("diffCleanupEfficiency: Four-edit elimination.", diffList(new Diff(DELETE, "abxyzcd"), new Diff(INSERT,
			"12xyz34")), diffs);

		diffs = diffList(new Diff(INSERT, "12"), new Diff(EQUAL, "x"), new Diff(DELETE, "cd"), new Diff(INSERT, "34"));
		dmp.diffCleanupEfficiency(diffs);
		assertEquals("diffCleanupEfficiency: Three-edit elimination.", diffList(new Diff(DELETE, "xcd"), new Diff(INSERT,
			"12x34")), diffs);

		diffs = diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "12"), new Diff(EQUAL, "xy"), new Diff(INSERT, "34"),
			new Diff(EQUAL, "z"), new Diff(DELETE, "cd"), new Diff(INSERT, "56"));
		dmp.diffCleanupEfficiency(diffs);
		assertEquals("diffCleanupEfficiency: Backpass elimination.", diffList(new Diff(DELETE, "abxyzcd"), new Diff(INSERT,
			"12xy34z56")), diffs);

		dmp.diffEditCost = 5;
		diffs = diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "12"), new Diff(EQUAL, "wxyz"), new Diff(DELETE, "cd"),
			new Diff(INSERT, "34"));
		dmp.diffCleanupEfficiency(diffs);
		assertEquals("diffCleanupEfficiency: High cost elimination.", diffList(new Diff(DELETE, "abwxyzcd"),
			new Diff(INSERT, "12wxyz34")), diffs);
		dmp.diffEditCost = 4;
	}

	public void testDiffPrettyHtml()
	{
		// Pretty print.
		LinkedList<Diff> diffs = diffList(new Diff(EQUAL, "a\n"), new Diff(DELETE, "<B>b</B>"), new Diff(INSERT, "c&d"));
		assertEquals("diffPrettyHtml:",
			"<span>a&para;<br></span><del style=\"background:#ffe6e6;\">&lt;B&gt;b&lt;/B&gt;</del><ins style=\"background:#e6ffe6;\">c&amp;d</ins>",
			dmp.diffPrettyHtml(diffs));
	}

	public void testDiffText()
	{
		// Compute the source and destination texts.
		LinkedList<Diff> diffs = diffList(new Diff(EQUAL, "jump"), new Diff(DELETE, "s"), new Diff(INSERT, "ed"), new Diff(
			EQUAL, " over "), new Diff(DELETE, "the"), new Diff(INSERT, "a"), new Diff(EQUAL, " lazy"));
		assertEquals("diffText1:", "jumps over the lazy", dmp.diffText1(diffs));
		assertEquals("diffText2:", "jumped over a lazy", dmp.diffText2(diffs));
	}

	public void testDiffDelta()
	{
		// Convert a diff into delta string.
		LinkedList<Diff> diffs = diffList(new Diff(EQUAL, "jump"), new Diff(DELETE, "s"), new Diff(INSERT, "ed"), new Diff(
			EQUAL, " over "), new Diff(DELETE, "the"), new Diff(INSERT, "a"), new Diff(EQUAL, " lazy"), new Diff(INSERT,
			"old dog"));
		String text1 = dmp.diffText1(diffs);
		assertEquals("diffText1: Base text.", "jumps over the lazy", text1);

		String delta = dmp.diffToDelta(diffs);
		assertEquals("diffToDelta:", "=4\t-1\t+ed\t=6\t-3\t+a\t=5\t+old dog", delta);

		// Convert delta string into a diff.
		assertEquals("diffFromDelta: Normal.", diffs, dmp.diffFromDelta(text1, delta));

		// Generates error (19 < 20).
		try
		{
			dmp.diffFromDelta(text1 + "x", delta);
			fail("diffFromDelta: Too long.");
		}
		catch (IllegalArgumentException ex)
		{
			// Exception expected.
		}

		// Generates error (19 > 18).
		try
		{
			dmp.diffFromDelta(text1.substring(1), delta);
			fail("diffFromDelta: Too short.");
		}
		catch (IllegalArgumentException ex)
		{
			// Exception expected.
		}

		// Generates error (%c3%xy invalid Unicode).
		try
		{
			dmp.diffFromDelta("", "+%c3%xy");
			fail("diffFromDelta: Invalid character.");
		}
		catch (IllegalArgumentException ex)
		{
			// Exception expected.
		}

		// Test deltas with special characters.
		diffs = diffList(new Diff(EQUAL, "\u0680 \000 \t %"), new Diff(DELETE, "\u0681 \001 \n ^"), new Diff(INSERT,
			"\u0682 \002 \\ |"));
		text1 = dmp.diffText1(diffs);
		assertEquals("diffText1: Unicode text.", "\u0680 \000 \t %\u0681 \001 \n ^", text1);

		delta = dmp.diffToDelta(diffs);
		assertEquals("diffToDelta: Unicode.", "=7\t-7\t+%DA%82 %02 %5C %7C", delta);

		assertEquals("diffFromDelta: Unicode.", diffs, dmp.diffFromDelta(text1, delta));

		// Verify pool of unchanged characters.
		diffs = diffList(new Diff(INSERT, "A-Z a-z 0-9 - _ . ! ~ * ' ( ) ; / ? : @ & = + $ , # "));
		String text2 = dmp.diffText2(diffs);
		assertEquals("diffText2: Unchanged characters.", "A-Z a-z 0-9 - _ . ! ~ * \' ( ) ; / ? : @ & = + $ , # ", text2);

		delta = dmp.diffToDelta(diffs);
		assertEquals("diffToDelta: Unchanged characters.", "+A-Z a-z 0-9 - _ . ! ~ * \' ( ) ; / ? : @ & = + $ , # ", delta);

		// Convert delta string into a diff.
		assertEquals("diffFromDelta: Unchanged characters.", diffs, dmp.diffFromDelta("", delta));


		// 160 kb string.
		String a = "abcdefghij";
		for (int i = 0; i < 14; i++)
		{
			a += a;
		}
		diffs = diffList(new Diff(INSERT, a));
		delta = dmp.diffToDelta(diffs);
		assertEquals("diff_toDelta: 160kb string.", "+" + a, delta);

		// Convert delta string into a diff.
		assertEquals("diff_fromDelta: 160kb string.", diffs, dmp.diffFromDelta("", delta));
	}

	public void testDiffXIndex()
	{
		// Translate a location in text1 to text2.
		LinkedList<Diff> diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "1234"), new Diff(EQUAL, "xyz"));
		assertEquals("diffXIndex: Translation on equality.", 5, dmp.diffXIndex(diffs, 2));

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "1234"), new Diff(EQUAL, "xyz"));
		assertEquals("diffXIndex: Translation on deletion.", 1, dmp.diffXIndex(diffs, 3));
	}

	public void testDiffLevenshtein()
	{
		LinkedList<Diff> diffs = diffList(new Diff(DELETE, "abc"), new Diff(INSERT, "1234"), new Diff(EQUAL, "xyz"));
		assertEquals("diff_levenshtein: Levenshtein with trailing equality.", 4, dmp.diffLevenshtein(diffs));

		diffs = diffList(new Diff(EQUAL, "xyz"), new Diff(DELETE, "abc"), new Diff(INSERT, "1234"));
		assertEquals("diff_levenshtein: Levenshtein with leading equality.", 4, dmp.diffLevenshtein(diffs));

		diffs = diffList(new Diff(DELETE, "abc"), new Diff(EQUAL, "xyz"), new Diff(INSERT, "1234"));
		assertEquals("diff_levenshtein: Levenshtein with middle equality.", 7, dmp.diffLevenshtein(diffs));
	}

	public void testDiffBisect()
	{
		// Normal.
		String a = "cat";
		String b = "map";
		// Since the resulting diff hasn't been normalized, it would be ok if
		// the insertion and deletion pairs are swapped.
		// If the order changes, tweak this test as required.
		LinkedList<Diff> diffs = diffList(new Diff(DELETE, "c"), new Diff(INSERT, "m"), new Diff(EQUAL, "a"), new Diff(
			DELETE, "t"), new Diff(INSERT, "p"));
		assertEquals("diffBisect: Normal.", diffs, dmp.diffBisect(a, b, Long.MAX_VALUE));

		// Timeout.
		diffs = diffList(new Diff(DELETE, "cat"), new Diff(INSERT, "map"));
		assertEquals("diffBisect: Timeout.", diffs, dmp.diffBisect(a, b, 0));
	}

	public void testDiffMain()
	{
		// Perform a trivial diff.
		LinkedList<Diff> diffs = diffList();
		assertEquals("diffMain: Null case.", diffs, dmp.diffMain("", "", false));

		diffs = diffList(new Diff(EQUAL, "abc"));
		assertEquals("diffMain: Equality.", diffs, dmp.diffMain("abc", "abc", false));

		diffs = diffList(new Diff(EQUAL, "ab"), new Diff(INSERT, "123"), new Diff(EQUAL, "c"));
		assertEquals("diffMain: Simple insertion.", diffs, dmp.diffMain("abc", "ab123c", false));

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "123"), new Diff(EQUAL, "bc"));
		assertEquals("diffMain: Simple deletion.", diffs, dmp.diffMain("a123bc", "abc", false));

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(INSERT, "123"), new Diff(EQUAL, "b"), new Diff(INSERT, "456"),
			new Diff(EQUAL, "c"));
		assertEquals("diffMain: Two insertions.", diffs, dmp.diffMain("abc", "a123b456c", false));

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "123"), new Diff(EQUAL, "b"), new Diff(DELETE, "456"),
			new Diff(EQUAL, "c"));
		assertEquals("diffMain: Two deletions.", diffs, dmp.diffMain("a123b456c", "abc", false));

		// Perform a real diff.
		// Switch off the timeout.
		dmp.diffTimeout = 0;
		diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "b"));
		assertEquals("diffMain: Simple case #1.", diffs, dmp.diffMain("a", "b", false));

		diffs = diffList(new Diff(DELETE, "Apple"), new Diff(INSERT, "Banana"), new Diff(EQUAL, "s are a"), new Diff(INSERT,
			"lso"), new Diff(EQUAL, " fruit."));
		assertEquals("diffMain: Simple case #2.", diffs, dmp.diffMain("Apples are a fruit.", "Bananas are also fruit.",
			false));

		diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "\u0680"), new Diff(EQUAL, "x"), new Diff(DELETE, "\t"),
			new Diff(INSERT, "\000"));
		assertEquals("diffMain: Simple case #3.", diffs, dmp.diffMain("ax\t", "\u0680x\000", false));

		diffs = diffList(new Diff(DELETE, "1"), new Diff(EQUAL, "a"), new Diff(DELETE, "y"), new Diff(EQUAL, "b"), new Diff(
			DELETE, "2"), new Diff(INSERT, "xab"));
		assertEquals("diffMain: Overlap #1.", diffs, dmp.diffMain("1ayb2", "abxab", false));

		diffs = diffList(new Diff(INSERT, "xaxcx"), new Diff(EQUAL, "abc"), new Diff(DELETE, "y"));
		assertEquals("diffMain: Overlap #2.", diffs, dmp.diffMain("abcy", "xaxcxabc", false));

		diffs = diffList(new Diff(DELETE, "ABCD"), new Diff(EQUAL, "a"), new Diff(DELETE, "="), new Diff(INSERT, "-"),
			new Diff(EQUAL, "bcd"), new Diff(DELETE, "="), new Diff(INSERT, "-"), new Diff(EQUAL, "efghijklmnopqrs"),
			new Diff(DELETE, "EFGHIJKLMNOefg"));
		assertEquals("diffMain: Overlap #3.", diffs, dmp.diffMain("ABCDa=bcd=efghijklmnopqrsEFGHIJKLMNOefg",
			"a-bcd-efghijklmnopqrs", false));

		diffs = diffList(new Diff(INSERT, " "), new Diff(EQUAL, "a"), new Diff(INSERT, "nd"), new Diff(EQUAL,
			" [[Pennsylvania]]"), new Diff(DELETE, " and [[New"));
		assertEquals("diffMain: Large equality.", diffs, dmp.diffMain("a [[Pennsylvania]] and [[New",
			" and [[Pennsylvania]]", false));

		dmp.diffTimeout = 0.1f;  // 100ms
		String a
			= "`Twas brillig, and the slithy toves\nDid gyre and gimble in the wabe:\nAll mimsy were the borogoves,\nAnd the mome raths outgrabe.\n";
		String b
			= "I am the very model of a modern major general,\nI've information vegetable, animal, and mineral,\nI know the kings of England, and I quote the fights historical,\nFrom Marathon to Waterloo, in order categorical.\n";
		// Increase the text lengths by 1024 times to ensure a timeout.
		for (int i = 0; i < 10; i++)
		{
			a += a;
			b += b;
		}
		long startTime = System.currentTimeMillis();
		dmp.diffMain(a, b);
		long endTime = System.currentTimeMillis();
		// Test that we took at least the timeout period.
		assertTrue("diffMain: Timeout min.", dmp.diffTimeout * 1000 <= endTime - startTime);
		// Test that we didn't take forever (be forgiving).
		// Theoretically this test could fail very occasionally if the
		// OS task swaps or locks up for a second at the wrong moment.
		assertTrue("diffMain: Timeout max.", dmp.diffTimeout * 1000 * 2 > endTime - startTime);
		dmp.diffTimeout = 0;

		// Test the linemode speedup.
		// Must be long to pass the 100 char cutoff.
		a
			= "1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n";
		b
			= "abcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\n";
		assertEquals("diffMain: Simple line-mode.", dmp.diffMain(a, b, true), dmp.diffMain(a, b, false));

		a
			= "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
		b
			= "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghij";
		assertEquals("diffMain: Single line-mode.", dmp.diffMain(a, b, true), dmp.diffMain(a, b, false));

		a
			= "1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n";
		b
			= "abcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n";
		String[] textsLinemode = diffRebuildtexts(dmp.diffMain(a, b, true));
		String[] textsTextmode = diffRebuildtexts(dmp.diffMain(a, b, false));
		assertArrayEquals("diffMain: Overlap line-mode.", textsTextmode, textsLinemode);

		// Test null inputs.
		try
		{
			dmp.diffMain(null, null);
			fail("diffMain: Null inputs.");
		}
		catch (IllegalArgumentException ex)
		{
			// Error expected.
		}
	}

	//  MATCH TEST FUNCTIONS
	public void testMatchAlphabet()
	{
		// Initialise the bitmasks for Bitap.
		Map<Character, Integer> bitmask;
		bitmask = new HashMap<>();
		bitmask.put('a', 4);
		bitmask.put('b', 2);
		bitmask.put('c', 1);
		assertEquals("matchAlphabet: Unique.", bitmask, dmp.matchAlphabet("abc"));

		bitmask = new HashMap<>();
		bitmask.put('a', 37);
		bitmask.put('b', 18);
		bitmask.put('c', 8);
		assertEquals("matchAlphabet: Duplicates.", bitmask, dmp.matchAlphabet("abcaba"));
	}

	public void testMatchBitap()
	{
		// Bitap algorithm.
		dmp.matchDistance = 100;
		dmp.matchThreshold = 0.5f;
		assertEquals("matchBitap: Exact match #1.", 5, dmp.matchBitap("abcdefghijk", "fgh", 5));

		assertEquals("matchBitap: Exact match #2.", 5, dmp.matchBitap("abcdefghijk", "fgh", 0));

		assertEquals("matchBitap: Fuzzy match #1.", 4, dmp.matchBitap("abcdefghijk", "efxhi", 0));

		assertEquals("matchBitap: Fuzzy match #2.", 2, dmp.matchBitap("abcdefghijk", "cdefxyhijk", 5));

		assertEquals("matchBitap: Fuzzy match #3.", -1, dmp.matchBitap("abcdefghijk", "bxy", 1));

		assertEquals("matchBitap: Overflow.", 2, dmp.matchBitap("123456789xx0", "3456789x0", 2));

		assertEquals("matchBitap: Before start match.", 0, dmp.matchBitap("abcdef", "xxabc", 4));

		assertEquals("matchBitap: Beyond end match.", 3, dmp.matchBitap("abcdef", "defyy", 4));

		assertEquals("matchBitap: Oversized pattern.", 0, dmp.matchBitap("abcdef", "xabcdefy", 0));

		dmp.matchThreshold = 0.4f;
		assertEquals("matchBitap: Threshold #1.", 4, dmp.matchBitap("abcdefghijk", "efxyhi", 1));

		dmp.matchThreshold = 0.3f;
		assertEquals("matchBitap: Threshold #2.", -1, dmp.matchBitap("abcdefghijk", "efxyhi", 1));

		dmp.matchThreshold = 0.0f;
		assertEquals("matchBitap: Threshold #3.", 1, dmp.matchBitap("abcdefghijk", "bcdef", 1));

		dmp.matchThreshold = 0.5f;
		assertEquals("matchBitap: Multiple select #1.", 0, dmp.matchBitap("abcdexyzabcde", "abccde", 3));

		assertEquals("matchBitap: Multiple select #2.", 8, dmp.matchBitap("abcdexyzabcde", "abccde", 5));

		dmp.matchDistance = 10;  // Strict location.
		assertEquals("matchBitap: Distance test #1.", -1, dmp.matchBitap("abcdefghijklmnopqrstuvwxyz", "abcdefg", 24));

		assertEquals("matchBitap: Distance test #2.", 0, dmp.matchBitap("abcdefghijklmnopqrstuvwxyz", "abcdxxefg", 1));

		dmp.matchDistance = 1000;  // Loose location.
		assertEquals("matchBitap: Distance test #3.", 0, dmp.matchBitap("abcdefghijklmnopqrstuvwxyz", "abcdefg", 24));
	}

	public void testMatchMain()
	{
		// Full match.
		assertEquals("matchMain: Equality.", 0, dmp.matchMain("abcdef", "abcdef", 1000));

		assertEquals("matchMain: Null text.", -1, dmp.matchMain("", "abcdef", 1));

		assertEquals("matchMain: Null pattern.", 3, dmp.matchMain("abcdef", "", 3));

		assertEquals("matchMain: Exact match.", 3, dmp.matchMain("abcdef", "de", 3));

		assertEquals("matchMain: Beyond end match.", 3, dmp.matchMain("abcdef", "defy", 4));

		assertEquals("matchMain: Oversized pattern.", 0, dmp.matchMain("abcdef", "abcdefy", 0));

		dmp.matchThreshold = 0.7f;
		assertEquals("matchMain: Complex match.", 4, dmp.matchMain("I am the very model of a modern major general.",
			" that berry ", 5));
		dmp.matchThreshold = 0.5f;

		// Test null inputs.
		try
		{
			dmp.matchMain(null, null, 0);
			fail("matchMain: Null inputs.");
		}
		catch (IllegalArgumentException ex)
		{
			// Error expected.
		}
	}

	//  PATCH TEST FUNCTIONS
	public void testPatchObj()
	{
		// Patch Object.
		Patch p = new Patch();
		p.start1 = 20;
		p.start2 = 21;
		p.length1 = 18;
		p.length2 = 17;
		p.diffs
			= diffList(new Diff(EQUAL, "jump"), new Diff(DELETE, "s"), new Diff(INSERT, "ed"), new Diff(EQUAL, " over "),
			new Diff(DELETE, "the"), new Diff(INSERT, "a"), new Diff(EQUAL, "\nlaz"));
		String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n %0Alaz\n";
		assertEquals("Patch: toString.", strp, p.toString());
	}

	public void testPatchFromText()
	{
		assertTrue("patchFromText: #0.", dmp.patchFromText("").isEmpty());

		String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n %0Alaz\n";
		assertEquals("patchFromText: #1.", strp, dmp.patchFromText(strp).get(0).toString());

		assertEquals("patchFromText: #2.", "@@ -1 +1 @@\n-a\n+b\n", dmp.patchFromText("@@ -1 +1 @@\n-a\n+b\n").get(0).
			toString());

		assertEquals("patchFromText: #3.", "@@ -1,3 +0,0 @@\n-abc\n", dmp.patchFromText("@@ -1,3 +0,0 @@\n-abc\n").get(0).
			toString());

		assertEquals("patchFromText: #4.", "@@ -0,0 +1,3 @@\n+abc\n", dmp.patchFromText("@@ -0,0 +1,3 @@\n+abc\n").get(0).
			toString());

		// Generates error.
		try
		{
			dmp.patchFromText("Bad\nPatch\n");
			fail("patchFromText: #5.");
		}
		catch (IllegalArgumentException ex)
		{
			// Exception expected.
		}
	}

	public void testPatchToText()
	{
		String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n  laz\n";
		List<Patch> patches;
		patches = dmp.patchFromText(strp);
		assertEquals("patchToText: Single.", strp, dmp.patchToText(patches));

		strp = "@@ -1,9 +1,9 @@\n-f\n+F\n oo+fooba\n@@ -7,9 +7,9 @@\n obar\n-,\n+.\n  tes\n";
		patches = dmp.patchFromText(strp);
		assertEquals("patchToText: Dual.", strp, dmp.patchToText(patches));
	}

	public void testPatchAddContext()
	{
		dmp.patchMargin = 4;
		Patch p;
		p = dmp.patchFromText("@@ -21,4 +21,10 @@\n-jump\n+somersault\n").get(0);
		dmp.patchAddContext(p, "The quick brown fox jumps over the lazy dog.");
		assertEquals("patchAddContext: Simple case.", "@@ -17,12 +17,18 @@\n fox \n-jump\n+somersault\n s ov\n", p.
			toString());

		p = dmp.patchFromText("@@ -21,4 +21,10 @@\n-jump\n+somersault\n").get(0);
		dmp.patchAddContext(p, "The quick brown fox jumps.");
		assertEquals("patchAddContext: Not enough trailing context.",
			"@@ -17,10 +17,16 @@\n fox \n-jump\n+somersault\n s.\n", p.toString());

		p = dmp.patchFromText("@@ -3 +3,2 @@\n-e\n+at\n").get(0);
		dmp.patchAddContext(p, "The quick brown fox jumps.");
		assertEquals("patchAddContext: Not enough leading context.", "@@ -1,7 +1,8 @@\n Th\n-e\n+at\n  qui\n", p.toString());

		p = dmp.patchFromText("@@ -3 +3,2 @@\n-e\n+at\n").get(0);
		dmp.patchAddContext(p, "The quick brown fox jumps.  The quick brown fox crashes.");
		assertEquals("patchAddContext: Ambiguity.", "@@ -1,27 +1,28 @@\n Th\n-e\n+at\n  quick brown fox jumps. \n", p.
			toString());
	}

	@SuppressWarnings("deprecation")
	public void testPatchMake()
	{
		LinkedList<Patch> patches;
		patches = dmp.patchMake("", "");
		assertEquals("patchMake: Null case.", "", dmp.patchToText(patches));

		String text1 = "The quick brown fox jumps over the lazy dog.";
		String text2 = "That quick brown fox jumped over a lazy dog.";
		String expectedPatch
			= "@@ -1,8 +1,7 @@\n Th\n-at\n+e\n  qui\n@@ -21,17 +21,18 @@\n jump\n-ed\n+s\n  over \n-a\n+the\n  laz\n";
		// The second patch must be "-21,17 +21,18", not "-22,17 +21,18" due to rolling context.
		patches = dmp.patchMake(text2, text1);
		assertEquals("patchMake: Text2+Text1 inputs.", expectedPatch, dmp.patchToText(patches));

		expectedPatch
			= "@@ -1,11 +1,12 @@\n Th\n-e\n+at\n  quick b\n@@ -22,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n  laz\n";
		patches = dmp.patchMake(text1, text2);
		assertEquals("patchMake: Text1+Text2 inputs.", expectedPatch, dmp.patchToText(patches));

		LinkedList<Diff> diffs = dmp.diffMain(text1, text2, false);
		patches = dmp.patchMake(diffs);
		assertEquals("patchMake: Diff input.", expectedPatch, dmp.patchToText(patches));

		patches = dmp.patchMake(text1, diffs);
		assertEquals("patchMake: Text1+Diff inputs.", expectedPatch, dmp.patchToText(patches));

		patches = dmp.patchMake(text1, text2, diffs);
		assertEquals("patchMake: Text1+Text2+Diff inputs (deprecated).", expectedPatch, dmp.patchToText(patches));

		patches = dmp.patchMake("`1234567890-=[]\\;',./", "~!@#$%^&*()_+{}|:\"<>?");
		assertEquals("patchToText: Character encoding.",
			"@@ -1,21 +1,21 @@\n-%601234567890-=%5B%5D%5C;',./\n+~!@#$%25%5E&*()_+%7B%7D%7C:%22%3C%3E?\n", dmp.patchToText(
				patches));

		diffs = diffList(new Diff(DELETE, "`1234567890-=[]\\;',./"), new Diff(INSERT, "~!@#$%^&*()_+{}|:\"<>?"));
		assertEquals("patchFromText: Character decoding.", diffs, dmp.patchFromText(
			"@@ -1,21 +1,21 @@\n-%601234567890-=%5B%5D%5C;',./\n+~!@#$%25%5E&*()_+%7B%7D%7C:%22%3C%3E?\n").get(0).diffs);

		text1 = "";
		for (int x = 0; x < 100; x++)
		{
			text1 += "abcdef";
		}
		text2 = text1 + "123";
		expectedPatch = "@@ -573,28 +573,31 @@\n cdefabcdefabcdefabcdefabcdef\n+123\n";
		patches = dmp.patchMake(text1, text2);
		assertEquals("patchMake: Long string with repeats.", expectedPatch, dmp.patchToText(patches));

		// Test null inputs.
		try
		{
			dmp.patchMake(null);
			fail("patchMake: Null inputs.");
		}
		catch (IllegalArgumentException ex)
		{
			// Error expected.
		}
	}

	public void testPatchSplitMax()
	{
		// Assumes that matchMaxBits is 32.
		LinkedList<Patch> patches;
		patches = dmp.patchMake("abcdefghijklmnopqrstuvwxyz01234567890",
			"XabXcdXefXghXijXklXmnXopXqrXstXuvXwxXyzX01X23X45X67X89X0");
		dmp.patchSplitMax(patches);
		assertEquals("patchSplitMax: #1.",
			"@@ -1,32 +1,46 @@\n+X\n ab\n+X\n cd\n+X\n ef\n+X\n gh\n+X\n ij\n+X\n kl\n+X\n mn\n+X\n op\n+X\n qr\n+X\n st\n+X\n uv\n+X\n wx\n+X\n yz\n+X\n 012345\n@@ -25,13 +39,18 @@\n zX01\n+X\n 23\n+X\n 45\n+X\n 67\n+X\n 89\n+X\n 0\n",
			dmp.patchToText(patches));

		patches = dmp.patchMake("abcdef1234567890123456789012345678901234567890123456789012345678901234567890uvwxyz",
			"abcdefuvwxyz");
		String oldToText = dmp.patchToText(patches);
		dmp.patchSplitMax(patches);
		assertEquals("patchSplitMax: #2.", oldToText, dmp.patchToText(patches));

		patches = dmp.patchMake("1234567890123456789012345678901234567890123456789012345678901234567890", "abc");
		dmp.patchSplitMax(patches);
		assertEquals("patchSplitMax: #3.",
			"@@ -1,32 +1,4 @@\n-1234567890123456789012345678\n 9012\n@@ -29,32 +1,4 @@\n-9012345678901234567890123456\n 7890\n@@ -57,14 +1,3 @@\n-78901234567890\n+abc\n",
			dmp.patchToText(patches));

		patches = dmp.patchMake("abcdefghij , h : 0 , t : 1 abcdefghij , h : 0 , t : 1 abcdefghij , h : 0 , t : 1",
			"abcdefghij , h : 1 , t : 1 abcdefghij , h : 1 , t : 1 abcdefghij , h : 0 , t : 1");
		dmp.patchSplitMax(patches);
		assertEquals("patchSplitMax: #4.",
			"@@ -2,32 +2,32 @@\n bcdefghij , h : \n-0\n+1\n  , t : 1 abcdef\n@@ -29,32 +29,32 @@\n bcdefghij , h : \n-0\n+1\n  , t : 1 abcdef\n",
			dmp.patchToText(patches));
	}

	public void testPatchAddPadding()
	{
		LinkedList<Patch> patches;
		patches = dmp.patchMake("", "test");
		assertEquals("patchAddPadding: Both edges full.", "@@ -0,0 +1,4 @@\n+test\n", dmp.patchToText(patches));
		dmp.patchAddPadding(patches);
		assertEquals("patchAddPadding: Both edges full.", "@@ -1,8 +1,12 @@\n %01%02%03%04\n+test\n %01%02%03%04\n", dmp.
			patchToText(patches));

		patches = dmp.patchMake("XY", "XtestY");
		assertEquals("patchAddPadding: Both edges partial.", "@@ -1,2 +1,6 @@\n X\n+test\n Y\n", dmp.patchToText(patches));
		dmp.patchAddPadding(patches);
		assertEquals("patchAddPadding: Both edges partial.", "@@ -2,8 +2,12 @@\n %02%03%04X\n+test\n Y%01%02%03\n", dmp.
			patchToText(patches));

		patches = dmp.patchMake("XXXXYYYY", "XXXXtestYYYY");
		assertEquals("patchAddPadding: Both edges none.", "@@ -1,8 +1,12 @@\n XXXX\n+test\n YYYY\n", dmp.
			patchToText(patches));
		dmp.patchAddPadding(patches);
		assertEquals("patchAddPadding: Both edges none.", "@@ -5,8 +5,12 @@\n XXXX\n+test\n YYYY\n", dmp.
			patchToText(patches));
	}

	public void testPatchApply()
	{
		dmp.matchDistance = 1000;
		dmp.matchThreshold = 0.5f;
		dmp.patchDeleteThreshold = 0.5f;
		LinkedList<Patch> patches;
		patches = dmp.patchMake("", "");
		Object[] results = dmp.patchApply(patches, "Hello world.");
		boolean[] boolArray = (boolean[]) results[1];
		String resultStr = results[0] + "\t" + boolArray.length;
		assertEquals("patchApply: Null case.", "Hello world.\t0", resultStr);

		patches = dmp.patchMake("The quick brown fox jumps over the lazy dog.",
			"That quick brown fox jumped over a lazy dog.");
		results = dmp.patchApply(patches, "The quick brown fox jumps over the lazy dog.");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
		assertEquals("patchApply: Exact match.", "That quick brown fox jumped over a lazy dog.\ttrue\ttrue", resultStr);

		results = dmp.patchApply(patches, "The quick red rabbit jumps over the tired tiger.");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
		assertEquals("patchApply: Partial match.", "That quick red rabbit jumped over a tired tiger.\ttrue\ttrue", resultStr);

		results = dmp.patchApply(patches, "I am the very model of a modern major general.");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
		assertEquals("patchApply: Failed match.", "I am the very model of a modern major general.\tfalse\tfalse", resultStr);

		patches = dmp.patchMake("x1234567890123456789012345678901234567890123456789012345678901234567890y", "xabcy");
		results = dmp.patchApply(patches,
			"x123456789012345678901234567890-----++++++++++-----123456789012345678901234567890y");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
		assertEquals("patchApply: Big delete, small change.", "xabcy\ttrue\ttrue", resultStr);

		patches = dmp.patchMake("x1234567890123456789012345678901234567890123456789012345678901234567890y", "xabcy");
		results = dmp.patchApply(patches,
			"x12345678901234567890---------------++++++++++---------------12345678901234567890y");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
		assertEquals("patchApply: Big delete, big change 1.",
			"xabc12345678901234567890---------------++++++++++---------------12345678901234567890y\tfalse\ttrue", resultStr);

		dmp.patchDeleteThreshold = 0.6f;
		patches = dmp.patchMake("x1234567890123456789012345678901234567890123456789012345678901234567890y", "xabcy");
		results = dmp.patchApply(patches,
			"x12345678901234567890---------------++++++++++---------------12345678901234567890y");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
		assertEquals("patchApply: Big delete, big change 2.", "xabcy\ttrue\ttrue", resultStr);
		dmp.patchDeleteThreshold = 0.5f;

		// Compensate for failed patch.
		dmp.matchThreshold = 0.0f;
		dmp.matchDistance = 0;
		patches = dmp.patchMake("abcdefghijklmnopqrstuvwxyz--------------------1234567890",
			"abcXXXXXXXXXXdefghijklmnopqrstuvwxyz--------------------1234567YYYYYYYYYY890");
		results = dmp.patchApply(patches, "ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567890");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
		assertEquals("patchApply: Compensate for failed patch.",
			"ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567YYYYYYYYYY890\tfalse\ttrue", resultStr);
		dmp.matchThreshold = 0.5f;
		dmp.matchDistance = 1000;

		patches = dmp.patchMake("", "test");
		String patchStr = dmp.patchToText(patches);
		dmp.patchApply(patches, "");
		assertEquals("patchApply: No side effects.", patchStr, dmp.patchToText(patches));

		patches = dmp.patchMake("The quick brown fox jumps over the lazy dog.", "Woof");
		patchStr = dmp.patchToText(patches);
		dmp.patchApply(patches, "The quick brown fox jumps over the lazy dog.");
		assertEquals("patchApply: No side effects with major delete.", patchStr, dmp.patchToText(patches));

		patches = dmp.patchMake("", "test");
		results = dmp.patchApply(patches, "");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0];
		assertEquals("patchApply: Edge exact match.", "test\ttrue", resultStr);

		patches = dmp.patchMake("XY", "XtestY");
		results = dmp.patchApply(patches, "XY");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0];
		assertEquals("patchApply: Near edge exact match.", "XtestY\ttrue", resultStr);

		patches = dmp.patchMake("y", "y123");
		results = dmp.patchApply(patches, "x");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0];
		assertEquals("patchApply: Edge partial match.", "x123\ttrue", resultStr);
	}

	private void assertArrayEquals(String errorMsg, Object[] a, Object[] b)
	{
		List<Object> listA = Arrays.asList(a);
		List<Object> listB = Arrays.asList(b);
		assertEquals(errorMsg, listA, listB);
	}

	private void assertLinesToCharsResultEquals(String errorMsg,
	                                            LinesToCharsResult a, LinesToCharsResult b)
	{
		assertEquals(errorMsg, a.chars1, b.chars1);
		assertEquals(errorMsg, a.chars2, b.chars2);
		assertEquals(errorMsg, a.lineArray, b.lineArray);
	}

	// Construct the two texts which made up the diff originally.
	private static String[] diffRebuildtexts(LinkedList<Diff> diffs)
	{
		String[] text =
			{
				"", ""
			};
		for (Diff myDiff : diffs)
		{
			if (myDiff.operation != DiffMatchPatch.Operation.INSERT)
			{
				text[0] += myDiff.text;
			}
			if (myDiff.operation != DiffMatchPatch.Operation.DELETE)
			{
				text[1] += myDiff.text;
			}
		}
		return text;
	}

	// Private function for quickly building lists of diffs.
	private static LinkedList<Diff> diffList(Diff... diffs)
	{
		return new LinkedList<Diff>(Arrays.asList(diffs));
	}
}
