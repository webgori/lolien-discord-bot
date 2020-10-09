package kr.webgori.lolien.discord.bot.service;

import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import kr.webgori.lolien.discord.bot.component.AuthenticationComponent;
import kr.webgori.lolien.discord.bot.dto.UserSessionDto;
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
import kr.webgori.lolien.discord.bot.response.UserInfoResponse;
import kr.webgori.lolien.discord.bot.response.user.AccessTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {
  private static final String USER_ROLE_DEFAULT = "USER";

  private final UserRepository userRepository;
  private final HttpServletRequest httpServletRequest;
  private final LolienSummonerRepository lolienSummonerRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserRoleRepository userRoleRepository;
  private final RoleRepository roleRepository;
  private final AuthenticationComponent authenticationComponent;

  @Value("${clien.url}")
  private String clienUrl;

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

    return UserInfoResponse
        .builder()
        .email(email)
        .nickname(nickname)
        .emailVerified(emailVerified)
        .clienId(clienId)
        .summonerName(summonerName)
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
}
