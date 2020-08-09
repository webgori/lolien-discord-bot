package kr.webgori.lolien.discord.bot.exception;

public class ClienLoginFailException extends RuntimeException {
  public ClienLoginFailException(String message) {
    super(message);
  }
}