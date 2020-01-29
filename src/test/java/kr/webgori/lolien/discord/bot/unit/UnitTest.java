package kr.webgori.lolien.discord.bot.unit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Comparator;
import java.util.List;
import org.assertj.core.util.Lists;
import org.junit.Test;

public class UnitTest {
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

  private String getSeasonFormat(String season) {
    String[] s = season.split("S");
    int seasonNumber = Integer.parseInt(s[1]);
    return seasonNumber <= 9 ? "S0" + seasonNumber : season;
  }
}