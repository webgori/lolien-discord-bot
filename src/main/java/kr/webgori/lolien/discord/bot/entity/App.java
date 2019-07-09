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
@Table(name = "app")
public class App {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  @Column(name = "api_key")
  private String apiKey;
}