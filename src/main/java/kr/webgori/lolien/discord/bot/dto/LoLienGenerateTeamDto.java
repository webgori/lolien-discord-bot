package kr.webgori.lolien.discord.bot.dto;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.LoLienSummoner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LoLienGenerateTeamDto {
  private List<LoLienSummoner> summonersTeam1;
  private List<LoLienSummoner> summonersTeam2;
  private int mmrDifference;
}
