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
import kr.webgori.lolien.discord.bot.component.MailComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@RequiredArgsConstructor
public class ClienUnitTest {
  private static final String REGISTER_VERIFY_EMAIL_SUBJECT = "LoLien.kr 회원가입 이메일 인증";
  private static final String REGISTER_VERIFY_EMAIL_TEXT = "LoLien.kr (https://lolien.kr) 회원가입 "
      + "이메일 인증 번호는 [%s] 입니다. 5분이 지나면 인증 번호는 만료됩니다.";

  @Autowired
  private MailComponent mailComponent;

  @Value("${clien.service.url}")
  private String clienUrl;

  private WebClient getWebClient() {
    WebClient webClient = new WebClient(BrowserVersion.CHROME);
    webClient.getOptions().setThrowExceptionOnScriptError(false);
    webClient.getOptions().setCssEnabled(false);
    return webClient;
  }

  @Test
  public void clienLogin() {
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
  public void sendEmail() {
    String authNumber = getAuthNumber();
    String emailVerifyText = getEmailVerifyText(authNumber);

    mailComponent.sendMail("no-reply@LoLien.kr", "webgori@gmail.com", REGISTER_VERIFY_EMAIL_SUBJECT,
        emailVerifyText);
  }

  private String getAuthNumber() {
    return RandomStringUtils.randomNumeric(6);
  }

  private String getEmailVerifyText(String authNumber) {
    return String.format(REGISTER_VERIFY_EMAIL_TEXT, authNumber);
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
