package kr.webgori.lolien.discord.bot.entity.user;

import com.fasterxml.jackson.annotation.JsonBackReference;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import kr.webgori.lolien.discord.bot.entity.LolienMatch;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueMatch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@EqualsAndHashCode(exclude = {"userRole", "lolienSummoner", "clienUser"})
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "user")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "`index`")
  private Integer index;

  @Column(nullable = false)
  private String email;

  @Column(name = "email_verified", nullable = false)
  private Boolean emailVerified;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private String nickname;

  @OneToOne(mappedBy = "user")
  private UserRole userRole;

  @OneToOne
  @JoinColumn(name = "clien_user_index", unique = true)
  private ClienUser clienUser;

  @OneToOne
  @JoinColumn(name = "summoner_index", unique = true)
  private LolienSummoner lolienSummoner;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @JsonBackReference(value = "lolienMatchUser")
  @OneToMany(mappedBy = "user")
  private List<LolienMatch> matches;

  @JsonBackReference(value = "lolienLeagueMatchUser")
  @OneToMany(mappedBy = "user")
  private List<LolienLeagueMatch> leagueMatches;
}
