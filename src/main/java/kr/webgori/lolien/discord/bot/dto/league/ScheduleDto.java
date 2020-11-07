package kr.webgori.lolien.discord.bot.dto.league;

import java.time.LocalDateTime;
import kr.webgori.lolien.discord.bot.dto.league.team.TeamDto;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ScheduleDto {
  private int idx;
  private TeamDto team;
  private TeamDto enemyTeam;
  private LocalDateTime matchDateTime;
  private String description1;
  private String description2;
  private String description3;
}
