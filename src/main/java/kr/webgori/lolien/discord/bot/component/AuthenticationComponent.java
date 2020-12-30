package kr.webgori.lolien.discord.bot.component;

import static kr.webgori.lolien.discord.bot.util.CommonUtil.DEFAULT_CHARSET;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.localDateTimeToTimestamp;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.objectToJsonString;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import kr.webgori.lolien.discord.bot.dto.UserSessionDto;
import kr.webgori.lolien.discord.bot.entity.user.User;
import kr.webgori.lolien.discord.bot.repository.user.UserRepository;
import kr.webgori.lolien.discord.bot.response.user.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

@Slf4j
@RequiredArgsConstructor
@Component
public class AuthenticationComponent {
  private static final String TOKEN_PREFIX = "Bearer";

  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;
  private final UserRepository userRepository;

  @Value("${jwt.secretKey}")
  private String secretKey;

  /**
   * getUserSessionDto.
   * @param user user
   * @return UserSessionDto
   */
  public UserSessionDto getUserSessionDto(User user) {
    LocalDateTime now = LocalDateTime.now();
    String email = user.getEmail();
    String hash = getHash(now, email);

    logger.error("now: {}, hash: {}", now, hash);

    return getUserSessionDto(now, user, hash);
  }

  /**
   * getUserSessionDto.
   * @param now 현재 시간
   * @param user user
   * @param hash hash
   * @return UserSessionDto
   */
  public UserSessionDto getUserSessionDto(LocalDateTime now, User user, String hash) {
    String email = user.getEmail();
    String nickname = user.getNickname();
    String role = user.getUserRole().getRole().getRole();

    return UserSessionDto
        .builder()
        .email(email)
        .nickname(nickname)
        .role(role)
        .createdAt(now)
        .hash(hash)
        .build();
  }

  /**
   * getUserSessionDto.
   * @param request request
   * @return UserSessionDto
   */
  public UserSessionDto getUserSessionDto(HttpServletRequest request) {
    String redisSessionKey = getRedisSessionKey(request);

    if (redisSessionKey.isEmpty()) {
      return UserSessionDto.builder().build();
    }

    //String key = String.format("users:%s", email);
    Object obj = redisTemplate.opsForValue().get(redisSessionKey);
    UserSessionDto userSessionDto = objectMapper.convertValue(obj, UserSessionDto.class);

    checkPresentSessionUserDto(userSessionDto);

    return userSessionDto;
  }

  private UserSessionDto getUserSessionDto(String accessToken) {
    String redisSessionKey = getRedisSessionKey(accessToken);

    if (redisSessionKey.isEmpty()) {
      return UserSessionDto.builder().email("").build();
    }

    //String key = String.format("users:%s", email);
    Object obj = redisTemplate.opsForValue().get(redisSessionKey);
    UserSessionDto userSessionDto = objectMapper.convertValue(obj, UserSessionDto.class);

    checkPresentSessionUserDto(userSessionDto);

    return userSessionDto;
  }

  private String getRedisSessionKey(HttpServletRequest request) {
    String accessToken = getAccessToken(request);
    return getRedisSessionKey(accessToken);
  }

  private String getRedisSessionKey(String accessToken) {
    if (accessToken.isEmpty()) {
      return getDefaultString();
    }

    Claims claims = getClaims(accessToken);

    if (claims.isEmpty()) {
      return getDefaultString();
    }

    String email = claims.get("email").toString();
    String hash = claims.get("hash").toString();

    return String.format("users:session:%s:%s", email, hash);
  }

  private String getAccessToken(HttpServletRequest request) {
    String jwt = Optional
        .ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
        .orElseGet(this::getDefaultString);

    if (jwt.isEmpty() || !jwt.startsWith(TOKEN_PREFIX)) {
      return getDefaultString();
    }

    return removeTokenPrefix(jwt);
  }

  private String getDefaultString() {
    return "";
  }

  private Claims getClaims(String token) {
    String hexSecretKey = getHexSecretKey();

    try {
      Claims claims = Jwts
          .parser()
          .setSigningKey(hexSecretKey)
          .parseClaimsJws(token)
          .getBody();

      if (!claims.containsKey("email")) {
        return new DefaultClaims();
      }

      return claims;
    } catch (ExpiredJwtException | BadCredentialsException | MalformedJwtException e) {
      logger.debug("", e);
    }

    return new DefaultClaims();
  }

