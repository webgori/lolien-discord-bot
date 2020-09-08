package kr.webgori.lolien.discord.bot.dto.statistics;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGamesStatisticsMatchDto {
  private LocalDate gameCreation;
  private int matchCount;
}
