package kr.webgori.lolien.discord.bot.dto.customgame.statistics;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MatchDto {
  private LocalDate gameCreation;
  private int matchCount;
}
