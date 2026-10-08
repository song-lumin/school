#!/bin/bash
# CodeArts 流水线「镜像构建 + 部署发布」阶段执行脚本
# 对应技术设计方案 10.3 节流水线最后两个阶段：
#   docker-build: docker build -t lost-found-backend:${BUILD_NUMBER} ./backend 等
#   deploy:       docker-compose down && docker-compose up -d
#
# 使用方式（二选一）：
#   A. 在 CodeArts「部署」应用中作为 Shell 脚本步骤执行（推荐，主机部署）
#   B. 在流水线「执行 Shell」插件中直接调用
#
# 环境变量（由流水线注入）：
#   BUILD_NUMBER   - 流水线构建编号
#   DOCKER_REPO    - 制品仓库地址，如 swr.cn-south-1.myhuaweicloud.com/<组织名>
#   DEPLOY_DIR     - 部署机上的项目目录，默认 /opt/school-lost-found

set -e

BUILD_NUMBER="${BUILD_NUMBER:-latest}"
DOCKER_REPO="${DOCKER_REPO:-swr.cn-south-1.myhuaweicloud.com/lost-found}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/school-lost-found}"

echo "=== [1/4] 镜像构建 ==="
docker build -t lost-found-backend:${BUILD_NUMBER} ./backend
docker build -t lost-found-frontend:${BUILD_NUMBER} ./frontend
# 打标签并推送到华为云 SWR 制品仓库
docker tag lost-found-backend:${BUILD_NUMBER}  ${DOCKER_REPO}/backend:${BUILD_NUMBER}
docker tag lost-found-frontend:${BUILD_NUMBER} ${DOCKER_REPO}/frontend:${BUILD_NUMBER}
docker push ${DOCKER_REPO}/backend:${BUILD_NUMBER}
docker push ${DOCKER_REPO}/frontend:${BUILD_NUMBER}

echo "=== [2/4] 下发部署目录 ==="
mkdir -p ${DEPLOY_DIR}
cp -r docker/docker-compose.yml ${DEPLOY_DIR}/
cp -r sql ${DEPLOY_DIR}/

echo "=== [3/4] 拉取最新镜像 ==="
cd ${DEPLOY_DIR}
docker-compose pull || true

echo "=== [4/4] 重启服务（技术方案 10.3 deploy 阶段：down + up -d） ==="
docker-compose down
docker-compose up -d
docker-compose ps

echo "=== 部署完成：前端 http://<主机IP>:80  后端 http://<主机IP>:8080 ==="
