package kr.webgori.lolien.discord.bot.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LeagueAddRequest {
    @NotBlank
    private String title;
}