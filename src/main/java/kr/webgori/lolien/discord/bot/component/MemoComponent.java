package kr.webgori.lolien.discord.bot.component;

import static java.time.format.DateTimeFormatter.ofPattern;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.sendErrorMessage;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.sendMessage;

import com.google.common.collect.Lists;
import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import kr.webgori.lolien.discord.bot.entity.Memo;
import kr.webgori.lolien.discord.bot.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class MemoComponent {
  private final MemoRepository memoRepository;

  /**
   * execute.
   * @param event event
   */
  @Transactional
  public void execute(MessageReceivedEvent event) {
    TextChannel textChannel = event.getTextChannel();
    String receivedMessage = event.getMessage().getContentDisplay();
    List<String> commands = Lists.newArrayList(receivedMessage.split(" "));

    if (commands.size() < 2) {
      sendSyntax(textChannel);
      return;
    }

    String subCommand = commands.get(1);

    switch (subCommand) {
      case "추가": {
        if (commands.size() < 4) {
          sendSyntax(textChannel);
          return;
        }

        StringBuilder descriptionBuilder = new StringBuilder();

        for (int i = 3; i < commands.size(); i++) {
          descriptionBuilder.append(commands.get(i));
        }

        String word = commands.get(2);
        String description = descriptionBuilder.toString();

        Boolean existsWord = memoRepository.existsByWord(word);

        Memo memo;
        String writer = event.getMember().getEffectiveName();
        String message;

        if (existsWord) {
          memo = memoRepository.findByWord(word);
          String oldDescription = memo.getDescription();

          description = String.format("%s | %s", oldDescription, description);
          memo.setDescription(description);

          memo.setWriter(writer);

          message = "메모가 추가되었습니다. (기존 메모에 덧붙임)";
        } else {
          memo = Memo.builder()
              .writer(writer)
              .word(word)
              .description(description)
              .build();

          message = "메모가 추가되었습니다.";
        }

        memoRepository.save(memo);
        sendMessage(textChannel, message);

        break;
      }
      case "삭제": {
        if (commands.size() < 3) {
          sendSyntax(textChannel);
          return;
        }

        String word = commands.get(2);
        Boolean existsWord = memoRepository.existsByWord(word);

        if (existsWord) {
          memoRepository.deleteByWord(word);
          sendMessage(textChannel, "단어가 삭제되었습니다.");
        } else {
          sendErrorMessage(textChannel, "단어가 존재하지 않습니다.", Color.RED);
        }

        break;
      }
      default: {
        String word = commands.get(1);
        Boolean existsWord = memoRepository.existsByWord(word);

        if (existsWord) {
          Memo memo = memoRepository.findByWord(word);

          String description = memo.getDescription();
          String writer = memo.getWriter();
          LocalDateTime lastModifiedLocalDateTime = memo.getLastModifiedDate();
          DateTimeFormatter formatter = ofPattern("yyyy-MM-dd HH:mm:ss");
          String lastModifiedDate = lastModifiedLocalDateTime.format(formatter);

          String message = String
              .format("%s: %s (%s, %s)", word, description, lastModifiedDate, writer);

          sendMessage(textChannel, message);
        } else {
          sendErrorMessage(textChannel, "단어가 존재하지 않습니다.", Color.RED);
        }

        break;
      }
    }
  }

  private void sendSyntax(TextChannel textChannel) {
    sendErrorMessage(textChannel, "잘못된 명령어 입니다. !도움말 명령어를 확인해 주세요.", Color.RED);
  }
}