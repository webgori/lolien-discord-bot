package kr.webgori.lolien.discord.bot.hooks;

import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;

@Slf4j
public class CustomEventListener implements EventListener {
  @Override
  public void onEvent(@Nonnull GenericEvent event) {
    if (event instanceof ReadyEvent) {
      logger.info("API is ready!");
    }
  }
}
