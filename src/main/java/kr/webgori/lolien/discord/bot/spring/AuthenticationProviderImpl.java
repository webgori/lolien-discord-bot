package kr.webgori.lolien.discord.bot.spring;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import kr.webgori.lolien.discord.bot.component.AuthenticationComponent;
import kr.webgori.lolien.discord.bot.dto.UserSessionDto;
import kr.webgori.lolien.discord.bot.entity.user.User;
import kr.webgori.lolien.discord.bot.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@RequiredArgsConstructor
public class AuthenticationProviderImpl implements AuthenticationProvider {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationComponent authenticationComponent;

  /**
   * 로그인 (인증 정보가 맞는지 확인).
   * @param authentication authentication
   * @return Authentication
   * @throws AuthenticationException AuthenticationException
   */
  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String email = authentication.getPrincipal().toString();
    String password = authentication.getCredentials().toString();

    authentication(email, password);

    User user = getUser(email);
    String role = user.getUserRole().getRole().getRole();

    SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(role);
    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    authorities.add(simpleGrantedAuthority);

    AuthenticationTokenImpl authenticationToken = new AuthenticationTokenImpl(email, authorities);
    authenticationToken.setAuthenticated(true);

    LocalDateTime now = LocalDateTime.now();
    String hash = authenticationComponent.getHash(now, email);

    //String hash = authenticationToken.getHash();
    //UserSessionDto userSessionDto = getUserSessionDto(user, hash);
    UserSessionDto userSessionDto = authenticationComponent.getUserSessionDto(now, user, hash);
    authenticationToken.setDetails(userSessionDto);

    //LocalDateTime createdAt = LocalDateTime.now();

    /*SessionUserDto sessionUserDto = SessionUserDto
        .builder()
        .email(email)
        .role(role)
        .createdAt(createdAt)
        .build();

    authenticationToken.setDetails(sessionUserDto);*/

    /*String idLowerCase = email.toLowerCase();
    String sessionKey = String.format("users:session:%s:%s", idLowerCase, hash);*/

    //String key = String.format("users:%s", idLowerCase);
    /*redisTemplate.opsForValue().set(sessionKey, userSessionDto);
    redisTemplate.expire(sessionKey, 30L, TimeUnit.MINUTES);*/

    authenticationComponent.addUserSessionToRedis(email, userSessionDto);

    return authenticationToken;
  }

  private void authentication(String email, String password) {
    checkExistsEmail(email);
    checkMatchesPassword(email, password);
  }

  private void checkExistsEmail(String email) {
    boolean existsByEmail = userRepository.existsByEmail(email);

    if (!existsByEmail) {
      throw new BadCredentialsException("이메일이 존재하지 않습니다.");
    }
  }

  private void checkMatchesPassword(String email, String rawPassword) {
    User user = getUser(email);
    String encodedPassword = user.getPassword();

    boolean matchesPassword = passwordEncoder.matches(rawPassword, encodedPassword);

    if (!matchesPassword) {
      throw new BadCredentialsException("비밀번호가 올바르지 않습니다.");
    }
  }

  private User getUser(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new BadCredentialsException("이메일이 올바르지 않습니다."));
  }

  @Override
  public boolean supports(Class<?> type) {
    return type.equals(UsernamePasswordAuthenticationToken.class);
  }
}
