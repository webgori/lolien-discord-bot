package kr.webgori.lolien.discord.bot.config;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
  @Value("${spring.redis.host}")
  private String host;

  @Value("${spring.redis.port}")
  private int port;

  @Value("${spring.redis.database}")
  private int database;

  @Value("${spring.redis.password}")
  private String password;

  /**
   * JedisConnectionFactory.
   * @return JedisConnectionFactory
   */
  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration("server", 6379);
    serverConfig.setHostName(host);
    serverConfig.setPort(port);
    serverConfig.setDatabase(database);
    Optional.ofNullable(password).ifPresent(serverConfig::setPassword);

    return new LettuceConnectionFactory(serverConfig);
  }

  /**
   * RedisTemplate.
   * @return RedisTemplate
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate() {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(new StringRedisSerializer());
    redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashValueSerializer(new StringRedisSerializer());
    redisTemplate.setConnectionFactory(redisConnectionFactory());
    return redisTemplate;
  }
}