// Copyright 2010 Google Inc. All Rights Reserved.

/**
 * Diff Speed Test
 *
 * @author fraser@google.com (Neil Fraser)
 */

package org.bitbucket.cowwoc.diffmatchpatch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Speedtest
{
	public static void main(String args[]) throws IOException
	{
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		String text1 = readFile(cl.getResource("Speedtest1.txt").getFile());
		String text2 = readFile(cl.getResource("Speedtest2.txt").getFile());

		DiffMatchPatch dmp = new DiffMatchPatch();
		dmp.diffTimeout = 0;

		// Execute one reverse diff as a warmup.
		dmp.diffMain(text2, text1, false);

		long start_time = System.nanoTime();
		dmp.diffMain(text1, text2, false);
		long end_time = System.nanoTime();
		System.out.printf("Elapsed time: %f\n", ((end_time - start_time) / 1000000000.0));
	}

	private static String readFile(String filename) throws IOException
	{
		// Read a file from disk and return the text contents.
		StringBuilder sb = new StringBuilder();
		FileReader input = new FileReader(filename);
		BufferedReader bufRead = new BufferedReader(input);
		try
		{
			String line = bufRead.readLine();
			while (line != null)
			{
				sb.append(line).append('\n');
				line = bufRead.readLine();
			}
		}
		finally
		{
			bufRead.close();
			input.close();
		}
		return sb.toString();
	}
}
