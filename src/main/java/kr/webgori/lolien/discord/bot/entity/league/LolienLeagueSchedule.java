package kr.webgori.lolien.discord.bot.entity.league;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
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
@Table(name = "lolien_league_schedule")
public class LolienLeagueSchedule {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  @OneToOne
  @JoinColumn(name = "team_idx")
  private LolienLeagueTeam teamIndex;

  @OneToOne
  @JoinColumn(name = "enemy_team_idx")
  private LolienLeagueTeam enemyTeamIndex;

  @Column(name = "match_date_time", nullable = false)
  private LocalDateTime matchDateTime;
}