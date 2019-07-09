package kr.webgori.lolien.discord.bot.config;

import javax.security.auth.login.LoginException;

import kr.webgori.lolien.discord.bot.component.*;
import kr.webgori.lolien.discord.bot.hooks.CustomEventListener;
import kr.webgori.lolien.discord.bot.hooks.CustomListenerAdapter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class JdaConfig {
  private final HelpComponent helpComponent;
  private final SummonerComponent summonerComponent;
  private final TeamGenerateComponent teamGenerateComponent;
  private final CustomGameComponent customGameComponent;
  private final MemoComponent memoComponent;

  @Value("${jda.discord.token}")
  private String token;

  @Bean
  public JDA jda() throws LoginException {
    return new JDABuilder(token)
            .addEventListener(new CustomEventListener())
            .addEventListener(new CustomListenerAdapter(
                    helpComponent, summonerComponent, teamGenerateComponent, customGameComponent, memoComponent))
            .build();
  }
}