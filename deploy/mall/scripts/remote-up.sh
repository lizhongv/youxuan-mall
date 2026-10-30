#!/usr/bin/env bash
# Linux 主机上启动 / 更新商城（由 deploy.sh 调用，也可手动执行）
set -euo pipefail
cd /opt/mall

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
