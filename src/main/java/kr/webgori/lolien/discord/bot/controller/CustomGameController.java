package kr.webgori.lolien.discord.bot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import kr.webgori.lolien.discord.bot.response.CustomGamesResponse;
import kr.webgori.lolien.discord.bot.response.StatisticsResponse;
import kr.webgori.lolien.discord.bot.response.league.StatisticsPickResponse;
import kr.webgori.lolien.discord.bot.service.CustomGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
public class CustomGameController {
  private final CustomGameService customGameService;

  @Operation(
      summary = "최근 내전 조회")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "OK",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = CustomGamesResponse.class)))
      })
  @GetMapping("v1/custom-game")
  public CustomGamesResponse getCustomGames(
      @RequestParam(value = "page", defaultValue = "1") int page,
      @RequestParam(value = "size", defaultValue = "5") int size) {
    return customGameService.getCustomGames(page, size);
  }

  @Operation(
      summary = "소환사 이름으로 내전 조회")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "OK",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = CustomGamesResponse.class)))
      })
  @GetMapping("v1/custom-game/{summoner-name}")
  public CustomGamesResponse getCustomGamesBySummoner(
      @PathVariable("summoner-name") String summonerName,
      @RequestParam(value = "page", defaultValue = "1") int page,
      @RequestParam(value = "size", defaultValue = "5") int size) {
    return customGameService.getCustomGamesBySummoner(summonerName, page, size);
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
  @GetMapping("v1/custom-game/statistics")
  public StatisticsResponse getStatistics() {
    return customGameService.getStatistics();
  }

  @Operation(
      summary = "내전 결과 제거",
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
  @DeleteMapping("v1/custom-game/result/{game-id}")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void deleteResult(@PathVariable("game-id") long gameId) {
    customGameService.deleteResult(gameId);
  }

  @Operation(
      summary = "리플레이 파일로 결과 등록",
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
  @PostMapping("v1/custom-game/result/files")
  public void addResultByFiles(List<MultipartFile> files) {
    customGameService.addResultByFiles(files);
  }

  @Operation(
      summary = "리플레이 다운로드")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "OK")
      })
  @GetMapping(value = "v1/custom-game/replay", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public HttpEntity<byte[]> getReplay(@RequestParam("match-index") int matchIndex) {
    return customGameService.getReplay(matchIndex);
  }
}
