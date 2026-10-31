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

# 1. 应用新发布包：先停服务端（运行中的 jar 在 Windows 上被占用无法覆盖），再解压依赖与应用
$incoming = "C:\mall\incoming"
$deps = Get-ChildItem $incoming -Filter "mall-deps-*.tgz" -ErrorAction SilentlyContinue | Select-Object -First 1
$manifest = Get-ChildItem $incoming -Filter "deps-*.txt" -ErrorAction SilentlyContinue | Select-Object -First 1
$release = Join-Path $incoming "mall-release.tgz"

# 按清单从 Maven 镜像下载依赖到 lib.new：已有且 sha256 一致的直接复用，全部校验通过后才替换 lib，
# 下载失败不影响正在运行的旧版本。curl.exe --noproxy 绕过本机系统代理直连国内镜像。
if ($manifest) {
    $id = $manifest.BaseName -replace '^deps-', ''
    Remove-Item -Recurse -Force app\lib.new -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force app\lib.new | Out-Null
    $lines = Get-Content $manifest.FullName -Encoding UTF8 | Where-Object { $_.Trim() }
    $reused = 0; $downloaded = 0
    foreach ($line in $lines) {
        $name, $url, $sha = $line -split "`t"
        $target = "app\lib.new\$name"
        $old = "app\lib\$name"
        if ((Test-Path $old) -and ((Get-FileHash $old -Algorithm SHA256).Hash -ieq $sha)) {
            Copy-Item $old $target; $reused++; continue
        }
        curl.exe -s -f -L --noproxy '*' --retry 3 --connect-timeout 15 -o $target $url
        if ($LASTEXITCODE -ne 0 -or (Get-FileHash $target -Algorithm SHA256).Hash -ine $sha) {
            throw "依赖下载或校验失败：$name（可改用 deploy.sh --upload-deps 直接上传依赖）"
        }
        $downloaded++
    }
    New-Item -ItemType File -Force "app\lib.new\.deps-$id" | Out-Null
    Write-Output "依赖已就绪：复用 $reused 个，下载 $downloaded 个"
}

if ($deps -or $manifest -or (Test-Path $release)) {
    if (Test-Path docker-compose.yml) { docker compose stop server 2>&1 | ForEach-Object { "$_" } }
}
if ($deps) {
    $id = $deps.BaseName -replace '^mall-deps-', ''
    Remove-Item -Recurse -Force app\lib -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force app\lib | Out-Null
    tar -xzf $deps.FullName -C app\lib
    if ($LASTEXITCODE -ne 0) { throw "依赖包解压失败" }
    New-Item -ItemType File -Force "app\lib\.deps-$id" | Out-Null
    Remove-Item $deps.FullName
    Write-Output "已更新依赖（$id）"
}
if ($manifest) {
    Remove-Item -Recurse -Force app\lib -ErrorAction SilentlyContinue
    Rename-Item app\lib.new lib
    Remove-Item $manifest.FullName
}
if (Test-Path $release) {
    Remove-Item -Force app\mall-server.jar -ErrorAction SilentlyContinue
    tar -xzf $release -C C:\mall
    if ($LASTEXITCODE -ne 0) { throw "发布包解压失败" }
    Remove-Item $release
    Write-Output "已更新应用"
}
# 本项目模块 jar 每次随应用包上传，覆盖到 lib
if (Test-Path app\lib-local) { Copy-Item -Force app\lib-local\* app\lib\ }
if (-not (Test-Path app\lib)) { throw "缺少依赖目录 app\lib，请重新执行 deploy.sh 上传依赖包" }

# 2. 首次部署生成随机密钥
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
# jar 是挂载进去的，compose 认为配置没变不会重启，这里显式重启以加载新版本与新的 Nginx 配置
docker compose restart server nginx 2>&1 | ForEach-Object { "$_" }

$deadline = (Get-Date).AddMinutes(4)
while ((Get-Date) -lt $deadline) {
    $state = docker inspect -f "{{.State.Health.Status}}" mall-server-1 2>$null
    if ($state -eq "healthy") { Write-Output "服务端已就绪"; exit 0 }
    Start-Sleep 5
}
docker compose logs --tail 80 server
throw "服务端 4 分钟内未就绪"
