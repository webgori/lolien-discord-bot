package kr.webgori.lolien.discord.bot.dto.customgame.statistics;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MatchDto {
  private LocalDate gameCreation;
  private int matchCount;
}
