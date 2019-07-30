package kr.webgori.lolien.discord.bot.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeagueAddResultRequest {
  @NotNull
  private Integer leagueIdx;

  @NotNull
  private Long matchId;

  @NotBlank
  private String entries;
}