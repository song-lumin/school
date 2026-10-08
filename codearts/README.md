# 华为 CodeArts 落地配置指南

> 对应《校园失物招领及诚信积分系统-技术设计方案》
> - 1.2 技术目标：基于华为 CodeArts 平台完成 Scrum 敏捷开发（两轮迭代）
> - 8. Sprint迭代规划（Sprint1 18任务 / Sprint2 10任务）
> - 10.3 华为 CodeArts DevOps 流水线（五阶段）
> - 12.2 代码交付物：CodeArts 流水线配置

---

## 0. 本仓库已就绪的 CodeArts 配置

| 文件 | 用途 | 对应技术方案章节 |
|------|------|-----------------|
| `codearts/pipeline.yml` | 流水线编排（五阶段） | 10.3 |
| `codearts/build-backend.yml` | 后端编译构建任务 | 10.3 build-backend |
| `codearts/build-frontend.yml` | 前端编译构建任务 | 10.3 build-frontend |
| `codearts/deploy.sh` | 镜像构建+部署发布脚本 | 10.3 docker-build / deploy |
| `codearts/workitems.csv` | Scrum 工作项导入（2个迭代+28任务） | 8.1 / 8.2 |

---

## 1. 创建项目（Scrum 工作项）

1. 登录 [华为云 CodeArts 控制台](https://www.huaweicloud.com/product/codearts.html)，区域选 **华南-广州（cn-south-1）**（与技术方案 10.3 的 codehub 地址一致）
2. 「项目列表 → 新建项目 → Scrum」，项目名：`校园失物招领及诚信积分系统`

## 2. 导入 Scrum 工作项（两轮迭代）

1. 进入项目 → 「工作项 → 迭代」：先手动新建迭代 `Sprint1-MVP核心闭环`、`Sprint2-智能匹配与增强`
2. 「工作项 → 批量导入 → 下载模板」，将 `codearts/workitems.csv` 中的行按模板列名填入后上传
   - CSV 已含 2 个迭代标题行 + Sprint1 的 18 个任务 + Sprint2 的 10 个任务（ID、描述、优先级均取自技术方案 8.1/8.2）
3. 导入后在「迭代看板」检查两个迭代的任务归属是否正确

## 3. 代码托管（CodeHub）

1. CodeArts → 「代码托管（CodeArts Repo）」→ 新建仓库：`school-lost-found`（**不要**初始化 README）
2. 在本地项目根目录执行推送：

```powershell
git remote add origin https://codehub-cn-south-1.devcloud.huaweicloud.com/<组织名>/school-lost-found.git
git push -u origin master
```

> 首次推送会要求华为云 HTTPS 凭证（用户名 + CodeHub 密码），在 CodeArts 「设置 → HTTPS 密码」中设置。
> 推送前请先提交当前未提交的变更（`git status` 查看）。

## 4. 编译构建任务

### 4.1 后端 backend-build
「编译构建 CodeArts Build」→ 新建任务 → 代码源选 `school-lost-found/master` → 构建环境选 **Maven3.9 + JDK17** → 切换「YAML 化构建」，粘贴 `codearts/build-backend.yml` 内容

### 4.2 前端 frontend-build
同上，构建环境选 **Node.js 18**，粘贴 `codearts/build-frontend.yml`

## 5. 流水线（五阶段）

「流水线 CodeArts Pipeline」→ 新建流水线 → 名称 `lost-found-pipeline` → 关联代码仓库与分支 `master` → 编辑阶段：

| 阶段 | 技术方案名称 | 插件 | 配置 |
|------|-------------|------|------|
| 1 | 代码检出 | 检出代码 | 默认（关联仓库 master） |
| 2 | 编译构建 | BuildExecute ×2 | 分别挂 backend-build / frontend-build |
| 3 | 单元测试 | Execute Maven | `cd backend && mvn test`，测试报告路径 `backend/target/surefire-reports/**` |
| 4 | 镜像构建 | 制作镜像并推送到SWR仓库 ×2 | Dockerfile：`backend/Dockerfile`、`frontend/Dockerfile`，标签 `${BUILD_NUMBER}` |
| 5 | 部署发布 | 执行Shell（部署） | 执行 `codearts/deploy.sh` |

也可直接在流水线详情「编辑(YAML)」粘贴 `codearts/pipeline.yml`。

> 阶段3 目标：核心 Service 层覆盖率 ≥50%（Sprint1）→ ≥70%（Sprint2），与技术方案 9.2 一致。

## 6. 部署准备

1. 「部署 CodeArts Deploy」→ 主机集群：新建 `lost-found-hosts`，添加目标主机（Linux，安装 agent）
2. 目标主机需预装 Docker 与 Docker Compose，并已 `docker login` 到 SWR
3. 「制品仓库 SWR」→ 创建组织 `lost-found`
4. `deploy.sh` 默认变量（可在流水线环境变量覆盖）：
   - `DOCKER_REPO`：`swr.cn-south-1.myhuaweicloud.com/lost-found`
   - `DEPLOY_DIR`：`/opt/school-lost-found`
5. 数据库初始化：compose 已挂载 `../sql` 到 `/docker-entrypoint-initdb.d`，首次启动自动执行

## 7. 代码检查（可选增强）

「代码检查 CodeArts Check」→ 新建任务 → 关联仓库 → 任务模板选「通用检查」→ 检查项含圈复杂度、重复代码、安全漏洞。可在流水线阶段 3 之后追加「代码检查」阶段。

## 8. 交付验收对照（技术方案 12.2）

- [x] 前端代码（Vue3 项目）
- [x] 后端代码（SpringBoot3 项目）
- [x] 数据库建表 SQL（`sql/`）
- [x] Docker 部署配置（`docker/docker-compose.yml` + 前后端 Dockerfile）
- [x] **CodeArts 流水线配置（`codearts/` 本目录 5 个文件）**
- [ ] 代码已推送到 CodeHub（执行第 3 节命令）
- [ ] 流水线在 CodeArts 控制台创建并跑通（第 4-5 节）

> ⚠️ 流水线任务的创建、主机集群授权必须在华为云控制台人工完成，仓库内 YAML 是其编排定义与操作依据。
