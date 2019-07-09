package kr.webgori.lolien.discord.bot.hooks;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.EventListener;

@Slf4j
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
public class CustomEventListener implements EventListener {
  @Override
  public void onEvent(Event event) {
    if (event instanceof ReadyEvent) {
      logger.info("API is ready!");
    }
  }
}
