package kr.webgori.lolien.discord.bot.exception;

public class LeagueExactEntriesNumberRequiredException extends RuntimeException {
    public LeagueExactEntriesNumberRequiredException(String message) {
        super(message);
    }
}