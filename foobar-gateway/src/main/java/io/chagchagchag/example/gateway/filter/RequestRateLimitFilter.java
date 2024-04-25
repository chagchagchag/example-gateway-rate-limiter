package io.chagchagchag.example.gateway.filter;

import java.nio.charset.StandardCharsets;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.support.HasRouteId;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RequestRateLimitFilter extends AbstractGatewayFilterFactory<RequestRateLimitFilter.Config> {
  private final RateLimiter<RedisRateLimiter.Config> rateLimiter;

  public RequestRateLimitFilter(RateLimiter<RedisRateLimiter.Config> rateLimiter) {
    super(Config.class);
    this.rateLimiter = rateLimiter;
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      var response = exchange.getResponse();
      var keyResolver = config.keyResolver;
      var routeId = config.routeId;

      return keyResolver.resolve(exchange)
          .flatMap(key -> this.rateLimiter.isAllowed(routeId, key))
          .flatMap(rateLimitResponse -> {
            boolean allowed = rateLimitResponse.isAllowed();
            if(allowed) return chain.filter(exchange);
            else return responseTooManyRequest(response);
          });
    };
  }

  public Mono<Void> responseTooManyRequest(ServerHttpResponse response){
    response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    return response.writeWith(
        Mono.just(
            response.bufferFactory().wrap(
                """
                |"success": false,
                |"message": "Too Many Requests"
                """.trim().getBytes(StandardCharsets.UTF_8)
            )
        )
    );
  }

  class Config implements HasRouteId {
    private KeyResolver keyResolver;
    private String routeId;
    public Config(KeyResolver keyResolver){
      this.keyResolver = keyResolver;
    }

    @Override
    public void setRouteId(String routeId) {
      this.routeId = routeId;
    }

    @Override
    public String getRouteId() {
      return this.routeId;
    }
  }
}
