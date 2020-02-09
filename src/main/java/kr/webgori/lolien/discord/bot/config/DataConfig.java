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
    String dataSourceUrl = ConfigComponent.getDataSourceUrl();
    String dataSourceUsername = ConfigComponent.getDataSourceUsername();
    String dataSourcePassword = ConfigComponent.getDataSourcePassword();

    return DataSourceBuilder
        .create()
        .type(HikariDataSource.class)
        .url(dataSourceUrl)
        .username(dataSourceUsername)
        .password(dataSourcePassword)
        .build();
  }
}