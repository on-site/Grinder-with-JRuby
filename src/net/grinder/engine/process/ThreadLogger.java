// Copyright (C) 2001, 2002, 2003, 2004 Philip Aston
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

package net.grinder.engine.process;

import net.grinder.common.Logger;


/**
 * A {@link Logger} that knows some thread specific context.
 *
 * @author Philip Aston
 * @version $Revision: 3832 $
 */
interface ThreadLogger extends Logger {
  /**
   * @return The thread number.
   */
  int getThreadNumber();

  /**
   * @return The current run number. -1 indicates that there is
   * no current run.
   */
  int getCurrentRunNumber();

  /**
   * @param run The current run number. Pass -1 to indicate that there
   * is no current run.
   */
  void setCurrentRunNumber(int run);

  /**
   * @return The current test number. -1 indicates that there is
   * no current test.
   */
  int getCurrentTestNumber();

  /**
   * @param testNumber The current test number. Pass -1 to indicate
   * that there is no current test.
   */
  void setCurrentTestNumber(int testNumber);
}
