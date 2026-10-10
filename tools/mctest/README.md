# tools/mctest —— 服务器 + bot 多版本测试层

原有 JVM 单测**原样保留**；这层是**另加**的"真机"验证：起一个无头 Minecraft 服务端，
让 mineflayer 当"真玩家"连进去，跑自检数据包（`ttellraw/need/autocheck/`），
回收结果按版本对比，考"程序该怎么做、有没有做对"。

## 本地跑
```bash
cd tools/mctest
npm i mineflayer@4.37.1                 # 或设 MF_PATH 指向已有的 mineflayer
python3 download_servers.py 1.20.5 1.21.5
for v in 1.20.5 1.21.5; do
  python3 run_one.py "$v" --java /path/to/java21/bin/java
done
```
- server.jar 下载到 `tools/mctest/servers/`（已 gitignore）。
- 运行目录在 `tools/mctest/work/`（已 gitignore）。
- `--servers/--work/--pack/--java/--mf/--name` 均可覆盖；也支持环境变量 `MC_SERVERS/MCTEST_WORK/MCTEST_PACK/MCTEST_JAVA/MF_PATH`。

## 版本与 JDK
服务端按版本要求不同 JDK：`1.20.4` 用 17；`1.20.5`–`1.21.x` 用 21；更新版本可能要求更高。CI 默认用 JDK 21 跑 `1.20.5 / 1.21.5`。

## CI
`.github/workflows/mctest.yml`（**手动触发** workflow_dispatch，默认 `1.20.5 1.21.5`），避免每个 PR 都下服务端 jar。

## 说明
- 本层**不改任何 app 代码**，只把它当"真机"跑。
- 目前跑的是 `autocheck` 自检包；后续会扩展成"**用程序生成的命令**在各版本上验证"。

## 用例驱动模式（C 第 2 步的接入口）
`run_cases.py` 读一个 cases.json，把每条命令在指定版本上真机跑，并断言 bot 收到的聊天：
```bash
python3 run_cases.py 1.21.5 --cases cases.example.json --java /path/to/java21/bin/java
```
用例格式见 `cases.example.json`：`{name, edition, java, bedrock, expectChat}`。
其中 `java`/`bedrock` 通常就是**程序生成**的双版本命令；后续加一个生成器把 app 的输出写进 cases.json，即可实现"用程序生成的命令在各版本验证"。
退出码：有失败则非 0（可直接当 CI 断言）。
