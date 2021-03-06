/*******************************************************************************
 * Copyright (c) 2019, 2019 IBM Corp. and others
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 * or the Apache License, Version 2.0 which accompanies this distribution and
 * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * This Source Code may also be made available under the following
 * Secondary Licenses when the conditions for such availability set
 * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 * General Public License, version 2 with the GNU Classpath
 * Exception [1] and GNU General Public License, version 2 with the
 * OpenJDK Assembly Exception [2].
 *
 * [1] https://www.gnu.org/software/classpath/license.html
 * [2] http://openjdk.java.net/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0 WITH Classpath-exception-2.0 OR LicenseRef-GPL-2.0 WITH Assembly-exception
 *******************************************************************************/

package org.openj9.test.attachAPI;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.openj9.test.util.PlatformInfo;
import org.openj9.test.util.StringUtilities;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static org.openj9.test.attachAPI.TargetManager.getVmId;

@Test(groups = { "level.extended" })
public class TestJcmd extends AttachApiTest {

	private static final String EXPECTED_STRING_FOUND = "Expected string found"; //$NON-NLS-1$
	private static final String HELP_COMMAND = "help"; //$NON-NLS-1$
	private static final String THREAD_PRINT = "Thread.print"; //$NON-NLS-1$
	private static final String GC_RUN = "GC.run"; //$NON-NLS-1$
	private static final String GC_CLASS_HISTOGRAM = "GC.class_histogram"; //$NON-NLS-1$
	private static final String JCMD_COMMAND = "jcmd"; //$NON-NLS-1$
	private static String[] JCMD_COMMANDS = { GC_CLASS_HISTOGRAM, GC_RUN, THREAD_PRINT, HELP_COMMAND };

	/*
	 * Contains strings expected to be contained in the outputs of various commands
	 */
	private static Map<String, String> commandExpectedOutputs;

	/**
	 * Test various ways of printing the help text, including undocumented but
	 * plausible variants
	 * 
	 * @throws IOException on error
	 */
	@Test
	public void testJcmdHelps() throws IOException {
		@SuppressWarnings("nls")
		String[] HELP_OPTIONS = { "-h", HELP_COMMAND, "-help", "--help" };
		for (String helpOption : HELP_OPTIONS) {
			List<String> jcmdOutput = runCommandAndLogOutput(Collections.singletonList(helpOption));
			/* Sample of the text from Jcmd.HELPTEXT */
			String expectedString = "run diagnostic command"; //$NON-NLS-1$
			Optional<String> searchResult = StringUtilities.searchSubstring(expectedString, jcmdOutput);
			assertTrue(searchResult.isPresent(), "Help text corrupt: " + jcmdOutput); //$NON-NLS-1$
		}
	}

	/**
	 * Test help for various commands
	 * @throws IOException on error
	 */
	@Test
	public void testCommandHelps() throws IOException {
		for (String command : JCMD_COMMANDS) {
			List<String> args = new ArrayList<>();
			args.add(getVmId());
			args.add(HELP_COMMAND);
			args.add(command);
			List<String> jcmdOutput = runCommandAndLogOutput(args);
			String expectedString = command + ":"; //$NON-NLS-1$
			log("Expected string: " + expectedString); //$NON-NLS-1$
			Optional<String> searchResult = StringUtilities.searchSubstring(expectedString, jcmdOutput);
			assertTrue(searchResult.isPresent(), "Help text corrupt: " + jcmdOutput); //$NON-NLS-1$
			log(EXPECTED_STRING_FOUND);
		}
	}

	@Test
	public void testListVms() throws IOException {
		String myId = getVmId();
		List<String> args = new ArrayList<>();
		List<String> jcmdOutput = runCommandAndLogOutput(args);
		Optional<String> searchResult = StringUtilities.searchSubstring(myId, jcmdOutput);
		String errorMessage = "My VMID missing from VM list"; //$NON-NLS-1$
		assertTrue(searchResult.isPresent(), errorMessage);
		args.add("-l"); //$NON-NLS-1$
		jcmdOutput = runCommandAndLogOutput(args);
		searchResult = StringUtilities.searchSubstring(myId, jcmdOutput);
		assertTrue(searchResult.isPresent(), errorMessage + jcmdOutput);
		log(EXPECTED_STRING_FOUND);
	}

