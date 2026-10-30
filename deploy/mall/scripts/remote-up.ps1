# Windows 主机上启动 / 更新商城（由 deploy.sh 调用，也可手动执行）
# 首次运行生成随机密码写入 .env；把演示图片复制到上传目录；然后 docker compose up。
# docker 把进度写到 stderr，Windows PowerShell 5.1 会当成错误终止脚本，所以这里用 Continue 并检查退出码
$ErrorActionPreference = "Continue"
Set-Location C:\mall

function New-Secret([int]$len = 32) {
    $bytes = New-Object byte[] $len
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return ([Convert]::ToBase64String($bytes) -replace '[+/=]', 'x')
}

if (-not (Test-Path .env)) {
    $envText = Get-Content .env.example -Raw -Encoding UTF8
    $envText = $envText -replace 'MYSQL_ROOT_PASSWORD=.*', "MYSQL_ROOT_PASSWORD=$(New-Secret 18)"
    $envText = $envText -replace 'MYSQL_PASSWORD=change-me-mall', "MYSQL_PASSWORD=$(New-Secret 18)"
    $envText = $envText -replace 'REDIS_PASSWORD=.*', "REDIS_PASSWORD=$(New-Secret 18)"
    $envText = $envText -replace 'MALL_JWT_SECRET=.*', "MALL_JWT_SECRET=$(New-Secret 48)"
    [IO.File]::WriteAllText("C:\mall\.env", $envText, (New-Object Text.UTF8Encoding $false))
    Write-Output "已生成 .env（随机密码）"
}

New-Item -ItemType Directory -Force data\uploads, data\logs | Out-Null
if (Test-Path seed\uploads) { Copy-Item -Recurse -Force seed\uploads\* data\uploads\ }

docker compose up -d --remove-orphans 2>&1 | ForEach-Object { "$_" }
if ($LASTEXITCODE -ne 0) { throw "docker compose up 失败" }
# jar 是挂载进去的，compose 认为配置没变不会重启，这里显式重启以加载新版本
docker compose restart server nginx 2>&1 | ForEach-Object { "$_" }

$deadline = (Get-Date).AddMinutes(4)
while ((Get-Date) -lt $deadline) {
    $state = docker inspect -f "{{.State.Health.Status}}" mall-server-1 2>$null
    if ($state -eq "healthy") { Write-Output "服务端已就绪"; exit 0 }
    Start-Sleep 5
}
docker compose logs --tail 80 server
throw "服务端 4 分钟内未就绪"
