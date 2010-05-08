// Copyright (C) 2010 Mike Stone
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

import java.lang.reflect.Array;
import java.util.List;


/**
 * Simple utility method(s) for use within JRuby test scripts.
 *
 * @author Mike Stone
 */
public class JRubyUtil {
    /**
     * Why do arrays in JRuby have to be so painful...?
     *
     * @param list The list to convert to an array.
     * @param type The type to convert the array to.
     */
    public static Object array(List<?> list, Class<?> type) {
        Object array = Array.newInstance(type, list.size());

        for (int i = 0; i < list.size(); i++) {
            Object value = list.get(i);
            Array.set(array, i, value);
        }

        return array;
    }

    /**
     * Wrap the given callback via the given test and return the
     * result.  The result is actually a new Callback instance to
     * ensure that it is wrapped properly (wrapping a JRuby object
     * doesn't seem to work so well... it often won't record the
     * method properly, either something extra is invoked, or nothing
     * is recorded).
     *
     * @param test The test to wrap with.
     * @param callback The callback to wrap.
     * @return The new wrapped callback.
     */
    public static Object wrap(Test test, final Callback callback) throws NotWrappableTypeException {
        Callback recorded = new Callback() {
            public Object call(Object arg) {
                return callback.call(arg);
            }
        };

        return test.wrap(recorded);
    }

    /**
     * Record the given callback via the given test and return the
     * result.  The result is actually a new Callback instance to
     * ensure that it is recorded properly (recording a JRuby object
     * doesn't seem to work so well... it often won't record the
     * method properly, either something extra is invoked, or nothing
     * is recorded).
     *
     * @param test The test to record with.
     * @param callback The callback to record.
     * @return The new recorded callback.
     */
    public static Callback record(Test test, final Callback callback) throws NonInstrumentableTypeException {
        Callback recorded = new Callback() {
            public Object call(Object arg) {
                return callback.call(arg);
            }
        };

        test.record(recorded);
        return recorded;
    }

    /**
     * A simple callback interface to make it easy to pass in a
     * closure from the JRuby side.
     */
    public static interface Callback {
        /**
         * Invoke the closure with the given argument.
         *
         * @param arg The parameter that the closure may need.
         * @return Whatever the closure wants to return.
         */
        Object call(Object arg);
    }
}
