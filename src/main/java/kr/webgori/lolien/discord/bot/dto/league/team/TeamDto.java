package kr.webgori.lolien.discord.bot.dto.league.team;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TeamDto {
  private String koreanName;
  private String englishName;
}
