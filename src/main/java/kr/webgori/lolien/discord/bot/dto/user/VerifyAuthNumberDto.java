package kr.webgori.lolien.discord.bot.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class VerifyAuthNumberDto {
  private String authNumber;
}
