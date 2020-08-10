package kr.webgori.lolien.discord.bot.entity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
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
@Table(name = "participant")
@ToString(exclude = {"match"})
public class LolienParticipant {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  @ManyToOne
  @JoinColumn(name = "match_idx", nullable = false)
  private LolienMatch match;

  @Column(name = "champion_id")
  private Integer championId;
  @Column(name = "participant_id")
  private Integer participantId;
  @Column(name = "spell1_id")
  private Integer spell1Id;
  @Column(name = "spell2_id")
  private Integer spell2Id;

  @OneToOne(mappedBy = "participant", cascade = CascadeType.ALL)
  @JoinColumn(name = "participant_stats_idx")
  private LolienParticipantStats stats;

  @Column(name = "team_id")
  private Integer teamId;

  @ManyToOne
  @JoinColumn(name = "summoner_idx", nullable = false)
  private LolienSummoner lolienSummoner;
}