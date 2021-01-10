package kr.webgori.lolien.discord.bot.entity;

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

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "user_position")
public class UserPosition {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "`index`")
  private Integer index;

  @OneToOne
  @JoinColumn(name = "position_index", nullable = false)
  private Position position;

  @ManyToOne
  @JoinColumn(name = "summoner_idx", nullable = false)
  private LolienSummoner lolienSummoner;
}
