// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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

package net.grinder.plugininterface;

import net.grinder.common.GrinderException;
import net.grinder.script.Grinder;
import net.grinder.statistics.StatisticsServices;
import net.grinder.util.TimeAuthority;


/**
 * <p>This class is used to share process information between the
 * Grinder and the plug-in.</p>
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision: 3762 $
 */
public interface PluginProcessContext {

  /**
   * Returns the script context.
   *
   * @return The script context.
   */
  Grinder.ScriptContext getScriptContext();

  /**
   * Returns the {@link PluginThreadListener} for the current thread.
   * Instances are created by the plug-in's implementation of {@link
   * GrinderPlugin#createThreadListener}.
   *
   * @return The thread listener for the current thread.
   * @exception GrinderException If the thread listener could not be obtained.
   */
  PluginThreadListener getPluginThreadListener() throws GrinderException;

  /**
   * Returns an interface through which statistics services can be obtained.
   *
   * @return A {@link StatisticsServices}.
   */
  StatisticsServices getStatisticsServices();

  /**
   * Something that knows the time.
   *
   * @return The time authority.
   */
  TimeAuthority getTimeAuthority();
}
