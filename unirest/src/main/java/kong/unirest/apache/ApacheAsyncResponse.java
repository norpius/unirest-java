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

import kong.unirest.*;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;

import java.io.*;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

class ApacheAsyncResponse extends RawResponseBase {

    private final SimpleHttpResponse r;

    ApacheAsyncResponse(SimpleHttpResponse response, Config config) {
        super(config);
        this.r = response;
    }

    @Override
    public int getStatus() {
        return r.getCode();
    }

    @Override
    public String getStatusText() {
        return r.getReasonPhrase();
    }

    @Override
    public Headers getHeaders() {
        Headers h = new Headers();
        Stream.of(r.getHeaders())
                .forEachOrdered(e -> h.add(e.getName(), e.getValue()));
        return h;
    }

    @Override
    public InputStream getContent() {
        return new ByteArrayInputStream(r.getBodyBytes());
    }

    @Override
    public byte[] getContentAsBytes() {
        return getUnzipped();
    }

    @Override
    public String getContentAsString() {
        return new String(getUnzipped());
    }

    private byte[] getUnzipped(){
        if (!hasContent()) {
            return new byte[0];
        }
        try {
            InputStream is = getContent();
            if (isGzipped(getEncoding())) {
                is = new GZIPInputStream(getContent());
            }
            return getBytes(is);
        } catch (IOException e2) {
            throw new UnirestException(e2);
        }
    }

    @Override
    public String getContentAsString(String charset) {
        try {
            if(charset == null){
                return getContentAsString();
            }
            return new String(r.getBodyBytes(), charset);
        } catch (UnsupportedEncodingException e) {
            throw new UnirestException(e);
        }
    }

    @Override
    public InputStreamReader getContentReader() {
        return new InputStreamReader(getContent());
    }

    @Override
    public boolean hasContent() {
        return r.getContentType() != null;
    }

    @Override
    public String getContentType() {
        return r.getContentType().getMimeType();
    }

    @Override
    public String getEncoding() {
        return Optional.ofNullable(r.getFirstHeader("Content-Encoding"))
                .map(h -> h.getValue())
                .orElse("");
    }
}
