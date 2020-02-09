package kr.webgori.lolien.discord.bot.unit;

import static com.google.common.collect.Lists.newArrayList;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.numberToRomanNumeral;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import kr.webgori.lolien.discord.bot.LolienDiscordBotApplication;
import kr.webgori.lolien.discord.bot.component.ConfigComponent;
import kr.webgori.lolien.discord.bot.entity.League;
import kr.webgori.lolien.discord.bot.entity.LoLienSummoner;
import kr.webgori.lolien.discord.bot.repository.LeagueRepository;
import kr.webgori.lolien.discord.bot.repository.LoLienSummonerRepository;
import lombok.extern.slf4j.Slf4j;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.constant.Platform;
import org.apache.logging.log4j.util.Strings;
import org.assertj.core.util.Lists;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = LolienDiscordBotApplication.class)
public class UnitTest {
  @Autowired
  private LoLienSummonerRepository loLienSummonerRepository;

  @Autowired
  private LeagueRepository leagueRepository;

  @Test
  public void compareTest() {
    List<String> leagues = Lists.newArrayList();

    leagues.add("S09");
    leagues.add("S08");
    leagues.add("S10");

    leagues.sort(Comparator.reverseOrder());

    assertThat(leagues.get(0), is("S10"));
    assertThat(leagues.get(1), is("S09"));
    assertThat(leagues.get(2), is("S08"));
  }

  @Test
  public void seasonTest() {
    String season4 = "S4";
    String season04 = getSeasonFormat(season4);

    assertThat(season04, is("S04"));

    String season10 = "S10";
    season10 = getSeasonFormat(season10);

    assertThat(season10, is("S10"));
  }

  @Test
  public void opGgTest() throws InterruptedException {
    List<LoLienSummoner> all = loLienSummonerRepository.findAll();

    for (LoLienSummoner loLienSummoner : all) {
      String summonerName = loLienSummoner.getSummonerName();

      Map<String, String> tiersFromOpGg = getTiersFromOpGg(summonerName);

      for (Map.Entry<String, String> entry : tiersFromOpGg.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();

        League league = League
            .builder()
            .loLienSummoner(loLienSummoner)
            .season(key)
            .tier(value)
            .build();

        leagueRepository.save(league);
      }
    }
  }

  @Test
  public void summonerOrderTest() throws RiotApiException {
    String riotApiKey = ConfigComponent.getRiotApiKey();
    ApiConfig config = new ApiConfig().setKey(riotApiKey);
    RiotApi riotApi = new RiotApi(config);

    long gameId = 4121228833L;
    Match match = riotApi.getMatch(Platform.KR, gameId);

    List<String> summonersName = match
        .getParticipantIdentities()
        .stream()
        .map(p -> p.getPlayer().getSummonerName())
        .collect(Collectors.toList());

    logger.error("{}", summonersName);
  }

  private String getSeasonFormat(String season) {
    String[] s = season.split("S");
    int seasonNumber = Integer.parseInt(s[1]);
    return seasonNumber <= 9 ? "S0" + seasonNumber : season;
  }

  /**
   * getTiersFromOpGg.
   *
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
          List<String> prevTierSplitList = newArrayList(prevTierLeaguePoints.split(" "));

          if (prevTierSplitList.size() > 2) {
            prevTierSplitList.remove(2);
          }

          prevTierSplitList.set(0, prevTierSplitList.get(0).toUpperCase(Locale.KOREAN));

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
}