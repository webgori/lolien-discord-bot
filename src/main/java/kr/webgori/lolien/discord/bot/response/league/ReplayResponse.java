package kr.webgori.lolien.discord.bot.response.league;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@SuppressFBWarnings(justification = "Generated code")
@Builder
@Data
public class ReplayResponse {
  @Schema(description = "리플레이 데이터")
  private byte[] replayData;
}
