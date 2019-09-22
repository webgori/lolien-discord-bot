package kr.webgori.lolien.discord.bot.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import kr.webgori.lolien.discord.bot.component.ConfigComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Slf4j
@Configuration
public class DataConfig {
  private final ConfigComponent configComponent;

  /**
   * dataSource.
   * @return dataSource
   */
  @Bean
  public DataSource dataSource() {
    return DataSourceBuilder
        .create()
        .type(HikariDataSource.class)
        .url(ConfigComponent.DATA_SOURCE_URL)
        .username(ConfigComponent.DATA_SOURCE_USERNAME)
        .password(ConfigComponent.DATA_SOURCE_PASSWORD)
        .build();
  }
}