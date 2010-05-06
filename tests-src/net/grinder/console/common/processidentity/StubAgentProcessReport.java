// Copyright (C) 2008 Philip Aston
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

package net.grinder.console.common.processidentity;

import net.grinder.common.processidentity.AgentIdentity;
import net.grinder.common.processidentity.ProcessIdentity;
import net.grinder.messages.agent.CacheHighWaterMark;
import net.grinder.messages.console.AgentAndCacheReport;


public final class StubAgentProcessReport implements AgentAndCacheReport {

  private final AgentIdentity m_identity;
  private final short m_state;
  private CacheHighWaterMark m_cacheHighWaterMark = null;

  public StubAgentProcessReport(AgentIdentity identity, short state) {
    m_identity = identity;
    m_state = state;
  }

  public AgentIdentity getAgentIdentity() {
    return m_identity;
  }

  public ProcessIdentity getIdentity() {
    return m_identity;
  }

  public short getState() {
    return m_state;
  }

  public CacheHighWaterMark getCacheHighWaterMark() {
    return m_cacheHighWaterMark;
  }

  public void setCacheHighWaterMark(CacheHighWaterMark highWaterMark) {
    m_cacheHighWaterMark = highWaterMark;
  }
}