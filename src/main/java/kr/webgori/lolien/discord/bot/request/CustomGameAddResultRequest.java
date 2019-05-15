package kr.webgori.lolien.discord.bot.request;

import lombok.Data;

@Data
public class CustomGameAddResultRequest {
  private long matchId;
  private String entries;
}