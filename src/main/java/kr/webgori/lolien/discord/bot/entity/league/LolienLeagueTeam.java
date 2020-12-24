package kr.webgori.lolien.discord.bot.entity.league;

import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "lolien_league_team")
public class LolienLeagueTeam {
  @Id
  private Integer idx;

  @Column(name = "korean_name", nullable = false)
  private String koreanName;

  @Column(name = "english_name", nullable = false)
  private String englishName;

  @JsonBackReference(value = "lolienLeagueTeamSummonerTeam")
  @OneToMany(mappedBy = "team")
  private List<LolienLeagueTeamSummoner> teamSummoners;
}
