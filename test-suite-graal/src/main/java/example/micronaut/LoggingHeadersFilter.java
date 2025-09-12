package example.micronaut;

import io.micronaut.core.order.Ordered;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.ServerHttpRequest;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.body.CloseableByteBody;
import io.micronaut.http.filter.ServerFilterPhase;
import io.micronaut.http.util.HttpHeadersUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static io.micronaut.http.annotation.Filter.MATCH_ALL_PATTERN;

@ServerFilter(MATCH_ALL_PATTERN)
class LoggingHeadersFilter implements Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingHeadersFilter.class);

    @RequestFilter
    void filterRequest(HttpRequest<?> request) {
        LOG.trace("{} {}", request.getMethod(), request.getUri());
        HttpHeadersUtil.trace(LOG, request.getHeaders());
        if (request instanceof ServerHttpRequest<?> serverHttpRequest) {
            try (CloseableByteBody byteBody = serverHttpRequest.byteBody().split()) {
                try (InputStream inputStream = byteBody.toInputStream()) {
                    String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    LOG.trace("Body: {}", body);
                } catch (IOException e) {
                    LOG.warn("Could not read body", e);
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return ServerFilterPhase.FIRST.order();
    }
}