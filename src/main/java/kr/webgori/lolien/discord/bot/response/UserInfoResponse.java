package kr.webgori.lolien.discord.bot.response;

import kr.webgori.lolien.discord.bot.dto.UserDto;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Builder
@Data
public class UserInfoResponse {
  private UserDto userInfo;
}
