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

import java.io.Serializable;

import net.grinder.common.AbstractTestSemantics;


/**
 * Scripts create <code>Test</code> instances which can then be used
 * to {@link #wrap} other Jython objects.
 *
 * <p>To The Grinder, a <em>test</em> is a unit of work against which
 * statistics are recorded. Tests are uniquely defined by a <em>test
 * number</em> and may also have a <em>description</em>. Scripts can
 * report many different types of thing against the same test, The
 * Grinder will aggregate the results.</p>
 *
 * <p>Creating a <code>Test</code> will automatically update The
 * Grinder console with the test number and the description. If
 * two <code>Tests</code> are created with the same number but a
 * different description, the console will show the first
 * description.</p>
 *
 * @author Philip Aston
 * @version $Revision: 4143 $
 */
public class Test extends AbstractTestSemantics implements Serializable {

  private static final long serialVersionUID = 1L;

  private final int m_number;
  private final String m_description;
  private final transient TestRegistry.RegisteredTest m_registeredTest;

  /**
   * Creates a new <code>Test</code> instance.
   *
   * @param number Test number.
   * @param description Test description.
   */
  public Test(int number, String description) {
    m_number = number;
    m_description = description;
    m_registeredTest = Grinder.grinder.getTestRegistry().register(this);
  }

  /**
   * Get the test number.
   *
   * @return The test number.
   */
  public final int getNumber() {
    return m_number;
  }

  /**
   * Get the test description.
   *
   * @return The test description.
   */
  public final String getDescription() {
    return m_description;
  }

  /**
   * Creates a proxy script object that has the same interface as
   * the passed object. The Grinder will delegate invocations on the
   * proxy object to the target object, timing and record the
   * success or failure of the invocation against the
   * <code>Test</code> statistics. This method can be called many
   * times, for many different targets.
   *
   * @param target Object to wrap.
   * @return The proxy.
   * @exception NotWrappableTypeException If the target object could not be
   * wrapped.
   */
  public final Object wrap(Object target) throws NotWrappableTypeException {
    return m_registeredTest.createProxy(target);
  }

  /**
   * Instrument the supplied {@code target} object. Subsequent calls to {@code
   * target} will be recorded against the statistics for this {@code Test}.
   *
   * @param target
   *          Object to instrument.
   * @throws NonInstrumentableTypeException
   *           If {@code target} could not be instrumented.
   */
  public final void record(Object target)
    throws NonInstrumentableTypeException {
    m_registeredTest.instrument(target);
  }
}

