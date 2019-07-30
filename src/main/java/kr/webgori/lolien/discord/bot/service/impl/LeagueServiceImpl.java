package kr.webgori.lolien.discord.bot.service.impl;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import kr.webgori.lolien.discord.bot.component.LeagueComponent;
import kr.webgori.lolien.discord.bot.entity.league.LoLienLeague;
import kr.webgori.lolien.discord.bot.repository.league.LoLienLeagueMatchRepository;
import kr.webgori.lolien.discord.bot.repository.league.LoLienLeagueRepository;
import kr.webgori.lolien.discord.bot.request.LeagueAddRequest;
import kr.webgori.lolien.discord.bot.request.LeagueAddResultRequest;
import kr.webgori.lolien.discord.bot.response.LeagueGetLeagueResponse;
import kr.webgori.lolien.discord.bot.response.LeagueGetLeaguesResponse;
import kr.webgori.lolien.discord.bot.service.LeagueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
@RequiredArgsConstructor
@Service
public class LeagueServiceImpl implements LeagueService {
  private final LoLienLeagueRepository loLienLeagueRepository;
  private final LeagueComponent leagueComponent;
  private final LoLienLeagueMatchRepository loLienLeagueMatchRepository;

  @Transactional(readOnly = true)
  @Override
  public LeagueGetLeaguesResponse getLeagues() {
    List<LoLienLeague> loLienLeagues = loLienLeagueRepository.findAll();
    List<LeagueGetLeagueResponse> leagues = Lists.newArrayList();

    for (LoLienLeague loLienLeague : loLienLeagues) {
      int idx = loLienLeague.getIdx();
      String title = loLienLeague.getTitle();
      LocalDateTime createdDate = loLienLeague.getCreatedDate();

      LeagueGetLeagueResponse leagueGetLeagueResponse = LeagueGetLeagueResponse
          .builder()
          .idx(idx)
          .title(title)
          .createdDate(createdDate)
          .build();

      leagues.add(leagueGetLeagueResponse);
    }

    return LeagueGetLeaguesResponse.builder().leagues(leagues).build();
  }

  @Transactional
  @Override
  public void addLeague(LeagueAddRequest leagueAddRequest) {
    String title = leagueAddRequest.getTitle();
    LoLienLeague loLienLeague = LoLienLeague.builder().title(title).build();
    loLienLeagueRepository.save(loLienLeague);
  }

  @Transactional
  @Override
  public void addLeagueResult(LeagueAddResultRequest leagueAddResultRequest) {
    long matchId = leagueAddResultRequest.getMatchId();

    boolean existsByGameId = loLienLeagueMatchRepository.existsByGameId(matchId);

    if (existsByGameId) {
      throw new IllegalArgumentException("이미 등록된 리그 결과 입니다.");
    }

    String entriesString = leagueAddResultRequest.getEntries();

    String[] entries = entriesString.split(",");

    if (entries.length != 10) {
      throw new IllegalArgumentException("게임 참여 인원이 잘못 되었습니다.");
    }

    int leagueIdx = leagueAddResultRequest.getLeagueIdx();

    leagueComponent.addResult(leagueIdx, matchId, entries);
  }
}