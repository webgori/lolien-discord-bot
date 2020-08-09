/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package kr.webgori.lolien.discord.bot.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import kr.webgori.lolien.discord.bot.dto.SessionUserDto;
import kr.webgori.lolien.discord.bot.service.LolienService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@RequiredArgsConstructor
public class AuthenticationProviderImpl implements AuthenticationProvider {
  private final RedisTemplate<String, Object> redisTemplate;
  private final LolienService lolienService;
  private final ObjectMapper objectMapper;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String clienId = authentication.getPrincipal().toString();
    String clienPassword = authentication.getCredentials().toString();

    lolienService.checkLogin(clienId, clienPassword);

    String role = "ROLE_USER";

    SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(role);
    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    authorities.add(simpleGrantedAuthority);

    AuthenticationTokenImpl authenticationToken = new AuthenticationTokenImpl(clienId, authorities);
    authenticationToken.setAuthenticated(true);

    LocalDateTime createdAt = LocalDateTime.now();

    SessionUserDto sessionUserDto = SessionUserDto
        .builder()
        .id(clienId)
        .createdAt(createdAt)
        .build();

    authenticationToken.setDetails(sessionUserDto);

    String idLowerCase = clienId.toLowerCase();
    String hash = authenticationToken.getHash();
    String key = String.format("%s:%s", idLowerCase, hash);

    redisTemplate.opsForValue().set(key, sessionUserDto);
    redisTemplate.expire(key, 90L, TimeUnit.DAYS);

    return authenticationToken;
  }

  @Override
  public boolean supports(Class<?> type) {
    return type.equals(UsernamePasswordAuthenticationToken.class);
  }
}
