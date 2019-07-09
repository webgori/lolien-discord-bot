package kr.webgori.lolien.discord.bot.config;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

@Profile({"local"})
@Configuration
public class EmbeddedRedisConfig {
  @Value("${spring.redis.port}")
  private int redisPort;

  private RedisServer redisServer;

  /**
   * 어플리케이션 실행시 EmbeddedRedis 시작.
   */
  @PostConstruct
  public void redisServer() {
    redisServer = RedisServer
            .builder()
            .port(redisPort)
            .setting("maxmemory 128M")
            .build();

    redisServer.start();
  }

  /**
   * 어플리케이션 종료시 EmbeddedRedis 종료.
   */
  @PreDestroy
  public void stopRedis() {
    if (redisServer != null) {
      redisServer.stop();
    }
  }
}