package kr.webgori.lolien.discord.bot.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
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
@Table(name = "league")
public class League {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  @Column
  private String tier;

  @Column
  private String season;

  @JsonManagedReference(value = "leagueLolienSummoner")
  @ManyToOne
  @JoinColumn(name = "summoner_idx", nullable = false)
  private LolienSummoner lolienSummoner;
}