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

import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.engine.process.ScriptEngine;


/**
 * Allow Scripts of multiple languages to be used.  This basically
 * passes scripts on to the appropriate engine.  It may be necessary
 * to use homogeneous scripts, but ultimately it would be cool if they
 * could be heterogeneous.
 *
 * @author Mike Stone
 */
public final class MultilingualScriptEngine implements ScriptEngine {
    @Override
    public void initialise(ScriptLocation script) throws EngineException {
    }

    @Override
    public WorkerRunnable createWorkerRunnable() throws EngineException {
        return null;
    }

    @Override
    public WorkerRunnable createWorkerRunnable(Object testRunner) throws EngineException {
        return null;
    }

    @Override
    public void shutdown() throws EngineException {
    }

    @Override
    public String getDescription() {
        return "Multilingual 0.0.1";
    }
}
