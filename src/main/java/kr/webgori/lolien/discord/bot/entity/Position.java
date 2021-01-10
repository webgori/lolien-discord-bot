package kr.webgori.lolien.discord.bot.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
@Table(name = "`position`")
public class Position {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "`index`")
  private Integer index;

  @Column(name = "`position`")
  private String position;
}
