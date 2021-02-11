package kr.webgori.lolien.discord.bot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDate;
import java.util.List;
import kr.webgori.lolien.discord.bot.request.LeagueAddRequest;
import kr.webgori.lolien.discord.bot.response.league.LeagueResponse;
import kr.webgori.lolien.discord.bot.response.league.ResultResponse;
import kr.webgori.lolien.discord.bot.response.league.ScheduleResponse;
import kr.webgori.lolien.discord.bot.response.league.StatisticsPickResponse;
import kr.webgori.lolien.discord.bot.response.league.StatisticsResponse;
import kr.webgori.lolien.discord.bot.response.league.SummonerForParticipationResponse;
import kr.webgori.lolien.discord.bot.response.league.TeamResponse;
import kr.webgori.lolien.discord.bot.service.LeagueService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
  public LeagueResponse getLeagues() {
    return leagueService.getLeagues();
  }

  @Operation(
      summary = "리그 추가",
      security = {
          @SecurityRequirement(name = "JWT")
      })
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
      summary = "리그 제거",
      security = {
          @SecurityRequirement(name = "JWT")
      })
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
      summary = "리플레이 파일로 리그 결과 등록",
      security = {
          @SecurityRequirement(name = "JWT")
      })
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized. 인증 정보를 찾을 수 없을 때"),
          @ApiResponse(
              responseCode = "204",
              description = "No Content")})
  @PostMapping("v1/leagues/result/files")
  public void addLeagueResult(int leagueIndex, int scheduleIdx, List<MultipartFile> files) {
    leagueService.addLeagueResultByFiles(leagueIndex, scheduleIdx, files);
  }

  @Operation(
      summary = "리그 결과 제거",
      security = {
          @SecurityRequirement(name = "JWT")
      })
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "204",
              description = "No Content",
              content = @Content(
                  mediaType = "application/json"))
      })
  @DeleteMapping("v1/leagues/result/{game-id}")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void deleteLeagueResult(@PathVariable("game-id") long gameId) {
    leagueService.deleteLeagueResult(gameId);
  }

  @Operation(
      summary = "리그 참가 가능한 소환사 조회")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "OK",
              content = @Content(
                  mediaType = "application/json"))
      })
  @GetMapping("v1/leagues/summoners/participation")
  public SummonerForParticipationResponse getSummonersForParticipation(
      @RequestParam("startDate")
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam("endDate")
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    return leagueService.getSummonersForParticipation(startDate, endDate);
  }

  @Operation(
      summary = "진행 결과 조회")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "OK",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ResultResponse.class)))
      })
  @GetMapping("v1/leagues/{league-index}")
  public ResultResponse getLeagueResultsByLeague(
      @PathVariable("league-index") int leagueIndex,
      @RequestParam(value = "scheduleIdx") int scheduleIdx,
      @RequestParam(value = "page", defaultValue = "1") int page,
      @RequestParam(value = "size", defaultValue = "5") int size) {
    return leagueService.getLeagueResultsByLeague(leagueIndex, scheduleIdx, page, size);
  }

  @Operation(
      summary = "팀 정보 조회")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "OK",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = TeamResponse.class)))
      })
  @GetMapping("v1/leagues/team")
  public TeamResponse getTeams() {
    return leagueService.getTeams();
  }

  @Operation(
      summary = "대진표 조회")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "OK",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ScheduleResponse.class)))
      })
  @GetMapping("v1/leagues/{league-idx}/schedule")
  public ScheduleResponse getSchedules(@PathVariable("league-idx") int leagueIdx, String order) {
    return leagueService.getSchedules(leagueIdx, order);
  }

  @Operation(
      summary = "통계 조회")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "OK",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatisticsResponse.class)))
      })
  @GetMapping("v1/leagues/{league-idx}/statistics")
  public StatisticsResponse getStatistics(@PathVariable("league-idx") int leagueIdx) {
    return leagueService.getStatistics(leagueIdx);
  }

  @Operation(
      summary = "픽 통계 조회")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "OK",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatisticsPickResponse.class)))
      })
  @GetMapping("v1/leagues/{league-idx}/statistics/pick")
  public StatisticsPickResponse getStatisticsPick(@PathVariable("league-idx") int leagueIdx) {
    return leagueService.getStatisticsPick(leagueIdx);
  }
}
