#!/usr/bin/env bash
# Linux 主机上启动 / 更新商城（由 deploy.sh 调用，也可手动执行）
set -euo pipefail
cd /opt/mall

# 1. 应用新发布包：先停服务端，再解压依赖与应用
deps="$(ls incoming/mall-deps-*.tgz 2>/dev/null | head -n1 || true)"
manifest="$(ls incoming/deps-*.txt 2>/dev/null | head -n1 || true)"

# 按清单从 Maven 镜像下载依赖到 lib.new：已有且 sha256 一致的直接复用，全部校验通过后才替换 lib
if [ -n "$manifest" ]; then
  mid="$(basename "$manifest" .txt)"; mid="${mid#deps-}"
  rm -rf app/lib.new && mkdir -p app/lib.new
  while IFS=$'	' read -r name url sha; do
    [ -n "$name" ] || continue
    if [ -f "app/lib/$name" ] && [ "$(sha256sum "app/lib/$name" | cut -d' ' -f1)" = "$sha" ]; then
      cp "app/lib/$name" "app/lib.new/$name"; continue
    fi
    curl -sfL --retry 3 --connect-timeout 15 -o "app/lib.new/$name" "$url"
    [ "$(sha256sum "app/lib.new/$name" | cut -d' ' -f1)" = "$sha" ] || { echo "依赖校验失败：$name" >&2; exit 1; }
  done < "$manifest"
  touch "app/lib.new/.deps-$mid"
  echo "依赖已就绪（$mid）"
fi
if [ -n "$deps" ] || [ -n "$manifest" ] || [ -f incoming/mall-release.tgz ]; then
  [ -f docker-compose.yml ] && docker compose stop server || true
fi
if [ -n "$deps" ]; then
  id="$(basename "$deps" .tgz)"; id="${id#mall-deps-}"
  rm -rf app/lib && mkdir -p app/lib
  tar -xzf "$deps" -C app/lib && touch "app/lib/.deps-$id" && rm -f "$deps"
  echo "已更新依赖（$id）"
fi
if [ -n "$manifest" ]; then
  rm -rf app/lib && mv app/lib.new app/lib && rm -f "$manifest"
fi
if [ -f incoming/mall-release.tgz ]; then
  tar -xzf incoming/mall-release.tgz -C /opt/mall && rm -f incoming/mall-release.tgz
  echo "已更新应用"
fi
[ -d app/lib-local ] && cp -f app/lib-local/* app/lib/
[ -d app/lib ] || { echo "缺少依赖目录 app/lib，请重新执行 deploy.sh 上传依赖包" >&2; exit 1; }

secret() { head -c "${1:-32}" /dev/urandom | base64 | tr -d '+/=\n' | head -c "${1:-32}"; }

if [ ! -f .env ]; then
  sed -e "s|^MYSQL_ROOT_PASSWORD=.*|MYSQL_ROOT_PASSWORD=$(secret 24)|" \
      -e "s|^MYSQL_PASSWORD=.*|MYSQL_PASSWORD=$(secret 24)|" \
      -e "s|^REDIS_PASSWORD=.*|REDIS_PASSWORD=$(secret 24)|" \
      -e "s|^MALL_JWT_SECRET=.*|MALL_JWT_SECRET=$(secret 48)|" .env.example > .env
  echo "已生成 .env（随机密码）"
fi

mkdir -p data/uploads data/logs
[ -d seed/uploads ] && cp -r seed/uploads/* data/uploads/

docker compose up -d --remove-orphans
docker compose restart server nginx

for _ in $(seq 1 48); do
  if [ "$(docker inspect -f '{{.State.Health.Status}}' mall-server-1 2>/dev/null)" = "healthy" ]; then
    echo "服务端已就绪"; exit 0
  fi
  sleep 5
done
docker compose logs --tail 80 server
echo "服务端 4 分钟内未就绪" >&2
exit 1
