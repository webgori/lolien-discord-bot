package kr.webgori.lolien.discord.bot.dto.user;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserDto {
  private String nickname;
  private String summonerName;
  private LocalDateTime createdAt;
}
