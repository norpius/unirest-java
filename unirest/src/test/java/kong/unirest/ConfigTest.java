/**
 * The MIT License
 *
 * Copyright for portions of unirest-java are held by Kong Inc (c) 2013.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package kong.unirest;

import kong.unirest.apache.*;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.core5.reactor.IOReactorStatus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ConfigTest {

    @Mock
    private CloseableHttpClient httpc;
    @Mock
    private PoolingHttpClientConnectionManager clientManager;
    @Mock
    private SyncIdleConnectionMonitorThread connMonitor;
    private MockCloseableClient asyncClient;
    @Mock
    private AsyncIdleConnectionMonitorThread asyncMonitor;
    @Mock
    private PoolingAsyncClientConnectionManager manager;

    @InjectMocks
    private Config config;

    @Before
    public void setUp() {
        asyncClient = new MockCloseableClient();
        asyncClient.start();
    }

    @Test
    public void shouldKeepConnectionTimeOutDefault(){
        assertEquals(Config.DEFAULT_CONNECT_TIMEOUT, config.getConnectionTimeout());
    }

    @Test
    public void shouldKeepSocketTimeoutDefault(){
        assertEquals(Config.DEFAULT_SOCKET_TIMEOUT, config.getSocketTimeout());
    }

    @Test
    public void shouldKeepMaxTotalDefault(){
        assertEquals(Config.DEFAULT_MAX_CONNECTIONS, config.getMaxConnections());
    }

    @Test
    public void shouldKeepMaxPerRouteDefault(){
        assertEquals(Config.DEFAULT_MAX_PER_ROUTE, config.getMaxPerRoutes());
    }

    @Test
    public void onceTheConfigIsRunningYouCannotChangeConfig(){
        config.httpClient(mock(org.apache.hc.client5.http.impl.classic.CloseableHttpClient.class));
        config.asyncClient(mock(CloseableHttpAsyncClient.class));

        TestUtil.assertException(() -> config.socketTimeout(533),
                UnirestConfigException.class,
                "Http Clients are already built in order to build a new config execute Unirest.config().reset() " +
                        "before changing settings. \n" +
                        "This should be done rarely.");

        Unirest.shutDown();
    }

    @Test @Ignore
    public void willNotRebuildIfNotClosableAsyncClient() {
        config.asyncClient(asyncClient);

        assertSame(asyncClient, config.getAsyncClient().getClient());
        assertSame(asyncClient, config.getAsyncClient().getClient());
    }

    @Test
    public void willRebuildIfEmpty() {
        assertSame(config.getAsyncClient(), config.getAsyncClient());
    }

    @Test
    public void willRebuildIfClosableAndStopped() {
        asyncClient.initiateShutdown();

        config.asyncClient(asyncClient);

        assertNotSame(asyncClient, config.getAsyncClient());
    }

    @Test
    public void testShutdown() throws IOException {
        asyncClient.start();

        Unirest.config()
                .httpClient(new ApacheClient(httpc, null, clientManager, connMonitor))
                .asyncClient(new ApacheAsyncClient(asyncClient, null, manager, asyncMonitor));

        Unirest.shutDown();

        verify(httpc).close();
        verify(clientManager).close();
        verify(connMonitor).interrupt();
        verify(asyncMonitor).interrupt();
        assertEquals(IOReactorStatus.INACTIVE, asyncClient.getStatus());
    }

    @Test
    public void willPowerThroughErrors() throws IOException {
        doThrow(new IOException("1")).when(httpc).close();
        doThrow(new RuntimeException("2")).when(clientManager).close();
        doThrow(new RuntimeException("3")).when(connMonitor).interrupt();
        asyncClient.throwOnClose(new RuntimeException("4"));
        doThrow(new RuntimeException("5")).when(asyncMonitor).interrupt();

        Unirest.config()
                .httpClient(new ApacheClient(httpc, null, clientManager, connMonitor))
                .asyncClient(new ApacheAsyncClient(asyncClient, null, manager, asyncMonitor));


        TestUtil.assertException(Unirest::shutDown,
                UnirestException.class,
                "java.io.IOException 1\n" +
                        "java.lang.RuntimeException 2\n" +
                        "java.lang.RuntimeException 3\n" +
                        "java.lang.RuntimeException 4\n" +
                        "java.lang.RuntimeException 5");

        verify(httpc).close();
        verify(clientManager).close();
        verify(connMonitor).interrupt();
        verify(asyncMonitor).interrupt();
    }

    private void asyncIsRunning() {
        when(asyncClient.getStatus()).thenReturn(IOReactorStatus.ACTIVE);
    }

    @Test
    public void doesNotBombOnNullOptions() throws IOException {
        asyncClient.start();

        Unirest.config()
                .httpClient(new ApacheClient(httpc, null, null, null))
                .asyncClient(new ApacheAsyncClient(asyncClient, null, null, null));

        Unirest.shutDown();

        verify(httpc).close();
        assertEquals(IOReactorStatus.INACTIVE, asyncClient.getStatus());
    }

    @Test
    public void ifTheNextAsyncClientThatIsReturnedIsAlsoOffThrowAnException(){
        AsyncClient c = mock(AsyncClient.class);
        when(c.isRunning()).thenReturn(false);
        config.asyncClient(g -> c);


        TestUtil.assertException(() -> config.getAsyncClient(),
                UnirestConfigException.class,
                "Attempted to get a new async client but it was not started. Please ensure it is");
    }

    @Test
    public void willNotRebuildIfRunning() {
        asyncClient.start();

        config.asyncClient(asyncClient);

        assertSame(asyncClient, config.getAsyncClient().getClient());
    }

    @Test
    public void provideYourOwnClientBuilder() {
        Client cli = mock(Client.class);

        config.httpClient(c -> cli);

        assertSame(cli, config.getClient());
    }

    @Test
    public void canSignalForShutdownHook() {
        assertFalse(config.shouldAddShutdownHook());
        config.addShutdownHook(true);
        assertTrue(config.shouldAddShutdownHook());
    }

    @Test
    public void canDisableGZipencoding() {
        assertTrue(config.isRequestCompressionOn());
        config.requestCompression(false);
        assertFalse(config.isRequestCompressionOn());

    }

    @Test
    public void canDisableAuthRetry() {
        assertTrue(config.isAutomaticRetries());
        config.automaticRetries(false);
        assertFalse(config.isAutomaticRetries());
    }

    @Test
    public void provideYourOwnAsyncClientBuilder() {
        AsyncClient cli = mock(AsyncClient.class);
        when(cli.isRunning()).thenReturn(true);

        config.asyncClient(c -> cli);

        assertSame(cli, config.getAsyncClient());
    }

    @Test
    public void canSetProxyViaSetter() {
        config.proxy(new Proxy("localhost", 8080, "ryan", "password"));
        assertProxy("localhost", 8080, "ryan", "password");

        config.proxy("local2", 8888);
        assertProxy("local2", 8888, null, null);

        config.proxy("local3", 7777, "barb", "12345");
        assertProxy("local3", 7777, "barb", "12345");
    }

    private void assertProxy(String host, Integer port, String username, String password) {
        assertEquals(host, config.getProxy().getHost());
        assertEquals(port, config.getProxy().getPort());
        assertEquals(username, config.getProxy().getUsername());
        assertEquals(password, config.getProxy().getPassword());
    }
}