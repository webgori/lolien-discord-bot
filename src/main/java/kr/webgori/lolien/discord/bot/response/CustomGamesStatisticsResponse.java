package kr.webgori.lolien.discord.bot.response;

import java.util.List;
import kr.webgori.lolien.discord.bot.dto.CustomGamesStatisticsMatchDto;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGamesStatisticsResponse {
  private List<CustomGamesStatisticsMatchDto> matches;
}
