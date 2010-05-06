// Copyright (C) 2009 - 2010 Philip Aston
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

package net.grinder.engine.process.instrumenter.dcr;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.List;

import net.grinder.common.Logger;
import net.grinder.engine.process.Instrumenter;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.agent.ExposeInstrumentation;
import net.grinder.util.weave.j2se6.ASMTransformerFactory;
import net.grinder.util.weave.j2se6.DCRWeaver;


/**
 * Factory which instantiates the DCR instrumenters.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public final class DCRInstrumenterFactory {

  /**
   * Attempt to create a {@code DCRInstrumenterFactory}.
   *
   * @param logger A logger to complain to if problems are found.
   * @return The factory, or {@code null} if one could not be created.
   */
  public static DCRInstrumenterFactory createFactory(Logger logger) {

    final Instrumentation instrumentation =
      ExposeInstrumentation.getInstrumentation();

    try {
      final Method m =
        Instrumentation.class.getMethod("isRetransformClassesSupported");

      if (!(Boolean)m.invoke(instrumentation)) {
        logger.output(
          "Java VM does not support class retransformation, DCR unavailable");

        return null;
      }
    }
    catch (Exception e1) {
      // Also catches case where instrumentation == null.
      logger.output(
        "Java VM does not support instrumentation, DCR unavailable");

      return null;
    }

    return new DCRInstrumenterFactory(instrumentation,
                                      RecorderLocator.class,
                                      RecorderLocator.getRecorderRegistry());
  }

  private final DCRWeaver m_weaver;
  private final RecorderRegistry m_recorderRegistry;

  /**
   * Constructor. Package scope for unit tests.
   */
  DCRInstrumenterFactory(Instrumentation instrumentation,
                         Class<?> recorderAdviceClass,
                         RecorderRegistry recorderRegistry) {

    m_recorderRegistry = recorderRegistry;
    final ASMTransformerFactory transformerFactory;

    try {
      transformerFactory = new ASMTransformerFactory(recorderAdviceClass);
    }
    catch (WeavingException e) {
      throw new AssertionError(e);
    }

    m_weaver = new DCRWeaver(transformerFactory, instrumentation);
  }

  /**
   * Add a Jython instrumenter.
   *
   * @param instrumenters The list of instrumenters to modify.
   * @return {@code true} if and only if {@code instrumenters} was modified.
   */
  public boolean addJythonInstrumenter(List<Instrumenter> instrumenters) {

    try {
      instrumenters.add(new Jython25Instrumenter(m_weaver,
                                                 m_recorderRegistry));
      return true;
    }
    catch (WeavingException e) {
      // Jython 2.5 not available, try Jython 2.1/2.2.
      try {
        instrumenters.add(new Jython22Instrumenter(m_weaver,
                                                   m_recorderRegistry));
        return true;
      }
      catch (WeavingException e1) {
        // No known version of Jython.
      }
    }

    return false;
  }

  /**
   * Add a Jython instrumenter.
   *
   * @param instrumenters The list of instrumenters to modify.
   * @return {@code true} if and only if {@code instrumenters} was modified.
   */
  public boolean addJavaInstrumenter(List<Instrumenter> instrumenters) {
    instrumenters.add(new JavaDCRInstrumenter(m_weaver, m_recorderRegistry));

    return true;
  }
}
