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

import kong.unirest.Config;
import kong.unirest.Headers;
import kong.unirest.RawResponseBase;
import kong.unirest.UnirestException;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.*;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

class ApacheResponse extends RawResponseBase {
    private final CloseableHttpResponse r;

    public ApacheResponse(CloseableHttpResponse r, Config config) {
        super(config);
        this.r = r;
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
        try {
            HttpEntity entity = r.getEntity();
            if (entity != null) {
                return entity.getContent();
            }
            return new ByteArrayInputStream(new byte[0]);
        } catch (IOException e) {
            throw new UnirestException(e);
        }
    }

    @Override
    public byte[] getContentAsBytes() {
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
        } finally {
            EntityUtils.consumeQuietly(r.getEntity());
        }
    }

    @Override
    public String getContentAsString() {
        return getContentAsString(null);
    }

    @Override
    public String getContentAsString(String charset) {
        if (!hasContent()) {
            return "";
        }
        try {
            String charSet = getCharset(charset);
            return new String(getContentAsBytes(), charSet);
        } catch (IOException e) {
            throw new UnirestException(e);
        }
    }

    private String getCharset(String charset) {
        if (charset == null || charset.trim().isEmpty()) {
            return getCharSet();
        }
        return charset;
    }

    @Override
    public InputStreamReader getContentReader() {
        return new InputStreamReader(getContent());
    }

    @Override
    public boolean hasContent() {
        return r.getEntity() != null;
    }

    @Override
    public String getContentType() {
        if (hasContent()) {
            String contentType = r.getEntity().getContentType();
            if (contentType != null) {
                return contentType;
            }
        }
        return "";
    }

    @Override
    public String getEncoding() {
        if (hasContent()) {
            String contentType = r.getEntity().getContentEncoding();
            if (contentType != null) {
                return contentType;
            }
        }
        return "";
    }
}
