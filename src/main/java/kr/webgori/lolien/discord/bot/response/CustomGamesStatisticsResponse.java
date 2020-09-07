package kr.webgori.lolien.discord.bot.response;

import java.util.List;
import kr.webgori.lolien.discord.bot.dto.CustomGamesStatisticsMatchDto;
import kr.webgori.lolien.discord.bot.dto.CustomGamesStatisticsMostBannedDto;
import kr.webgori.lolien.discord.bot.dto.CustomGamesStatisticsMostPlayedChampionDto;
import kr.webgori.lolien.discord.bot.dto.CustomGamesStatisticsMostPlayedSummonerDto;
import kr.webgori.lolien.discord.bot.dto.CustomGamesStatisticsMostWinningDto;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGamesStatisticsResponse {
  private List<CustomGamesStatisticsMatchDto> matches;
  private List<CustomGamesStatisticsMostBannedDto> mostBannedList;
  private List<CustomGamesStatisticsMostPlayedChampionDto> mostPlayedChampionList;
  private List<CustomGamesStatisticsMostWinningDto> mostWinningList;
  private List<CustomGamesStatisticsMostPlayedSummonerDto> mostPlayedSummonerList;
}
