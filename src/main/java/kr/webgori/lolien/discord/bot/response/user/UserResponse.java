package kr.webgori.lolien.discord.bot.response.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import kr.webgori.lolien.discord.bot.dto.user.UserDto;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserResponse {
  @Schema(description = "사용자 목록")
  private List<UserDto> users;
}
