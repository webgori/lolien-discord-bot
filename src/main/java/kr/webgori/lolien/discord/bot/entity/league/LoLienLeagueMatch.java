package kr.webgori.lolien.discord.bot.entity.league;

import lombok.*;

import javax.persistence.*;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "lolien_league_match")
@ToString(exclude = {"participants", "teams"})
@EqualsAndHashCode(exclude = {"teams", "participants"})
public class LoLienLeagueMatch {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  @Column(name = "game_creation")
  private Long gameCreation;

  @Column(name = "game_duration")
  private Long gameDuration;

  @Column(name = "game_id")
  private Long gameId;

  @Column(name = "game_mode")
  private String gameMode;

  @Column(name = "game_type")
  private String gameType;

  @Column(name = "game_version")
  private String gameVersion;

  @Column(name = "map_id")
  private Integer mapId;

  @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private Set<LoLienLeagueParticipant> participants;

  @Column(name = "platform_id")
  private String platformId;

  @Column(name = "queue_id")
  private Integer queueId;

  @Column(name = "season_id")
  private Integer seasonId;

  @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<LoLienLeagueTeamStats> teams;

  @ManyToOne
  @JoinColumn(name = "lolien_league_idx", nullable = false)
  private LoLienLeague lolienLeague;
}