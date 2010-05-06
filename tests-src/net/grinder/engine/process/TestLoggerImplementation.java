// Copyright (C) 2007 - 2008 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.process;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;

import net.grinder.common.Logger;
import net.grinder.engine.common.EngineException;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.FileUtilities;
import net.grinder.testutility.StubPrintStream;


/**
 * Unit test case for <code>LoggerImplementation</code>.
 *
 * @author Philip Aston
 * @version $Revision: 3995 $
 */
public class TestLoggerImplementation extends AbstractFileTestCase {

  private PrintStream m_oldStdout;
  private PrintStream m_oldStderr;

  private StubPrintStream m_stdout;
  private StubPrintStream m_stderr;

  protected void setUp() throws Exception {
    super.setUp();

    m_oldStdout = System.out;
    m_oldStderr = System.err;

    m_stdout = new StubPrintStream();
    m_stderr = new StubPrintStream();

    System.setOut(m_stdout);
    System.setErr(m_stderr);
  }

  protected void tearDown() throws Exception {
    System.setOut(m_oldStdout);
    System.setErr(m_oldStderr);

    super.tearDown();
  }

  public void testBasics() throws Exception {
    final File directory = new File(getDirectory(), "logs");

    assertTrue(directory.mkdir());
    FileUtilities.setCanAccess(directory, false);

    try {
      new LoggerImplementation("grinder123",
        directory.getPath(),
        true,
        2);
      fail("Expected EngineException");
    }
    catch (EngineException e) {
    }

    FileUtilities.setCanAccess(directory, true);

    final LoggerImplementation logger =
      new LoggerImplementation("grinder123",
                               directory.getPath(),
                               true,
                               2);

    assertTrue(directory.exists());
    assertEquals(0, directory.list().length);

    assertNotNull(logger.getFilenameFactory());
    assertSame(logger.getFilenameFactory(), logger.getFilenameFactory());

    LoggerImplementation.tick();
  }

  public void testDataWriter() throws Exception {
    final File directory = new File(getDirectory(), "logs");

    final LoggerImplementation logger =
      new LoggerImplementation("grinder123",
                               directory.getPath(),
                               true,
                               2);

    final PrintWriter dataWriter = logger.getDataWriter();
    assertSame(dataWriter, logger.getDataWriter());

    assertEquals(0, directory.listFiles().length);


    dataWriter.write("Test");
    assertEquals(0, directory.listFiles().length);


    dataWriter.flush();

    final File[] files = directory.listFiles();
    assertEquals(1, files.length);

    assertEquals("data_grinder123.log", files[0].getName());
  }

  public void testProcessLogger() throws Exception {
    final File directory = new File(getDirectory(), "logs");

    final LoggerImplementation logger =
      new LoggerImplementation("grinder123",
                               directory.getPath(),
                               true,
                               2);

    final Logger processLogger = logger.getProcessLogger();
    assertSame(processLogger, logger.getProcessLogger());


    processLogger.output("Hello");

    final File[] files = directory.listFiles();
    assertEquals(1, files.length);

    assertEquals("out_grinder123.log", files[0].getName());
    assertEquals(1, countLines(files[0]));

    final String line = readLastLine(files[0]);

    AssertUtilities.assertContains(line, "(process grinder123)");
    AssertUtilities.assertContains(line, "Hello");

    assertEquals(0, m_stdout.getOutputAndReset().length);
    assertEquals(0, m_stderr.getOutputAndReset().length);


    processLogger.error("An error");
    assertEquals(2, directory.listFiles().length);
    assertEquals(2, countLines(files[0]));

    final File errorFile = new File(directory, "error_grinder123.log");
    final String errorLine = readLastLine(errorFile);

    AssertUtilities.assertContains(errorLine, "(process grinder123)");
    AssertUtilities.assertContains(errorLine, "An error");

    AssertUtilities.assertContains(
      new String(m_stdout.getOutputAndReset()), "There were errors");

    assertEquals(errorLine + System.getProperty("line.separator"),
                 new String(m_stderr.getOutputAndReset()));

    processLogger.error("Second error");
    assertEquals(2, directory.listFiles().length);
    assertEquals(3, countLines(files[0])); // Every error has a line in the output.
    assertEquals(2, countLines(errorFile));
    assertEquals(0, m_stdout.getOutputAndReset().length); // Only warn about errors to terminal once.
    assertEquals(0, m_stderr.getOutputAndReset().length);


    processLogger.error("Third error", 0);
    assertEquals(3, countLines(files[0]));
    assertEquals(2, countLines(errorFile));
    assertEquals(0, m_stdout.getOutputAndReset().length);
    assertEquals(0, m_stderr.getOutputAndReset().length);


    processLogger.error(
      "Fourth error, this one's a little bit longer so we can check were truncated",
      Logger.TERMINAL);
    assertEquals(4, countLines(files[0]));
    AssertUtilities.assertContains(readLastLine(files[0]), "...");
    assertEquals(2, countLines(errorFile));
    assertEquals(0, m_stdout.getOutputAndReset().length);
    AssertUtilities.assertContains(
      new String(m_stderr.getOutputAndReset()), "Fourth error");


    processLogger.output("Goodbye");

    assertEquals(5, countLines(files[0]));
    assertEquals(2, directory.listFiles().length);

    logger.close();


    processLogger.output("test");
    processLogger.error("test");
    assertEquals(5, countLines(files[0]));
    assertEquals(2, countLines(errorFile));


    final LoggerImplementation noFileLogger =
      new LoggerImplementation("grinder123",
                               directory.getPath(),
                               false,
                               2);

    final Logger noFileProcessLoger = noFileLogger.getProcessLogger();

    noFileProcessLoger.error("error");

    AssertUtilities.assertContains(
      new String(m_stdout.getOutputAndReset()), "There were errors");
    assertEquals(2, directory.listFiles().length);
  }

