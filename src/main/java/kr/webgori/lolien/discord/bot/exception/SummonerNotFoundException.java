package kr.webgori.lolien.discord.bot.exception;

public class SummonerNotFoundException extends RuntimeException {
  public SummonerNotFoundException(String message) {
    super(message);
  }
}