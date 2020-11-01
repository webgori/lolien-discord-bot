package kr.webgori.lolien.discord.bot.component;

import static kr.webgori.lolien.discord.bot.util.CommonUtil.sendMessage;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class HelpComponent {
  /**
   * execute.
   * @param event event
   */
  public void execute(MessageReceivedEvent event) {
    TextChannel textChannel = event.getTextChannel();

    String commandDesc = "┌LolienBot 도움말 입니다. 자세한 문의사항은 GitHub ( https://github.com/webgori/spring-discord-bot ) 이슈 등록 부탁드립니다.";
    String summonerCommand = "├!소환사 등록 소환사명: 소환사를 Database에 등록합니다.";
    String teamGenerateCommand = "├!팀구성 밸런스 소환사명1, 소환사명2, 소환사명3 ...: 내전 팀 구성시 평균 티어를 맞추어 팀을 구성합니다.";
    String customGameCommand1 = "├!내전 결과 조회: 최근 5개의 내전 이력을 보여줍니다.";
    String customGameCommand2 = "├!내전 결과 등록 대전기록-URL 참가자목록(순서대로): 내전 후의 결과 데이터를 Database에 등록합니다.";
    String customGameCommand3 = "├!내전 모스트 소환사명: 내전시 해당 소환사가 가장 많이 선택했던 챔프를 1위부터 5위까지 승률과 함께 보여줍니다.";
    String customGameCommand4 = "├!내전 참여횟수: 내전 참여 횟수를 소환사명과 함께 1위부터 5위까지 보여줍니다.";
    String customGameCommand5 = "├!내전 참여횟수 소환사명1, 소환사명2, 소환사명3 ...: 해당 소환사명의 내전 참여 횟수를 보여줍니다.";
    String customGameCommand6 = "├!메모 추가 단어: 해당 단어의 메모를 추가합니다.";
    String customGameCommand7 = "├!메모 삭제 단어: 해당 단어의 메모를 삭제합니다.";
    String customGameCommand8 = "└!메모 단어: 해당 단어의 메모를 보여줍니다.";

    List<String> commands = Lists.newArrayList(commandDesc, summonerCommand, teamGenerateCommand,
        customGameCommand1, customGameCommand2, customGameCommand3, customGameCommand4,
        customGameCommand5, customGameCommand6, customGameCommand7, customGameCommand8);

    for (String command : commands) {
      sendMessage(textChannel, command);
    }
  }
}