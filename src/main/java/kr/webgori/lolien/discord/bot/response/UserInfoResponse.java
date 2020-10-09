package kr.webgori.lolien.discord.bot.response;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Builder
@Data
public class UserInfoResponse {
  private String email;
  private String nickname;
  private boolean emailVerified;
  private String clienId;
  private String summonerName;
}
