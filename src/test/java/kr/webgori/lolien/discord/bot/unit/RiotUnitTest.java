package kr.webgori.lolien.discord.bot.unit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class RiotUnitTest {
  @Test
  public void getCloseDataDragonVersion() {
    List<String> versions = Arrays.asList("10.16.1", "10.15.1", "10.14.1", "10.13.1", "10.12.1",
        "10.11.1", "10.10.3216176", "10.10.3208608", "10.10.5", "10.10.4", "10.10.1", "10.9.1",
        "10.8.1", "10.7.1", "10.6.1", "10.5.1", "10.4.1", "10.3.1", "10.2.1", "10.1.1", "9.24.2",
        "9.24.1", "9.23.1", "9.22.1", "9.21.1", "9.20.1", "9.19.1", "9.18.1", "9.17.1", "9.16.1",
        "9.15.1", "9.14.1", "9.13.1", "9.12.1", "9.11.1", "9.10.1", "9.9.1", "9.8.1", "9.7.2",
        "9.7.1");

    String gameVersion = "10.15.4.5.7";
    String regexVersion = String.format("%s.%s",
        gameVersion.split("\\.")[0],
        gameVersion.split("\\.")[1]) ;

    Pattern pattern = Pattern.compile("(" + regexVersion + ").+");
    String dataDragonVersion = "";

    for (String version : versions) {
      Matcher matcher = pattern.matcher(version);

      while (matcher.find()) {
        String group = matcher.group();
        dataDragonVersion = group;
      }
    }

    assertThat(dataDragonVersion, is("10.15.1"));
  }
}
