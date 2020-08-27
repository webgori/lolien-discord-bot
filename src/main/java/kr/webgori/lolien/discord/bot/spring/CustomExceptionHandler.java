package kr.webgori.lolien.discord.bot.spring;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import javax.servlet.http.HttpServletRequest;
import kr.webgori.lolien.discord.bot.exception.AlreadyAddedSummonerException;
import kr.webgori.lolien.discord.bot.exception.SummonerNotFoundException;
import kr.webgori.lolien.discord.bot.response.ExceptionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@RequiredArgsConstructor
@ControllerAdvice
public class CustomExceptionHandler {
  private final HttpServletRequest httpServletRequest;

  @ExceptionHandler(value = BadCredentialsException.class)
  public ResponseEntity<Void> badCredentialsException() {
    HttpStatus httpStatus = HttpStatus.UNAUTHORIZED;
    return new ResponseEntity<>(httpStatus);
  }

  @ExceptionHandler(value = AlreadyAddedSummonerException.class)
  public ResponseEntity<Void> alreadyAddedSummonerException() {
    HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
    return new ResponseEntity<>(httpStatus);
  }

  @ExceptionHandler(value = SummonerNotFoundException.class)
  public ResponseEntity<Void> summonerNotFoundException() {
    HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
    return new ResponseEntity<>(httpStatus);
  }

  /**
   * illegalArgumentException.
   * @param exception IllegalArgumentException
   * @return ResponseEntity
   */
  @ExceptionHandler(value = IllegalArgumentException.class)
  public ResponseEntity<ExceptionResponse> illegalArgumentException(
      IllegalArgumentException exception) {

    String message = exception.getMessage();

    HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
    int status = httpStatus.value();
    String error = httpStatus.getReasonPhrase();

    String requestUri = httpServletRequest.getRequestURI();

    LocalDateTime dateTime = LocalDateTime.now();
    long timestamp = Timestamp.valueOf(dateTime).getTime();

    ExceptionResponse exceptionResponse = new ExceptionResponse();
    exceptionResponse.setTimestamp(timestamp);
    exceptionResponse.setPath(requestUri);
    exceptionResponse.setMessage(message);
    exceptionResponse.setStatus(status);
    exceptionResponse.setError(error);

    return new ResponseEntity<>(exceptionResponse, httpStatus);
  }

  @ExceptionHandler(value = AccessDeniedException.class)
  public ResponseEntity accessDeniedException(AccessDeniedException exception) {
    HttpStatus httpStatus = HttpStatus.FORBIDDEN;
    return new ResponseEntity<>(httpStatus);
  }

  /**
   * exception.
   * @param exception exception
   * @return ResponseEntity
   */
  @ExceptionHandler(value = Exception.class)
  public ResponseEntity<Object> exception(Exception exception) {
    logger.error("", exception);

    HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    return new ResponseEntity<>(httpStatus);
  }
}
