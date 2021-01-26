package kr.webgori.lolien.discord.bot.component;

import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Component
public class GameComponent {
  public byte[] getReplayBytes(MultipartFile file) {
    if (file == null) {
      return new byte[0];
    }

    try {
      InputStream inputStream = file.getInputStream();
      return IOUtils.toByteArray(inputStream);
    } catch (IOException e) {
      logger.error("", e);
    }

    return new byte[0];
  }
}
