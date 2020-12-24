package kr.webgori.lolien.discord.bot.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
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
@Table(name = "user_role")
public class UserRole {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "`index`")
  private Integer index;

  @JsonIgnore
  @OneToOne
  @JoinColumn(name = "user_index", unique = true, nullable = false)
  private User user;

  @OneToOne
  @JoinColumn(name = "role_index", nullable = false)
  private Role role;
}