  public void testThreadLogger() throws Exception {
    final File directory = new File(getDirectory(), "logs");

    final LoggerImplementation logger =
      new LoggerImplementation("grinder123",
                               directory.getPath(),
                               true,
                               2);

    final ThreadLogger threadLogger1 = logger.createThreadLogger(1);
    final ThreadLogger threadLogger2= logger.createThreadLogger(2);
    assertNotSame(threadLogger1, threadLogger2);

    assertEquals(1, threadLogger1.getThreadNumber());
    assertEquals(2, threadLogger2.getThreadNumber());
    assertEquals(-1, threadLogger1.getCurrentRunNumber());
    assertEquals(-1, threadLogger1.getCurrentTestNumber());
    assertNotNull(threadLogger1.getOutputLogWriter());
    assertNotNull(threadLogger2.getErrorLogWriter());
    assertNotSame(threadLogger1.getOutputLogWriter(),
                  threadLogger2.getErrorLogWriter());


    threadLogger1.setCurrentRunNumber(10);
    threadLogger1.setCurrentTestNumber(35);
    assertEquals(-1, threadLogger2.getCurrentRunNumber());
    assertEquals(-1, threadLogger2.getCurrentTestNumber());
    assertEquals(10, threadLogger1.getCurrentRunNumber());
    assertEquals(35, threadLogger1.getCurrentTestNumber());


    threadLogger2.setCurrentRunNumber(-1);
    assertEquals(-1, threadLogger2.getCurrentRunNumber());


    threadLogger1.output("Hello");
    final File[] files = directory.listFiles();
    assertEquals(1, files.length);
    assertEquals(1, countLines(files[0]));
    final String line = readLastLine(files[0]);
    AssertUtilities.assertContains(line, "(thread 1 run 10 test 35)");
    AssertUtilities.assertContains(line, "Hello");


    threadLogger1.output("World");
    assertEquals("out_grinder123.log", files[0].getName());
    assertEquals(2, countLines(files[0]));
    final String line2 = readLastLine(files[0]);
    AssertUtilities.assertContains(line2, "(thread 1 run 10 test 35)");
    AssertUtilities.assertContains(line2, "World");


    threadLogger2.output("Another thread");
    assertEquals(3, countLines(files[0]));
    final String line3 = readLastLine(files[0]);
    AssertUtilities.assertContains(line3, "(thread 2)");
    AssertUtilities.assertContains(line3, "Another thread");


    assertEquals(0, m_stdout.getOutputAndReset().length);
    assertEquals(0, m_stderr.getOutputAndReset().length);
  }

  public void testFileManagement() throws Exception {
    final File directory = new File(getDirectory(), "logs");

    final LoggerImplementation logger1 =
      new LoggerImplementation("grinder123",
                               directory.getPath(),
                               true,
                               1);

    logger1.getDataWriter().write("hello");
    logger1.getDataWriter().flush();
    logger1.getProcessLogger().error("message");

    assertEquals(3, directory.listFiles().length);


    final LoggerImplementation logger2 =
      new LoggerImplementation("grinder123",
                               directory.getPath(),
                               true,
                               1);

    logger2.getDataWriter().write("hello");
    logger2.getDataWriter().flush();
    logger2.getProcessLogger().error("message");

    assertEquals(6, directory.listFiles().length);


    final LoggerImplementation logger3 =
      new LoggerImplementation("grinder123",
                               directory.getPath(),
                               true,
                               1);

    logger3.getProcessLogger().error("message");

    final File[] files = directory.listFiles();

    assertEquals(5, files.length);

    AssertUtilities.assertArrayContainsAll(
      files,
      new File[] {
          new File(directory, "out_grinder123.log"),
          new File(directory, "error_grinder123.log"),
          new File(directory, "out_grinder123.log00002"),
          new File(directory, "error_grinder123.log00002"),
          new File(directory, "data_grinder123.log00002"),
      });


    final LoggerImplementation logger4 =
      new LoggerImplementation("grinder123",
                               directory.getPath(),
                               true,
                               10);

    logger4.getDataWriter().write("hello");
    logger4.getDataWriter().flush();

    final File[] files2 = directory.listFiles();

    assertEquals(6, files2.length);

    AssertUtilities.assertArrayContainsAll(
      files2,
      new File[] {
          new File(directory, "data_grinder123.log"),
          new File(directory, "out_grinder123.log00002"),
          new File(directory, "error_grinder123.log00002"),
          new File(directory, "data_grinder123.log00002"),
          new File(directory, "out_grinder123.log00003"),
          new File(directory, "error_grinder123.log00003"),
      });


    final LoggerImplementation logger5 =
      new LoggerImplementation("grinder123",
                               directory.getPath(),
                               true,
                               -1);

    logger5.getProcessLogger().error("message");

    final File[] files3 = directory.listFiles();

    assertEquals(2, files3.length);

    AssertUtilities.assertArrayContainsAll(
      files3,
      new File[] {
          new File(directory, "out_grinder123.log"),
          new File(directory, "error_grinder123.log"),
      });
  }
}
