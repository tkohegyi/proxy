// ========================================================================
// $Id: LifeCycleThread.java,v 1.9 2005/08/13 00:01:28 gregwilkins Exp $
// Copyright 1999-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package net.lightbody.bmp.proxy.jetty.util;

import net.lightbody.bmp.proxy.jetty.log.LogFactory;
import org.apache.commons.logging.Log;

import java.io.InterruptedIOException;

/* ------------------------------------------------------------ */

/**
 * Base Thread class implementing LifeCycle.
 *
 * @author Greg Wilkins (gregw)
 * @version $Revision: 1.9 $
 */
public abstract class LifeCycleThread implements LifeCycle, Runnable {
    private static Log log = LogFactory.getLog(LifeCycleThread.class);

    private boolean _running;
    private boolean _daemon;
    private Thread _thread;

    /* ------------------------------------------------------------ */
    public boolean isDaemon() {
        return _daemon;
    }

    /* ------------------------------------------------------------ */
    public void setDaemon(boolean d) {
        _daemon = d;
    }

    /* ------------------------------------------------------------ */
    public Thread getThread() {
        return _thread;
    }

    /* ------------------------------------------------------------ */
    public boolean isStarted() {
        return _running;
    }

    /* ------------------------------------------------------------ */
    public synchronized void start()
            throws Exception {
        if (_running) {
            log.debug("Already started");
            return;
        }
        _running = true;
        if (_thread == null) {
            _thread = new Thread(this);
            _thread.setDaemon(_daemon);
        }
        _thread.start();
    }

    /* ------------------------------------------------------------ */

    /**
     *
     */
    public synchronized void stop()
            throws InterruptedException {
        _running = false;
        if (_thread != null) {
            _thread.interrupt();
            _thread.join();
        }
    }


    /* ------------------------------------------------------------ */

    /**
     *
     */
    public final void run() {
        try {
            while (_running) {
                try {
                    loop();
                } catch (InterruptedException e) {
                    LogSupport.ignore(log, e);
                } catch (InterruptedIOException e) {
                    LogSupport.ignore(log, e);
                } catch (Exception e) {
                    if (exception(e))
                        break;
                } catch (Error e) {
                    if (error(e))
                        break;
                }
            }
        } finally {
            _running = false;
        }
    }

    /* ------------------------------------------------------------ */

    /**
     * Handle exception from loop.
     *
     * @param e The exception
     * @return True of the loop should continue;
     */
    public boolean exception(Exception e) {
        log.warn(LogSupport.EXCEPTION, e);
        return true;
    }

    /* ------------------------------------------------------------ */

    /**
     * Handle error from loop.
     *
     * @param e The exception
     * @return True of the loop should continue;
     */
    public boolean error(Error e) {
        log.warn(LogSupport.EXCEPTION, e);
        return true;
    }

    /* ------------------------------------------------------------ */

    /**
     * @throws InterruptedException
     * @throws InterruptedIOException
     */
    public abstract void loop()
            throws InterruptedException,
            InterruptedIOException,
            Exception;

}
