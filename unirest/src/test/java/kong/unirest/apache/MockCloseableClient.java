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

package kong.unirest.apache;

import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.nio.AsyncPushConsumer;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.HandlerFactory;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.ExceptionEvent;
import org.apache.hc.core5.reactor.IOReactorStatus;
import org.apache.hc.core5.util.TimeValue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

public class MockCloseableClient extends CloseableHttpAsyncClient {
    private IOReactorStatus status = IOReactorStatus.INACTIVE;
    private RuntimeException closeException;

    @Override
    public void start() {
        status = IOReactorStatus.ACTIVE;
    }

    @Override
    public IOReactorStatus getStatus() {
        return status;
    }

    @Override
    public List<ExceptionEvent> getExceptionLog() {
        return Collections.emptyList();
    }

    @Override
    public void awaitShutdown(TimeValue waitTime) throws InterruptedException {

    }

    @Override
    public void initiateShutdown() {
        setInactive();
    }

    @Override
    public void shutdown(CloseMode closeMode) {
        setInactive();
    }

    @Override
    public void register(String hostname, String uriPattern, Supplier<AsyncPushConsumer> supplier) {

    }

    @Override
    public void close() throws IOException {
        setInactive();
    }

    private void setInactive() {
        if(closeException != null){
            throw closeException;
        }
        status = IOReactorStatus.INACTIVE;
    }

    @Override
    public <T> Future<T> execute(AsyncRequestProducer requestProducer, AsyncResponseConsumer<T> responseConsumer, HandlerFactory<AsyncPushConsumer> pushHandlerFactory, HttpContext context, FutureCallback<T> callback) {
        return null;
    }

    public void throwOnClose(RuntimeException e) {
        this.closeException = e;
    }
}
