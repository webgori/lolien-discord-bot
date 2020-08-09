package kr.webgori.lolien.discord.bot.request;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LolienUserAddSummonerRequest {
  @NotBlank
  private String summonerName;
}
