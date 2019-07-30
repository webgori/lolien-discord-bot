package kr.webgori.lolien.discord.bot.controller;

import kr.webgori.lolien.discord.bot.request.LeagueAddRequest;
import kr.webgori.lolien.discord.bot.request.LeagueAddResultRequest;
import kr.webgori.lolien.discord.bot.response.LeagueGetLeaguesResponse;
import kr.webgori.lolien.discord.bot.service.LeagueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("league")
public class LeagueController {
  private final LeagueService leagueService;

  @GetMapping
  public LeagueGetLeaguesResponse getLeagues() {
    return leagueService.getLeagues();
  }

  @PostMapping
  public void addLeague(@RequestBody LeagueAddRequest leagueAddRequest) {
    leagueService.addLeague(leagueAddRequest);
  }

  @PostMapping("result")
  public void addLeagueResult(@RequestBody LeagueAddResultRequest leagueAddResultRequest) {
    leagueService.addLeagueResult(leagueAddResultRequest);
  }
}