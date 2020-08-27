package kr.webgori.lolien.discord.bot.spring;

import kr.webgori.lolien.discord.bot.exception.AlreadyAddedSummonerException;
import kr.webgori.lolien.discord.bot.exception.SummonerNotFoundException;
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

  @ExceptionHandler(value = IllegalArgumentException.class)
  public ResponseEntity<Void> illegalArgumentException() {
    HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
    return new ResponseEntity<>(httpStatus);
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
