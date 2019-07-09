package kr.webgori.lolien.discord.bot.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "memo")
public class Memo {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  @Column(nullable = false)
  private String writer;

  @Column(nullable = false, unique = true)
  private String word;

  @Lob
  @Column(nullable = false)
  private String description;

  @CreatedDate
  @Column(name = "created_date", nullable = false)
  private LocalDateTime createdDate;

  @LastModifiedDate
  @Column(name = "last_modified_date", nullable = false)
  private LocalDateTime lastModifiedDate;
}