# SReplay
ReplayMod服务端录制mod，通过使用类似于carpet的假人来录制。单机同样可用。

* 在玩家列表栏里面显示假人已录制的时长、文件大小等；
* 可以设置成当附近没有玩家时暂停录像；
* 允许玩家通过一个内置的http服务器下载录像文件；
* 在不改变玩家区块加载范围的情况下更改录像视距；
* 可锁定时间/天气。

目前支持1.14.4，对于新版本应该只需要修改gradle文件中的Minecraft版本和yarn mapping版本。

## 使用
使用`/sreplay help`来获取一般用法帮助，`/sreplay help set <选项名>`来获取不同录像选项的说明。

## Configuration
配置文件在`config/sreplay.json`，每次启动时如果文件不存在就会被创建。

* `savePath` 录像文件的保存路径;
* `serverName` 录像文件中的服务器名;
* `sizeLimit` 默认录像文件大小上限，单位：MB;
* `autoReconnect` 默认自动续录标志;
* `playerNamePattern` 假人玩家名需要符合的正则表达式，试图召唤玩家名不符的假人时将会出现`非法玩家名`的错误。（提示：如果不想有此限制可以将该项设置成`.*`）;
* `formats` 各种消息的格式，每个格式都是**原始JSON文本**。具体的格式配置请查看配置文件。
### 内置http服务器配置
* `serverListenAddress` 服务器监听地址。大多数时候可以设置成`0.0.0.0`.
* `serverPort` 服务器监听的端口。
* `serverHostName` 下载链接中显示的服务器域名。
* `downloadTimeout` 下载链接失效时限，单位是毫秒。