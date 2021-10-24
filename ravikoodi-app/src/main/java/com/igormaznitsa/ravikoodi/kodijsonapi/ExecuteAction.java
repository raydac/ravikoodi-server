/*
 * Copyright 2018 Igor Maznitsa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.ravikoodi.kodijsonapi;

public enum ExecuteAction {
  LEFT("left"), RIGHT("right"), UP("up"), DOWN("down"), PAGEUP("pageup"), PAGEDOWN("pagedown"), SELECT("select"),
  HIGHLIGHT("highlight"), PARENTDIR("parentdir"), PARENTFOLDER("parentfolder"), BACK("back"), MENU("menu"), PREVIOUSMENU("previousmenu"),
  INFO("info"), PAUSE("pause"), STOP("stop"), SKIPNEXT("skipnext"), SKIPPREVIOUS("skipprevious"), FULLSCREEN("fullscreen"), ASPECTRATIO("aspectratio"),
  STEPFORWARD("stepforward"), STEPBACK("stepback"), BIGSTEPFORWARD("bigstepforward"), BIGSTEPBACK("bigstepback"),
  CHAPTERORBIGSTEPFORWARD("chapterorbigstepforward"), CHAPTERORBIGSTEPBACK("chapterorbigstepback"), OSD("osd"),
  SHOWSUBTITLES("showsubtitles"), NEXTSUBTITLE("nextsubtitle"), CYCLESUBTITLE("cyclesubtitle"), PLAYERDEBUG("playerdebug"),
  CODECINFO("codecinfo"), PLAYERPROCESSINFO("playerprocessinfo"), NEXTPICTURE("nextpicture"), PREVIOUSPICTURE("previouspicture"),
  ZOOMOUT("zoomout"), ZOOMIN("zoomin"), PLAYLIST("playlist"), QUEUE("queue"), ZOOMNORMAL("zoomnormal"), ZOOMLEVEL1("zoomlevel1"),
  ZOOMLEVEL2("zoomlevel2"), ZOOMLEVEL3("zoomlevel3"), ZOOMLEVEL4("zoomlevel4"), ZOOMLEVEL5("zoomlevel5"), ZOOMLEVEL6("zoomlevel6"),
  ZOOMLEVEL7("zoomlevel7"), ZOOMLEVEL8("zoomlevel8"), ZOOMLEVEL9("zoomlevel9"), NEXTCALIBRATION("nextcalibration"),
  RESETCALIBRATION("resetcalibration"), ANALOGMOVE("analogmove"), ANALOGMOVEX("analogmovex"), ANALOGMOVEY("analogmovey"),
  ROTATE("rotate"), ROTATECCW("rotateccw"), CLOSE("close"), SUBTITLEDELAYMINUS("subtitledelayminus"), SUBTITLEDELAY("subtitledelay"),
  SUBTITLEDELAYPLUS("subtitledelayplus"), AUDIODELAYMINUS("audiodelayminus"), AUDIODELAY("audiodelay"), AUDIODELAYPLUS("audiodelayplus"),
  SUBTITLESHIFTUP("subtitleshiftup"), SUBTITLESHIFTDOWN("subtitleshiftdown"), SUBTITLEALIGN("subtitlealign"), AUDIONEXTLANGUAGE("audionextlanguage"),
  VERTICALSHIFTUP("verticalshiftup"), VERTICALSHIFTDOWN("verticalshiftdown"), NEXTRESOLUTION("nextresolution"), AUDIOTOGGLEDIGITAL("audiotoggledigital"),
  NUMBER0("number0"), NUMBER1("number1"), NUMBER2("number2"), NUMBER3("number3"), NUMBER4("number4"), NUMBER5("number5"), NUMBER6("number6"),
  NUMBER7("number7"), NUMBER8("number8"), NUMBER9("number9"), SMALLSTEPBACK("smallstepback"), FASTFORWARD("fastforward"), REWIND("rewind"), PLAY("play"),
  PLAYPAUSE("playpause"), SWITCHPLAYER("switchplayer"), DELETE("delete"), COPY("copy"), MOVE("move"), SCREENSHOT("screenshot"), RENAME("rename"),
  TOGGLEWATCHED("togglewatched"), SCANITEM("scanitem"), RELOADKEYMAPS("reloadkeymaps"), VOLUMEUP("volumeup"), VOLUMEDOWN("volumedown"), MUTE("mute"),
  BACKSPACE("backspace"), SCROLLUP("scrollup"), SCROLLDOWN("scrolldown"), ANALOGFASTFORWARD("analogfastforward"), ANALOGREWIND("analogrewind"), MOVEITEMUP("moveitemup"),
  MOVEITEMDOWN("moveitemdown"), CONTEXTMENU("contextmenu"), SHIFT("shift"), SYMBOLS("symbols"), CURSORLEFT("cursorleft"), CURSORRIGHT("cursorright"),
  SHOWTIME("showtime"), ANALOGSEEKFORWARD("analogseekforward"), ANALOGSEEKBACK("analogseekback"), SHOWPRESET("showpreset"), NEXTPRESET("nextpreset"),
  PREVIOUSPRESET("previouspreset"), LOCKPRESET("lockpreset"), RANDOMPRESET("randompreset"), INCREASEVISRATING("increasevisrating"),
  DECREASEVISRATING("decreasevisrating"), SHOWVIDEOMENU("showvideomenu"), ENTER("enter"), INCREASERATING("increaserating"),
  DECREASERATING("decreaserating"), SETRATING("setrating"), TOGGLEFULLSCREEN("togglefullscreen"),
  NEXTSCENE("nextscene"), PREVIOUSSCENE("previousscene"), NEXTLETTER("nextletter"), PREVLETTER("prevletter"),
  JUMPSMS2("jumpsms2"), JUMPSMS3("jumpsms3"), JUMPSMS4("jumpsms4"), JUMPSMS5("jumpsms5"), JUMPSMS6("jumpsms6"), JUMPSMS7("jumpsms7"),
  JUMPSMS8("jumpsms8"), JUMPSMS9("jumpsms9"), FILTER("filter"), FILTERCLEAR("filterclear"), FILTERSMS2("filtersms2"), FILTERSMS3("filtersms3"),
  FILTERSMS4("filtersms4"), FILTERSMS5("filtersms5"), FILTERSMS6("filtersms6"), FILTERSMS7("filtersms7"), FILTERSMS8("filtersms8"),
  FILTERSMS9("filtersms9"), FIRSTPAGE("firstpage"), LASTPAGE("lastpage"), GUIPROFILE("guiprofile"), RED("red"), GREEN("green"),
  YELLOW("yellow"), BLUE("blue"), INCREASEPAR("increasepar"), DECREASEPAR("decreasepar"), VOLAMPUP("volampup"),
  VOLAMPDOWN("volampdown"), VOLUMEAMPLIFICATION("volumeamplification"), CREATEBOOKMARK("createbookmark"),
  CREATEEPISODEBOOKMARK("createepisodebookmark"), SETTINGSRESET("settingsreset"),
  SETTINGSLEVELCHANGE("settingslevelchange"), STEREOMODE("stereomode"),
  NEXTSTEREOMODE("nextstereomode"), PREVIOUSSTEREOMODE("previousstereomode"), TOGGLESTEREOMODE("togglestereomode"),
  STEREOMODETOMONO("stereomodetomono"), CHANNELUP("channelup"), CHANNELDOWN("channeldown"), PREVIOUSCHANNELGROUP("previouschannelgroup"),
  NEXTCHANNELGROUP("nextchannelgroup"), PLAYPVR("playpvr"), PLAYPVRTV("playpvrtv"), PLAYPVRRADIO("playpvrradio"),
  RECORD("record"), TOGGLECOMMSKIP("togglecommskip"), SHOWTIMERRULE("showtimerrule"), LEFTCLICK("leftclick"),
  RIGHTCLICK("rightclick"), MIDDLECLICK("middleclick"), DOUBLECLICK("doubleclick"), LONGCLICK("longclick"), WHEELUP("wheelup"),
  WHEELDOWN("wheeldown"), MOUSEDRAG("mousedrag"), MOUSEMOVE("mousemove"), TAP("tap"), LONGPRESS("longpress"), PANGESTURE("pangesture"),
  ZOOMGESTURE("zoomgesture"), ROTATEGESTURE("rotategesture"), SWIPELEFT("swipeleft"), SWIPERIGHT("swiperight"), SWIPEUP("swipeup"),
  SWIPEDOWN("swipedown"), ERROR("error"), NOOP("noop");

  private final String value;

  public String getAction(){
    return this.value;
  }
  
  private ExecuteAction(final String value) {
    this.value = value;
  }

}
