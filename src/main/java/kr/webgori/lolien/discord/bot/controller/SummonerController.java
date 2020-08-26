package kr.webgori.lolien.discord.bot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import kr.webgori.lolien.discord.bot.service.SummonerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class SummonerController {
  private final SummonerService summonerService;

  @Operation(
      summary = "소환사 존재 여부 확인")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "204",
              description = "No Content",
              content = @Content(
                  mediaType = "application/json"))
      })
  @GetMapping("v1/summoners/{summoner-name}")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void existSummonerBySummonerName(
      @PathVariable("summoner-name") String summonerName) {
    summonerService.existSummonerBySummonerName(summonerName);
  }
}
