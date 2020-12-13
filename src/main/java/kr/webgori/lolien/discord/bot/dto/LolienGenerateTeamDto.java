package kr.webgori.lolien.discord.bot.dto;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LolienGenerateTeamDto {
  private List<LolienSummoner> summonersTeam1;
  private List<LolienSummoner> summonersTeam2;
  private int mmrDifference;
  private int team1Mmr;
  private int team2Mmr;
}
