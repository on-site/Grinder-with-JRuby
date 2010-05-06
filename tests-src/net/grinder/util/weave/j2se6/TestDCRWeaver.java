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

package net.grinder.util.weave.j2se6;

import static net.grinder.testutility.AssertUtilities.assertArraysEqual;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.weave.Weaver;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.Weaver.TargetSource;
import net.grinder.util.weave.j2se6.DCRWeaver.ClassFileTransformerFactory;


/**
 * Unit test for {@link DCRWeaver}.
 * TestDCRWeaver.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestDCRWeaver extends TestCase {
  final RandomStubFactory<ClassFileTransformerFactory>
    m_classFileTransformerFactoryStubFactory =
      RandomStubFactory.create(ClassFileTransformerFactory.class);
  final ClassFileTransformerFactory m_classFileTransformerFactory =
    m_classFileTransformerFactoryStubFactory.getStub();

  final RandomStubFactory<Instrumentation> m_instrumentationStubFactory =
    RandomStubFactory.create(Instrumentation.class);
  final Instrumentation m_instrumentation =
    m_instrumentationStubFactory.getStub();

  @SuppressWarnings("unused")
  private void myMethod() {
  }

  @SuppressWarnings("unused")
  private void myOtherMethod() {
  }

  public void testMethodRegistration() throws Exception {
    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory,
                                        m_instrumentation);

    final CallData createCall =
      m_classFileTransformerFactoryStubFactory.assertSuccess(
        "create", PointCutRegistry.class);
    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();

    final Object transformer = createCall.getResult();

    m_instrumentationStubFactory.assertSuccess(
      "addTransformer", transformer, true);
    m_instrumentationStubFactory.assertNoMoreCalls();

    final Method method = getClass().getDeclaredMethod("myMethod");

    final String l1 = weaver.weave(method, TargetSource.FIRST_PARAMETER);
    final String l2 = weaver.weave(method, TargetSource.FIRST_PARAMETER);
    assertEquals(l1, l2);

    weaver.weave(method, TargetSource.FIRST_PARAMETER);

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();
    m_instrumentationStubFactory.assertNoMoreCalls();

    final PointCutRegistry pointCutRegistry =
      (PointCutRegistry) createCall.getParameters()[0];

    final String internalClassName = getClass().getName().replace('.', '/');

    final Map<Constructor<?>, List<WeavingDetails>> constructorPointCuts =
      pointCutRegistry.getConstructorPointCutsForClass(internalClassName);

    assertNull(constructorPointCuts);

    final Map<Method, List<WeavingDetails>> methodPointCuts =
      pointCutRegistry.getMethodPointCutsForClass(internalClassName);

    assertEquals(1, methodPointCuts.size());

    final List<WeavingDetails> locations1 = methodPointCuts.get(method);
    assertEquals(1, locations1.size());
    final String location1 = locations1.get(0).getLocation();
    assertNotNull(location1);

    final Method method2 = getClass().getDeclaredMethod("myOtherMethod");

    weaver.weave(method, TargetSource.FIRST_PARAMETER);
    weaver.weave(method2, TargetSource.FIRST_PARAMETER);

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();
    m_instrumentationStubFactory.assertNoMoreCalls();

    final Map<Method, List<WeavingDetails>> pointCuts2 =
      pointCutRegistry.getMethodPointCutsForClass(internalClassName);

    assertEquals(2, pointCuts2.size());

    final List<WeavingDetails> locations2 = pointCuts2.get(method);
    assertEquals(1, locations2.size());

    assertEquals(new WeavingDetails(location1, TargetSource.FIRST_PARAMETER),
                 locations2.get(0));
    assertNotNull(pointCuts2.get(method2));
  }

  public void testConstructorRegistration() throws Exception {
    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory,
                                        m_instrumentation);

    final CallData createCall =
      m_classFileTransformerFactoryStubFactory.assertSuccess(
        "create", PointCutRegistry.class);
    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();

    final Constructor<?> constructor = getClass().getDeclaredConstructor();

    final String l1 = weaver.weave(constructor);
    final String l2 = weaver.weave(constructor);
    assertEquals(l1, l2);

    weaver.weave(constructor);

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();

    final PointCutRegistry pointCutRegistry =
      (PointCutRegistry) createCall.getParameters()[0];

    final String internalClassName = getClass().getName().replace('.', '/');

    final Map<Constructor<?>, List<WeavingDetails>> constructorPointCuts =
      pointCutRegistry.getConstructorPointCutsForClass(internalClassName);

    assertEquals(1, constructorPointCuts.size());

    final Map<Method, List<WeavingDetails>> methodPointCuts =
      pointCutRegistry.getMethodPointCutsForClass(internalClassName);

    assertNull(methodPointCuts);

    final List<WeavingDetails> locations1 =
      constructorPointCuts.get(constructor);

    assertEquals(1, locations1.size());

    final String location1 = locations1.get(0).getLocation();
    assertNotNull(location1);
  }

  public void testWeavingWithInstrumentation() throws Exception {
    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory,
                                        m_instrumentation);

    final CallData createCall =
      m_classFileTransformerFactoryStubFactory.assertSuccess(
        "create", PointCutRegistry.class);
    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();

    final Object transformer = createCall.getResult();

    m_instrumentationStubFactory.assertSuccess(
      "addTransformer", transformer, true);
    m_instrumentationStubFactory.assertNoMoreCalls();

    final Method method = getClass().getDeclaredMethod("myMethod");
    weaver.weave(method, TargetSource.FIRST_PARAMETER);

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();

    weaver.applyChanges();

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();

    final CallData retransformClassesCall =
      m_instrumentationStubFactory.assertSuccess("retransformClasses",
                                                 new Class[0].getClass());
    assertArraysEqual((Class[])retransformClassesCall.getParameters()[0],
                      new Class[] { getClass(),});

    m_instrumentationStubFactory.assertNoMoreCalls();

    weaver.weave(method, TargetSource.FIRST_PARAMETER);
    weaver.applyChanges();

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();
    m_instrumentationStubFactory.assertNoMoreCalls();
  }

  public void testWeavingWithBadInstrumentation() throws Exception {
    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory,
                                        m_instrumentation);

    final Method method = getClass().getDeclaredMethod("myMethod");

    weaver.weave(method, TargetSource.FIRST_PARAMETER);
    weaver.weave(method, TargetSource.FIRST_PARAMETER);

    final Exception uce = new UnmodifiableClassException();
    m_instrumentationStubFactory.setThrows("retransformClasses", uce);

    try {
      weaver.applyChanges();
      fail("Expected WeavingException");
    }
    catch (WeavingException e) {
      assertSame(e.getCause(), uce);
    }
  }
}
