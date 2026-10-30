#!/usr/bin/env bash
# 一键发布：构建 → 测试 → 打包 → 上传 → 启动 → 冒烟测试
# 用法：bash deploy/mall/deploy.sh <ssh 主机> [--skip-tests] [--skip-web]
#   目标主机需已安装 Docker（含 compose 插件），Windows 主机解压到 C:\mall，Linux 主机解压到 /opt/mall。
set -euo pipefail

HOST="${1:?用法: deploy.sh <ssh 主机> [--skip-tests] [--skip-web]}"
shift
SKIP_TESTS=false
SKIP_WEB=false
for arg in "$@"; do
  case "$arg" in
    --skip-tests) SKIP_TESTS=true ;;
    --skip-web) SKIP_WEB=true ;;
    *) echo "未知参数: $arg" >&2; exit 1 ;;
  esac
done

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/build/mall-release"
# 办公网络常设置了境外代理，Maven 阿里云镜像与 npmmirror 直连更快
NOPROXY=(env -u HTTP_PROXY -u HTTPS_PROXY -u http_proxy -u https_proxy)

echo "==> 构建服务端"
MVN_ARGS=(-q -pl mall-server -am package)
$SKIP_TESTS && MVN_ARGS+=(-DskipTests)
(cd "$ROOT" && "${NOPROXY[@]}" ./mvnw "${MVN_ARGS[@]}")

if ! $SKIP_WEB; then
  for app in mall-admin mall-h5; do
    echo "==> 构建前端 $app"
    (cd "$ROOT/web/$app" && "${NOPROXY[@]}" npm ci --no-audit --no-fund && npm run build)
  done
fi

echo "==> 组装发布包"
rm -rf "$OUT" && mkdir -p "$OUT/app" "$OUT/web"
cp "$ROOT/deploy/mall/docker-compose.yml" "$ROOT/deploy/mall/.env.example" "$OUT/"
cp -r "$ROOT/deploy/mall/nginx" "$ROOT/deploy/mall/scripts" "$OUT/"
cp -r "$ROOT/deploy/mall/seed" "$OUT/seed"
cp "$ROOT/mall-server/target/mall-server.jar" "$OUT/app/"
cp -r "$ROOT/web/mall-admin/dist" "$OUT/web/console"
cp -r "$ROOT/web/mall-h5/dist" "$OUT/web/h5"
(cd "$OUT" && tar -czf ../mall-release.tgz .)

echo "==> 上传并启动（$HOST）"
if ssh "$HOST" "ver" >/dev/null 2>&1; then
  scp -q "$ROOT/build/mall-release.tgz" "$HOST:C:/mall/mall-release.tgz"
  ssh "$HOST" "powershell -NoProfile -Command \"New-Item -ItemType Directory -Force C:/mall | Out-Null; tar -xzf C:/mall/mall-release.tgz -C C:/mall; powershell -NoProfile -ExecutionPolicy Bypass -File C:/mall/scripts/remote-up.ps1\""
  BASE_HOST="$(ssh -G "$HOST" | awk '/^hostname /{print $2}')"
else
  ssh "$HOST" "mkdir -p /opt/mall"
  scp -q "$ROOT/build/mall-release.tgz" "$HOST:/opt/mall/mall-release.tgz"
  ssh "$HOST" "cd /opt/mall && tar -xzf mall-release.tgz && bash scripts/remote-up.sh"
  BASE_HOST="$(ssh -G "$HOST" | awk '/^hostname /{print $2}')"
fi

echo "==> 冒烟测试"
PORT="${HTTP_PORT:-8090}"
"${NOPROXY[@]}" python "$ROOT/scripts/smoke_test.py" "http://$BASE_HOST:$PORT"
echo "==> 完成：商城 http://$BASE_HOST:$PORT/  后台 http://$BASE_HOST:$PORT/console/"
