package kr.webgori.lolien.discord.bot.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
@Table(name = "`match`")
@ToString(exclude = {"participants", "teams"})
@EqualsAndHashCode(exclude = {"teams", "participants"})
public class LolienMatch {
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

  @JsonBackReference(value = "lolienParticipantMatch")
  @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private Set<LolienParticipant> participants;

  @Column(name = "platform_id")
  private String platformId;
  @Column(name = "queue_id")
  private Integer queueId;
  @Column(name = "season_id")
  private Integer seasonId;

  @JsonBackReference(value = "lolienTeamStatsMatch")
  @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<LolienTeamStats> teams;

  @JsonManagedReference(value = "lolienMatchUser")
  @ManyToOne
  @JoinColumn(name = "user_idx", nullable = false)
  private User user;
}
