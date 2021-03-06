package kr.webgori.lolien.discord.bot.entity.league;

import java.util.Set;
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
import kr.webgori.lolien.discord.bot.entity.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "lolien_league_match")
@ToString(exclude = {"participants", "teams"})
@EqualsAndHashCode(exclude = {"teams", "participants"})
public class LolienLeagueMatch {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  @Column(name = "game_creation", nullable = false)
  private Long gameCreation;

  @Column(name = "game_duration", nullable = false)
  private Long gameDuration;

  @Column(name = "game_id", unique = true, nullable = false)
  private Long gameId;

  @Column(name = "game_mode", nullable = false)
  private String gameMode;

  @Column(name = "game_type", nullable = false)
  private String gameType;

  @Column(name = "game_version", nullable = false)
  private String gameVersion;

  @Column(name = "map_id", nullable = false)
  private Integer mapId;

  @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private Set<LolienLeagueParticipant> participants;

  @Column(name = "platform_id", nullable = false)
  private String platformId;

  @Column(name = "queue_id", nullable = false)
  private Integer queueId;

  @Column(name = "season_id", nullable = false)
  private Integer seasonId;

  @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<LolienLeagueTeamStats> teams;

  @ManyToOne
  @JoinColumn(name = "lolien_league_idx", nullable = false)
  private LolienLeague lolienLeague;

  @ManyToOne
  @JoinColumn(name = "lolien_league_schedule_idx", nullable = false)
  private LolienLeagueSchedule schedule;

  @ManyToOne
  @JoinColumn(name = "user_idx", nullable = false)
  private User user;
}
