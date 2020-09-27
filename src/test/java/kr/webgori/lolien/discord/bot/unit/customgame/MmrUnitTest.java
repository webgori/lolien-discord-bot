package kr.webgori.lolien.discord.bot.unit.customgame;

import static kr.webgori.lolien.discord.bot.util.CommonUtil.getRandomNumber;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import kr.webgori.lolien.discord.bot.entity.LolienParticipant;
import kr.webgori.lolien.discord.bot.entity.LolienSeasonCompensation;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

@Slf4j
public class MmrUnitTest {
  @RepeatedTest(10)
  void test_01_ShouldValidResultMmrWhenAddResult() {
    List<LolienSummoner> team1Summoners = getRandomLolienSummoner();

    double team1MmrAverage = team1Summoners
        .stream()
        .mapToInt(LolienSummoner::getMmr)
        .average()
        .orElse(0);

    List<LolienSummoner> team2Summoners = getRandomLolienSummoner();

    double team2MmrAverage = team2Summoners
        .stream()
        .mapToInt(LolienSummoner::getMmr)
        .average()
        .orElse(0);

    int teamId = getRandomNumber(1, 2) == 1 ? 100 : 200;
    boolean win = getRandomNumber(0, 1) != 0;

    int mmr = 336;

    if (win) {
      int resultMmr = 0;

      if (teamId == 100) {
        if (mmr > team2MmrAverage) {
          resultMmr = (int) (mmr / team2MmrAverage * 1);
        } else if (mmr < team2MmrAverage) {
          resultMmr = (int) (team2MmrAverage / mmr * 1.5);
        }
      } else if (teamId == 200) {
        if (mmr > team1MmrAverage) {
          resultMmr = (int) (mmr / team1MmrAverage * 1);
        } else if (mmr < team1MmrAverage) {
          resultMmr = (int) (team1MmrAverage / mmr * 1.5);
        }
      }

      mmr += resultMmr;
    } else {
      int resultMmr = 0;

      if (teamId == 100) {
        if (mmr > team2MmrAverage) {
          resultMmr = (int) (mmr / team2MmrAverage * 1.5);
        } else if (mmr < team2MmrAverage) {
          resultMmr = (int) (team2MmrAverage / mmr * 1);
        }
      } else if (teamId == 200) {
        if (mmr > team1MmrAverage) {
          resultMmr = (int) (mmr / team1MmrAverage * 1.5);
        } else if (mmr < team1MmrAverage) {
          resultMmr = (int) (team1MmrAverage / mmr * 1);
        }
      }

      mmr -= resultMmr;
    }

    assertThat(mmr, greaterThanOrEqualTo(330));
    assertThat(mmr, lessThanOrEqualTo(340));
  }

  private List<LolienSummoner> getRandomLolienSummoner() {
    List<LolienSummoner> lolienSummoners = Lists.newArrayList();

    for (int i = 0; i < 5; i++) {
      Integer randomNumber = getRandomNumber(1, 2) == 1 ? getRandomNumber(200, 350) : null;

      LolienSummoner lolienSummoner = LolienSummoner
          .builder()
          .mmr(randomNumber)
          .build();

      lolienSummoners.add(lolienSummoner);
    }

    return lolienSummoners;
  }

  @Test
  public void test_01_ShouldValidMmrWhenInitMmr() {
    Integer score = 8;

    Float compensationValue = 4.5f;

    float season10Mmr = score * compensationValue;

    int mmr = (int) (0f + 0f + season10Mmr);

    System.out.println(mmr);
  }
}
