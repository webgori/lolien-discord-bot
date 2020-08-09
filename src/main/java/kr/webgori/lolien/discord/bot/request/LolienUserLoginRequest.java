package kr.webgori.lolien.discord.bot.request;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LolienUserLoginRequest {
  @NotBlank
  private String id;

  @NotBlank
  private String password;
}
