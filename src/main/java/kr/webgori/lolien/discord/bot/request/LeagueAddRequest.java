package kr.webgori.lolien.discord.bot.request;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeagueAddRequest {
  @NotBlank
  private String title;
}