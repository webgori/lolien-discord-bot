package kr.webgori.lolien.discord.bot.service;

import kr.webgori.lolien.discord.bot.request.LeagueAddRequest;
import kr.webgori.lolien.discord.bot.request.LeagueAddResultRequest;
import kr.webgori.lolien.discord.bot.response.LeagueGetLeaguesResponse;

public interface LeagueService {
  LeagueGetLeaguesResponse getLeagues();

  void addLeague(LeagueAddRequest leagueAddRequest);

  void addLeagueResult(LeagueAddResultRequest leagueAddResultRequest);
}