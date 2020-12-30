package kr.webgori.lolien.discord.bot.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import kr.webgori.lolien.discord.bot.component.ConfigComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Slf4j
@Configuration
public class DataConfig {
  private final ConfigComponent configComponent;

  @Value("${spring.datasource.hikari.max-lifetime}")
  private int maxLifeTime;

  /**
   * dataSource.
   * @return dataSource
   */
  @Bean
  public DataSource dataSource() {
    String dataSourceUrl = ConfigComponent.getDataSourceUrl();
    String dataSourceUsername = ConfigComponent.getDataSourceUsername();
    String dataSourcePassword = ConfigComponent.getDataSourcePassword();

    HikariDataSource hikariDataSource = new HikariDataSource();
    hikariDataSource.setJdbcUrl(dataSourceUrl);
    hikariDataSource.setUsername(dataSourceUsername);
    hikariDataSource.setPassword(dataSourcePassword);
    hikariDataSource.setMaxLifetime(maxLifeTime);

    return hikariDataSource;
  }
}
