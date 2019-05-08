package kong.unirest;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class WtfTest {
    @Test
    public void name() throws URISyntaxException {
        URI url = URI.create("http://localhost:4567/foo.csv");

        assertEquals("", url.getHost());
    }
}
