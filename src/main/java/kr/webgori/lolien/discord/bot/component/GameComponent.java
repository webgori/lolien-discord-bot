package kr.webgori.lolien.discord.bot.component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@SuppressFBWarnings(justification = "Generated code")
@Slf4j
@RequiredArgsConstructor
@Component
public class GameComponent {
  /**
   * 리플레이 파일을 byte[]로 변환.
   * @param file file
   * @return byte[]
   */
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
