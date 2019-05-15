package kr.webgori.lolien.discord.bot.entity;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
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
@Table(name = "`match`")
public class LoLienMatch {
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
  private List<LoLienParticipant> participants;

  @Column(name = "platform_id")
  private String platformId;
  @Column(name = "queue_id")
  private Integer queueId;
  @Column(name = "season_id")
  private Integer seasonId;

  @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<LoLienTeamStats> teams;
}