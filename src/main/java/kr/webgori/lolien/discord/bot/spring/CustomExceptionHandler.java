package kr.webgori.lolien.discord.bot.spring;

import static kr.webgori.lolien.discord.bot.util.CommonUtil.getTimestamp;

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
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    ExceptionResponse exceptionResponse = new ExceptionResponse();
    long timestamp = getTimestamp();
    exceptionResponse.setTimestamp(timestamp);

    String requestUri = httpServletRequest.getRequestURI();
    exceptionResponse.setPath(requestUri);

    String message = exception.getMessage();
    exceptionResponse.setMessage(message);

    HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
    int status = httpStatus.value();
    exceptionResponse.setStatus(status);

    String error = httpStatus.getReasonPhrase();
    exceptionResponse.setError(error);

    return new ResponseEntity<>(exceptionResponse, httpStatus);
  }

  /**
   * accessDeniedException.
   * @param exception exception
   * @return ResponseEntity
   */
  @ExceptionHandler(value = AccessDeniedException.class)
  public ResponseEntity accessDeniedException(AccessDeniedException exception) {
    logger.error("", exception);

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

  /**
   * methodArgumentNotValidException.
   * @param exception exception
   * @return ResponseEntity
   */
  @ExceptionHandler(value = MethodArgumentNotValidException.class)
  public ResponseEntity methodArgumentNotValidException(MethodArgumentNotValidException exception) {
    logger.error("", exception);

    HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
    return new ResponseEntity<>(httpStatus);
  }
}
