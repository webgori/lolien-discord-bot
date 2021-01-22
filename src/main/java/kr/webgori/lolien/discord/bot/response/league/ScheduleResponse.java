package kr.webgori.lolien.discord.bot.response.league;

import java.util.List;
import kr.webgori.lolien.discord.bot.dto.league.ScheduleDto;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ScheduleResponse {
  private List<ScheduleDto> schedules;
}
