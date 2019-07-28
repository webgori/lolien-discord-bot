package kr.webgori.lolien.discord.bot.entity.league;

import lombok.*;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "lolien_league_team_bans")
@EqualsAndHashCode(exclude = {"teamStats"})
public class LoLienLeagueTeamBans {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  @ManyToOne
  @JoinColumn(name = "team_stats_idx", nullable = false)
  private LoLienLeagueTeamStats teamStats;

  @Column(name = "champion_id")
  private Integer championId;
  @Column(name = "pick_turn")
  private Integer pickTurn;
}