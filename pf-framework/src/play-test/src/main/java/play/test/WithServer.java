/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.test;

import org.junit.After;
import org.junit.Before;

/**
 * Provides a server to JUnit tests. Make your test class extend this class and an HTTP server will be started before each test is invoked.
 * You can setup the fake application and port to use by overriding the provideFakeApplication and providePort methods.
 * Within a test, the running application and the TCP port are available through the app and port fields, respectively.
 */
public class WithServer {

    protected FakeApplication app;
    protected int port;
    protected TestServer testServer;

    /**
     * Override this method to setup the fake application to use.
     *
     * @return The fake application used by the server
     */
    protected FakeApplication provideFakeApplication() {
        return Helpers.fakeApplication();
    }

    /**
     * Override this method to setup the port to use.
     *
     * @return The TCP port used by the server
     */
    protected int providePort() {
        return play.api.test.Helpers.testServerPort();
    }

    @Before
    public void startServer() {
        if (testServer != null) {
            testServer.stop();
        }
        app = provideFakeApplication();
        port = providePort();
        testServer = Helpers.testServer(port, app);
        testServer.start();
    }

    @After
    public void stopServer() {
        if (testServer != null) {
            testServer.stop();
            testServer = null;
            app = null;
        }
    }
}
