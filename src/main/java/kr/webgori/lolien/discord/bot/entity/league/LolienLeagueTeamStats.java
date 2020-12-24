package kr.webgori.lolien.discord.bot.entity.league;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "lolien_league_team_stats")
@ToString(exclude = {"match", "bans"})
public class LolienLeagueTeamStats {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  @JsonManagedReference(value = "lolienLeagueTeamStatsMatch")
  @ManyToOne
  @JoinColumn(name = "match_idx", nullable = false)
  private LolienLeagueMatch match;

  @JsonBackReference(value = "lolienLeagueTeamBansTeamStats")
  @OneToMany(mappedBy = "teamStats", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<LolienLeagueTeamBans> bans;

  @Column(name = "baron_kills")
  private Integer baronKills;
  @Column(name = "dominion_victory_score")
  private Integer dominionVictoryScore;
  @Column(name = "dragon_kills")
  private Integer dragonKills;
  @Column(name = "first_baron")
  private Boolean firstBaron;
  @Column(name = "first_blood")
  private Boolean firstBlood;
  @Column(name = "first_dragon")
  private Boolean firstDragon;
  @Column(name = "first_inhibitor")
  private Boolean firstInhibitor;
  @Column(name = "first_rift_herald")
  private Boolean firstRiftHerald;
  @Column(name = "first_tower")
  private Boolean firstTower;
  @Column(name = "inhibitor_kills")
  private Integer inhibitorKills;
  @Column(name = "rift_herald_kills")
  private Integer riftHeraldKills;
  @Column(name = "team_id")
  private Integer teamId;
  @Column(name = "tower_kills")
  private Integer towerKills;
  @Column(name = "vilemaw_kills")
  private Integer vilemawKills;
  private String win;
}