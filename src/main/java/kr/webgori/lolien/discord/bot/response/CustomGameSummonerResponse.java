package kr.webgori.lolien.discord.bot.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGameSummonerResponse {
  private int idx;

  private String summonerName;

  private int championId;

  private long totalDamageDealtToChampions;

  private int spell1Id;

  private int spell2Id;

  private int kills;

  private int deaths;

  private int assists;

  private int champLevel;

  private int totalMinionsKilled;

  private int item0;

  private int item1;

  private int item2;

  private int item3;

  private int item4;

  private int item5;

  private int item6;

  private int wardsPlaced;

  @Schema(description = "100: blue, 200: red")
  private int teamId;
}