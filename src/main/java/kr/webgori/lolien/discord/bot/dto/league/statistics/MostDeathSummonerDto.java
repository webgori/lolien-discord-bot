package kr.webgori.lolien.discord.bot.dto.league.statistics;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MostDeathSummonerDto {
  private String summonerName;
  @Builder.Default private List<Integer> deaths = Lists.newArrayList();

  public void addDeath(int death) {
    this.deaths.add(death);
  }
}
