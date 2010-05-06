// Copyright (C) 2002 - 2009 Philip Aston
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

package net.grinder.script;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;
import net.grinder.engine.process.Instrumenter;
import net.grinder.engine.process.StubTestRegistry;
import net.grinder.engine.process.ScriptEngine.Recorder;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test case for <code>Test</code>.
 *
 * @author Philip Aston
 * @version $Revision: 4143 $
 */
public class TestTest extends TestCase {

  private RandomStubFactory<Instrumenter> s_instrumenterStubFactory;

  public TestTest(String name) {
    super(name);
  }

  protected void setUp() throws Exception {
    StubTestRegistry.stubTestRegistry();
    s_instrumenterStubFactory = StubTestRegistry.getInstrumenterStubFactory();
  }

  public void testGetters() throws Exception {
    final Test test  = new Test(1, "description");

    assertEquals(1, test.getNumber());
    assertEquals("description", test.getDescription());
  }

  public void testOrdering() throws Exception {
    final int size = 100;

    final Set<Test> sorted = new TreeSet<Test>();
    final List<Integer> keys = new ArrayList<Integer>(size);

    for (int i=0; i<size; i++) {
      keys.add(new Integer(i));
    }

    Collections.shuffle(keys);

    for (Integer i : keys) {
      sorted.add(new Test(i, i.toString()));
    }

    int i = 0;

    for (Test test : sorted) {
      assertEquals(i++, test.getNumber());
    }
  }

  public void testEquality() throws Exception {

    // Equality depends only on test number.
    final Test t1 = new Test(57, "one thing");
    final Test t2 = new Test(57, "leads to");
    final Test t3 = new Test(58, "another");

    assertEquals(t1, t2);
    assertEquals(t2, t1);
    assertTrue(!t1.equals(t3));
    assertTrue(!t3.equals(t1));
    assertTrue(!t2.equals(t3));
    assertTrue(!t3.equals(t2));
  }

  public void testIsSerializable() throws Exception {

    final Test test = new Test(123, "test");

    final ByteArrayOutputStream byteArrayOutputStream =
      new ByteArrayOutputStream();

    final ObjectOutputStream objectOutputStream =
      new ObjectOutputStream(byteArrayOutputStream);

    objectOutputStream.writeObject(test);
  }

  public void testWrap() throws Exception {
    final Test t1 = new Test(1, "six cars");
    final Test t2 = new Test(2, "house in ireland");

    final Integer i = new Integer(10);

    s_instrumenterStubFactory.assertNoMoreCalls();

    final Object proxy1 = t1.wrap(i);

    final Object[] parameters =
      s_instrumenterStubFactory.assertSuccess("createInstrumentedProxy",
                                              Test.class,
                                              Recorder.class,
                                              Object.class).getParameters();

    assertSame(t1, parameters[0]);
    assertSame(i, parameters[2]);

    final Object proxy2 = t2.wrap(i);
    assertNotSame(proxy1, proxy2);
  }

  public void testRecord() throws Exception {
    final Test t1 = new Test(1, "bigger than your dad");

    final Integer i = new Integer(10);

    s_instrumenterStubFactory.assertNoMoreCalls();

    t1.record(i);

    final Object[] parameters =
      s_instrumenterStubFactory.assertSuccess("instrument",
                                              Test.class,
                                              Recorder.class,
                                              Object.class).getParameters();

    assertSame(t1, parameters[0]);
    assertSame(i, parameters[2]);
  }
}
