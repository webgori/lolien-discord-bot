package kr.webgori.lolien.discord.bot.dto.league.statistics;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MostVisionScoreSummonerDto {
  private String summonerName;
  private float averageOfVisionScore;
  @Builder.Default private List<Long> visionScores = Lists.newArrayList();

  public void addVisionScore(long visionScore) {
    this.visionScores.add(visionScore);
  }
}
