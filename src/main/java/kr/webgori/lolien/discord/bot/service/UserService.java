package kr.webgori.lolien.discord.bot.service;

import static kr.webgori.lolien.discord.bot.util.CommonUtil.objectToJsonString;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlHiddenInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.Cookie;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import kr.webgori.lolien.discord.bot.component.AuthenticationComponent;
import kr.webgori.lolien.discord.bot.dto.UserDto;
import kr.webgori.lolien.discord.bot.dto.UserSessionDto;
import kr.webgori.lolien.discord.bot.dto.user.ClienSendMessageDto;
import kr.webgori.lolien.discord.bot.dto.user.ClienSessionDto;
import kr.webgori.lolien.discord.bot.dto.user.UserRegisterVerifyClienIdDto;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.user.ClienUser;
import kr.webgori.lolien.discord.bot.entity.user.Role;
import kr.webgori.lolien.discord.bot.entity.user.User;
import kr.webgori.lolien.discord.bot.entity.user.UserRole;
import kr.webgori.lolien.discord.bot.exception.AlreadyAddedSummonerException;
import kr.webgori.lolien.discord.bot.exception.SummonerNotFoundException;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
import kr.webgori.lolien.discord.bot.repository.user.RoleRepository;
import kr.webgori.lolien.discord.bot.repository.user.UserRepository;
import kr.webgori.lolien.discord.bot.repository.user.UserRoleRepository;
import kr.webgori.lolien.discord.bot.request.LolienUserAddSummonerRequest;
import kr.webgori.lolien.discord.bot.request.user.AccessTokenRequest;
import kr.webgori.lolien.discord.bot.request.user.LogoutRequest;
import kr.webgori.lolien.discord.bot.request.user.RegisterRequest;
import kr.webgori.lolien.discord.bot.request.user.VerifyClienIdRequest;
import kr.webgori.lolien.discord.bot.response.UserInfoResponse;
import kr.webgori.lolien.discord.bot.response.user.AccessTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {
  private static final String USER_ROLE_DEFAULT = "USER";
  private static final String CLIEN_SESSION_REDIS_KEY = "clien:session";
  private static final String CLIEN_SEND_MESSAGE = "LoLien.kr (https://lolien.kr) 회원가입 "
      + "인증 번호는 %s 입니다. 5분이 지나면 인증 번호는 만료됩니다.";
  private static final String USER_REGISTER_VERIFY_CLIEN_ID_REDIS_KEY =
      "users:register:verify:clien-id:%s";

  private final UserRepository userRepository;
  private final HttpServletRequest httpServletRequest;
  private final LolienSummonerRepository lolienSummonerRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserRoleRepository userRoleRepository;
  private final RoleRepository roleRepository;
  private final AuthenticationComponent authenticationComponent;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;
  private final RestTemplate restTemplate;

  @Value("${clien.url}")
  private String clienUrl;

  @Value("${clien.id}")
  private String clienId;

  @Value("${clien.password}")
  private String clienPassword;

  @Value("${clien.message:send:url}")
  private String clienMessageSendUrl;

  /**
   * register.
   * @param request request
   */
  public void register(RegisterRequest request) {
    String email = request.getEmail();
    boolean existsByEmail = userRepository.existsByEmail(email);

    if (existsByEmail) {
      throw new IllegalArgumentException("이미 가입되어 있는 이메일 입니다.");
    }

    String password = request.getPassword();
    String encryptedPassword = passwordEncoder.encode(password);
    String nickname = request.getNickname();

    User user = User
        .builder()
        .email(email)
        .emailVerified(false)
        .password(encryptedPassword)
        .nickname(nickname)
        .build();

    userRepository.save(user);

    Role role = getUserRole();

    UserRole userRole = UserRole
        .builder()
        .role(role)
        .user(user)
        .build();

    userRoleRepository.save(userRole);
  }

  private Role getUserRole() {
    Role role = roleRepository.findByRole(USER_ROLE_DEFAULT);

    if (Objects.isNull(role)) {
      throw new IllegalArgumentException("사용자 권한이 존재하지 않습니다.");
    }

    return role;
  }

  /**
   * addSummoner.
   * @param request request
   */
  public void addSummoner(LolienUserAddSummonerRequest request) {
    User user = authenticationComponent.getUser(httpServletRequest);
    LolienSummoner lolienSummoner = user.getLolienSummoner();

    if (Objects.nonNull(lolienSummoner)) {
      throw new AlreadyAddedSummonerException("");
    }

    String summonerName = request.getSummonerName();
    lolienSummoner = lolienSummonerRepository.findBySummonerName(summonerName);

    if (Objects.isNull(lolienSummoner)) {
      throw new SummonerNotFoundException("");
    }

    user.setLolienSummoner(lolienSummoner);
    userRepository.save(user);
  }

  /**
   * deleteSummoner.
   */
  public void deleteSummoner() {
    User user = authenticationComponent.getUser(httpServletRequest);
    LolienSummoner lolienSummoner = user.getLolienSummoner();

    if (Objects.isNull(lolienSummoner)) {
      throw new SummonerNotFoundException("");
    }

    user.setLolienSummoner(null);
    userRepository.save(user);
  }

  /**
   * getUserInfo.
   * @return UserInfoResponse
   */
  public UserInfoResponse getUserInfo() {
    User user = authenticationComponent.getUser(httpServletRequest);
    String email = user.getEmail();
    String nickname = user.getNickname();
    boolean emailVerified = user.getEmailVerified();

    ClienUser clienUser = user.getClienUser();
    String clienId = null;

    if (Objects.nonNull(clienUser)) {
      clienId = user.getClienUser().getClienId();
    }

    LolienSummoner lolienSummoner = user.getLolienSummoner();
    String summonerName = null;

    if (Objects.nonNull(lolienSummoner)) {
      summonerName = lolienSummoner.getSummonerName();
    }

    UserDto userInfo = getUserDto(email, nickname, emailVerified, clienId, summonerName);

    return UserInfoResponse
        .builder()
        .userInfo(userInfo)
        .build();
  }

  private UserDto getUserDto(String email,
                             String nickname,
                             boolean emailVerified,
                             String clienId,
                             String summerName) {
    return UserDto
        .builder()
        .email(email)
        .nickname(nickname)
        .emailVerified(emailVerified)
        .clienId(clienId)
        .summonerName(summerName)
        .build();
  }

  /**
   * deleteUser.
   * @param request request
   */
  @Transactional
  public void deleteUser(HttpServletRequest request) {
    User user = authenticationComponent.getUser(httpServletRequest);
    UserRole userRole = user.getUserRole();
    userRoleRepository.delete(userRole);
    userRepository.delete(user);

    logout(request, true, null, null);
  }

  /**
   * logout.
   * @param request request
   */
  public void logout(LogoutRequest request) {
    String accessToken = request.getAccessToken();
    String refreshToken = request.getRefreshToken();

    logout(null, false, accessToken, refreshToken);
  }

  private void logout(HttpServletRequest request, boolean deleteUser, String accessToken,
                      String refreshToken) {
    String email;

    if (deleteUser) {
      email = authenticationComponent.getEmail(request);
      authenticationComponent.deleteSessionFromRedis(request);
    } else {
      email = authenticationComponent.getEmail(accessToken);
      authenticationComponent.deleteSessionFromRedis(accessToken);
    }

    authenticationComponent.deleteRefreshTokenFromRedis(email, deleteUser, refreshToken);
  }

  /**
   * getAccessToken.
   * @param httpServletRequest httpServletRequest
   * @param accessTokenRequest accessTokenRequest
   * @return AccessTokenResponse
   */
  public AccessTokenResponse getAccessToken(HttpServletRequest httpServletRequest,
                                            AccessTokenRequest accessTokenRequest) {
    String email = accessTokenRequest.getEmail();
    authenticationComponent.checkExistsRefreshToken(httpServletRequest, email);

    String accessToken = authenticationComponent.generateAccessToken(email);

    User user = authenticationComponent.getUser(email);
    UserSessionDto userSessionDto = authenticationComponent.getUserSessionDto(user);
    authenticationComponent.addUserSessionToRedis(email, userSessionDto);

    return AccessTokenResponse
        .builder()
        .accessToken(accessToken)
        .build();
  }

  /**
   * 클리앙 아이디 인증.
   * @param request request
   */
  public void verifyClienId(VerifyClienIdRequest request) {
    String authNumber = getAuthNumber();
    callClienSendMessageApi(request, authNumber);
    setClienIdToAuthNumber(request, authNumber);
  }

  private void setClienIdToAuthNumber(VerifyClienIdRequest request, String authNumber) {
    UserRegisterVerifyClienIdDto userRegisterVerifyClienIdDto = UserRegisterVerifyClienIdDto
        .builder()
        .authNumber(authNumber)
        .build();

    String userRegisterVerifyClienIdRedisKey = getUserRegisterVerifyClienIdRedisKey(request);

    redisTemplate
        .opsForValue()
        .set(userRegisterVerifyClienIdRedisKey, userRegisterVerifyClienIdDto);

    redisTemplate.expire(userRegisterVerifyClienIdRedisKey, 5L, TimeUnit.MINUTES);
  }

  private String getUserRegisterVerifyClienIdRedisKey(VerifyClienIdRequest request) {
    String clienId = request.getClienId();
    return String.format(USER_REGISTER_VERIFY_CLIEN_ID_REDIS_KEY, clienId);
  }

  private void callClienSendMessageApi(VerifyClienIdRequest verifyClienIdRequest,
                                       String authNumber) {
    ClienSessionDto clienSessionDto = getClienSessionDto();

    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);

    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    String cookieSession = getCookieSession(clienSessionDto);
    headers.set("Cookie", cookieSession);

    String csrf = clienSessionDto.getCsrf();
    headers.set("X-CSRF-TOKEN", csrf);

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    String jsonSendMessage = getJsonSendMessage(authNumber);
    params.add("param", jsonSendMessage);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

    String clienSendMessageUrl = getClienSendMessageUrl(verifyClienIdRequest);

    ResponseEntity<String> exchange = restTemplate
        .exchange(clienSendMessageUrl, HttpMethod.POST, request, String.class);

    String body = exchange.getBody();

    if (Objects.isNull(body) || !body.equals("true")) {
      throw new IllegalArgumentException("클리앙 아이디를 인증하는 중 문제가 발생하였습니다.");
    }
  }

  private String getClienSendMessageUrl(VerifyClienIdRequest verifyClienIdRequest) {
    return clienMessageSendUrl + verifyClienIdRequest.getClienId();
  }

  private String getJsonSendMessage(String authNumber) {
    ClienSendMessageDto sendMessageDto = getSendMessageDto(authNumber);

    return objectToJsonString(sendMessageDto);
  }

  private ClienSendMessageDto getSendMessageDto(String authNumber) {
    String message = String.format(CLIEN_SEND_MESSAGE, authNumber);

    return ClienSendMessageDto
        .builder()
        .contents(message)
        .build();
  }

  private String getAuthNumber() {
    return RandomStringUtils.randomNumeric(1, 6);
  }

  private String getCookieSession(ClienSessionDto clienSessionDto) {
    String session = clienSessionDto.getSession();
    return String.format("SESSION=%s", session);
  }

  private ClienSessionDto getClienSessionDto() {
    ClienSessionDto clienSessionDto = getClienSessionDtoFromRedis();

    if (Objects.isNull(clienSessionDto)) {
      clienSessionDto = getClienSessionInfoByLogin();
      setClienSessionDtoFromRedis(clienSessionDto);
    }

    return clienSessionDto;
  }

  private ClienSessionDto getClienSessionInfoByLogin() {
    try {
      WebClient webClient = getWebClient();
      HtmlPage htmlPage = webClient.getPage(clienUrl);

      HtmlForm form = htmlPage.getHtmlElementById("loginForm");
      HtmlTextInput inputId = form.getInputByName("userId");
      HtmlPasswordInput inputPw = form.getInputByName("userPassword");

      inputId.setValueAttribute(clienId);
      inputPw.setValueAttribute(clienPassword);

      htmlPage = form.getButtonByName("로그인하기").click();
      boolean login = htmlPage.asText().contains("로그아웃");

      if (!login) {
        throw new IllegalArgumentException("클리앙 아이디를 인증하는 중 문제가 발생하였습니다.");
      }

      HtmlHiddenInput csrfHiddenInput = htmlPage.getElementByName("_csrf");
      String csrf = csrfHiddenInput.getValueAttribute();

      Set<Cookie> cookies = webClient.getCookieManager().getCookies();

      Cookie cookie = cookies
          .stream()
          .filter(c -> c.getName().equals("SESSION"))
          .findFirst().orElseThrow(() -> new InvalidCookieException("not found session"));

      String session = cookie.getValue();

      return ClienSessionDto
          .builder()
          .csrf(csrf)
          .session(session)
          .build();
    } catch (IOException e) {
      logger.error("", e);
      throw new IllegalArgumentException("클리앙 아이디를 인증하는 중 문제가 발생하였습니다.");
    }
  }

  private WebClient getWebClient() {
    WebClient webClient = new WebClient(BrowserVersion.CHROME);
    webClient.getOptions().setThrowExceptionOnScriptError(false);
    webClient.getOptions().setCssEnabled(false);
    return webClient;
  }

  private ClienSessionDto getClienSessionDtoFromRedis() {
    Object object = redisTemplate.opsForValue().get(CLIEN_SESSION_REDIS_KEY);
    return objectMapper.convertValue(object, ClienSessionDto.class);
  }

  private void setClienSessionDtoFromRedis(ClienSessionDto clienSessionDto) {
    redisTemplate.opsForValue().set(CLIEN_SESSION_REDIS_KEY, clienSessionDto);
  }
}