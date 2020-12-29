
package kr.webgori.lolien.discord.bot.dto;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UserSessionDto {
  @Builder.Default private String email = "";
  private String nickname;
  private String role;
  private LocalDateTime createdAt;
  private String hash;

  /**
   * hasExpired.
   * @return hasExpired
   */
  public boolean hasExpired() {
    if (Objects.isNull(createdAt)) {
      return true;
    }

    createdAt = createdAt.plusMinutes(30);
    LocalDateTime now = LocalDateTime.now();

    return createdAt.isBefore(now);
  }
}
