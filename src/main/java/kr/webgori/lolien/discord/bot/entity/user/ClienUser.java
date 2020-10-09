package kr.webgori.lolien.discord.bot.entity.user;

import java.time.LocalDateTime;
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
import org.hibernate.annotations.CreationTimestamp;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "clien_user")
public class ClienUser {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "`index`")
  private Integer index;

  @Column(name = "clien_id", nullable = false)
  private String clienId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}
