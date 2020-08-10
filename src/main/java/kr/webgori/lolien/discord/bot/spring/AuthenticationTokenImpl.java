package kr.webgori.lolien.discord.bot.spring;

import static kr.webgori.lolien.discord.bot.util.CommonUtil.getTimestamp;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import kr.webgori.lolien.discord.bot.dto.SessionUserDto;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.DigestUtils;

@Slf4j
@ToString(callSuper = true)
public class AuthenticationTokenImpl extends AbstractAuthenticationToken {
  private String username;

  public AuthenticationTokenImpl(String principal,
                                 Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.username = principal;
  }

  /**
   * authenticate.
   */
  public void authenticate() {
    Object details = getDetails();

    if (Objects.isNull(details) || !(details instanceof SessionUserDto)) {
      setAuthenticated(false);
    } else {
      SessionUserDto sessionUserDto = (SessionUserDto) details;
      boolean expired = sessionUserDto.hasExpired();

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
    return username;
  }

  /**
   * getHash.
   * @return hash
   */
  public String getHash() {
    SessionUserDto sessionUserDto = (SessionUserDto) getDetails();
    LocalDateTime createdAt = sessionUserDto.getCreatedAt();
    long timestamp = getTimestamp(createdAt);

    String hashString = String.format("%s_%d", username, timestamp);
    byte[] hashBytes = hashString.getBytes();
    return DigestUtils.md5DigestAsHex(hashBytes);
  }
}
