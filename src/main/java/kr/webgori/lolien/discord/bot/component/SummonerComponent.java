package kr.webgori.lolien.discord.bot.component;

import static kr.webgori.lolien.discord.bot.component.TeamGenerateComponent.CURRENT_SEASON;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.getSeasonFormat;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.numberToRomanNumeral;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.sendErrorMessage;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.sendMessage;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import kr.webgori.lolien.discord.bot.entity.League;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.repository.LeagueRepository;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class SummonerComponent {
  public static final String DEFAULT_TIER = "UNRANKED";

  private final LolienSummonerRepository lolienSummonerRepository;
  private final LeagueRepository leagueRepository;

  public static String getDefaultTier() {
    return DEFAULT_TIER;
  }

  public static String getCurrentSeason() {
    return CURRENT_SEASON;
  }

  /**
   * execute.
   * @param event event
   */
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
      boolean hasSummonerName = lolienSummonerRepository.existsBySummonerName(summonerName);

      if (hasSummonerName) {
        sendErrorMessage(textChannel, "이미 등록되어 있는 소환사 이름 입니다.", Color.RED);
        return;
      }

      String riotApiKey = ConfigComponent.getRiotApiKey();
      ApiConfig config = new ApiConfig().setKey(riotApiKey);
      RiotApi riotApi = new RiotApi(config);

      Summoner summoner = riotApi.getSummonerByName(Platform.KR, summonerName);
      String summonerId = summoner.getId();
      String accountId = summoner.getAccountId();
      boolean existsByAccountId = lolienSummonerRepository.existsByAccountId(accountId);

      // 변경한 소환사명 갱신
      if (existsByAccountId) {
        LolienSummoner lolienSummoner = lolienSummonerRepository.findByAccountId(accountId);
        String oldSummonerName = lolienSummoner.getSummonerName();
        lolienSummoner.setSummonerName(summonerName);
        lolienSummonerRepository.save(lolienSummoner);
        String infoMessage = String.format("%s 소환사명을 %s로 갱신하였습니다.", oldSummonerName, summonerName);
        sendMessage(textChannel, infoMessage);
        return;
      }

      Set<LeagueEntry> leagueEntrySet = riotApi
          .getLeagueEntriesBySummonerId(Platform.KR, summonerId);

      List<LeagueEntry> leagueEntries = Lists.newArrayList(leagueEntrySet);

      String tier = DEFAULT_TIER;

      for (LeagueEntry leagueEntry : leagueEntries) {
        if (leagueEntry.getQueueType().equals("RANKED_SOLO_5x5")) {
          tier = leagueEntry.getTier() + "-" + leagueEntry.getRank();
        }
      }

      int summonerLevel = summoner.getSummonerLevel();

      saveClienSummoner(summoner, tier, summonerId, summonerName, summonerLevel);

      Map<String, String> tiersFromOpGg = getTiersFromOpGg(summonerName);

      for (Map.Entry<String, String> entry : tiersFromOpGg.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        LolienSummoner lolienSummoner = lolienSummonerRepository
            .findBySummonerName(summonerName);

        League league = League
            .builder()
            .lolienSummoner(lolienSummoner)
            .season(key)
            .tier(value)
            .build();

        leagueRepository.save(league);
      }
    } catch (RiotApiException e) {
      int errorCode = e.getErrorCode();
      if (errorCode == RiotApiException.FORBIDDEN) {
        sendErrorMessage(textChannel,
            "Riot API Key가 만료되어 기능이 정상적으로 작동하지 않습니다."
                + "개발자에게 알려주세요.", Color.RED);
        throw new IllegalArgumentException("api-key-expired");
      } else if (errorCode == RiotApiException.DATA_NOT_FOUND) {
        String errorMessage = String.format("%s 소환사가 존재하지 않습니다.", summonerName);
        sendErrorMessage(textChannel, errorMessage, Color.RED);
        throw new IllegalArgumentException("invalid summoner name");
      } else {
        logger.error("", e);
        throw new IllegalArgumentException("riotApiException");
      }
    }

    String sendMessage = String.format("%s 소환사가 성공적으로 등록 되었습니다.", summonerName);
    sendMessage(textChannel, sendMessage);
  }

  /**
   * saveClienSummoner.
   * @param summoner summoner
   * @param tier tier
   * @param summonerId summonerId
   * @param summonerName summonerName
   * @param summonerLevel summonerLevel
   */
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

    LolienSummoner lolienSummoner = LolienSummoner
        .builder()
        .id(summonerId)
        .accountId(accountId)
        .summonerName(summonerName)
        .summonerLevel(summonerLevel)
        .leagues(leagues)
        .build();

    league.setLolienSummoner(lolienSummoner);

    lolienSummonerRepository.save(lolienSummoner);
  }

  /**
   * getTiersFromOpGg.
   * @param summonerName summonerName
   * @return Map map
   */
  private Map<String, String> getTiersFromOpGg(String summonerName) {
    Map<String, String> tiersMap = Maps.newHashMap();

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
            prevSeason = getSeasonFormat(seasonElement.text());
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

          prevTierSplitList.set(0, prevTierSplitList.get(0).toUpperCase());

          if (prevTierSplitList.get(1).equals("5")) {
            prevTierSplitList.set(1, "4");
          }

          if (!prevTierSplitList.get(1).equals("1") && !prevTierSplitList.get(1).equals("2")
              && !prevTierSplitList.get(1).equals("3") && !prevTierSplitList.get(1).equals("4")) {
            logger.error(prevTierSplitList.get(1));
            prevTierSplitList.set(1, "1");
          }

          prevTierSplitList.set(1, numberToRomanNumeral(prevTierSplitList.get(1)));
          String prevTier = String.join("-", prevTierSplitList);

          tiersMap.put(prevSeason, prevTier);
        }
      }
    } catch (IOException e) {
      logger.error("", e);
    }

    return tiersMap;
  }

  private void sendSyntax(TextChannel textChannel) {
    sendErrorMessage(textChannel, "잘못된 명령어 입니다. !소환사 등록 소환사명", Color.RED);
  }
}