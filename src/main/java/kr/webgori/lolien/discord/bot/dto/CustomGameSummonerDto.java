package kr.webgori.lolien.discord.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGameSummonerDto {
  private int idx;

  private String summonerName;

  private int championId;

  private String championUrl;

  private String championName;

  private long totalDamageDealtToChampions;

  private int spell1Id;

  private int spell2Id;

  private String spell1Url;

  private String spell2Url;

  private String spell1Name;

  private String spell2Name;

  private String spell1Description;

  private String spell2Description;

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

  private String item0Url;

  private String item1Url;

  private String item2Url;

  private String item3Url;

  private String item4Url;

  private String item5Url;

  private String item6Url;

  private String item0Name;

  private String item1Name;

  private String item2Name;

  private String item3Name;

  private String item4Name;

  private String item5Name;

  private String item6Name;

  private String item0Description;

  private String item1Description;

  private String item2Description;

  private String item3Description;

  private String item4Description;

  private String item5Description;

  private String item6Description;

  private int wardsPlaced;

  @Schema(description = "팀 (100: blue, 200: red)")
  private int teamId;

  @Schema(description = "승패 여부 (true: 승, false: 패)")
  private boolean win;
}