package kr.webgori.lolien.discord.bot.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import kr.webgori.lolien.discord.bot.component.ConfigComponent;
import kr.webgori.lolien.discord.bot.dto.SessionUserDto;
import kr.webgori.lolien.discord.bot.spring.AuthenticationTokenImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAuthenticationService {
  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;
  private static String secret;
  private static final String tokenPrefix = "Bearer";
  private final ConfigComponent configComponent;

  private static void setSecret(String secret) {
    TokenAuthenticationService.secret = secret;
  }

  private void checkSecretKey() {
    if (Objects.isNull(secret)) {
      String jwtSecretKey = configComponent.getJwtSecretKey();

      if (Objects.isNull(jwtSecretKey)) {
        configComponent.getJwtSecretKeyFromConfig();
        jwtSecretKey = configComponent.getJwtSecretKey();
      }

      String secret = Sha512DigestUtils.shaHex(jwtSecretKey);
      setSecret(secret);
    }
  }

  void addAuthentication(HttpServletResponse response, AuthenticationTokenImpl authTokenImpl) {
    checkSecretKey();

    Map<String, Object> claims = new HashMap<>();

    String clienId = authTokenImpl.getPrincipal().toString();
    claims.put("clienId", clienId);

    String hash = authTokenImpl.getHash();
    claims.put("hash", hash);

    Collection<GrantedAuthority> authorities = authTokenImpl.getAuthorities();

    List<String> roles = authorities
        .stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList());

    claims.put("roles", roles);

    String jwt = Jwts
        .builder()
        .setSubject(authTokenImpl.getPrincipal().toString())
        .setClaims(claims)
        .setExpiration(Timestamp.valueOf(LocalDateTime.now().plusDays(90)))
        .signWith(SignatureAlgorithm.HS512, secret)
        .compact();

    String headerValue = String.format("%s %s", tokenPrefix, jwt);
    response.addHeader(HttpHeaders.AUTHORIZATION, headerValue);
  }

  Authentication getAuthentication(HttpServletRequest request, ServletResponse response) {
    checkSecretKey();

    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    boolean presentToken = Objects.nonNull(authorization);

    if (!presentToken) {
      throw new IllegalArgumentException("Required Header 'Authorization' is not present");
    }

    if (!authorization.startsWith("Bearer ")) {
      throw new IllegalArgumentException("not found token prefix");
    }

    String token = request
        .getHeader(HttpHeaders.AUTHORIZATION)
        .replace(tokenPrefix, "")
        .trim();

    try {
      Claims claims = Jwts
          .parser()
          .setSigningKey(secret)
          .parseClaimsJws(token)
          .getBody();

      if (!claims.containsKey("clienId")) {
        throw new IllegalArgumentException("invalid JWT");
      }

      String clienId = claims.get("clienId").toString();
      String hash = claims.get("hash").toString();

      String key = String.format("%s:%s", clienId, hash);
      Object obj = redisTemplate.opsForValue().get(key);
      SessionUserDto sessionUserDto = objectMapper.convertValue(obj, SessionUserDto.class);

      boolean presentSession = Objects.nonNull(sessionUserDto);

      if (!presentSession) {
        throw new AccessDeniedException("");
      }

      String role = "ROLE_USER";

      SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(role);
      List<SimpleGrantedAuthority> authorities = new ArrayList<>();
      authorities.add(simpleGrantedAuthority);

      String id = sessionUserDto.getId();
      AuthenticationTokenImpl authTokenImpl = new AuthenticationTokenImpl(id, authorities);
      authTokenImpl.setDetails(sessionUserDto);
      authTokenImpl.authenticate();

      return authTokenImpl;
    } catch (MalformedJwtException | ExpiredJwtException | SignatureException e) {
      logger.error("", e);

      if (Objects.nonNull(response)) {
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        httpServletResponse.setStatus(HttpStatus.BAD_REQUEST.value());
      }
    }

    return null;
  }

  /**
   * getClienId.
   * @param httpServletRequest httpServletRequest
   * @return clien id
   */
  public String getClienId(HttpServletRequest httpServletRequest) {
    Authentication authentication = getAuthentication(httpServletRequest, null);

    if (Objects.isNull(authentication)) {
      throw new AccessDeniedException("");
    }

    return authentication.getPrincipal().toString();
  }
}
