package com.example.apigateway.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestControllerAdvice
// This annotation marks the class to provide global exception handling across all @Controller and @RestController classes.
// It is the reactive version of @ControllerAdvice.
public class JwtValidationException {

    @ExceptionHandler(WebClientResponseException.class)
    // This method is triggered whenever a WebClientResponseException (e.g., a 4xx or 5xx error from an upstream service)
    // is thrown, typically after a failed JWT validation call to the auth service.
    public Mono<Void> handleUnauthorizedException(ServerWebExchange exchange) {

        // Set the HTTP response status code for the client to 401 Unauthorized.
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

        // Complete the response chain and immediately send the response back to the client.
        // Returning Mono<Void> is standard for reactive error handlers that terminate the request.
        return exchange.getResponse().setComplete();
    }
}
