package kr.webgori.lolien.discord.bot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import javax.validation.Valid;
import kr.webgori.lolien.discord.bot.request.LolienUserAddSummonerRequest;
import kr.webgori.lolien.discord.bot.request.LolienUserLoginRequest;
import kr.webgori.lolien.discord.bot.service.LolienService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class LolienController {
  private final LolienService lolienService;

  @Operation(
      summary = "클리앙 아이디로 로그인")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = "text/plain")),
          @ApiResponse(
              responseCode = "204",
              description = "No Content")})
  @PostMapping("v1/users/login")
  public void login(@RequestBody LolienUserLoginRequest request) {

  }

  @Operation(
      summary = "소환사 등록",
      security = {
          @SecurityRequirement(name = "JWT")
      })
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "204",
              description = "No Content"),
          @ApiResponse(
              responseCode = "400",
              description = "Bad Request. 추가할 소환사가 없거나, 이미 추가되어 있을 때"),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized. 인증 정보를 찾을 수 없을 때"),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden. 세션이 만료 됬을 때")
      })
  @PostMapping("v1/users/summoner")
  //@ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void addSummoner(@RequestBody @Valid LolienUserAddSummonerRequest request) {
    lolienService.addSummoner(request);
  }

  @Operation(
      summary = "소환사 제거",
      security = {
          @SecurityRequirement(name = "JWT")
      })
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "204",
              description = "No Content"),
          @ApiResponse(
              responseCode = "400",
              description = "Bad Request. 제거할 소환사가 없을 때"),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized. 인증 정보를 찾을 수 없을 때"),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden. 세션이 만료 됬을 때")
      })
  @DeleteMapping("v1/users/summoner")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void deleteSummoner() {
    lolienService.deleteSummoner();
  }
}
