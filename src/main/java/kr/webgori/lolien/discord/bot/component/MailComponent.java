package kr.webgori.lolien.discord.bot.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class MailComponent {
  private final JavaMailSender javaMailSender;

  /**
   * 메일 보내기.
   * @param from from
   * @param to to
   * @param subject subject
   * @param text text
   */
  public void sendMail(String from, String to, String subject, String text) {
    try {
      SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
      simpleMailMessage.setFrom(from);
      simpleMailMessage.setTo(to);
      simpleMailMessage.setSubject(subject);
      simpleMailMessage.setText(text);

      javaMailSender.send(simpleMailMessage);
    } catch (MailException e) {
      logger.error("", e);
    }
  }
}
