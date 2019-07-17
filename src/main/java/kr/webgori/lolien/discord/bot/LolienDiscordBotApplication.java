package kr.webgori.lolien.discord.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class LolienDiscordBotApplication {
  public static void main(String[] args) {
    SpringApplication.run(LolienDiscordBotApplication.class, args);
  }
}