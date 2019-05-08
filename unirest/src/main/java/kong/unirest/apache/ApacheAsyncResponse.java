package kong.unirest.apache;

import kong.unirest.*;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.stream.Stream;

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
        return r.getBodyBytes();
    }

    @Override
    public String getContentAsString() {
        return r.getBodyText();
    }

    @Override
    public String getContentAsString(String charset) {
        try {
            if(charset == null){
                return r.getBodyText();
            }
            return new String(r.getBodyBytes(), charset);
        } catch (UnsupportedEncodingException e) {
            throw new UnirestException(e);
        }
    }

    @Override
    public InputStreamReader getContentReader() {
        return null;
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
        return r.getContentType().getCharset().name();
    }
}
