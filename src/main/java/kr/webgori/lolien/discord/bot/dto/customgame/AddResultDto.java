package kr.webgori.lolien.discord.bot.dto.customgame;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import kr.webgori.lolien.discord.bot.entity.LolienMatch;
import kr.webgori.lolien.discord.bot.entity.LolienParticipant;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.LolienTeamStats;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.rithms.riot.api.endpoints.match.dto.Match;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class AddResultDto {
  private String[] entries;
  private Match match;
  @Builder.Default
  private Set<LolienParticipant> lolienParticipantSet = Sets.newHashSet();
  @Builder.Default
  private Set<LolienTeamStats> lolienTeamStatsSet = Sets.newHashSet();
  private LolienMatch lolienMatch;
  @Builder.Default
  private List<LolienSummoner> lolienSummoners = Lists.newArrayList();

  public void addLolienSummoner(LolienSummoner lolienSummoner) {
    this.lolienSummoners.add(lolienSummoner);
  }
}
