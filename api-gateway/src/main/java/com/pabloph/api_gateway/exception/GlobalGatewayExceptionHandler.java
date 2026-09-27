package com.pabloph.api_gateway.exception;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

@Component
@Order(-2)
public class GlobalGatewayExceptionHandler implements WebExceptionHandler {

    private final GatewayErrorResponseWriter errorResponseWriter;

    public GlobalGatewayExceptionHandler(GatewayErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
        if (exception instanceof ResponseStatusException responseStatusException) {
            HttpStatus status = HttpStatus.valueOf(responseStatusException.getStatusCode().value());
            String message = responseStatusException.getReason() != null
                    ? responseStatusException.getReason()
                    : status.getReasonPhrase();
            return errorResponseWriter.write(exchange, status, message);
        }

        if (hasCause(exception, ConnectException.class)) {
            return errorResponseWriter.write(exchange, HttpStatus.SERVICE_UNAVAILABLE, "Upstream service is unavailable");
        }

        if (hasCause(exception, TimeoutException.class)) {
            return errorResponseWriter.write(exchange, HttpStatus.GATEWAY_TIMEOUT, "Upstream service timed out");
        }

        return errorResponseWriter.write(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected gateway error");
    }

    private boolean hasCause(Throwable exception, Class<? extends Throwable> causeType) {
        Throwable current = exception;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
