package kr.webgori.lolien.discord.bot.dto.league;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LeagueDto {
  private int idx;
  private String title;
  private LocalDateTime createdDate;
}