package kr.webgori.lolien.discord.bot.component;

import kr.webgori.lolien.discord.bot.dto.customgame.AddResultDto;
import kr.webgori.lolien.discord.bot.entity.LolienMatch;
import kr.webgori.lolien.discord.bot.repository.LolienMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class GameTransactionComponent {
  private final LolienMatchRepository lolienMatchRepository;

  @Transactional
  public void addResult(AddResultDto addResultDto) {
    LolienMatch lolienMatch = addResultDto.getLolienMatch();
    saveLolienMatch(lolienMatch);
  }

  private void saveLolienMatch(LolienMatch lolienMatch) {
    lolienMatchRepository.save(lolienMatch);
  }
}
