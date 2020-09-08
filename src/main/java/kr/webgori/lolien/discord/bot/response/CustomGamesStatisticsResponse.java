package kr.webgori.lolien.discord.bot.response;

import java.util.List;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMatchDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostAssistDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostBannedDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostDeathDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostKillDeathAssistDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostKillDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostPlayedChampionDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostPlayedSummonerDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostWinningDto;
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
  private List<CustomGamesStatisticsMostKillDeathAssistDto> mostKillDeathAssistList;
  private List<CustomGamesStatisticsMostKillDto> mostKillList;
  private List<CustomGamesStatisticsMostDeathDto> mostDeathList;
  private List<CustomGamesStatisticsMostAssistDto> mostAssistList;
}
