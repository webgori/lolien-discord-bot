package kr.webgori.lolien.discord.bot.service.impl;

import com.google.common.collect.Lists;
import java.time.LocalDateTime;
import java.util.List;
import kr.webgori.lolien.discord.bot.component.LeagueComponent;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeague;
import kr.webgori.lolien.discord.bot.repository.league.LolienLeagueMatchRepository;
import kr.webgori.lolien.discord.bot.repository.league.LolienLeagueRepository;
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
@RequiredArgsConstructor
@Service
public class LeagueServiceImpl implements LeagueService {
  private final LolienLeagueRepository lolienLeagueRepository;
  private final LeagueComponent leagueComponent;
  private final LolienLeagueMatchRepository lolienLeagueMatchRepository;

  @Transactional(readOnly = true)
  @Override
  public LeagueGetLeaguesResponse getLeagues() {
    List<LolienLeague> lolienLeagues = lolienLeagueRepository.findAll();
    List<LeagueGetLeagueResponse> leagues = Lists.newArrayList();

    for (LolienLeague lolienLeague : lolienLeagues) {
      int idx = lolienLeague.getIdx();
      String title = lolienLeague.getTitle();
      LocalDateTime createdDate = lolienLeague.getCreatedDate();

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
    LolienLeague lolienLeague = LolienLeague.builder().title(title).build();
    lolienLeagueRepository.save(lolienLeague);
  }

  @Transactional
  @Override
  public void addLeagueResult(LeagueAddResultRequest leagueAddResultRequest) {
    long matchId = leagueAddResultRequest.getMatchId();

    boolean existsByGameId = lolienLeagueMatchRepository.existsByGameId(matchId);

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