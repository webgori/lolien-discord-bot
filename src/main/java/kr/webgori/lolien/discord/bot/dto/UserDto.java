package kr.webgori.lolien.discord.bot.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserDto {
  private String email;
  private String nickname;
  private boolean emailVerified;
  private String clienId;
  private String summonerName;
}
