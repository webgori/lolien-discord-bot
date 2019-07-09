package kr.webgori.lolien.discord.bot.repository;

import kr.webgori.lolien.discord.bot.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo, Integer> {
    Boolean existsByWord(String word);

    Memo findByWord(String word);

    void deleteByWord(String word);
}