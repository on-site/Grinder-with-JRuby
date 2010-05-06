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

import static extra166y.CustomConcurrentHashMap.IDENTITY;
import static extra166y.CustomConcurrentHashMap.STRONG;
import static extra166y.CustomConcurrentHashMap.WEAK;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.grinder.common.UncheckedGrinderException;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.ScriptEngine.Recorder;
import extra166y.CustomConcurrentHashMap;


/**
 * Static methods that weaved code uses to dispatch enter and exit calls to the
 * appropriate {@link Recorder}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public final class RecorderLocator implements RecorderRegistry {

  private static final RecorderLocator s_instance = new RecorderLocator();

  /**
   * Accessor for the unit tests.
   */
  static void clearRecorders() {
    s_instance.m_recorders.clear();
  }

  /**
   * Target reference -> location -> recorder list. Location strings are
   * interned, so we use an identity hash map for both maps. We use concurrent
   * structures throughout to avoid synchronisation. The target reference is the
   * first key to minimise the cost of traversing woven code for
   * non-instrumented references, which is important if {@code Object}, {@code
   * PyObject}, etc. are instrumented.
   */
  private final ConcurrentMap<Object,
                              ConcurrentMap<String, List<Recorder>>>
  m_recorders =
      new CustomConcurrentHashMap<Object,
                                  ConcurrentMap<String, List<Recorder>>>(
            WEAK, IDENTITY, STRONG, IDENTITY, 101);

  private List<Recorder> getRecorderList(Object target,
                                         String locationID) {

    final ConcurrentMap<String, List<Recorder>> locationMap =
      m_recorders.get(target);

    if (locationMap != null) {
      final List<Recorder> list = locationMap.get(locationID);

      if (list != null) {
        return list;
      }
    }

    return Collections.<Recorder>emptyList();
  }

  /**
   * Called when a weaved method is entered.
   *
   * @param target
   *          The reference used to call the method. The class is used for
   *          static methods or constructors.
   * @param location
   *          Unique identity generated when the method was instrumented.
   *          Will be interned.
   * @throws EngineException
   */
  public static void enter(Object target, String location) {

    if (target == null) {
      // We don't allow recorders to register for a null target,
      // but weaved code can be called with null.
      return;
    }

    // Beware when enabling the following logging - calls on the object itself
    // may fail subtly.
//   System.out.printf("enter(%s, %s, %s)%n",
//                     System.identityHashCode(target),
//                     target.getClass(),
//                     location);

    try {
      for (Recorder recorder : s_instance.getRecorderList(target, location)) {

//        System.out.printf(" -> %s%n", System.identityHashCode(recorder));

        recorder.start();
      }
    }
    catch (EngineException e) {
      throw new RecordingFailureException(e);
    }
  }

  /**
   * Called when a weaved method is exited.
   *
   * @param target
   *          The reference used to call the method. The class is used for
   *          static methods or constructors.
   * @param location
   *          Unique identity generated when the method was instrumented.
   *          Will be interned.
   * @param success
   *          {@code true} if the exit was a normal return, {code false} if an
   *          exception was thrown.
   */
  public static void exit(Object target, String location, boolean success) {

    if (target == null) {
      // We don't allow recorders to register for a null target,
      // but weaved code can be called with null.
      return;
    }

    // Beware when enabling the following logging - calls on the object itself
    // may fail subtly.
//    System.out.printf("exit(%s, %s, %s, %s)%n",
//                      System.identityHashCode(target),
//                      target.getClass(),
//                      location,
//                      success);

    final List<Recorder> recorders =
      s_instance.getRecorderList(target, location);

    // Iterate over recorders in reverse.
    final ListIterator<Recorder> i = recorders.listIterator(recorders.size());

    try {
      while (i.hasPrevious()) {
        final Recorder recorder = i.previous();

//        System.out.printf(" -> %s%n", System.identityHashCode(recorder));

        recorder.end(success);
      }
    }
    catch (EngineException e) {
      throw new RecordingFailureException(e);
    }
  }

  /**
   * Expose our registry to the package.
   *
   * @return The registry.
   */
  static RecorderRegistry getRecorderRegistry() {
    return s_instance;
  }

  /**
   * {@inheritDoc}.
   */
  public void register(Object target, String location, Recorder recorder) {

    // We will create and quickly discard many maps and lists here to avoid
    // needing to lock the ConcurrentMaps. It is important that the
    // enter/exit methods are lock free, the instrumentation registration
    // process can be relatively slow.

    final ConcurrentMap<String, List<Recorder>> newMap =
      new CustomConcurrentHashMap<String, List<Recorder>>(
            STRONG, IDENTITY, STRONG, IDENTITY, 0);

    final ConcurrentMap<String, List<Recorder>> oldMap =
      m_recorders.putIfAbsent(target, newMap);

    final ConcurrentMap<String, List<Recorder>> locationMap =
      oldMap != null ? oldMap : newMap;

    final List<Recorder> newList = new CopyOnWriteArrayList<Recorder>();

    final List<Recorder> oldList =
      locationMap.putIfAbsent(location.intern(), newList);

    (oldList != null ? oldList : newList).add(recorder);
  }

  private static final class RecordingFailureException
    extends UncheckedGrinderException {

    private RecordingFailureException(EngineException cause) {
      super("Recording Failure", cause);
    }
  }
}
