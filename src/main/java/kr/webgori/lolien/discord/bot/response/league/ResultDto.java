package kr.webgori.lolien.discord.bot.response.league;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import kr.webgori.lolien.discord.bot.dto.CustomGameSummonerDto;
import kr.webgori.lolien.discord.bot.dto.CustomGameTeamDto;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ResultDto {
  private int idx;

  private Long gameCreation;

  private Long gameDuration;

  private Long gameId;

  private String gameMode;

  private String gameType;

  private String gameVersion;

  private Integer mapId;

  private String platformId;

  private Integer queueId;

  private Integer seasonId;

  @Schema(description = "Blue팀 소환사 목록")
  private List<CustomGameSummonerDto> blueTeamSummoners;

  @Schema(description = "Red팀 소환사 목록")
  private List<CustomGameSummonerDto> redTeamSummoners;

  @Schema(description = "팀 정보")
  private List<CustomGameTeamDto> teams;
}