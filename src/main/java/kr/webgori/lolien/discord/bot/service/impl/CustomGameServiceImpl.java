package kr.webgori.lolien.discord.bot.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import kr.webgori.lolien.discord.bot.component.CustomGameComponent;
import kr.webgori.lolien.discord.bot.repository.LoLienMatchRepository;
import kr.webgori.lolien.discord.bot.request.CustomGameAddResultRequest;
import kr.webgori.lolien.discord.bot.service.CustomGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
@RequiredArgsConstructor
@Service
public class CustomGameServiceImpl implements CustomGameService {
  private final LoLienMatchRepository loLienMatchRepository;
  private final CustomGameComponent customGameComponent;

  @Transactional
  @Override
  public void addResult(CustomGameAddResultRequest customGameAddResultRequest) {
    long matchId = customGameAddResultRequest.getMatchId();
    String entriesString = customGameAddResultRequest.getEntries();

    boolean existsByGameId = loLienMatchRepository.existsByGameId(matchId);

    if (existsByGameId) {
      throw new IllegalArgumentException("이미 등록된 리그 결과 입니다.");
    }

    String[] entries = entriesString.split(",");

    if (entries.length != 10) {
      throw new IllegalArgumentException("게임 참여 인원이 잘못 되었습니다.");
    }

    customGameComponent.addResult(matchId, entries);
  }
}