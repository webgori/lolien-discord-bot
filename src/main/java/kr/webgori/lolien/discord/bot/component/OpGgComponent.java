package kr.webgori.lolien.discord.bot.component;

import static com.google.common.collect.Lists.newArrayList;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.numberToRomanNumeral;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import kr.webgori.lolien.discord.bot.entity.League;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class OpGgComponent {
  /**
   * op.gg 에서 티어 조회.
   * @param lolienSummoner lolienSummoner
   * @return 리그 목록
   */
  public List<League> getLeaguesFromOpGg(LolienSummoner lolienSummoner) {
    String summonerName = lolienSummoner.getSummonerName();

    Map<String, String> tiersFromOpGg = getTiersFromOpGg(summonerName);
    List<League> leagues = Lists.newArrayList();

    for (Map.Entry<String, String> entry : tiersFromOpGg.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      if (key.equals("S2020")) {
        key = "S10";
      }

      League league = League
          .builder()
          .lolienSummoner(lolienSummoner)
          .season(key)
          .tier(value)
          .build();

      leagues.add(league);
    }

    return leagues;
  }

  /**
   * getTiersFromOpGg.
   *
   * @param summonerName summonerName
   * @return Map map
   */
  public Map<String, String> getTiersFromOpGg(String summonerName) {
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

  private String getSeasonFormat(String season) {
    String[] s = season.split("S");
    int seasonNumber = Integer.parseInt(s[1]);
    return seasonNumber <= 9 ? "S0" + seasonNumber : season;
  }
}
