package kr.webgori.lolien.discord.bot.spring;

import java.io.IOException;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@RequiredArgsConstructor
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
  private final RequestMappingHandlerMapping requestMappingHandlerMapping;

  @Override
  public void handle(
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse,
      AccessDeniedException accessDeniedException) throws IOException {

    logger.error("", accessDeniedException);

    HttpStatus httpStatus = HttpStatus.UNAUTHORIZED;

    String message = accessDeniedException.getMessage();

    if (message.equals("Access is denied")) {
      message = "탈퇴된 회원입니다.";
    } else {
      message = "아이디 또는 비밀번호를 확인해주세요.";
    }

    try {
      HandlerExecutionChain handler = requestMappingHandlerMapping.getHandler(httpServletRequest);

      if (Objects.isNull(handler)) {
        httpStatus = HttpStatus.NOT_FOUND;
        message = HttpStatus.NOT_FOUND.getReasonPhrase();
      }
    } catch (Exception e) {
      logger.info("", e);
      httpStatus = HttpStatus.NOT_FOUND;
      message = HttpStatus.NOT_FOUND.getReasonPhrase();
    }

    int status = httpStatus.value();

    httpServletResponse.sendError(status, message);
  }
}
