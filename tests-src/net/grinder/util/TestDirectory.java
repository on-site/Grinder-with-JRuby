// Copyright (C) 2004 - 2009 Philip Aston
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

package net.grinder.util;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.Set;

import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.FileUtilities;
import net.grinder.testutility.Serializer;


/**
 * Unit test case for {@link Directory}.
 *
 * @author Philip Aston
 * @version $Revision: 4005 $
 */
public class TestDirectory extends AbstractFileTestCase {

  public void testConstruction() throws Exception {

    final File file = new File(getDirectory(), "x");
    assertTrue(file.createNewFile());

    try {
      new Directory(file);
      fail("Expected DirectoryException");
    }
    catch (Directory.DirectoryException e) {
    }

    final Directory directory = new Directory(getDirectory());
    assertEquals(0, directory.getWarnings().length);

    assertEquals(getDirectory(), directory.getFile());

    assertEquals(new File("."), new Directory(null).getFile());
  }

  public void testDefaultConstructor() throws Exception {

    final Directory directory = new Directory();
    final File cwd = new File(System.getProperty("user.dir"));
    assertEquals(cwd.getCanonicalPath(),
                 directory.getFile().getCanonicalPath());
  }

  public void testEquality() throws Exception {

    final Directory d1 = new Directory(getDirectory());
    final Directory d2 = new Directory(getDirectory());

    final File f = new File(getDirectory(), "comeonpilgrimyouknowhelovesyou");
    assertTrue(f.mkdir());

    final Directory d3 = new Directory(f);

    assertEquals(d1, d1);
    assertEquals(d1, d2);
    AssertUtilities.assertNotEquals(d2, d3);

    assertEquals(d1.hashCode(), d1.hashCode());
    assertEquals(d1.hashCode(), d2.hashCode());

    AssertUtilities.assertNotEquals(d1, null);
    AssertUtilities.assertNotEquals(d1, f);
  }

  public void testListContents() throws Exception {

    final Directory directory = new Directory(getDirectory());

    final String[] files = {
      "first/three",
      "will-not-be-picked-up",
      "because/they/are/too/old",
      "directory/foo/bah/blah",
      "directory/blah",
      "a/b/c/d/e",
      "a/b/f/g/h",
      "a/b/f/g/i",
      "x",
      "y/z",
      "another",
    };

    final Set<File> expected = new HashSet<File>();

    for (int i=0; i<files.length; ++i) {
      final File file = new File(getDirectory(), files[i]);
      file.getParentFile().mkdirs();
      assertTrue(file.createNewFile());

      if (i < 3) {
        assertTrue(file.setLastModified(10000L * (i + 1)));
      }
      else {
        // Result uses relative paths.
        expected.add(new File(files[i]));
      }
    }

    final File[] badDirectories = {
      new File(getDirectory(), "directory/foo/bah/blah.cantread"),
      new File(getDirectory(), "cantread"),
    };

    for (int i = 0; i < badDirectories.length; ++i) {
      badDirectories[i].getParentFile().mkdirs();
      assertTrue(badDirectories[i].mkdir());
      FileUtilities.setCanAccess(badDirectories[i], false);
    }

    final File[] filesAfterTimeT = directory.listContents(
      new FileFilter() {
        public boolean accept(File file) {
          return file.isDirectory() || file.lastModified() > 50000L;
        }
      });

    for (int i=0; i<filesAfterTimeT.length; ++i) {
      assertTrue("Contains " + filesAfterTimeT[i],
                 expected.contains(filesAfterTimeT[i]));
    }

    final String[] warnings = directory.getWarnings();
    assertEquals(badDirectories.length, warnings.length);

    final StringBuffer warningsBuffer = new StringBuffer();

    for (int i = 0; i < warnings.length; ++i) {
      warningsBuffer.append(warnings[i]);
      warningsBuffer.append("\n");
    }

    final String warningsString = warningsBuffer.toString();

    for (int i = 0; i < badDirectories.length; ++i) {
      assertTrue(warningsBuffer + " contains " + badDirectories[i].getPath(),
                 warningsString.indexOf(badDirectories[i].getPath()) > -1);

      FileUtilities.setCanAccess(badDirectories[i], true);
    }

    final File[] allFiles =
      directory.listContents(Directory.getMatchAllFilesFilter());
    assertEquals(files.length, allFiles.length);
  }

  public void testDeleteContents() throws Exception {

    final Directory directory = new Directory(getDirectory());

    final String[] files = {
      "directory/foo/bah/blah",
      "directory/blah",
      "a/b/c/d/e",
      "a/b/f/g/h",
      "a/b/f/g/i",
      "x",
      "y/z",
      "another",
    };

    for (int i = 0; i < files.length; ++i) {
      final File file = new File(getDirectory(), files[i]);
      file.getParentFile().mkdirs();
      assertTrue(file.createNewFile());
    }

    assertTrue(getDirectory().list().length > 0);

    directory.deleteContents();

    assertEquals(0, getDirectory().list().length);

    // Can't test that deleteContents() throws an exception if
    // contents couldn't be deleted as File.delete() ignores file
    // permissions on W2K.
  }