  private void checkPresentSessionUserDto(UserSessionDto userSessionDto) {
    boolean presentSession = Objects.nonNull(userSessionDto);

    if (!presentSession) {
      throw new BadCredentialsException("");
    }
  }

  /**
   * generateAccessToken.
   * @param email email
   * @param userSessionDto userSessionDto
   * @param role role
   * @return accessToken
   */
  public String generateAccessToken(String email, UserSessionDto userSessionDto, String role) {
    Map<String, Object> claims = Maps.newHashMap();
    claims.put("email", email);
    claims.put("role", role);

    String hash = userSessionDto.getHash();
    claims.put("hash", hash);

    Timestamp expiration = Timestamp.valueOf(LocalDateTime.now().plusMinutes(30));
    String hexSecretKey = getHexSecretKey();

    return Jwts
        .builder()
        .setSubject(email)
        .setClaims(claims)
        .setExpiration(expiration)
        .signWith(SignatureAlgorithm.HS512, hexSecretKey)
        .compact();
  }

  /**
   * generateAccessToken.
   * @param email email
   * @return accessToken
   */
  public String generateAccessToken(String email) {
    Map<String, Object> claims = Maps.newHashMap();
    claims.put("email", email);

    User user = getUser(email);
    UserSessionDto userSessionDto = getUserSessionDto(user);

    String hash = userSessionDto.getHash();
    claims.put("hash", hash);

    String role = userSessionDto.getRole();
    claims.put("role", role);

    Timestamp expiration = Timestamp.valueOf(LocalDateTime.now().plusMinutes(30));
    String hexSecretKey = getHexSecretKey();

    return Jwts
        .builder()
        .setSubject(email)
        .setClaims(claims)
        .setExpiration(expiration)
        .signWith(SignatureAlgorithm.HS512, hexSecretKey)
        .compact();
  }

