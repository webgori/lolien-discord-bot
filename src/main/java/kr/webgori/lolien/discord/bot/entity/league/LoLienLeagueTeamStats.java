package kr.webgori.lolien.discord.bot.entity.league;

import lombok.*;

import javax.persistence.*;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "team_stats")
@ToString(exclude = {"match", "bans"})
public class LoLienLeagueTeamStats {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  @ManyToOne
  @JoinColumn(name = "match_idx", nullable = false)
  private LoLienLeagueMatch match;

  @OneToMany(mappedBy = "teamStats", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<LoLienLeagueTeamBans> bans;

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