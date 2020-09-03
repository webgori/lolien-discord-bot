package kr.webgori.lolien.discord.bot.response;

import java.util.List;
import kr.webgori.lolien.discord.bot.dto.CustomGamesStatisticsMatchDto;
import kr.webgori.lolien.discord.bot.dto.CustomGamesStatisticsMostBannedDto;
import kr.webgori.lolien.discord.bot.dto.CustomGamesStatisticsMostPlayedDto;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGamesStatisticsResponse {
  private List<CustomGamesStatisticsMatchDto> matches;
  private List<CustomGamesStatisticsMostBannedDto> mostBannedDto;
  private List<CustomGamesStatisticsMostPlayedDto> mostPlayedDto;
}
