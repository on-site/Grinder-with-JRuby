// Copyright (C) 2009 Philip Aston
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import net.grinder.common.Test;
import net.grinder.engine.process.Instrumenter;
import net.grinder.engine.process.ScriptEngine.Recorder;
import net.grinder.script.NonInstrumentableTypeException;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.util.weave.Weaver;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.Weaver.TargetSource;


/**
 * DCRInstrumenter.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
abstract class DCRInstrumenter implements Instrumenter {

  private static final String[] NON_INSTRUMENTABLE_PACKAGES = {
    "net.grinder.engine.process",
    "extra166y",
    "org.objectweb.asm",
  };

  private static final ClassLoader BOOTSTRAP_CLASSLOADER =
    Object.class.getClassLoader();

  private final Weaver m_weaver;
  private final RecorderRegistry m_recorderRegistry;

  /**
   * Constructor for DCRInstrumenter.
   *
   * @param weaver The weaver.
   * @param recorderRegistry The recorder registry.
   */
  public DCRInstrumenter(Weaver weaver,
                         RecorderRegistry recorderRegistry) {
    m_weaver = weaver;
    m_recorderRegistry = recorderRegistry;
  }

  /**
   * {@inheritDoc}
   */
  public Object createInstrumentedProxy(Test test,
                                        Recorder recorder,
                                        Object target)
    throws NotWrappableTypeException {

    try {
      return instrument(test, recorder, target) ? target : null;
    }
    catch (NonInstrumentableTypeException e) {
      throw new NotWrappableTypeException(e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean instrument(Test test, Recorder recorder, Object target)
    throws NonInstrumentableTypeException {

    final boolean changed = instrument(target, recorder);

    if (changed) {
      try {
        m_weaver.applyChanges();
      }
      catch (WeavingException e) {
        throw new NonInstrumentableTypeException(e.getMessage());
      }
    }

    return changed;
  }

  protected abstract boolean instrument(Object target,  Recorder recorder)
    throws NonInstrumentableTypeException;

  public void instrument(Object target,
                         Constructor<?> constructor,
                         Recorder recorder)
   throws NonInstrumentableTypeException {

    checkWrappable(constructor.getDeclaringClass());

    final String location = m_weaver.weave(constructor);

//    System.out.printf("register(%s, %s, %s, %s)%n",
//                      target.hashCode(), location,
//                      target,
//                      constructor);

    m_recorderRegistry.register(target, location, recorder);
  }

  public void instrument(Object target,
                         Method method,
                         TargetSource targetSource,
                         Recorder recorder)
    throws NonInstrumentableTypeException {

    checkWrappable(method.getDeclaringClass());

    final String location = m_weaver.weave(method, targetSource);

//    System.out.printf("register(%s, %s, %s, %s)%n",
//                      target.hashCode(), location,
//                      target,
//                      method);

    m_recorderRegistry.register(target, location, recorder);
  }

  protected static final void checkWrappable(Class<?> theClass)
    throws NonInstrumentableTypeException {

    if (!isInstrumentable(theClass)) {
      throw new NonInstrumentableTypeException("Cannot instrument " + theClass);
    }
  }

  protected static final boolean isInstrumentable(Class<?> targetClass) {

    // We disallow instrumentation of these classes to avoid the need for
    // complex protection against recursion in the engine itself.
    // Also, classes from the bootstrap classloader can't statically
    // refer to RecorderLocator.
    if (targetClass.getClassLoader() == BOOTSTRAP_CLASSLOADER) {
      return false;
    }

    // Package can be null.
    final Package thePackage = targetClass.getPackage();

    if (thePackage != null) {
      final String packageName = thePackage.getName();

      for (String prefix : NON_INSTRUMENTABLE_PACKAGES) {
        if (packageName.startsWith(prefix)) {
          return false;
        }
      }
    }

    return true;
  }
}
