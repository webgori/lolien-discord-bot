package kr.webgori.lolien.discord.bot.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import kr.webgori.lolien.discord.bot.component.AuthenticationComponent;
import kr.webgori.lolien.discord.bot.dto.UserSessionDto;
import kr.webgori.lolien.discord.bot.spring.AuthenticationTokenImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAuthenticationService {
  private final AuthenticationComponent authenticationComponent;

  void addAuthentication(HttpServletResponse response, AuthenticationTokenImpl authTokenImpl)
      throws IOException {

    String email = authTokenImpl.getPrincipal().toString();
    UserSessionDto userSessionDto = (UserSessionDto) authTokenImpl.getDetails();

    Collection<GrantedAuthority> authorities = authTokenImpl.getAuthorities();
    String role = authenticationComponent.getRoleFromAuthorities(authorities);

    String accessToken = authenticationComponent.generateAccessToken(email, userSessionDto, role);

    String refreshToken = authenticationComponent.generateRefreshToken();
    authenticationComponent.addRefreshTokenToRedis(email, refreshToken);

    String responseBody = authenticationComponent.getLoginResponseBodyString(accessToken,
        refreshToken);

    response.getWriter().write(responseBody);
    response.getWriter().flush();
    response.getWriter().close();
  }

  Authentication getAuthentication(HttpServletRequest request, ServletResponse response) {
    try {
      UserSessionDto userSessionDto = authenticationComponent
          .getUserSessionDto(request);

      String email = userSessionDto.getEmail();

      if (email.isEmpty()) {
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());

        throw new BadCredentialsException("계정 정보가 유효하지 않습니다.");
      }

      List<SimpleGrantedAuthority> authorities = authenticationComponent.getAuthorities(
          userSessionDto);
      AuthenticationTokenImpl authTokenImpl = new AuthenticationTokenImpl(email, authorities);
      authTokenImpl.setDetails(userSessionDto);
      authTokenImpl.authenticate();

      return authTokenImpl;
    } catch (MalformedJwtException | ExpiredJwtException | SignatureException e) {
      logger.error("", e);

      HttpServletResponse httpServletResponse = (HttpServletResponse) response;
      httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
    }

    throw new BadCredentialsException("계정 정보가 유효하지 않습니다.");
  }
}
