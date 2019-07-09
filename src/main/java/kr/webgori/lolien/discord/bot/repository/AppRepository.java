package kr.webgori.lolien.discord.bot.repository;

import kr.webgori.lolien.discord.bot.entity.App;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRepository extends JpaRepository<App, Integer> {

}