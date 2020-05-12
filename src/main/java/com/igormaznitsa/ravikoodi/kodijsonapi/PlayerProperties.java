package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(force = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerProperties {
  
  public static final String[] ALL_NAMES = {
    "type", 
    "partymode", 
    "speed", 
    "time", 
    "percentage", 
    "totaltime", 
    "playlistid", 
    "position", 
    "repeat", 
    "shuffled", 
    "canseek", 
    "canchangespeed", 
    "canmove", 
    "canzoom", 
    "canrotate", 
    "canshuffle", 
    "canrepeat", 
    "currentaudiostream", 
    "audiostreams", 
    "subtitleenabled", 
    "currentsubtitle", 
    "subtitles", 
    "live"
  };
  
  private final String type;
  private final boolean partymode;
  private final long speed;
  private final Time time;
  private final Time totaltime;
  private final double percentage;
  private final long playlistid;
  private final long position;
  private final String repeat;
  private final boolean shuffled;
  private final boolean canseek;
  private final boolean canchangespeed;
  private final boolean canmove;
  private final boolean canzoom;
  private final boolean canrotate;
  private final boolean canshuffle;
  private final boolean canrepeat;
  private final AudioStream currentaudiostream;
  private final AudioStream[] audiostreams;
  private final boolean subtitleenabled;
  private final Subtitle currentsubtitle;
  private final Subtitle[] subtitles;
  private final boolean live;
  
}
