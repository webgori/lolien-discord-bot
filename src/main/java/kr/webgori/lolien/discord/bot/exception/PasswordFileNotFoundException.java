package kr.webgori.lolien.discord.bot.exception;

public class PasswordFileNotFoundException extends RuntimeException {
  public PasswordFileNotFoundException(String message) {
    super(message);
  }
}