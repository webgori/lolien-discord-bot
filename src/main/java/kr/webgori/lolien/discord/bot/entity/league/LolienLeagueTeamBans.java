package kr.webgori.lolien.discord.bot.entity.league;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "lolien_league_team_bans")
@EqualsAndHashCode(exclude = {"teamStats"})
public class LolienLeagueTeamBans {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  @ManyToOne
  @JoinColumn(name = "team_stats_idx", nullable = false)
  private LolienLeagueTeamStats teamStats;

  @Column(name = "champion_id")
  private Integer championId;
  @Column(name = "pick_turn")
  private Integer pickTurn;
}