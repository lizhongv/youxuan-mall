#!/usr/bin/env bash
# 一键发布：构建 → 测试 → 打包 → 上传 → 启动 → 冒烟测试
# 用法：bash deploy/mall/deploy.sh <ssh 主机> [--skip-tests] [--skip-web]
#   目标主机需已安装 Docker（含 compose 插件）。Windows 主机安装到 C:\mall，Linux 主机安装到 /opt/mall。
#
# 为适应慢速或不稳定的网络（如经中继的 Tailscale），胖 jar 拆成两部分：
#   mall-release.tgz        应用 jar（约 1MB）、本项目模块 jar、前端、配置，每次上传；
#   deps-<指纹>.txt         第三方依赖清单，目标服务器自己从阿里云 Maven 镜像下载并校验 sha256，
#                           只有依赖变化（指纹不同）时才需要；加 --upload-deps 则改为直接上传依赖包（约 130MB）。
set -euo pipefail

HOST="${1:?用法: deploy.sh <ssh 主机> [--skip-tests] [--skip-web] [--upload-deps]}"
shift
SKIP_TESTS=false
SKIP_WEB=false
UPLOAD_DEPS=false
for arg in "$@"; do
  case "$arg" in
    --skip-tests) SKIP_TESTS=true ;;
    --skip-web) SKIP_WEB=true ;;
    --upload-deps) UPLOAD_DEPS=true ;;
    *) echo "未知参数: $arg" >&2; exit 1 ;;
  esac
done

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
BUILD="$ROOT/build"
OUT="$BUILD/mall-release"
# 办公网络常设置了境外代理，Maven 阿里云镜像与 npmmirror 直连更快
NOPROXY=(env -u HTTP_PROXY -u HTTPS_PROXY -u http_proxy -u https_proxy)

# 慢速链路上 scp 偶尔断开，重试几次再放弃
upload() {
  local src="$1" dst="$2"
  for i in 1 2 3 4 5; do
    if scp -q "$src" "$HOST:$dst"; then return 0; fi
    echo "    上传中断，第 $i 次重试…" >&2
    sleep 5
  done
  echo "上传失败：$src" >&2
  return 1
}

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
rm -rf "$OUT" "$BUILD/extract" && mkdir -p "$OUT/app" "$OUT/web"
# Spring Boot 工具模式把胖 jar 拆成「应用 jar + lib/ 依赖」，应用 jar 通过清单引用 lib/
java -Djarmode=tools -jar "$ROOT/mall-server/target/mall-server.jar" extract --destination "$BUILD/extract" >/dev/null
cp "$BUILD/extract/mall-server.jar" "$OUT/app/"
# 依赖清单：第三方 jar 由服务器从镜像下载；本项目模块（每次构建都会变）放进 app/lib-local 随应用包上传
python -I "$ROOT/deploy/mall/scripts/make_deps_manifest.py" "$BUILD/extract/lib" "$HOME/.m2/repository"   "$BUILD/deps.txt" "$OUT/app/lib-local"
# 指纹只取第三方依赖清单（文件名 + sha256），本项目模块变化不会触发重新下载
DEPS_ID="$(sha256sum "$BUILD/deps.txt" | cut -c1-12)"
DEPS_TXT="$BUILD/deps-$DEPS_ID.txt"
DEPS_TGZ="$BUILD/mall-deps-$DEPS_ID.tgz"
cp "$BUILD/deps.txt" "$DEPS_TXT"
$UPLOAD_DEPS && { [ -f "$DEPS_TGZ" ] || (cd "$BUILD/extract/lib" && tar -czf "$DEPS_TGZ" .); }
cp "$ROOT/deploy/mall/docker-compose.yml" "$ROOT/deploy/mall/.env.example" "$OUT/"
cp -r "$ROOT/deploy/mall/nginx" "$ROOT/deploy/mall/scripts" "$OUT/"
cp -r "$ROOT/deploy/mall/seed" "$OUT/seed"
cp -r "$ROOT/web/mall-admin/dist" "$OUT/web/console"
cp -r "$ROOT/web/mall-h5/dist" "$OUT/web/h5"
(cd "$OUT" && tar -czf ../mall-release.tgz .)
echo "    应用包 $(du -h "$BUILD/mall-release.tgz" | cut -f1)（依赖指纹 $DEPS_ID）"

echo "==> 上传并启动（$HOST）"
# Windows 的 OpenSSH 默认 shell 可能是 cmd 或 PowerShell，用 uname 判断 Linux 更可靠
if ! ssh "$HOST" "uname -s" 2>/dev/null | grep -qi linux; then
  BASE="C:/mall"
  ssh "$HOST" "powershell -NoProfile -Command \"New-Item -ItemType Directory -Force $BASE/incoming | Out-Null\""
  if ssh "$HOST" "powershell -NoProfile -Command \"Test-Path $BASE/app/lib/.deps-$DEPS_ID\"" | grep -qi true; then
    echo "    依赖未变化，跳过依赖包"
  else
    if $UPLOAD_DEPS; then
      echo "    上传依赖包…"
      upload "$DEPS_TGZ" "$BASE/incoming/mall-deps-$DEPS_ID.tgz"
    else
      echo "    上传依赖清单（服务器从镜像下载）…"
      upload "$DEPS_TXT" "$BASE/incoming/deps-$DEPS_ID.txt"
    fi
  fi
  upload "$BUILD/mall-release.tgz" "$BASE/incoming/mall-release.tgz"
  # 先只解出启动脚本，其余文件由脚本在停止服务端之后再解压（运行中的 jar 在 Windows 上被占用无法覆盖）
  ssh "$HOST" "powershell -NoProfile -ExecutionPolicy Bypass -Command \"tar -xzf $BASE/incoming/mall-release.tgz -C $BASE ./scripts;& $BASE/scripts/remote-up.ps1\""
else
  BASE="/opt/mall"
  ssh "$HOST" "mkdir -p $BASE/incoming"
  if ssh "$HOST" "test -f $BASE/app/lib/.deps-$DEPS_ID"; then
    echo "    依赖未变化，跳过依赖包"
  else
    if $UPLOAD_DEPS; then
      echo "    上传依赖包…"
      upload "$DEPS_TGZ" "$BASE/incoming/mall-deps-$DEPS_ID.tgz"
    else
      echo "    上传依赖清单（服务器从镜像下载）…"
      upload "$DEPS_TXT" "$BASE/incoming/deps-$DEPS_ID.txt"
    fi
  fi
  upload "$BUILD/mall-release.tgz" "$BASE/incoming/mall-release.tgz"
  ssh "$HOST" "tar -xzf $BASE/incoming/mall-release.tgz -C $BASE ./scripts && bash $BASE/scripts/remote-up.sh"
fi
BASE_HOST="$(ssh -G "$HOST" | awk '/^hostname /{print $2}')"

echo "==> 冒烟测试"
PORT="${HTTP_PORT:-8090}"
"${NOPROXY[@]}" python -I "$ROOT/scripts/smoke_test.py" "http://$BASE_HOST:$PORT"
echo "==> 完成：商城 http://$BASE_HOST:$PORT/  后台 http://$BASE_HOST:$PORT/console/"
