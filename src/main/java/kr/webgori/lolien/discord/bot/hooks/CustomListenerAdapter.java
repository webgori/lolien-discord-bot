package kr.webgori.lolien.discord.bot.hooks;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import kr.webgori.lolien.discord.bot.component.CustomGameComponent;
import kr.webgori.lolien.discord.bot.component.HelpComponent;
import kr.webgori.lolien.discord.bot.component.MemoComponent;
import kr.webgori.lolien.discord.bot.component.SummonerComponent;
import kr.webgori.lolien.discord.bot.component.TeamGenerateComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

@RequiredArgsConstructor
@Slf4j
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
public class CustomListenerAdapter extends ListenerAdapter {
  private final HelpComponent helpComponent;
  private final SummonerComponent summonerComponent;
  private final TeamGenerateComponent teamGenerateComponent;
  private final CustomGameComponent customGameComponent;
  private final MemoComponent memoComponent;

  @Override
  public void onMessageReceived(MessageReceivedEvent event) {
    if (event.isFromType(ChannelType.PRIVATE)) {
      logger.info("[PM] {}: {}\n", event.getAuthor().getName(),
          event.getMessage().getContentDisplay());
    } else {
      String botId = event.getJDA().getSelfUser().getId();
      String memberId = event.getMember().getUser().getId();

      if (!botId.equals(memberId)) {
        String message = event.getMessage().getContentDisplay();

        List<String> messages = Lists.newArrayList(message.split(" "));

        String command = messages.get(0);

        switch (command) {
          case "!도움말":
            helpComponent.execute(event);
            break;
          case "!소환사":
            summonerComponent.execute(event);
            break;
          case "!팀구성":
            teamGenerateComponent.execute(event);
            break;
          case "!내전":
            customGameComponent.execute(event);
            break;
          case "!메모":
            memoComponent.execute(event);
            break;
          default:
            break;
        }

        logger.info("[{}][{}] {}: {}\n", event.getGuild().getName(),
            event.getTextChannel().getName(), event.getMember().getEffectiveName(),
            event.getMessage().getContentDisplay());
      }
    }
  }
}