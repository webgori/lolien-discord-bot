package kr.webgori.lolien.discord.bot.component;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import kr.webgori.lolien.discord.bot.entity.League;
import kr.webgori.lolien.discord.bot.entity.LoLienSummoner;
import kr.webgori.lolien.discord.bot.repository.LeagueRepository;
import kr.webgori.lolien.discord.bot.repository.LoLienSummonerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.league.dto.LeaguePosition;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import static kr.webgori.lolien.discord.bot.util.CommonUtil.*;

@Slf4j
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
@RequiredArgsConstructor
@Component
public class SummonerComponent {
  private static final String DEFAULT_TIER = "UNRANKED";
  private static final String CURRENT_SEASON = "S9";

  private final LoLienSummonerRepository loLienSummonerRepository;
  private final LeagueRepository leagueRepository;

  @Value("${riot.api.key}")
  private String riotApiKey;

  public void execute(MessageReceivedEvent event) {
    TextChannel textChannel = event.getTextChannel();
    String receivedMessage = event.getMessage().getContentDisplay();
    List<String> commands = Lists.newArrayList(receivedMessage.split(" "));

    if (commands.size() < 3) {
      sendSyntax(textChannel);
      return;
    }

    String subCommand = commands.get(1);

    if (!subCommand.equals("등록")) {
      sendSyntax(textChannel);
      return;
    }

    StringBuilder summonerNameBuilder = new StringBuilder();

    for (int i = 2; i < commands.size(); i++) {
      summonerNameBuilder.append(commands.get(i));
    }

    String summonerName = summonerNameBuilder.toString();

    try {
      ApiConfig config = new ApiConfig().setKey(riotApiKey);
      RiotApi riotApi = new RiotApi(config);

      boolean hasSummonerName = loLienSummonerRepository.existsBySummonerName(summonerName);

      if (hasSummonerName) {
        sendErrorMessage(textChannel, "이미 등록되어 있는 소환사 이름 입니다.", Color.RED);
        return;
      }

      Summoner summoner = riotApi.getSummonerByName(Platform.KR, summonerName);
      String summonerId = summoner.getId();
      String accountId = summoner.getAccountId();
      boolean existsByAccountId = loLienSummonerRepository.existsByAccountId(accountId);

      // 변경한 소환사명 갱신
      if (existsByAccountId) {
        LoLienSummoner loLienSummoner = loLienSummonerRepository.findByAccountId(accountId);
        String oldSummonerName = loLienSummoner.getSummonerName();
        loLienSummoner.setSummonerName(summonerName);
        loLienSummonerRepository.save(loLienSummoner);
        String infoMessage = String.format("%s 소환사명을 %s로 갱신하였습니다.", oldSummonerName, summonerName);
        sendMessage(textChannel, infoMessage);
        return;
      }

      Set<LeaguePosition> leaguePositions = riotApi
              .getLeaguePositionsBySummonerId(Platform.KR, summonerId);
      List<LeaguePosition> leaguePositionList = new ArrayList<>(leaguePositions);

      String tier = DEFAULT_TIER;

      for (LeaguePosition leaguePosition : leaguePositionList) {
        if (leaguePosition.getQueueType().equals("RANKED_SOLO_5x5")) {
          tier = leaguePosition.getTier() + "-" + leaguePosition.getRank();
        }
      }

      int summonerLevel = summoner.getSummonerLevel();

      saveClienSummoner(summoner, tier, summonerId, summonerName, summonerLevel);

      String opGgUrl = String.format("https://www.op.gg/summoner/userName=%s", summonerName);

      try {
        Document document = Jsoup.connect(opGgUrl).timeout(600000).get();
        Elements pastRankList = document.getElementsByClass("PastRankList");

        for (Element element : pastRankList) {
          Elements tierElements = element.getElementsByTag("li");
          for (Element tierElement : tierElements) {
            Elements seasonElements = tierElement.getElementsByTag("b");

            String prevSeason = Strings.EMPTY;

            for (Element seasonElement : seasonElements) {
              prevSeason = seasonElement.text();
            }

            boolean title = tierElement.hasAttr("title");

            if (!title) {
              continue;
            }

            String prevTierLeaguePoints = tierElement.attr("title");
            List<String> prevTierSplitList = Lists.newArrayList(prevTierLeaguePoints.split(" "));

            if (prevTierSplitList.size() > 2) {
              prevTierSplitList.remove(2);
            }

            prevTierSplitList.set(0, prevTierSplitList.get(0).toUpperCase(Locale.KOREAN));

            if (prevTierSplitList.get(1).equals("5")) {
              prevTierSplitList.set(1, "4");
            }

            prevTierSplitList.set(1, numberToRomanNumeral(prevTierSplitList.get(1)));
            String prevTier = String.join("-", prevTierSplitList);

            LoLienSummoner loLienSummoner = loLienSummonerRepository
                    .findBySummonerName(summonerName);

            League league = League
                    .builder()
                    .loLienSummoner(loLienSummoner)
                    .season(prevSeason)
                    .tier(prevTier)
                    .build();

            leagueRepository.save(league);
          }
        }
      } catch (IOException e) {
        logger.error("{}", e);
      }
    } catch (RiotApiException e) {
      int errorCode = e.getErrorCode();
      if (errorCode == RiotApiException.FORBIDDEN) {
        sendErrorMessage(textChannel, "Riot API Key가 만료되어 기능이 정상적으로 작동하지 않습니다. 개발자에게 알려주세요.", Color.RED);
        throw new IllegalArgumentException("api-key-expired");
      } else if (errorCode == RiotApiException.DATA_NOT_FOUND) {
        String errorMessage = String.format("%s 소환사가 존재하지 않습니다.", summonerName);
        sendErrorMessage(textChannel, errorMessage, Color.RED);
        throw new IllegalArgumentException("invalid summoner name");
      } else {
        logger.error("{}", e);
        throw new IllegalArgumentException("riotApiException");
      }
    }

    String sendMessage = String.format("%s 소환사가 성공적으로 등록 되었습니다.", summonerName);
    sendMessage(textChannel, sendMessage);
  }

  private void saveClienSummoner(Summoner summoner, String tier, String summonerId,
                                 String summonerName, int summonerLevel) {
    League league = League
            .builder()
            .season(CURRENT_SEASON)
            .tier(tier)
            .build();

    String accountId = summoner.getAccountId();
    List<League> leagues = new ArrayList<>();
    leagues.add(league);

    LoLienSummoner loLienSummoner = LoLienSummoner
            .builder()
            .id(summonerId)
            .accountId(accountId)
            .summonerName(summonerName)
            .summonerLevel(summonerLevel)
            .leagues(leagues)
            .build();

    league.setLoLienSummoner(loLienSummoner);

    loLienSummonerRepository.save(loLienSummoner);
  }

  private void sendSyntax(TextChannel textChannel) {
    sendErrorMessage(textChannel, "잘못된 명령어 입니다. !소환사 등록 소환사명", Color.RED);
  }
}