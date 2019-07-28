package kr.webgori.lolien.discord.bot.controller;

import kr.webgori.lolien.discord.bot.request.CustomGameAddResultRequest;
import kr.webgori.lolien.discord.bot.service.CustomGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("custom-game")
public class CustomGameController {
  private final CustomGameService customGameService;

  @PostMapping
  public void addResult(@RequestBody CustomGameAddResultRequest customGameAddResultRequest) {
    customGameService.addResult(customGameAddResultRequest);
  }
}