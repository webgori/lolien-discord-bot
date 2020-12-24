package kr.webgori.lolien.discord.bot.entity.league;

import com.fasterxml.jackson.annotation.JsonBackReference;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
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
  @JoinColumn(name = "team_idx", nullable = false)
  private LolienLeagueTeam team;

  @OneToOne
  @JoinColumn(name = "enemy_team_idx", nullable = false)
  private LolienLeagueTeam enemyTeam;

  @JsonBackReference(value = "lolienLeagueMatchSchedule")
  @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL)
  private List<LolienLeagueMatch> match;

  @Column(name = "match_date_time", nullable = false)
  private LocalDateTime matchDateTime;

  @Column(nullable = false)
  private String description1;

  @Column(nullable = false)
  private String description2;

  @Column(nullable = false)
  private String description3;
}
