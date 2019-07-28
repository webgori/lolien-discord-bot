package kr.webgori.lolien.discord.bot.entity.league;

import kr.webgori.lolien.discord.bot.entity.LoLienSummoner;
import lombok.*;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "lolien_league_participant")
@ToString(exclude = {"match"})
public class LoLienLeagueParticipant {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  @ManyToOne
  @JoinColumn(name = "match_idx", nullable = false)
  private LoLienLeagueMatch match;

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
  private LoLienLeagueParticipantStats stats;

  @Column(name = "team_id")
  private Integer teamId;

  @ManyToOne
  @JoinColumn(name = "summoner_idx", nullable = false)
  private LoLienSummoner loLienSummoner;
}