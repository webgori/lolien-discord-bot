
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
public class SessionUserDto {
  private String id;
  private String password;
  private LocalDateTime createdAt;

  public boolean hasExpired() {
    if (Objects.isNull(createdAt)) {
      return true;
    }

    createdAt = createdAt.plusDays(90);
    LocalDateTime now = LocalDateTime.now();

    return createdAt.isBefore(now);
  }
}
