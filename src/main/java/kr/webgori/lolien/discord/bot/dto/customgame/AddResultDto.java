package kr.webgori.lolien.discord.bot.dto.customgame;

import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import kr.webgori.lolien.discord.bot.entity.LolienMatch;
import kr.webgori.lolien.discord.bot.entity.LolienParticipant;
import kr.webgori.lolien.discord.bot.entity.LolienTeamStats;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.match.dto.Participant;

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
}
