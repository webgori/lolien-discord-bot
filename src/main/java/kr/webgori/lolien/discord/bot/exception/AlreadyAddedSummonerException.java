package kr.webgori.lolien.discord.bot.exception;

public class AlreadyAddedSummonerException extends RuntimeException {
  public AlreadyAddedSummonerException(String message) {
    super(message);
  }
}