package kr.webgori.lolien.discord.bot.response;

import java.time.LocalDate;
import java.util.List;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MatchDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostAssistDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostBannedDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostDeathDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostFirstBloodKillDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostFirstTowerKillDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostGoldEarnedDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostKillDeathAssistDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostKillDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostNeutralMinionsKilledDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostPlayedChampionDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostPlayedSummonerDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostTotalDamageDealtToChampionsDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostTotalDamageTakenDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostVisionScoreDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostWinningDto;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class StatisticsResponse {
  private LocalDate startDateOfMonth;
  private LocalDate endDateOfMonth;
  private List<MatchDto> matches;
  private List<MostBannedDto> mostBannedList;
  private List<MostPlayedChampionDto> mostPlayedChampionList;
  private List<MostWinningDto> mostWinningList;
  private List<MostPlayedSummonerDto> mostPlayedSummonerList;
  private List<MostKillDeathAssistDto> mostKillDeathAssistList;
  private MostKillDto mostKill;
  private MostDeathDto mostDeath;
  private MostAssistDto mostAssist;
  private MostVisionScoreDto mostVisionScore;
  private MostTotalDamageDealtToChampionsDto mostTotalDamageDealtToChampions;
  private MostTotalDamageTakenDto mostTotalDamageTaken;
  private MostGoldEarnedDto mostGoldEarned;
  private MostNeutralMinionsKilledDto mostNeutralMinionsKilled;
  private MostFirstTowerKillDto mostFirstTowerKill;
  private MostFirstBloodKillDto mostFirstBloodKill;
}
