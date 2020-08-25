package kr.webgori.lolien.discord.bot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import kr.webgori.lolien.discord.bot.request.LeagueAddRequest;
import kr.webgori.lolien.discord.bot.request.LeagueAddResultRequest;
import kr.webgori.lolien.discord.bot.response.LeagueGetLeaguesResponse;
import kr.webgori.lolien.discord.bot.service.LeagueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class LeagueController {
  private final LeagueService leagueService;

  @Operation(
      summary = "리그 목록 조회")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "OK",
              content = @Content(
                  mediaType = "application/json"))
      })
  @GetMapping("v1/leagues")
  public LeagueGetLeaguesResponse getLeagues() {
    return leagueService.getLeagues();
  }

  @Operation(
      summary = "리그 추가")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "201",
              description = "No Content",
              content = @Content(
                  mediaType = "application/json"))
      })
  @PostMapping("v1/leagues")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void addLeague(@RequestBody LeagueAddRequest leagueAddRequest) {
    leagueService.addLeague(leagueAddRequest);
  }

  @Operation(
      summary = "리그 제거")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "201",
              description = "No Content",
              content = @Content(
                  mediaType = "application/json"))
      })
  @DeleteMapping("v1/leagues/{league-idx}")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void deleteLeague(@PathVariable("league-idx") int leagueIdx) {
    leagueService.deleteLeague(leagueIdx);
  }

  @Operation(
      summary = "내전 결과 등록",
      hidden = true)
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized. 인증 정보를 찾을 수 없을 때"),
          @ApiResponse(
              responseCode = "204",
              description = "No Content")})
  @PostMapping("v1/leagues/result")
  public void addLeagueResult(@RequestBody LeagueAddResultRequest leagueAddResultRequest) {
    leagueService.addLeagueResult(leagueAddResultRequest);
  }
}