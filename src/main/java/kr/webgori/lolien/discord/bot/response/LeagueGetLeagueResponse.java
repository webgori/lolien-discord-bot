package kr.webgori.lolien.discord.bot.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LeagueGetLeagueResponse {
  private int idx;
  private String title;
  private LocalDateTime createdDate;
}