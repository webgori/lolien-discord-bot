package kr.webgori.lolien.discord.bot.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserInfoDto {
  private String email;
  private String nickname;
  private boolean emailVerified;
  private String clienId;
  private String summonerName;
  private List<String> positions;
}