	@Test
	public void testCommandNoOptions() throws IOException {
		for (String command : JCMD_COMMANDS) {
			List<String> args = new ArrayList<>();
			args.add(getVmId());
			args.add(command);
			List<String> jcmdOutput = runCommandAndLogOutput(args);
			String expectedString = commandExpectedOutputs.getOrDefault(command, "Test error: expected output not defined"); //$NON-NLS-1$
			log("Expected string: " + expectedString); //$NON-NLS-1$
			Optional<String> searchResult = StringUtilities.searchSubstring(expectedString, jcmdOutput);
			assertTrue(searchResult.isPresent(), "Expected string not found " + expectedString); //$NON-NLS-1$
			log(EXPECTED_STRING_FOUND);
		}
	}

	@Test
	public void testThreadPrint() throws IOException {
		String LockedSyncsOption = "-l"; //$NON-NLS-1$
		@SuppressWarnings("nls")
		String[] options = {"", LockedSyncsOption, LockedSyncsOption + "=true", LockedSyncsOption + "=false"};
		for (String option : options) {
			List<String> args = new ArrayList<>();
			args.add(getVmId());
			args.add(THREAD_PRINT);
			if (!option.isEmpty()) {
				args.add(option);
			}
			boolean addSynchronizers = !option.endsWith("false") && !option.isEmpty(); //$NON-NLS-1$
			List<String> jcmdOutput = runCommandAndLogOutput(args);
			String expectedString = "Locked ownable synchronizers"; //$NON-NLS-1$
			log("Expected string: " + expectedString); //$NON-NLS-1$
			Optional<String> searchResult = StringUtilities.searchSubstring(expectedString, jcmdOutput);
			assertEquals(searchResult.isPresent(), addSynchronizers, "Output contains locked synchronizer information: " + expectedString); //$NON-NLS-1$
			log(EXPECTED_STRING_FOUND);
		}
	}

	@Test
	public void testClassHistogramAll() throws IOException {
		List<String> args = new ArrayList<>();
		args.add(getVmId());
		args.add(GC_CLASS_HISTOGRAM);
		args.add("-all"); //$NON-NLS-1$
		List<String> jcmdOutput = runCommandAndLogOutput(args);
		String expectedString = commandExpectedOutputs.getOrDefault(GC_CLASS_HISTOGRAM, "Test error: expected output not defined"); //$NON-NLS-1$
		log("Expected string: " + expectedString); //$NON-NLS-1$
		Optional<String> searchResult = StringUtilities.searchSubstring(expectedString, jcmdOutput);
		assertTrue(searchResult.isPresent(), "Expected string not found: " + expectedString); //$NON-NLS-1$
		log(EXPECTED_STRING_FOUND);
	}

	@BeforeMethod
	protected void setUp(Method testMethod) {
		testName = testMethod.getName();
		log("------------------------------------\nstarting " + testName); //$NON-NLS-1$
	}

	@BeforeSuite
	protected void setupSuite() {
		assertTrue(PlatformInfo.isOpenJ9(), "This test is valid only on OpenJ9"); //$NON-NLS-1$
		getJdkUtilityPath(JCMD_COMMAND);
		commandExpectedOutputs = new HashMap<>();
		commandExpectedOutputs.put(HELP_COMMAND, THREAD_PRINT);
		commandExpectedOutputs.put(GC_CLASS_HISTOGRAM, "java.util.HashMap"); //$NON-NLS-1$
		commandExpectedOutputs.put(GC_RUN, "Command succeeded"); //$NON-NLS-1$
		commandExpectedOutputs.put(THREAD_PRINT, "Attach API wait loop"); //$NON-NLS-1$
	}

}
