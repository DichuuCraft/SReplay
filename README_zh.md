# SReplay
ReplayMod服务端录制mod，通过使用类似于carpet的假人来录制。单机同样可用。

* 在玩家列表栏里面显示假人已录制的时长、文件大小等；
* 可以设置成当附近没有玩家时暂停录像；
* 允许玩家通过一个内置的http服务器下载录像文件。

目前支持1.14.4，对于新版本应该只需要修改gradle文件中的Minecraft版本和yarn mapping版本。

## 使用
* `/sreplay player <玩家名> spawn [<录像文件名>]` 召唤一个录像假人并开始录制，此时假人相当于一个使用ReplayMod录制的真玩家。如果文件名未指定那么文件名将会根据当前日期和时间生成;
* `/sreplay player <玩家名> kill` 删除一个假人并停止录制，保存录像文件;
* `/sreplay player <玩家名> tp` 将一个假人传送至你的位置;
* `/sreplay player <玩家名> respawn [<录像文件名>]` 停止并保存当前的录制，然后在原地开始新一轮的录制;
* `/sreplay player <玩家名> set sizeLimit <文件上限大小>` 设置指定假人的录像文件大小上限，单位是MB。`-1`为无上限;
* `/sreplay player <玩家名> set timeLimit <时间上限>` 设置指定假人的录像时间上限，单位是秒。`-1`为无上限;
* `/sreplay player <玩家名> set autoRestart <auto restart>` 设置自动续录标志。如果该标志为`true`那么当文件或时间上限超过之后就会自动停止录制并重新开始新的录制;
* `/sreplay player <玩家名> set autoPause <auto pause>` 设置自动暂停标志。如果为`true`那么当周围没有玩家的时候就会自动暂停，并在有玩家之后继续录制；
* `/sreplay player <玩家名> pause|resume` 暂停/继续录制；
* `/sreplay player <玩家名> marker <标记名>` 添加一个标记；
* `/sreplay list` 列表所有已保存的录像文件;
* `/sreplay delete <录像文件名>` 删除指定的录像文件，需要用`/sreplay confirm <确认码>`确认;
* `/sreplay reload` 重新加载配置文件。
* `/sreplay get <录像文件名>` 生成一个用于下载给定录像文件的临时URL。当第一个请求或者超时之后这个链接就会自动失效。
* `/sreplay server start|stop` 启动/停止用于下载录像文件的http服务器。

## Configuration
配置文件在`config/sreplay.json`，每次启动时如果文件不存在就会被创建。

* `savePath` 录像文件的保存路径;
* `serverName` 录像文件中的服务器名;
* `sizeLimit` 默认录像文件大小上限，单位：MB;
* `autoReconnect` 默认自动续录标志;
* `playerNamePattern` 假人玩家名需要符合的正则表达式，试图召唤玩家名不符的假人时将会出现`非法玩家名`的错误。（提示：如果不想有此限制可以将该项设置成`.*`）;
* `formats` 各种消息的格式，每个格式都是**原始JSON文本**。具体的格式配置请查看配置文件。