  public void testCreate() throws Exception {
    final String[] directories = {
      "toplevel",
      "down/a/few",
    };

    for (int i=0; i<directories.length; ++i) {
      final Directory directory =
        new Directory(new File(getDirectory(), directories[i]));
      assertFalse(directory.getFile().exists());
      directory.create();
      assertTrue(directory.getFile().exists());
    }

    final File file = new File(getDirectory(), "readonly");
    assertTrue(file.createNewFile());
    FileUtilities.setCanAccess(file, false);

    try {
      new Directory(new File(getDirectory(), "readonly/foo")).create();
      fail("Expected DirectoryException");
    }
    catch (Directory.DirectoryException e) {
    }
  }

  public void testDelete() throws Exception {
    final Directory directory1 =
      new Directory(new File(getDirectory(), "a/directory"));
    directory1.create();
    assertTrue(directory1.getFile().exists());
    directory1.delete();
    assertFalse(directory1.getFile().exists());

    final Directory directory2 =
      new Directory(new File(getDirectory(), "another"));
    directory2.create();
    final File file2 = new File(getDirectory(), "another/file");
    assertTrue(file2.createNewFile());

    try {
      directory2.delete();
      fail("Expected DirectoryException");
    }
    catch (Directory.DirectoryException e) {
    }
  }

  public void testGetRelativePath() throws Exception {
    final String[] files = {
      "path1",
      "some/other/path",
    };

    final Directory directory = new Directory(getDirectory());

    for (int i = 0; i < files.length; ++i) {
      final File absoluteFile = new File(getDirectory(), files[i]);

      final File result = directory.getRelativePath(absoluteFile);
      assertFalse(result.isAbsolute());
      assertEquals(absoluteFile, new File(getDirectory(), result.getPath()));

      final File relativeFile = new File(files[i]);

      final File result2 = directory.getRelativePath(relativeFile);
      assertFalse(result2.isAbsolute());
      assertEquals(relativeFile, result2);
    }

    // Absolute file outside of directory.
    assertNull(directory.getRelativePath(new File("blah").getAbsoluteFile()));
  }

  public void testIsParentOf() throws Exception {
    final File f1 = new File("xfoo");
    final File f2 = new File("xfoo/bah");
    final File f3 = new File("xfoo/bah/blah");
    final File f4 = new File("xfoo/bah/dah");

    assertTrue(new Directory(f1).isParentOf(f2));
    assertTrue(new Directory(f1).isParentOf(f3));

    assertFalse(new Directory(f2).isParentOf(f2));
    assertFalse(new Directory(f2).isParentOf(f1));
    assertFalse(new Directory(f3).isParentOf(f1));
    assertFalse(new Directory(f3).isParentOf(f4));
  }

  public void testCopyTo() throws Exception {
    final Set<File> files = new HashSet<File>() {{
      add(new File("a file"));
      add(new File("directory/.afile"));
      add(new File("directory/b/c/d/e"));
    }};

    for (File relativeFile : files) {
      final File absoluteFile =
        new File(getDirectory(), relativeFile.getPath());

      createRandomFile(absoluteFile);
    }

    final Directory sourceDirectory = new Directory(getDirectory());

    final File output = new File(getDirectory(), "output");
    final Directory outputDirectory = new Directory(output);
    outputDirectory.create();
    final File overwritten = new File(output, "should be deleted");
    createRandomFile(overwritten);

    assertTrue(overwritten.exists());

    sourceDirectory.copyTo(outputDirectory, false);

    assertFalse(overwritten.exists());

    final File[] contents =
      outputDirectory.listContents(Directory.getMatchAllFilesFilter());

    for (int i = 0; i < contents.length; ++i) {
      assertTrue("Original contains '" + contents[i] + "'",
                 files.contains(contents[i]));
    }

    assertEquals(files.size(), contents.length);

    sourceDirectory.copyTo(outputDirectory, true);

    final File[] contents2 =
      outputDirectory.listContents(Directory.getMatchAllFilesFilter());

    for (int i = 0; i < contents2.length; ++i) {
      if (!contents2[i].getPath().startsWith("output")) {
        assertTrue("Original contains '" + contents2[i] + "'",
                   files.contains(contents2[i]));
      }
    }

    final File[] contents3 =
      new Directory(new File("output/output"))
      .listContents(Directory.getMatchAllFilesFilter());

    for (int i = 0; i < contents3.length; ++i) {
      assertTrue("Original contains '" + contents3[i] + "'",
                 files.contains(contents3[i]));
    }

    assertEquals(files.size() * 2, contents2.length);

    final Directory missingSourceDirectory =
      new Directory(sourceDirectory.getFile(new File("missing")));

    final Directory missingOutputDirectory =
      new Directory(outputDirectory.getFile(new File("notthere")));

    assertFalse(missingSourceDirectory.getFile().exists());
    assertFalse(missingOutputDirectory.getFile().exists());

    try {
      missingSourceDirectory.copyTo(missingOutputDirectory, false);
      fail("Expected DirectoryException");
    }
    catch (Directory.DirectoryException e) {
    }

    assertFalse(missingSourceDirectory.getFile().exists());
    assertFalse(missingOutputDirectory.getFile().exists());
  }

  public void testSerialization() throws Exception {
    final Directory original = new Directory(getDirectory());

    assertEquals(original, Serializer.serialize(original));
  }
}
