package kr.webgori.lolien.discord.bot.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGameTeamDto {
  private List<CustomGameTeamBanDto> bans;
}
