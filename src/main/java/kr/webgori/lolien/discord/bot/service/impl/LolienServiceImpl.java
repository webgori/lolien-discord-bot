package kr.webgori.lolien.discord.bot.service.impl;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.Cookie;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.LolienUser;
import kr.webgori.lolien.discord.bot.exception.AlreadyAddedSummonerException;
import kr.webgori.lolien.discord.bot.exception.SummonerNotFoundException;
import kr.webgori.lolien.discord.bot.jwt.TokenAuthenticationService;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
import kr.webgori.lolien.discord.bot.repository.LolienUserRepository;
import kr.webgori.lolien.discord.bot.request.LolienUserAddSummonerRequest;
import kr.webgori.lolien.discord.bot.service.LolienService;
import kr.webgori.lolien.discord.bot.spring.AuthenticationTokenImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class LolienServiceImpl implements LolienService {
  private final LolienUserRepository lolienUserRepository;
  private final TokenAuthenticationService tokenAuthenticationService;
  private final HttpServletRequest httpServletRequest;
  private final LolienSummonerRepository lolienSummonerRepository;

  @Value("${clien.url}")
  private String clienUrl;

  @Override
  public void login(AuthenticationTokenImpl authTokenImpl, HttpServletResponse response) {
    String clienId = authTokenImpl.getPrincipal().toString();
    boolean existsById = lolienUserRepository.existsByClienId(clienId);

    if (!existsById) {
      LolienUser lolienUser = LolienUser
          .builder()
          .clienId(clienId)
          .build();

      lolienUserRepository.save(lolienUser);
    }

    response.setStatus(HttpStatus.NO_CONTENT.value());
  }

  private WebClient getWebClient() {
    WebClient webClient = new WebClient(BrowserVersion.CHROME);
    webClient.getOptions().setThrowExceptionOnScriptError(false);
    webClient.getOptions().setCssEnabled(false);

    //webClient.waitForBackgroundJavaScript(60000);
    return webClient;
  }

  private boolean isSuccessfulLogin(String id, String password) {
    try {
      WebClient webClient = getWebClient();
      HtmlPage htmlPage = webClient.getPage(clienUrl);

      HtmlForm form = htmlPage.getHtmlElementById("loginForm");
      HtmlTextInput inputId = form.getInputByName("userId");
      HtmlPasswordInput inputPw = form.getInputByName("userPassword");

      inputId.setValueAttribute(id);
      inputPw.setValueAttribute(password);

      htmlPage = form.getButtonByName("로그인하기").click();
      return htmlPage.asText().contains("로그아웃");
    } catch (IOException e) {
      logger.error("", e);
      throw new BadCredentialsException("");
    }
  }

  public void checkLogin(String id, String password) {
    boolean successfulLogin = isSuccessfulLogin(id, password);

    if (!successfulLogin) {
      throw new BadCredentialsException("");
    }
  }

  private String getSession(WebClient webClient) {
    Set<Cookie> cookies = webClient.getCookieManager().getCookies();

    Cookie cookie = cookies
        .stream()
        .filter(c -> c.getName().equals("SESSION"))
        .findFirst().orElseThrow(() -> new InvalidCookieException("not found session"));

    return cookie.getValue();
  }

  @Override
  public void addSummoner(LolienUserAddSummonerRequest request) {
    String clienId = tokenAuthenticationService.getClienId(httpServletRequest);
    boolean existsByClienId = lolienUserRepository.existsByClienId(clienId);

    if (!existsByClienId) {
      throw new BadCredentialsException("not found clien user");
    }

    LolienUser lolienUser = lolienUserRepository.findByClienId(clienId);
    LolienSummoner lolienSummoner = lolienUser.getLolienSummoner();

    if (Objects.nonNull(lolienSummoner)) {
      throw new AlreadyAddedSummonerException("");
    }

    String summonerName = request.getSummonerName();
    lolienSummoner = lolienSummonerRepository.findBySummonerName(summonerName);

    if (Objects.isNull(lolienSummoner)) {
      throw new SummonerNotFoundException("");
    }

    lolienUser.setLolienSummoner(lolienSummoner);
    lolienUserRepository.save(lolienUser);
  }

  @Override
  public void deleteSummoner() {
    String clienId = tokenAuthenticationService.getClienId(httpServletRequest);
    boolean existsByClienId = lolienUserRepository.existsByClienId(clienId);

    if (!existsByClienId) {
      throw new BadCredentialsException("not found clien user");
    }

    LolienUser lolienUser = lolienUserRepository.findByClienId(clienId);
    LolienSummoner lolienSummoner = lolienUser.getLolienSummoner();

    if (Objects.isNull(lolienSummoner)) {
      throw new SummonerNotFoundException("");
    }

    lolienUser.setLolienSummoner(null);
    lolienUserRepository.save(lolienUser);
  }
}
