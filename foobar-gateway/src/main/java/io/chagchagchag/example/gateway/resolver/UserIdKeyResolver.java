package io.chagchagchag.example.gateway.resolver;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component("userIdAsKeyResolver")
public class UserIdKeyResolver implements KeyResolver {
  private final Logger logger = LoggerFactory.getLogger(UserIdKeyResolver.class);
  @Override
  public Mono<String> resolve(ServerWebExchange exchange) {
    final String userId = exchange.getRequest()
        .getHeaders()
        .getFirst("USER-ID");

    return Mono.just(userId)
        .map(str ->
            Optional.ofNullable(str)
                .map(id -> {
                  logger.debug("USER-ID = {}", id);
                  return id;
                })
                .orElseThrow(() -> {
                  exchange.getResponse().setComplete();
                  return new IllegalArgumentException("존재하지 않는 아이디입니다.");
                })
        );
  }
}
