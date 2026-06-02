# DeepSeek API 宝塔服务器环境变量配置

后端通过 Spring 配置读取进程环境变量，不需要把密钥写进代码或提交到仓库。

## 必需变量

```bash
export DEEPSEEK_API_KEY="sk-你的DeepSeek密钥"
```

## 可选变量

```bash
export DEEPSEEK_BASE_URL="https://api.deepseek.com"
export DEEPSEEK_CHAT_COMPLETIONS_PATH="/chat/completions"
export DEEPSEEK_PLAN_MODEL="deepseek-v4-flash"
export DEEPSEEK_CHAT_MODEL="deepseek-v4-flash"
export DEEPSEEK_THINKING_ENABLED="false"
export DEEPSEEK_REASONING_EFFORT="high"
```

默认请求地址为：

```text
https://api.deepseek.com/chat/completions
```

如果服务商或网关要求旧路径，可以只覆盖路径：

```bash
export DEEPSEEK_CHAT_COMPLETIONS_PATH="/v1/chat/completions"
```

## 宝塔面板配置方式

### Java 项目管理器

1. 打开宝塔面板，进入 Java 项目管理器。
2. 找到 TravelMate 后端项目，进入项目设置。
3. 在环境变量或启动参数区域添加：

```text
DEEPSEEK_API_KEY=sk-你的DeepSeek密钥
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_CHAT_COMPLETIONS_PATH=/chat/completions
```

4. 保存后重启 Java 项目。

### Shell 启动脚本

如果你用脚本启动 JAR，可以在 `java -jar` 前加入：

```bash
#!/bin/bash
export DB_PASSWORD="你的MySQL密码"
export DEEPSEEK_API_KEY="sk-你的DeepSeek密钥"
export DEEPSEEK_BASE_URL="https://api.deepseek.com"
export DEEPSEEK_CHAT_COMPLETIONS_PATH="/chat/completions"

java -jar /www/wwwroot/travelmate/backend-0.0.1-SNAPSHOT.jar
```

### systemd 服务

如果宝塔最终生成或你手写 systemd 服务，可在 service 文件中加入：

```ini
[Service]
Environment="DB_PASSWORD=你的MySQL密码"
Environment="DEEPSEEK_API_KEY=sk-你的DeepSeek密钥"
Environment="DEEPSEEK_BASE_URL=https://api.deepseek.com"
Environment="DEEPSEEK_CHAT_COMPLETIONS_PATH=/chat/completions"
ExecStart=/usr/bin/java -jar /www/wwwroot/travelmate/backend-0.0.1-SNAPSHOT.jar
```

修改后执行：

```bash
sudo systemctl daemon-reload
sudo systemctl restart travelmate
```

## 验证

重启后查看后端日志：

```bash
journalctl -u travelmate -n 100 --no-pager
```

如果没有配置密钥，AI 行程、客服和游记审核会继续走原来的本地降级逻辑；如果密钥错误或 DeepSeek 请求失败，也会保留原来的降级处理或错误声明。
