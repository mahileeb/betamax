/*
 * Copyright 2011 Rob Fletcher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.freeside.betamax.proxy.jetty

import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.component.AbstractLifeCycle.AbstractLifeCycleListener
import org.eclipse.jetty.util.component.LifeCycle

import java.util.concurrent.CountDownLatch
import java.util.logging.Logger

class SimpleServer extends AbstractLifeCycleListener {

    static final int DEFAULT_PORT = 5000

    final String host
    protected int port
    private Server server
    private CountDownLatch startedLatch
    private CountDownLatch stoppedLatch

    private static final log = Logger.getLogger(getClass().name)

    SimpleServer() {
        this(DEFAULT_PORT)
    }

    SimpleServer(int port) {
        // if there is no network connection we need to connect to the server via hostname rather than IP. At least this
        // is true on a Mac. Suspect it may be OS dependent. I should really make an effort to understand this but this
        // workaround means the proxy seems to operate correctly with or without a network connection. The weird thing
        // is that the hostName _doesn't_ work when there _is_ a network connection.
        try {
            def localAddress = InetAddress.localHost
            if (localAddress.loopbackAddress) {
                log.info "local address is loopback, using hostname $localAddress.hostName"
                host = localAddress.hostName
            } else {
                host = localAddress.hostAddress
            }
        } catch (UnknownHostException e) {
            log.info "unable to determine local host name, using localhost"
            host = 'localhost'
        }
        this.port = port
    }

    String getUrl() {
        "http://$host:$port/"
    }

    void start(Class<? extends Handler> handlerClass) {
        start handlerClass.newInstance()
    }

    void start(Handler handler) {
        startedLatch = new CountDownLatch(1)
        stoppedLatch = new CountDownLatch(1)

        server = createServer(port)
        server.handler = handler
        server.addLifeCycleListener(this)
        server.start()

        startedLatch.await()
    }

    void stop() {
        if (server) {
            server.stop()
            stoppedLatch.await()
        }
    }

    void setPort(int port) {
        if (running) {
            throw new IllegalStateException('Cannot set port once the server is already started')
        }
        this.port = port
    }

    int getPort() {
        port
    }

    boolean isRunning() {
        startedLatch?.count == 0 && stoppedLatch?.count > 0
    }

    @Override
    void lifeCycleStarted(LifeCycle event) {
        log.fine 'started...'
        startedLatch.countDown()
    }

    @Override
    void lifeCycleStopped(LifeCycle event) {
        log.fine 'stopped...'
        stoppedLatch.countDown()
    }

    protected Server createServer(int port) {
        new Server(port)
    }

}
