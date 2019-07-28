package kr.webgori.lolien.discord.bot.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class LeagueAddResultRequest {
    @NotNull
    private Integer leagueIdx;

    @NotNull
    private Long matchId;

    @NotBlank
    private String entries;
}