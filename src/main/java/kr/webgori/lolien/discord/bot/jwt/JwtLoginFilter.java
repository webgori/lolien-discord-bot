
package kr.webgori.lolien.discord.bot.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import kr.webgori.lolien.discord.bot.dto.SessionUserDto;
import kr.webgori.lolien.discord.bot.service.LolienService;
import kr.webgori.lolien.discord.bot.spring.AuthenticationTokenImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

public class JwtLoginFilter extends AbstractAuthenticationProcessingFilter {
  private TokenAuthenticationService tokenAuthenticationService;
  private LolienService lolienService;
  private ObjectMapper objectMapper;

  /**
   * JwtLoginFilter.
   * @param url url
   * @param authenticationManager authenticationManager
   * @param service service
   * @param lolienService lolienService
   * @param objectMapper objectMapper
   */
  public JwtLoginFilter(String url, AuthenticationManager authenticationManager,
                        TokenAuthenticationService service, LolienService lolienService,
                        ObjectMapper objectMapper) {
    super(new AntPathRequestMatcher(url, "POST"));
    setAuthenticationManager(authenticationManager);
    tokenAuthenticationService = service;
    this.lolienService = lolienService;
    this.objectMapper = objectMapper;
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest httpServletRequest,
                                              HttpServletResponse hsr1)
      throws AuthenticationException, IOException {

    ServletInputStream inputStream = httpServletRequest.getInputStream();
    SessionUserDto sessionUserDto = objectMapper.readValue(inputStream, SessionUserDto.class);

    String clienId = sessionUserDto.getId();

    if (StringUtils.isBlank(clienId)) {
      throw new BadCredentialsException("");
    }

    String clienPassword = sessionUserDto.getPassword();

    if (StringUtils.isBlank(clienPassword)) {
      throw new BadCredentialsException("");
    }

    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(clienId,
        clienPassword);

    return getAuthenticationManager().authenticate(token);
  }

  @Override
  protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                          FilterChain chain, Authentication authentication) {

    AuthenticationTokenImpl authTokenImpl = (AuthenticationTokenImpl) authentication;
    tokenAuthenticationService.addAuthentication(response, authTokenImpl);

    lolienService.login(authTokenImpl, response);
  }
}
