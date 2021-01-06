# SReplay

[中文文档](README_zh.md)

Server-side recording mod for [ReplayMod](https://github.com/ReplayMod/ReplayMod). It records the game using fake players so can be used on servers for 24/7 recording. It's also an alternative way to record single player world.

* Display recorded time, recorded file size, etc. in tab list.
* Can be set to pause automatically when no players are near by.
* Allow players to download recording files via an embedded http server.
* Variable view distance, but not chunk loading distance.
* Lock day time or weather in recording.

Tested in Minecraft 1.14.4, should also work in later versions with only trivial modifications on the code (minecraft version and yarn mapping version of fabric-loom).

## Use
Use `/sreplay help` to get general usage, and `/sreplay help set <option name>` to get help with the specified recording option.

## Configuration
Cnofiguration file is `config/sreplay.json`, it will be created each time start the mod if not exist.

* `savePath` Recording save path;
* `serverName` Server name stored in Replay file;
* `sizeLimit` Default recording size limit for each recording bot, in MB;
* `autoReconnect` Default auto restart flag;
* `playerNamePattern` Pattern that every bot name must match, attempting to spawn a bot with invalid name would result in an `invalidName` error. (Hint: change this to `.*` if you want any player name to be valid);
* `formats` Message formats, each format entry is **raw JSON text**. See the configuration file for more detail.
### Configurations for embeded http server
* `serverListenAddress` Server listen address. `0.0.0.0` would be fine most of the time.
* `serverPort` Server port.
* `serverHostName` Server domain name in the downloading Links.
* `downloadTimeout` Link expiring time out, in milliseconds.