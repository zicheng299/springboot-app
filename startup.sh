#!/usr/bin/env bash
set -e

echo ""
echo "  ========================================"
echo "    Dropnote - 文件上传中心"
echo "  ========================================"
echo ""

# ---- Check Java ----
if ! command -v java &>/dev/null; then
    echo "  [错误] 未找到 Java，请安装 JDK 17+"
    exit 1
fi

echo "  Java: $(java -version 2>&1 | head -1)"
echo ""

# ---- Build & Run ----
echo "  [1/2] 正在编译项目..."
./gradlew compileKotlin --quiet

echo "  [2/2] 正在启动服务 (端口 28080)..."
echo ""

if command -v open &>/dev/null; then
    open http://localhost:28080
elif command -v xdg-open &>/dev/null; then
    xdg-open http://localhost:28080
fi

./gradlew bootRun
