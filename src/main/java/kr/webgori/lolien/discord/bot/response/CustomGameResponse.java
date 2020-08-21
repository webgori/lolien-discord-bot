package kr.webgori.lolien.discord.bot.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import kr.webgori.lolien.discord.bot.entity.LolienParticipant;
import kr.webgori.lolien.discord.bot.entity.LolienTeamStats;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGameResponse {
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
  private List<CustomGameSummonerResponse> blueTeamSummoners;

  @Schema(description = "Red팀 소환사 목록")
  private List<CustomGameSummonerResponse> redTeamSummoners;
}