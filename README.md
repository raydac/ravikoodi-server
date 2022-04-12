![BANNER](assets/github-social-preview.png)

[![License Apache 2.0](https://img.shields.io/badge/license-Apache%20License%202.0-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Java 11+](https://img.shields.io/badge/java-11%2b-green.svg)](https://bell-sw.com/pages/downloads/#/java-11-lts)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-cyan.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)
[![YooMoney donation](https://img.shields.io/badge/donation-Yoo.money-blue.svg)](https://yoomoney.ru/to/41001158080699)

# Changelog

- __1.1.8 (12-apr-2022)__
  - added local file publishing through Tools->Published files
  - updated Spring Boot to 2.6.6
  - refactoring

- __1.1.7 (30-oct-2021)__
  - removed Fast-Robot screen grabber
  - added Youtube links processing (through Kodi Youtube plugin)
  - embedded JDK changed to BellSoft Liberica JDK 17.0.1+12
  - SSL certificate generator replaced by pre-generated certificate 
  - Launch4j starter replaced by self-written one
  - updated Spring Boot to 2.5.6
  - removed application bundle for MacOS

[Full changelog](changelog.txt)

# Introduction

At home I use [Raspberry PI 3](https://www.raspberrypi.org/products/raspberry-pi-3-model-b/) (with installed [KODI media center](https://kodi.tv/)) to watch TV-shows and clips. There are number of browser plug-ins to open web links directly on KODI, but I need broadcast some media-content directly from my laptop. KODI supports sharing through Samba etc. but I am too lazy and like one-button solutions. So I decided to write a small easy utility to provide needed one-button way.

# How to load and start?

## If you don't have installed Java or don't care about that

It is a Java application so that in ideal it needs pre-installed Java 1.8+, but since 1.1.0 release Iprovide also pre-built versions with embedded JDK images, they can be started without installed Java:
 - [version for Linux with JDK image](https://github.com/raydac/ravikoodi-server/releases/download/1.1.8/ravikoodi-app-1.1.8-linux-jdk-amd64.tar.gz)
 - [version for Windows with JDK image](https://github.com/raydac/ravikoodi-server/releases/download/1.1.8/ravikoodi-app-1.1.8-windows-jdk-amd64.zip)
 - [version for MacOS with JDK image](https://github.com/raydac/ravikoodi-server/releases/download/1.1.8/ravikoodi-app-1.1.8-macos-jdk-amd64.zip)
 - [version for MacOS (ARM64) with JDK image](https://github.com/raydac/ravikoodi-server/releases/download/1.1.8/ravikoodi-app-1.1.8-macos-jdk-aarch64.zip)

You can just load needed archive, unpack in a folder and start its executable file.

## Requirements

To be working well, the application requires:
 - max 100 Mb on hard-disk
 - more or less powerful computer (especially for high bitrate screencasting)
 - pre-installed [Java 11+](https://bell-sw.com/) for versions without embedded JDK image
 - pre-installed [FFmpeg](https://www.ffmpeg.org/) for screencasting

![screenshot with NIMBUS L&F](assets/screenshot.png)   

# Application

## Some technical details
It is a Spring Boot based application with embedded Jetty web server. For screencast it starts external FFmpeg application and communicate with it through loopback TCP ports. Start of application takes 5-10 seconds and just after start it eats about 300-700 Mb of RAM. For screencasting it makes screenshots so that be carefult in use on weak computers.   
![scheme](assets/architecture.png)

## How to build?
It is absolutely free and open-source application (under Apache 2.0 license), I don't ask for any fee for use of it (but you could make some donation and I would be very appreciate for that).
1. You need Maven to build project
2. Go to into project folder and call `mvn clean install` to get compiled JAR, it can be started separately through `java -jar <JAR_FILE>`
3. To get release versions, you should use `mvn clean install -Ppublish`, in the `target` folder you will find all prepared archives
4. To get SH version for Linux, you should use `mvn clean install -Ppublishsh`, in the `target` folder you will find SH version of the application

## Tune KODI
Select network settings ofyour KODI player (its appearance depends on version)   
![kodi http](assets/kodi_settings.png)   
Enable __Allow remote control via HTTP__, select address, port and access credentials.

## Tune application server

Open options panel `Tools -> Options`.   
![option panel](assets/optionspanel.png)
1. In __Web server__ select in __Host__ your computer's network interface (which is visible for KODI)
2. In __Web server__ select any free port on your computer in __Port__ (it is better use port number bigger than 1024)
3. In __Kodi__ enter network address of your KODI machine in __Address__ and KODI listening port in __Port__
4. If you use any password protection for access to your KODI, enter needed credentials into __Name__ and __Password__

After listed steps, your local video server is prepared for broadcasting to KODI.

# Play content
1. Press __File tree__ icon in the button tool bar and select folder contains media-contend through File dialog.   
![file tree button](assets/tool_folders.png)
2. Select needed content in the file tree   
![select media file](assets/tree_selected_content.png)
3. If you want open the selected media file in the default system player, then click twice the media file or press button   
![start in system](assets/tool_system_play.png)
4. Press button `Play selected item on KODI` to send the media file to internal server and start broadcasting to KODI.   
![start on KODI](assets/tool_play_on_kodi.png)
5. If you have tuned screencast options (and you have provided FFmpeg) then you can start screencast   
![start screencast](assets/tool_play_screencast.png)  
