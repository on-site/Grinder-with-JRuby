// Copyright (C) 2000 - 2009 Philip Aston
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

package net.grinder.common;

import java.io.PrintWriter;

import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.CallData;
import net.grinder.testutility.StubPrintWriter;
import net.grinder.testutility.RandomStubFactory;


public abstract class AbstractLoggerStubFactory<T extends Logger>
  extends RandomStubFactory<T> {

  private StubPrintWriter m_outputLogWriter = new StubPrintWriter();
  private StubPrintWriter m_errorLogWriter = new StubPrintWriter();

  protected AbstractLoggerStubFactory(Class<T> c) {
    super(c);
  }

  public T getLogger() {
    return getStub();
  }

  public StubPrintWriter getOutputLogWriter() {
    return m_outputLogWriter;
  }

  public StubPrintWriter getErrorLogWriter() {
    return m_errorLogWriter;
  }

  public PrintWriter override_getOutputLogWriter(Object proxy) {
    return getOutputLogWriter();
  }

  public PrintWriter override_getErrorLogWriter(Object proxy) {
    return getErrorLogWriter();
  }

  public CallData assertOutputMessage(String message) {
    final CallData callData = assertSuccess("output", String.class);
    assertEquals(message, callData.getParameters()[0]);
    return callData;
  }

  public CallData assertOutputMessageContains(String message) {
    final CallData callData = assertSuccess("output", String.class);
    AssertUtilities.assertContains(
      callData.getParameters()[0].toString(), message);
    return callData;
  }

  public CallData assertErrorMessage(String message) {
    final CallData callData = assertSuccess("error", String.class);
    assertEquals(message, callData.getParameters()[0]);
    return callData;
  }

  public CallData assertErrorMessageContains(String message) {
    final CallData callData = assertSuccess("error", String.class);
    AssertUtilities.assertContains(
      callData.getParameters()[0].toString(), message);
    return callData;
  }
}

