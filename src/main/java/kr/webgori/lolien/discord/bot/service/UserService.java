package kr.webgori.lolien.discord.bot.service;

import static kr.webgori.lolien.discord.bot.component.SummonerComponent.DEFAULT_TIER;
import static kr.webgori.lolien.discord.bot.component.TeamGenerateComponent.CURRENT_SEASON;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.getSeasonFormat;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.numberToRomanNumeral;
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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import kr.webgori.lolien.discord.bot.component.AuthenticationComponent;
import kr.webgori.lolien.discord.bot.component.ConfigComponent;
import kr.webgori.lolien.discord.bot.component.MailComponent;
import kr.webgori.lolien.discord.bot.component.TeamGenerateComponent;
import kr.webgori.lolien.discord.bot.component.UserTransactionComponent;
import kr.webgori.lolien.discord.bot.dto.UserInfoDto;
import kr.webgori.lolien.discord.bot.dto.UserSessionDto;
import kr.webgori.lolien.discord.bot.dto.user.ClienSendMessageDto;
import kr.webgori.lolien.discord.bot.dto.user.ClienSessionDto;
import kr.webgori.lolien.discord.bot.dto.user.UserDto;
import kr.webgori.lolien.discord.bot.dto.user.VerifyAuthNumberDto;
import kr.webgori.lolien.discord.bot.entity.League;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.user.ClienUser;
import kr.webgori.lolien.discord.bot.entity.user.Role;
import kr.webgori.lolien.discord.bot.entity.user.User;
import kr.webgori.lolien.discord.bot.entity.user.UserRole;
import kr.webgori.lolien.discord.bot.exception.AlreadyAddedSummonerException;
import kr.webgori.lolien.discord.bot.exception.SummonerNotFoundException;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
import kr.webgori.lolien.discord.bot.repository.user.ClienUserRepository;
import kr.webgori.lolien.discord.bot.repository.user.RoleRepository;
import kr.webgori.lolien.discord.bot.repository.user.UserRepository;
import kr.webgori.lolien.discord.bot.request.LolienUserAddSummonerRequest;
import kr.webgori.lolien.discord.bot.request.user.AccessTokenRequest;
import kr.webgori.lolien.discord.bot.request.user.LogoutRequest;
import kr.webgori.lolien.discord.bot.request.user.RegisterRequest;
import kr.webgori.lolien.discord.bot.request.user.VerifyClienIdRequest;
import kr.webgori.lolien.discord.bot.request.user.VerifyEmailRequest;
import kr.webgori.lolien.discord.bot.response.UserInfoResponse;
import kr.webgori.lolien.discord.bot.response.user.AccessTokenResponse;
import kr.webgori.lolien.discord.bot.response.user.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {
  private static final String USER_ROLE_DEFAULT = "USER";
  private static final String CLIEN_SESSION_REDIS_KEY = "clien:session";
  private static final String REGISTER_VERIFY_EMAIL_SUBJECT = "[LoLien.kr] 이메일 인증번호가 도착하였습니다";
  private static final String REGISTER_VERIFY_EMAIL_TEXT = "LoLien.kr 회원가입 "
      + "이메일 인증 번호는 [%s] 입니다. 5분이 지나면 인증 번호는 만료됩니다.";
  private static final String REGISTER_VERIFY_EMAIL_REDIS_KEY =
      "users:register:verify:email:%s";
  private static final String REGISTER_VERIFY_CLIEN_ID_TEXT = "LoLien.kr 회원가입 "
      + "클리앙 아이디 인증 번호는 [%s] 입니다. 5분이 지나면 인증 번호는 만료됩니다.";
  private static final String REGISTER_VERIFY_CLIEN_ID_REDIS_KEY =
      "users:register:verify:clien-id:%s";

  private final UserRepository userRepository;
  private final HttpServletRequest httpServletRequest;
  private final LolienSummonerRepository lolienSummonerRepository;
  private final PasswordEncoder passwordEncoder;
  private final RoleRepository roleRepository;
  private final AuthenticationComponent authenticationComponent;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;
  private final RestTemplate restTemplate;
  private final MailComponent mailComponent;
  private final UserTransactionComponent userTransactionComponent;
  private final ClienUserRepository clienUserRepository;
  private final TeamGenerateComponent teamGenerateComponent;

  @Value("${clien.service.url}")
  private String clienUrl;

  @Value("${clien.id}")
  private String clienId;

  @Value("${clien.password}")
  private String clienPassword;

  @Value("${clien.message.send.url}")
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

    verifyEmailAuthNumber(request);

    ClienUser clienUser = getClienUser(request);
    LolienSummoner lolienSummoner = getLolienSummoner(request);
    List<League> leagues = getLeagues(request, lolienSummoner);

    User user = getUser(request, clienUser, lolienSummoner);
    Role role = getUserRole();
    UserRole userRole = getUserRole(user, role);
    user.setUserRole(userRole);

    userTransactionComponent.register(user, userRole, clienUser, lolienSummoner, leagues);
  }

  private UserRole getUserRole(User user, Role role) {
    return UserRole
          .builder()
          .user(user)
          .role(role)
          .build();
  }

  private Role getUserRole() {
    Role role = roleRepository.findByRole(USER_ROLE_DEFAULT);

    if (Objects.isNull(role)) {
      throw new IllegalArgumentException("사용자 권한이 존재하지 않습니다.");
    }

    return role;
  }

  private User getUser(RegisterRequest request, ClienUser clienUser,
                       LolienSummoner lolienSummoner) {
    String email = request.getEmail();
    String nickname = request.getNickname();
    String password = request.getPassword();
    String encryptedPassword = passwordEncoder.encode(password);

    return User
        .builder()
        .email(email)
        .nickname(nickname)
        .emailVerified(true)
        .password(encryptedPassword)
        .clienUser(clienUser)
        .lolienSummoner(lolienSummoner)
        .build();
  }

  private void verifyEmailAuthNumber(RegisterRequest request) {
    VerifyAuthNumberDto verifyAuthNumberDto = getVerifyEmailAuthNumberDtoFromRedis(request);

    if (Objects.isNull(verifyAuthNumberDto)) {
      throw new IllegalArgumentException("이메일 인증 번호가 만료 되었습니다.");
    }

    String authNumber = verifyAuthNumberDto.getAuthNumber();
    String emailAuthNumber = request.getEmailAuthNumber();

    if (!authNumber.equals(emailAuthNumber)) {
      throw new IllegalArgumentException("이메일 인증 번호가 올바르지 않습니다.");
    }
  }

  private VerifyAuthNumberDto getVerifyEmailAuthNumberDtoFromRedis(RegisterRequest request) {
    String verifyEmailRedisKey = getVerifyEmailRedisKey(request);

    Object object = redisTemplate.opsForValue().get(verifyEmailRedisKey);
    return objectMapper.convertValue(object, VerifyAuthNumberDto.class);
  }

  private LolienSummoner getLolienSummoner(RegisterRequest request) {
    String summerName = request.getSummonerName().replaceAll("\\s+", "");
    LolienSummoner lolienSummoner = lolienSummonerRepository.findBySummonerName(summerName);

    if (Objects.isNull(lolienSummoner)) {
      return getNewLolienSummoner(request);
    }

    checkExistsUser(lolienSummoner);

    return lolienSummoner;
  }

  private LolienSummoner getNewLolienSummoner(RegisterRequest request) {
    Summoner summoner = getSummoner(request);
    String summonerId = summoner.getId();
    String accountId = summoner.getAccountId();
    String summonerName = request.getSummonerName();
    int summonerLevel = summoner.getSummonerLevel();

    return LolienSummoner
        .builder()
        .id(summonerId)
        .accountId(accountId)
        .summonerName(summonerName)
        .summonerLevel(summonerLevel)
        .build();
  }

  private List<League> getLeagues(RegisterRequest request, LolienSummoner lolienSummoner) {
    List<League> leagues = lolienSummoner.getLeagues();

    if (Objects.isNull(leagues)) {
      leagues = Lists.newArrayList();
      addCurrentSeasonLeague(request, lolienSummoner, leagues);
      addOtherSeasonLeague(request, lolienSummoner, leagues);
    }

    return leagues;
  }

  private void addCurrentSeasonLeague(RegisterRequest request, LolienSummoner lolienSummoner,
                                      List<League> leagues) {
    String currentSeasonTier = getCurrentSeasonTier(request);

    League league = League
        .builder()
        .season(CURRENT_SEASON)
        .tier(currentSeasonTier)
        .lolienSummoner(lolienSummoner)
        .build();

    leagues.add(league);
  }

  private String getCurrentSeasonTier(RegisterRequest request) {
    Summoner summoner = getSummoner(request);
    RiotApi riotApi = getRiotApi();

    try {
      String summonerId = summoner.getId();
      Set<LeagueEntry> leagueEntrySet = riotApi
          .getLeagueEntriesBySummonerId(Platform.KR, summonerId);

      List<LeagueEntry> leagueEntries = Lists.newArrayList(leagueEntrySet);

      String tier = DEFAULT_TIER;

      for (LeagueEntry leagueEntry : leagueEntries) {
        if (leagueEntry.getQueueType().equals("RANKED_SOLO_5x5")) {
          tier = leagueEntry.getTier() + "-" + leagueEntry.getRank();
        }
      }

      return tier;
    } catch (RiotApiException e) {
      int errorCode = e.getErrorCode();
      if (errorCode == RiotApiException.FORBIDDEN) {
        throw new IllegalArgumentException(
            "Riot API Key가 만료되어 기능이 정상적으로 작동하지 않습니다. 개발자에게 알려주세요.");
      } else if (errorCode == RiotApiException.DATA_NOT_FOUND) {
        String summonerName = request.getSummonerName();
        String errorMessage = String.format("%s 소환사가 존재하지 않습니다.", summonerName);
        throw new IllegalArgumentException(errorMessage);
      } else {
        logger.error("", e);
        throw new IllegalArgumentException("riotApiException");
      }
    }
  }

  private void addOtherSeasonLeague(RegisterRequest request, LolienSummoner lolienSummoner,
                                    List<League> leagues) {
    String summonerName = request.getSummonerName();

    Map<String, String> tiersFromOpGg = getTiersFromOpGg(summonerName);

    for (Map.Entry<String, String> entry : tiersFromOpGg.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      League league = League
          .builder()
          .lolienSummoner(lolienSummoner)
          .season(key)
          .tier(value)
          .build();

      leagues.add(league);
    }
  }

  /**
   * getTiersFromOpGg.
   * @param summonerName summonerName
   * @return Map map
   */
  private Map<String, String> getTiersFromOpGg(String summonerName) {
    Map<String, String> tiersMap = Maps.newHashMap();

    String opGgUrl = String.format("https://www.op.gg/summoner/userName=%s", summonerName);

    try {
      Document document = Jsoup.connect(opGgUrl).timeout(600000).get();
      Elements pastRankList = document.getElementsByClass("PastRankList");

      for (Element element : pastRankList) {
        Elements tierElements = element.getElementsByTag("li");
        for (Element tierElement : tierElements) {
          Elements seasonElements = tierElement.getElementsByTag("b");

          String prevSeason = Strings.EMPTY;

          for (Element seasonElement : seasonElements) {
            prevSeason = getSeasonFormat(seasonElement.text());
          }

          boolean title = tierElement.hasAttr("title");

          if (!title) {
            continue;
          }

          String prevTierLeaguePoints = tierElement.attr("title");
          List<String> prevTierSplitList = Lists.newArrayList(prevTierLeaguePoints.split(" "));

          if (prevTierSplitList.size() > 2) {
            prevTierSplitList.remove(2);
          }

          prevTierSplitList.set(0, prevTierSplitList.get(0).toUpperCase(Locale.KOREAN));

          if (prevTierSplitList.get(1).equals("5")) {
            prevTierSplitList.set(1, "4");
          }

          if (!prevTierSplitList.get(1).equals("1") && !prevTierSplitList.get(1).equals("2")
              && !prevTierSplitList.get(1).equals("3") && !prevTierSplitList.get(1).equals("4")) {
            logger.error(prevTierSplitList.get(1));
            prevTierSplitList.set(1, "1");
          }

          prevTierSplitList.set(1, numberToRomanNumeral(prevTierSplitList.get(1)));
          String prevTier = String.join("-", prevTierSplitList);

          tiersMap.put(prevSeason, prevTier);
        }
      }
    } catch (IOException e) {
      logger.error("", e);
    }

    return tiersMap;
  }

  private Summoner getSummoner(RegisterRequest request) {
    RiotApi riotApi = getRiotApi();
    String summonerName = request.getSummonerName();

    try {
      return riotApi.getSummonerByName(Platform.KR, summonerName);
    } catch (RiotApiException e) {
      int errorCode = e.getErrorCode();
      if (errorCode == RiotApiException.FORBIDDEN) {
        throw new IllegalArgumentException(
            "Riot API Key가 만료되어 기능이 정상적으로 작동하지 않습니다. 개발자에게 알려주세요.");
      } else if (errorCode == RiotApiException.DATA_NOT_FOUND) {
        String errorMessage = String.format("%s 소환사가 존재하지 않습니다.", summonerName);
        throw new IllegalArgumentException(errorMessage);
      } else {
        logger.error("", e);
        throw new IllegalArgumentException("riotApiException");
      }
    }
  }

  @NotNull
  private RiotApi getRiotApi() {
    String riotApiKey = ConfigComponent.getRiotApiKey();
    ApiConfig config = new ApiConfig().setKey(riotApiKey);
    return new RiotApi(config);
  }

  private void checkExistsUser(LolienSummoner lolienSummoner) {
    User user = lolienSummoner.getUser();

    if (Objects.nonNull(user)) {
      throw new IllegalArgumentException("이미 등록되어 있는 소환사 이름 입니다.");
    }
  }

  private ClienUser getClienUser(RegisterRequest request) {
    checkExistsClienUser(request);
    verifyClienIdAuthNumber(request);

    String clienId = request.getClienId();

    return ClienUser
        .builder()
        .clienId(clienId)
        .build();
  }

  private void checkExistsClienUser(RegisterRequest request) {
    String clienId = request.getClienId();
    boolean existsByClienId = clienUserRepository.existsByClienId(clienId);

    if (existsByClienId) {
      throw new IllegalArgumentException("이미 등록되어 있는 클리앙 아이디 입니다.");
    }
  }

  private void verifyClienIdAuthNumber(RegisterRequest request) {
    VerifyAuthNumberDto verifyAuthNumberDto = getVerifyClienIdAuthNumberDtoFromRedis(request);

    if (Objects.isNull(verifyAuthNumberDto)) {
      throw new IllegalArgumentException("클리앙 아이디 인증 번호가 만료 되었습니다.");
    }

    String authNumber = verifyAuthNumberDto.getAuthNumber();
    String clienIdAuthNumber = request.getClienIdAuthNumber();

    if (!authNumber.equals(clienIdAuthNumber)) {
      throw new IllegalArgumentException("클리앙 아이디 인증 번호가 올바르지 않습니다.");
    }
  }

  private VerifyAuthNumberDto getVerifyClienIdAuthNumberDtoFromRedis(RegisterRequest request) {
    String verifyEmailRedisKey = getVerifyClienIdRedisKey(request);

    Object object = redisTemplate.opsForValue().get(verifyEmailRedisKey);
    return objectMapper.convertValue(object, VerifyAuthNumberDto.class);
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

    UserInfoDto userInfo = getUserDto(email, nickname, emailVerified, clienId, summonerName);

    return UserInfoResponse
        .builder()
        .userInfo(userInfo)
        .build();
  }

  private UserInfoDto getUserDto(String email,
                                 String nickname,
                                 boolean emailVerified,
                                 String clienId,
                                 String summerName) {
    return UserInfoDto
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
  public void deleteUser(HttpServletRequest request) {
    User user = authenticationComponent.getUser(httpServletRequest);
    UserRole userRole = user.getUserRole();
    LolienSummoner lolienSummoner = user.getLolienSummoner();
    List<League> leagues = lolienSummoner.getLeagues();
    ClienUser clienUser = user.getClienUser();

    userTransactionComponent.deleteUser(user, userRole, clienUser, lolienSummoner, leagues);

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
   * 이메일 인증.
   * @param request request
   */
  public void verifyEmail(VerifyEmailRequest request) {
    String authNumber = getAuthNumber();
    sendEmail(request, authNumber);
    setEmailToAuthNumber(request, authNumber);
  }

  private void sendEmail(VerifyEmailRequest request, String authNumber) {
    String email = request.getEmail();
    String emailVerifyText = getEmailVerifyText(authNumber);

    mailComponent.sendMail("no-reply@LoLien.kr", email, REGISTER_VERIFY_EMAIL_SUBJECT,
        emailVerifyText);
  }

  private void setEmailToAuthNumber(VerifyEmailRequest request, String authNumber) {
    VerifyAuthNumberDto verifyAuthNumberDto = VerifyAuthNumberDto
        .builder()
        .authNumber(authNumber)
        .build();

    String verifyEmailRedisKey = getVerifyEmailRedisKey(request);

    redisTemplate
        .opsForValue()
        .set(verifyEmailRedisKey, verifyAuthNumberDto);

    redisTemplate.expire(verifyEmailRedisKey, 5L, TimeUnit.MINUTES);
  }

  private String getVerifyEmailRedisKey(RegisterRequest request) {
    String email = request.getEmail();
    return getVerifyEmailRedisKey(email);
  }

  private String getVerifyEmailRedisKey(VerifyEmailRequest request) {
    String email = request.getEmail();
    return getVerifyEmailRedisKey(email);
  }

  private String getVerifyEmailRedisKey(String email) {
    return String.format(REGISTER_VERIFY_EMAIL_REDIS_KEY, email);
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
    VerifyAuthNumberDto verifyAuthNumberDto = VerifyAuthNumberDto
        .builder()
        .authNumber(authNumber)
        .build();

    String verifyClienIdRedisKey = getVerifyClienIdRedisKey(request);

    redisTemplate
        .opsForValue()
        .set(verifyClienIdRedisKey, verifyAuthNumberDto);

    redisTemplate.expire(verifyClienIdRedisKey, 5L, TimeUnit.MINUTES);
  }

  private String getVerifyClienIdRedisKey(RegisterRequest request) {
    String clienId = request.getClienId();
    return getVerifyClienIdRedisKey(clienId);
  }

  private String getVerifyClienIdRedisKey(VerifyClienIdRequest request) {
    String clienId = request.getClienId();
    return getVerifyClienIdRedisKey(clienId);
  }

  private String getVerifyClienIdRedisKey(String clienId) {
    return String.format(REGISTER_VERIFY_CLIEN_ID_REDIS_KEY, clienId);
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
      deleteClienSessionDtoFromRedis();
      throw new IllegalArgumentException("클리앙 아이디를 인증하는 중 문제가 발생하였습니다. "
          + "다시한번 시도 해보시거나, 관리자에게 문의 해주세요.");
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
    String message = String.format(REGISTER_VERIFY_CLIEN_ID_TEXT, authNumber);

    return ClienSendMessageDto
        .builder()
        .contents(message)
        .build();
  }

  private String getAuthNumber() {
    return RandomStringUtils.randomNumeric(6);
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
        throw new IllegalArgumentException("클리앙 아이디를 인증하는 중 문제가 발생하였습니다. "
            + "다시한번 시도 해보시거나, 관리자에게 문의 해주세요.");
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
      throw new IllegalArgumentException("클리앙 아이디를 인증하는 중 문제가 발생하였습니다. "
          + "다시한번 시도 해보시거나, 관리자에게 문의 해주세요.");
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

  private void deleteClienSessionDtoFromRedis() {
    redisTemplate.delete(CLIEN_SESSION_REDIS_KEY);
  }

  private String getEmailVerifyText(String authNumber) {
    return String.format(REGISTER_VERIFY_EMAIL_TEXT, authNumber);
  }

  public UserResponse getUsers() {
    List<UserDto> usersDto = getUsersDto();
    return UserResponse.builder().users(usersDto).build();
  }

  private List<UserDto> getUsersDto() {
    List<User> users = userRepository.findAll();
    List<UserDto> usersDto = Lists.newArrayList();

    for (User user : users) {
      String nickname = user.getNickname();

      LocalDateTime createdAt = user.getCreatedAt();

      LolienSummoner lolienSummoner = user.getLolienSummoner();
      String summonerName = lolienSummoner.getSummonerName();
      Integer mmr = lolienSummoner.getMmr();

      Optional<League> optionalLatestTier = teamGenerateComponent
              .getMostTierInLatest3Seasons(lolienSummoner);

      League latestTier = optionalLatestTier.orElseGet(() -> League.builder().tier("").build());
      String tier = latestTier.getTier();

      UserDto userDto = UserDto
              .builder()
              .nickname(nickname)
              .summonerName(summonerName)
              .tier(tier)
              .mmr(mmr)
              .createdAt(createdAt)
              .build();

      usersDto.add(userDto);
    }

    return usersDto;
  }
}
