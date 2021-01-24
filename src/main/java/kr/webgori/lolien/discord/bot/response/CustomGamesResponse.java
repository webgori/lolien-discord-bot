package kr.webgori.lolien.discord.bot.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGamesResponse {
  private List<CustomGameDto> customGames;
  private int totalPages;
}