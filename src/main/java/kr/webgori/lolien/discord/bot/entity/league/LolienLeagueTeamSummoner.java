package kr.webgori.lolien.discord.bot.entity.league;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "lolien_league_team_summoner")
public class LolienLeagueTeamSummoner {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  @ManyToOne
  @JoinColumn(name = "team_idx", nullable = false)
  private LolienLeagueTeam team;

  @OneToOne
  @JoinColumn(name = "summoner_idx", unique = true)
  private LolienSummoner summoner;
}
