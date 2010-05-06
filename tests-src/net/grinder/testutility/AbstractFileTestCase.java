// Copyright (C) 2004 - 2008 Philip Aston
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

package net.grinder.testutility;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;


/**
 * Abstract test case that manages a temporary directory.
 *
 * @author Philip Aston
 * @version $Revision: 3869 $
 */
public abstract class AbstractFileTestCase extends TestCase {

  protected static Random s_random  = new Random();

  private File m_directory;

  protected void tearDown() throws Exception {
    if (m_directory != null) {
      delete(m_directory);
    }
  }

  private static void delete(File f) throws Exception {

    if (f.isDirectory()) {
      final File[] children = f.listFiles();

      if (children == null) {
        System.err.println("Could not list directory '" + f + "'");
      }
      else {
        for (int i=0; i<children.length; ++i) {
          delete(children[i]);
        }
      }
    }

    if (!f.delete()) {
      System.err.println("Could not delete file '" + f + "'");
    }
  }

  protected final File getDirectory() throws IOException {
    if (m_directory == null) {
      m_directory = File.createTempFile(getClass().getName(), "test");
      assertTrue(m_directory.delete());
      assertTrue(m_directory.mkdir());
      m_directory.deleteOnExit();
    }

    return m_directory;
  }

  protected final void createRandomFile(File file) throws IOException {
    file.getParentFile().mkdirs();

    final OutputStream out = new FileOutputStream(file);
    final byte[] bytes = new byte[s_random.nextInt(2000)];
    s_random.nextBytes(bytes);
    out.write(bytes);
    out.close();
  }

  protected String readLastLine(File file) throws IOException {
    final BufferedReader reader = new BufferedReader(new FileReader(file));

    try {
      String last = null;

      while (true) {
        final String line = reader.readLine();
        if (line == null) {
          return last;
        }

        last = line;
      }
    }
    finally {
      reader.close();
    }
  }

  protected int countLines(File file) throws IOException {
    final BufferedReader reader = new BufferedReader(new FileReader(file));

    try {
      int result = 0;

      while (true) {
        final String line = reader.readLine();
        if (line == null) {
          return result;
        }

        ++result;
      }
    }
    finally {
      reader.close();
    }
  }
}
