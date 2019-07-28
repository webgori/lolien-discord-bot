package kr.webgori.lolien.discord.bot.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
public class CustomGameAddLeagueResultRequest {
    private long matchId;
    private String entries;
}