  /**
   * email 로 User 조회.
   * @param email email
   * @return User
   */
  public User getUser(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new BadCredentialsException("이메일이 올바르지 않습니다."));
  }

  /**
   * getUser.
   * @param httpServletRequest httpServletRequest
   * @return user
   */
  public Optional<User> getUser(HttpServletRequest httpServletRequest) {
    String email = getEmail(httpServletRequest);

    if (email.isEmpty()) {
      return Optional.empty();
    }

    return userRepository.findByEmail(email);
  }

  /**
   * getHash.
   * @param now now
   * @param email email
   * @return hash
   */
  public String getHash(LocalDateTime now, String email) {
    long timestamp = localDateTimeToTimestamp(now);
    String hashString = String.format("%s_%d", email, timestamp);
    byte[] hashBytes = hashString.getBytes(DEFAULT_CHARSET);
    return DigestUtils.md5DigestAsHex(hashBytes);
  }

  private boolean isExistsRefreshToken(HttpServletRequest httpServletRequest, String email) {
    String refreshTokenKey = getRefreshTokenKey(email);
    String refreshToken = getRefreshToken(httpServletRequest);
    Boolean member = redisTemplate.opsForSet().isMember(refreshTokenKey, refreshToken);

    if (Objects.isNull(member)) {
      return false;
    }

    return member;
  }

  /**
   * checkExistsRefreshToken.
   * @param httpServletRequest httpServletRequest
   * @param email email
   */
  public void checkExistsRefreshToken(HttpServletRequest httpServletRequest, String email) {
    boolean existsRefreshToken = isExistsRefreshToken(httpServletRequest, email);

    if (!existsRefreshToken) {
      throw new BadCredentialsException("유효하지 않은 refreshToken 입니다.");
    }
  }

  public String generateRefreshToken() {
    return RandomStringUtils.randomAlphanumeric(32);
  }

  /**
   * addRefreshTokenToRedis.
   * @param email email
   * @param refreshToken refreshToken
   */
  public void addRefreshTokenToRedis(String email, String refreshToken) {
    String refreshTokenKey = getRefreshTokenKey(email);
    redisTemplate.opsForSet().add(refreshTokenKey, refreshToken);
    redisTemplate.expire(refreshTokenKey, 30L, TimeUnit.DAYS);
  }

  private String getRefreshTokenKey(String email) {
    return String.format("users:refresh-token:%s", email);
  }

  /**
   * getLoginResponseBodyString.
   * @param accessToken accessToken
   * @param refreshToken refreshToken
   * @return loginResponse
   */
  public String getLoginResponseBodyString(String accessToken, String refreshToken) {
    LoginResponse loginResponse = LoginResponse
        .builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();

    return objectToJsonString(loginResponse);
  }

  private String getRefreshToken(HttpServletRequest request) {
    String refreshToken = request.getHeader(HttpHeaders.AUTHORIZATION);

    checkPresentToken(refreshToken);
    checkPresentTokenPrefix(refreshToken);
    return removeTokenPrefix(refreshToken);
  }

  private void checkPresentTokenPrefix(String authorization) {
    if (!authorization.startsWith(TOKEN_PREFIX)) {
      throw new IllegalArgumentException("not found token prefix");
    }
  }

  private void checkPresentToken(String authorization) {
    Optional
        .ofNullable(authorization)
        .orElseThrow(
            () -> new BadCredentialsException("Required Header 'Authorization' is not present"));
  }

  private String removeTokenPrefix(String jwt) {
    return jwt.replace(TOKEN_PREFIX, "").trim();
  }

  /**
   * addUserSessionToRedis.
   * @param email email
   * @param userSessionDto userSessionDto
   */
  public void addUserSessionToRedis(String email, UserSessionDto userSessionDto) {
    email = email.toLowerCase();
    String hash = userSessionDto.getHash();
    String sessionKey = String.format("users:session:%s:%s", email, hash);

    redisTemplate.opsForValue().set(sessionKey, userSessionDto);
    redisTemplate.expire(sessionKey, 30L, TimeUnit.MINUTES);
  }

  /**
   * getEmail.
   * @param request httpServletRequest
   * @return email
   */
  public String getEmail(HttpServletRequest request) {
    UserSessionDto userSessionDto = getUserSessionDto(request);
    return userSessionDto.getEmail();
  }

  /**
   * getEmail.
   * @param accessToken accessToken
   * @return email
   */
  public String getEmail(String accessToken) {
    UserSessionDto userSessionDto = getUserSessionDto(accessToken);

    if (Objects.isNull(userSessionDto)) {
      throw new AccessDeniedException("");
    }

    return userSessionDto.getEmail();
  }

  private String getHexSecretKey() {
    return Sha512DigestUtils.shaHex(secretKey);
  }

  /**
   * getRoleFromAuthorities.
   * @param authorities authorities
   * @return role
   */
  public String getRoleFromAuthorities(Collection<GrantedAuthority> authorities) {
    return authorities
        .stream()
        .map(GrantedAuthority::getAuthority)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("사용자 권한을 찾을 수 없습니다."));
  }

  /**
   * getAuthorities.
   * @param userSessionDto userSessionDto
   * @return authorities
   */
  public List<SimpleGrantedAuthority> getAuthorities(UserSessionDto userSessionDto) {
    String role = userSessionDto.getRole();
    SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(role);
    return Collections.singletonList(simpleGrantedAuthority);
  }

  public void deleteSessionFromRedis(HttpServletRequest request) {
    String redisSessionKey = getRedisSessionKey(request);
    redisTemplate.delete(redisSessionKey);
  }

  public void deleteSessionFromRedis(String accessToken) {
    String redisSessionKey = getRedisSessionKey(accessToken);
    redisTemplate.delete(redisSessionKey);
  }

  /**
   * deleteRefreshTokenFromRedis.
   * @param email email
   * @param deleteUser deleteUser
   * @param refreshToken refreshToken
   */
  public void deleteRefreshTokenFromRedis(String email, boolean deleteUser, String refreshToken) {
    String refreshTokenKey = getRefreshTokenKey(email);

    if (deleteUser) {
      redisTemplate.delete(refreshTokenKey);
    } else {
      redisTemplate.opsForSet().remove(refreshTokenKey, refreshToken);
    }
  }
}
