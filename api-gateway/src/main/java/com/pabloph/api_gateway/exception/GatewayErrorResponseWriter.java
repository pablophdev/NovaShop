package com.pabloph.api_gateway.exception;

import com.pabloph.api_gateway.dto.ErrorResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
public class GatewayErrorResponseWriter {

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value(),
                null
        );

        byte[] bytes = toJson(errorResponse).getBytes();

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String toJson(ErrorResponse errorResponse) {
        return "{\"timestamp\":\"" + escape(errorResponse.timestamp().toString())
                + "\",\"status\":" + errorResponse.status()
                + ",\"error\":\"" + escape(errorResponse.error())
                + "\",\"message\":\"" + escape(errorResponse.message())
                + "\",\"path\":\"" + escape(errorResponse.path())
                + "\",\"validationErrors\":null}";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
