package kr.webgori.lolien.discord.bot.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import kr.webgori.lolien.discord.bot.dto.CustomGameSummonerDto;
import kr.webgori.lolien.discord.bot.dto.CustomGameTeamDto;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGameDto {
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

  @Schema(description = "내전 결과 삭제 가능 여부 (true: 삭제 가능, false: 삭제 불가")
  private boolean deleteAble;

  @Schema(description = "리플레이 다운로드 가능 여부")
  private boolean replayDownloadable;
}
