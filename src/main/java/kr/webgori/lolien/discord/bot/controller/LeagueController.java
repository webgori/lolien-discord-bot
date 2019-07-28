package kr.webgori.lolien.discord.bot.controller;

import kr.webgori.lolien.discord.bot.request.LeagueAddRequest;
import kr.webgori.lolien.discord.bot.request.LeagueAddResultRequest;
import kr.webgori.lolien.discord.bot.response.LeagueGetLeaguesResponse;
import kr.webgori.lolien.discord.bot.service.LeagueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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