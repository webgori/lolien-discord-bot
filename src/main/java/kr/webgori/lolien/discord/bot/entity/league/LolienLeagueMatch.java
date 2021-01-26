package kr.webgori.lolien.discord.bot.entity.league;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
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

@SuppressFBWarnings(justification = "Generated code")
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

  @Column(name = "game_creation")
  private Long gameCreation;

  @Column(name = "game_duration")
  private Long gameDuration;

  @Column(name = "game_id", unique = true)
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
  private Set<LolienLeagueParticipant> participants;

  @Column(name = "platform_id")
  private String platformId;

  @Column(name = "queue_id")
  private Integer queueId;

  @Column(name = "season_id")
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

  @Lob
  @Column(columnDefinition = "LONGBLOB", nullable = false)
  private byte[] replay;
}
