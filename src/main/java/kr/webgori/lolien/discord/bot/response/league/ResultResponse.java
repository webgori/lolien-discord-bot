package kr.webgori.lolien.discord.bot.response.league;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ResultResponse {
  private List<ResultDto> results;
  private int totalPages;
}
