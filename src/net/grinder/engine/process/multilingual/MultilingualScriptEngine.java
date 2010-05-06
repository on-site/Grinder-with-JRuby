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

package net.grinder.engine.process.multilingual;

import java.util.HashMap;
import java.util.Map;

import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.engine.process.ScriptEngine;
import net.grinder.engine.process.jruby.JRubyScriptEngine;
import net.grinder.engine.process.jython.JythonScriptEngine;


/**
 * Allow Scripts of multiple languages to be used.  This basically
 * passes scripts on to the appropriate engine.  It may be necessary
 * to use homogeneous scripts, but ultimately it would be cool if they
 * could be heterogeneous.
 *
 * @author Mike Stone
 */
public final class MultilingualScriptEngine implements ScriptEngine {
    private static final Map<String, Class<? extends ScriptEngine>> engineTypes;

    static {
        engineTypes = new HashMap<String, Class<? extends ScriptEngine>>();
        engineTypes.put("py", JythonScriptEngine.class);
        engineTypes.put("jy", JythonScriptEngine.class);
        engineTypes.put("rb", JRubyScriptEngine.class);
        engineTypes.put("jrb", JRubyScriptEngine.class);
    }

    private ScriptEngine engine;

    @Override
    public void initialise(ScriptLocation script) throws EngineException {
        if (engine == null) {
            setupEngine(script);
        }

        engine.initialise(script);
    }

    private void setupEngine(ScriptLocation script) throws EngineException {
        String extension = getExtension(script);
        Class<? extends ScriptEngine> type = engineTypes.get(extension);

        if (type == null) {
            throw new EngineException("Cannot find engine for extension " + extension);
        }

        try {
            engine = type.newInstance();
        } catch (InstantiationException e) {
            throw new EngineException("Failed to instantiate engine of type " + type.getName() + " for extension " + extension, e);
        } catch (IllegalAccessException e) {
            throw new EngineException("Illegal access for engine of type " + type.getName() + " for extension " + extension, e);
        }
    }

    private String getExtension(ScriptLocation script) {
        String[] split = script.getFile().getName().split(".");

        // No extension...
        if (split.length < 2) {
            return "";
        }

        return split[split.length - 1];
    }

    @Override
    public WorkerRunnable createWorkerRunnable() throws EngineException {
        return engine.createWorkerRunnable();
    }

    @Override
    public WorkerRunnable createWorkerRunnable(Object testRunner) throws EngineException {
        return engine.createWorkerRunnable(testRunner);
    }

    @Override
    public void shutdown() throws EngineException {
        engine.shutdown();
    }

    @Override
    public String getDescription() {
        String version = "Multilingual 0.0.1";

        if (engine != null) {
            return version + ": " + engine.getDescription();
        }

        return version;
    }
}
