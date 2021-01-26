package kr.webgori.lolien.discord.bot.component;

import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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

  private String getFilename(long gameId) {
    return "KR-" + gameId + ".rofl";
  }

  /**
   * 리플레이 헤더.
   * @param gameId gameId
   * @return HttpHeaders
   */
  public HttpHeaders getReplayHeader(long gameId) {
    HttpHeaders httpHeaders = new HttpHeaders();
    String filename = getFilename(gameId);
    httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
    return httpHeaders;
  }
}
