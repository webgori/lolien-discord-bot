package kr.webgori.lolien.discord.bot.response;

import kr.webgori.lolien.discord.bot.dto.UserInfoDto;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserInfoResponse {
  private UserInfoDto userInfo;
}
