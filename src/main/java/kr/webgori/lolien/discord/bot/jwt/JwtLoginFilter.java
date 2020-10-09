
package kr.webgori.lolien.discord.bot.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import kr.webgori.lolien.discord.bot.dto.SessionUserDto;
import kr.webgori.lolien.discord.bot.spring.AuthenticationTokenImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

public class JwtLoginFilter extends AbstractAuthenticationProcessingFilter {
  private TokenAuthenticationService tokenAuthenticationService;
  private ObjectMapper objectMapper;

  /**
   * JwtLoginFilter.
   * @param url url
   * @param authenticationManager authenticationManager
   * @param service service
   * @param objectMapper objectMapper
   */
  public JwtLoginFilter(String url, AuthenticationManager authenticationManager,
                        TokenAuthenticationService service,
                        ObjectMapper objectMapper) {
    super(new AntPathRequestMatcher(url, "POST"));
    setAuthenticationManager(authenticationManager);
    tokenAuthenticationService = service;
    this.objectMapper = objectMapper;
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request,
                                              HttpServletResponse response)
      throws AuthenticationException, IOException {

    ServletInputStream inputStream = request.getInputStream();
    SessionUserDto sessionUserDto = objectMapper.readValue(inputStream, SessionUserDto.class);

    String email = sessionUserDto.getEmail();

    if (StringUtils.isBlank(email)) {
      throw new BadCredentialsException("");
    }

    String password = sessionUserDto.getPassword();

    if (StringUtils.isBlank(password)) {
      throw new BadCredentialsException("");
    }

    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(email,
        password);

    return getAuthenticationManager().authenticate(token);
  }

  /**
   * 로그인 성공 후 인증 정보 추가.
   * @param request request
   * @param response response
   * @param chain chain
   * @param authentication authentication
   */
  @Override
  protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                          FilterChain chain, Authentication authentication)
      throws IOException {

    AuthenticationTokenImpl authTokenImpl = (AuthenticationTokenImpl) authentication;
    tokenAuthenticationService.addAuthentication(response, authTokenImpl);

    setStatusNoContent(response);
  }

  private void setStatusNoContent(HttpServletResponse response) {
    response.setStatus(HttpStatus.NO_CONTENT.value());
  }
}
