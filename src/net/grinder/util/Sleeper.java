// Copyright (C) 2006 Philip Aston
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

import net.grinder.common.GrinderException;


/**
 * Something that can sleep.
 *
 * @author Philip Aston
 * @version $Revision: 3873 $
 */
public interface Sleeper extends TimeAuthority {

  /**
   * Shutdown this <code>Sleeper</code>. Once called, all sleep
   * method invocations will throw {@link ShutdownException},
   * including those already sleeping.
   */
  void shutdown();

  /**
   * Sleep for a time based on the meanTime parameter. The actual
   * time is taken from a pseudo normal distribution. Approximately
   * 99.75% of times will be within (100* limit9975Factor) percent
   * of the meanTime.
   *
   * @param meanTime Mean time.
   * @throws ShutdownException If this <code>Sleeper</code> has been shutdown.
   */
  void sleepNormal(long meanTime) throws ShutdownException;

  /**
   * Sleep for a random time drawn from a pseudo normal distribution.
   *
   * @param meanTime Mean time.
   * @param sigma Standard deviation.
   * @throws ShutdownException If this <code>Sleeper</code> has been shutdown.
   */
  void sleepNormal(long meanTime, long sigma) throws ShutdownException;

  /**
   * Sleep for a time based on the maximumTime parameter. The actual
   * time is taken from a pseudo random flat distribution between 0
   * and maximumTime.
   *
   * @param maximumTime Maximum time.
   * @throws ShutdownException If this <code>Sleeper</code> has been shutdown.
   */
  void sleepFlat(long maximumTime) throws ShutdownException;

  /**
   * Exception used to indicate that a Sleeper has been shutdown.
   */
  public static final class ShutdownException extends GrinderException {

    /**
     * Constructor. Public for unit tests.
     *
     * @param message Why we were shut down.
     */
    public ShutdownException(String message) {
      super(message);
    }
  }
}
