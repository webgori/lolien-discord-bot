package kr.webgori.lolien.discord.bot.jwt;

import java.io.IOException;
import java.util.Collection;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {
  private final TokenAuthenticationService tokenAuthenticationService;

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse response,
                       FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    Authentication authentication = tokenAuthenticationService.getAuthentication(request, response);
    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

    if (authorities.isEmpty()) {
      HttpServletResponse httpServletResponse = (HttpServletResponse) response;
      httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
    } else {
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(servletRequest, response);
  }
}
