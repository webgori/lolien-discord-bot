package kr.webgori.lolien.discord.bot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import kr.webgori.lolien.discord.bot.request.CustomGameAddResultRequest;
import kr.webgori.lolien.discord.bot.response.CustomGamesResponse;
import kr.webgori.lolien.discord.bot.service.CustomGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class CustomGameController {
  private final CustomGameService customGameService;

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
  @PostMapping("v1/custom-game/result")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void addResult(@RequestBody CustomGameAddResultRequest customGameAddResultRequest) {
    customGameService.addResult(customGameAddResultRequest);
  }

  @Operation(
      summary = "최근 내전 조회")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "No Content",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = CustomGamesResponse.class)))
      })
  @GetMapping("v1/custom-game")
  public CustomGamesResponse getCustomGames() {
    return customGameService.getCustomGames();
  }

  @Operation(
      summary = "소환사 이름으로 내전 조회")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "No Content",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = CustomGamesResponse.class)))
      })
  @GetMapping("v1/custom-game/{summoner-name}")
  public CustomGamesResponse getCustomGamesBySummoner(
      @PathVariable("summoner-name") String summonerName) {
    return customGameService.getCustomGamesBySummoner(summonerName);
  }
}
