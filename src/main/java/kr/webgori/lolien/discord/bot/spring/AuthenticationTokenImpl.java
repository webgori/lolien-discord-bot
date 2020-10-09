package kr.webgori.lolien.discord.bot.spring;

import java.util.Collection;
import java.util.Objects;
import kr.webgori.lolien.discord.bot.dto.UserSessionDto;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

@Slf4j
@ToString(callSuper = true)
public class AuthenticationTokenImpl extends AbstractAuthenticationToken {
  private String email;

  public AuthenticationTokenImpl(String principal,
                                 Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.email = principal;
  }

  /**
   * authenticate.
   */
  public void authenticate() {
    Object details = getDetails();

    if (Objects.isNull(details) || !(details instanceof UserSessionDto)) {
      setAuthenticated(false);
    } else {
      UserSessionDto userSessionDto = (UserSessionDto) details;
      boolean expired = userSessionDto.hasExpired();

      setAuthenticated(true);

      if (expired) {
        setAuthenticated(false);
      }
    }
  }

  @Override
  public Object getCredentials() {
    return "";
  }

  @Override
  public Object getPrincipal() {
    return email;
  }
}
