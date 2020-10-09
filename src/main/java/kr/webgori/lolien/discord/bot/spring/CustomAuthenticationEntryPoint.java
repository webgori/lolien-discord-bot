package kr.webgori.lolien.discord.bot.spring;

import java.io.IOException;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@RequiredArgsConstructor
@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final RequestMappingHandlerMapping requestMappingHandlerMapping;

  @Override
  public void commence(HttpServletRequest httpServletRequest,
                       HttpServletResponse httpServletResponse,
                       AuthenticationException authException) throws IOException {

    logger.error("", authException);

    HttpStatus httpStatus = HttpStatus.UNAUTHORIZED;
    String message = "아이디 또는 비밀번호를 확인해주세요.";

    try {
      Objects
          .requireNonNull(requestMappingHandlerMapping.getHandler(httpServletRequest))
          .getHandler();
    } catch (Exception e) {
      logger.info("", e);
      httpStatus = HttpStatus.NOT_FOUND;
      message = HttpStatus.NOT_FOUND.getReasonPhrase();
    }

    int status = httpStatus.value();
    httpServletResponse.sendError(status, message);
  }
}
