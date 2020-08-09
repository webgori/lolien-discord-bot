package kr.webgori.lolien.discord.bot.response;

import lombok.Data;

@Data
public class ExceptionResponse {
  private long timestamp;
  private int status;
  private String error;
  private String message;
  private String path;
}