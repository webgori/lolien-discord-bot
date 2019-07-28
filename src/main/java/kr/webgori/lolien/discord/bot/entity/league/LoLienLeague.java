package kr.webgori.lolien.discord.bot.entity.league;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "lolien_league")
public class LoLienLeague {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  private String title;

  @OneToMany(mappedBy = "lolienLeague", cascade = CascadeType.ALL)
  private List<LoLienLeagueMatch> loLienLeagueMatches;

  @CreatedDate
  @Column(name = "created_date", nullable = false)
  private LocalDateTime createdDate;
}