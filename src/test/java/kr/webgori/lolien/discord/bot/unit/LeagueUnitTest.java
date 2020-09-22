package kr.webgori.lolien.discord.bot.unit;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class LeagueUnitTest {
  @Test
  public void getSummonerName() {
    Pattern pattern = Pattern.compile("\\\\\"NAME\\\\\":\\\\\"([A-Za-z0-9가-힣 ]*)\\\\\"");
    String json = "";

    Matcher matcher = pattern.matcher(json);

    while (matcher.find()) {
      String group = matcher.group();
      assertThat(group, notNullValue());

      logger.error(group);
    }
  }
}
