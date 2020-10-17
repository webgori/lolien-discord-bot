package kr.webgori.lolien.discord.bot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import kr.webgori.lolien.discord.bot.request.LolienUserAddSummonerRequest;
import kr.webgori.lolien.discord.bot.request.user.AccessTokenRequest;
import kr.webgori.lolien.discord.bot.request.user.LoginRequest;
import kr.webgori.lolien.discord.bot.request.user.LogoutRequest;
import kr.webgori.lolien.discord.bot.request.user.RegisterRequest;
import kr.webgori.lolien.discord.bot.request.user.VerifyClienIdRequest;
import kr.webgori.lolien.discord.bot.request.user.VerifyEmailRequest;
import kr.webgori.lolien.discord.bot.response.UserInfoResponse;
import kr.webgori.lolien.discord.bot.response.user.AccessTokenResponse;
import kr.webgori.lolien.discord.bot.response.user.LoginResponse;
import kr.webgori.lolien.discord.bot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class UserController {
  private final UserService userService;

  @Operation(
      summary = "회원가입")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "204",
              description = "No Content"),
          @ApiResponse(
              responseCode = "400",
              description = "Bad Request. 회원가입 양식이 올바르지 않음")
      })
  @PostMapping("v1/users/register")
  public void register(@RequestBody @Valid RegisterRequest request) {
    userService.register(request);
  }

  @Operation(
      summary = "로그인")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = "text/plain")),
          @ApiResponse(
              responseCode = "200",
              description = "OK",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = LoginResponse.class))),
      })
  @PostMapping("v1/users/login")
  public void login(@RequestBody @Valid LoginRequest request) {

  }

  @Operation(
      summary = "accessToken 갱신",
      security = {
          @SecurityRequirement(name = "JWT")
      })
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "OK",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = AccessTokenResponse.class))),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized. 인증 정보를 찾을 수 없을 때"),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden. 세션이 만료 됬을 때")
      })
  @PostMapping("v1/users/access-token")
  public AccessTokenResponse getAccessToken(HttpServletRequest httpServletRequest,
                                            @RequestBody @Valid
                                                AccessTokenRequest accessTokenRequest) {
    return userService.getAccessToken(httpServletRequest, accessTokenRequest);
  }

  @Operation(
      summary = "로그아웃")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "204",
              description = "No Content"),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized. 인증 정보를 찾을 수 없을 때"),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden. 세션이 만료 됬을 때")
      })
  @PostMapping("v1/users/logout")
  public void logout(@RequestBody @Valid LogoutRequest request) {
    userService.logout(request);
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
    userService.addSummoner(request);
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
    userService.deleteSummoner();
  }

  @Operation(
      summary = "사용자 정보 조회",
      security = {
          @SecurityRequirement(name = "JWT")
      })
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "OK",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = UserInfoResponse.class))),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized. 인증 정보를 찾을 수 없을 때"),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden. 세션이 만료 됬을 때")
      })
  @GetMapping("v1/users")
  public UserInfoResponse getUserInfo() {
    return userService.getUserInfo();
  }

  @Operation(
      summary = "사용자 탈퇴",
      security = {
          @SecurityRequirement(name = "JWT")
      })
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "204",
              description = "No Content"),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized. 인증 정보를 찾을 수 없을 때"),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden. 세션이 만료 됬을 때")
      })
  @DeleteMapping("v1/users")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void deleteUser(HttpServletRequest request) {
    userService.deleteUser(request);
  }

  @Operation(
      summary = "이메일 인증")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "204",
              description = "No Content")
      })
  @PostMapping("v1/users/register/verify/email")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void verifyEmail(@RequestBody @Valid VerifyEmailRequest request) {
    userService.verifyEmail(request);
  }

  @Operation(
      summary = "클리앙 아이디 인증")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "204",
              description = "No Content")
      })
  @PostMapping("v1/users/register/verify/clien-id")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void verifyClienId(@RequestBody @Valid VerifyClienIdRequest request) {
    userService.verifyClienId(request);
  }
}
