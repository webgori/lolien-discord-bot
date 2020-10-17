package kr.webgori.lolien.discord.bot.unit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlHiddenInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.Cookie;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
public class ClienUnitTest {
  private WebClient getWebClient() {
    WebClient webClient = new WebClient(BrowserVersion.CHROME);
    webClient.getOptions().setThrowExceptionOnScriptError(false);
    webClient.getOptions().setCssEnabled(false);
    return webClient;
  }

  @Test
  public void clienLogin() {
    String clienUrl = "https://www.clien.net/service/";
    String id = "";
    String password = "";

    try {
      WebClient webClient = getWebClient();
      HtmlPage htmlPage = webClient.getPage(clienUrl);

      HtmlForm form = htmlPage.getHtmlElementById("loginForm");
      HtmlTextInput inputId = form.getInputByName("userId");
      HtmlPasswordInput inputPw = form.getInputByName("userPassword");

      inputId.setValueAttribute(id);
      inputPw.setValueAttribute(password);

      htmlPage = form.getButtonByName("로그인하기").click();
      boolean login = htmlPage.asText().contains("로그아웃");

      assertThat(login, is(true));

      HtmlHiddenInput csrfHiddenInput = htmlPage.getElementByName("_csrf");
      String csrf = csrfHiddenInput.getValueAttribute();

      assertThat(csrf.length(), greaterThan(0));

      Set<Cookie> cookies = webClient.getCookieManager().getCookies();

      Cookie cookie = cookies
          .stream()
          .filter(c -> c.getName().equals("SESSION"))
          .findFirst().orElseThrow(() -> new InvalidCookieException("not found session"));

      String value = cookie.getValue();
      System.out.println(value);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void timestamp() {
    LocalDateTime now = LocalDateTime.now();

    long timestamp1 = now
        .atZone(ZoneId.of("Asia/Seoul"))
        .toInstant()
        .getEpochSecond();

    System.out.println(timestamp1);

    long timestamp2 = now
        .atZone(ZoneId.of("GMT")).toInstant()
        .getEpochSecond();

    System.out.println(timestamp2);

    long timestamp3 = Timestamp.valueOf(LocalDateTime.now()).getTime();

    System.out.println(timestamp3);
  }
}
