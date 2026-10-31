"""生成依赖清单：让目标服务器自己从 Maven 镜像下载依赖 jar，而不是从开发机上传 130MB。

用法：python -I make_deps_manifest.py <lib 目录> <本地 Maven 仓库> <输出清单> <本地 jar 输出目录>
清单每行：文件名<TAB>下载地址<TAB>sha256。
在本地仓库找不到、或属于本项目自身模块（com/platform/cloud）的 jar 复制到本地 jar 输出目录，随应用包一起上传。
"""
import hashlib
import os
import shutil
import sys

MIRROR = "https://maven.aliyun.com/repository/public/"
PROJECT_GROUP = "com/platform/cloud/"

lib_dir, repo, manifest, local_dir = sys.argv[1:5]

# 本地仓库按文件名建索引；同名文件（不同 groupId）极少见，遇到时以字节内容比对确定
index: dict[str, list[str]] = {}
for root, _, files in os.walk(repo):
    for f in files:
        if f.endswith(".jar"):
            index.setdefault(f, []).append(os.path.join(root, f))


def sha256(path: str) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as fp:
        for chunk in iter(lambda: fp.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


os.makedirs(local_dir, exist_ok=True)
lines, local = [], 0
for name in sorted(os.listdir(lib_dir)):
    path = os.path.join(lib_dir, name)
    digest = sha256(path)
    match = next((c for c in index.get(name, []) if sha256(c) == digest), None)
    rel = os.path.relpath(match, repo).replace(os.sep, "/") if match else None
    if rel is None or rel.startswith(PROJECT_GROUP):
        shutil.copy2(path, os.path.join(local_dir, name))
        local += 1
    else:
        lines.append(f"{name}\t{MIRROR}{rel}\t{digest}")

with open(manifest, "w", encoding="utf-8", newline="\n") as fp:
    fp.write("\n".join(lines) + "\n")
print(f"镜像下载 {len(lines)} 个，随包上传 {local} 个")
