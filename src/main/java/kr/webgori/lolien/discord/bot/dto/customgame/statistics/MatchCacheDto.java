package kr.webgori.lolien.discord.bot.dto.customgame.statistics;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.LolienMatch;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MatchCacheDto {
  private List<LolienMatch> matches;
}
