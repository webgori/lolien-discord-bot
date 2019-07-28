package kr.webgori.lolien.discord.bot.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import kr.webgori.lolien.discord.bot.component.LeagueComponent;
import kr.webgori.lolien.discord.bot.entity.league.LoLienLeague;
import kr.webgori.lolien.discord.bot.exception.LeagueAlreadyAddedResultException;
import kr.webgori.lolien.discord.bot.exception.LeagueExactEntriesNumberRequiredException;
import kr.webgori.lolien.discord.bot.repository.league.LoLienLeagueMatchRepository;
import kr.webgori.lolien.discord.bot.repository.league.LoLienLeagueRepository;
import kr.webgori.lolien.discord.bot.request.LeagueAddRequest;
import kr.webgori.lolien.discord.bot.request.LeagueAddResultRequest;
import kr.webgori.lolien.discord.bot.response.LeagueGetLeaguesResponse;
import kr.webgori.lolien.discord.bot.service.LeagueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
@RequiredArgsConstructor
@Service
public class LeagueServiceImpl implements LeagueService {
  private final JDA jda;
  private final LoLienLeagueRepository loLienLeagueRepository;
  private final LeagueComponent leagueComponent;
  private final LoLienLeagueMatchRepository loLienLeagueMatchRepository;

  @Transactional(readOnly = true)
  @Override
  public LeagueGetLeaguesResponse getLeagues() {
    List<LoLienLeague> leagues = loLienLeagueRepository.findAll();
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
      throw new LeagueAlreadyAddedResultException("already added result");
    }

    String entriesString = leagueAddResultRequest.getEntries();

    String[] entries = entriesString.split(",");

    if (entries.length != 10) {
      String message = String.format("required entries 10 but %d", entries.length);
      throw new LeagueExactEntriesNumberRequiredException(message);
    }

    int leagueIdx = leagueAddResultRequest.getLeagueIdx();

    leagueComponent.addResult(leagueIdx, matchId, entries);
  }
}