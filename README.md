# SReplay

[中文文档](README_zh.md)

Server-side recording mod for [ReplayMod](https://github.com/ReplayMod/ReplayMod). It records the game using fake players so can be used on servers for 24/7 recording. It's also an alternative way to record single player world.

Tested in Minecraft 1.14.4, should also work in later versions with only trivial modifications on the code.

## Use
* `/sreplay player <player name> spawn [<recording file name>]` Spawn a recording bot and start recording. This is similiar to a client side player recording session. If no file name specified, file name will be based on current time and date;
* `/sreplay player <player name> kill` Kick a recording bot and save recording files (`.mcpr`);
* `/sreplay player <player name> tp` Teleport a bot to your position;
* `/sreplay player <player name> respawn [<recording file name>]` Stop the current recording session of the specified bot, save recording file, and start a new recording session on the same bot;
* `/sreplay player <player name> set sizeLimit <size limit>` Set recording file size limit for the specified bot, in MB. 
* `/sreplay player <player name> set autoRestart <auto restart>` Set auto restart flag. When size limit exceeds a new recording session will be started if this flag is on.
* `/sreplay list` List all saved replay file;
* `/sreplay delete <recording file name>` Delete a recording file. Needs confirming using `/sreplay confirm <code>`;
* `/sreplay reload` Reload configuration.

## Configuration
Cnofiguration file is `config/sreplay.json`.

* `savePath` Recording save path;
* `serverName` Server name stored in Replay file;
* `sizeLimit` Default recording size limit for each recording bot, in MB;
* `autoReconnect` Default auto restart flag;
* `playerNamePattern` Pattern that every bot name must match;
* `formats` Message formats, each format entry is written in Minecraft primitive JSON. See the configuration file for more detail.