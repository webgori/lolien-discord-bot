package kr.webgori.lolien.discord.bot.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
@Table(name = "team_bans")
public class LoLienTeamBans {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  @ManyToOne
  @JoinColumn(name = "team_stats_idx", nullable = false)
  private LoLienTeamStats teamStats;

  @Column(name = "champion_id")
  private Integer championId;
  @Column(name = "pick_turn")
  private Integer pickTurn;
}