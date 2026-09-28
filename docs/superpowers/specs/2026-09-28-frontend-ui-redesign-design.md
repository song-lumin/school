# 前端 UI 改造设计:Pinterest 风格 + GSAP ImageTrail

日期:2026-09-28
状态:已批准(用户确认:核心页面优先 / 完整移植 GSAP 版 / 绿调+瀑布流)

## 背景与目标

「拾回」校园失物招领及诚信积分系统前端(Vue 3 + Element Plus + Vite)现有 15 个视图,整体为扁平表格风格。本次改造将核心页面升级为 Pinterest 风格的卡片化视觉语言,并把 reactbits.dev 的 ImageTrail 动画(React + GSAP 实现)移植为 Vue 组合式函数。

成功标准:
- 首页 Hero 区有流畅的 GSAP 图片拖尾动画(鼠标+触摸),支持变体切换
- 失物招领列表从 el-table 表格变为瀑布流卡片墙
- 详情页大图优先、导航栏毛玻璃化
- `vue-tsc && vite build` 通过;浏览器实测首页动画、瀑布流、响应式断点(760px/420px)
- 保留绿色主色 `#26745c` 与「拾回」品牌感;尊重 `prefers-reduced-motion`

## 范围

**改造(4 个视图 + 2 个基础设施文件):**
1. HomeView.vue — Hero 接入 GSAP ImageTrail;线索区改瀑布流
2. ItemListView.vue — el-table 改瀑布流卡片墙;筛选栏 pill 化
3. ItemDetailView.vue — 大图优先布局
4. LayoutView.vue — 毛玻璃导航栏
5. 新增 `src/composables/useImageTrail.ts` — GSAP 动画核心
6. 新增 `src/components/ImageTrail.vue` — Vue 包装组件
7. `src/styles/theme.css` — 新增圆角/阴影/间距设计令牌

**不动:** LoginView、RegisterView、ItemPublishView、ProfileView、ClaimListView、DisputeView、CertificateView、LostNoticeListView、LostNoticeDetailView、AdminView、PointAdminView、router、api、stores。

## 1. 动画基础设施

### 依赖

`gsap ^3.12.0`(运行时依赖,~70KB gzip)。全量引入 gsap 核心,不引入插件。

### useImageTrail.ts(组合式函数)

移植 reactbits ImageTrail 的核心机制:

- rAF 循环内计算指针移动距离(`Math.hypot`),超过阈值 80px 时触发 `showNextImage()`
- 指针位置支持鼠标与触摸(pointer events),坐标换算为容器相对坐标
- 图片在容器内绝对定位,初始 `scale: 1, opacity: 0`
- 每次激活:先 `gsap.killTweensOf(el)` 防冲突,再用 `gsap.timeline()` 驱动 transform/opacity/filter 动画,`onComplete` 后归零
- `onBeforeUnmount` 完整清理:取消 rAF、移除监听、kill 所有 tween

内置 3 种变体(精选自 reactbits 7 种):

| 变体 | 行为 | 默认 |
|------|------|------|
| V1 经典 | 淡入→漂向当前指针→缩小(scale .2)淡出 | ✓ |
| V3 飞散 | 图片 `yPercent: -200` 向上飞出,水平随机 ±30% 漂移 | |
| V5 旋转跟随 | 根据指针移动方向(顺/逆时针)旋转,沿移动方向漂移 | |

### ImageTrail.vue(包装组件)

- Props:`images: string[]`(必填)、`variant: 1 | 3 | 5`(默认 1)、`threshold?: number`(默认 80)
- 插槽兜底:`images` 为空时渲染默认 slot(让父组件放静态装饰)
- 图片兜底策略:父组件负责传入有效图片列表;若物品均无真实照片,父组件用分类色调 SVG data-URL 瓦片兜底(见 3.2),保证动画在数据少时可演示
- 无障碍:动画层 `aria-hidden="true"`;`prefers-reduced-motion: reduce` 时整个动画禁用(与现有 theme.css 全局降级规则一致)

## 2. 设计令牌(theme.css 增量)

保留现有绿色主色与全部 `--el-color-primary-*` 映射,新增:

```css
--lf-radius-sm: 8px;
--lf-radius: 12px;        /* 卡片 */
--lf-radius-lg: 16px;
--lf-shadow-1: 0 1px 2px rgb(33 64 48 / 6%);
--lf-shadow-2: 0 8px 24px rgb(33 64 48 / 10%);   /* hover 浮起 */
--lf-shadow-3: 0 16px 48px rgb(33 64 48 / 16%);  /* 弹层 */
--lf-space-1..6: 4/8/12/16/24/32px
```

现有组件覆盖(el-card、el-button 等)保持不动;新令牌只服务新样式,不回改旧页面,保证 11 个未改造页面零回归。

## 3. 首页 HomeView

### 3.1 Hero 区

- `intro-scene` 替换为 `<ImageTrail>` 组件:传入最新物品真实图片(过滤 example.com 占位图),不足 6 张时用 SVG 兜底瓦片补齐;变体切换按钮(经典/飞散/旋转)放在场景角标处
- 保留现有场景装饰(校园卡/钥匙/笔记本色块)与文案,删除旧的原生 CSS 拖尾代码(`onScenePointerMove`、`trailImages`、`trail-out` keyframes)
- Hero 左侧文案、搜索框、指标条结构不变,仅套用新圆角/阴影令牌

### 3.2 SVG 兜底瓦片

`src/utils/placeholder.ts` 新增 `categoryTile(category: string): string`:按分类色调(绿 #81a78e / 珊瑚 #d88770 / 蓝 #7899a5 / 黄 #c3a668,复用现有 categoryTone 逻辑)生成内联 SVG data-URL,上绘分类对应 Element Plus 图标路径。同时供 ImageTrail 兜底与瀑布流无图卡片使用。

### 3.3 线索瀑布流

- 「刚刚出现的线索」:3 等分 grid → 瀑布流。实现:`CSS columns` 方案(column-count: 3 + break-inside: avoid)。选 columns 而非 JS 分列:阅读顺序为纵向、与 Pinterest 视觉习惯一致,零 JS、天然响应式(column-count 随断点 3→2→1)
- 卡片:图片不定高(`aspect-ratio` 由图片自身决定,无图时用兜底瓦片固定 4:3)、圆角 `--lf-radius`、hover 浮起(`--lf-shadow-2` + translateY(-4px))
- 现有 3 种交替高度 class(found-card-1/2)删除

## 4. 失物招领列表 ItemListView

- `el-table` → 瀑布流卡片墙(同 3.3 的 columns 方案)
- 卡片内容:图片(兜底瓦片)、标题、分类 tag、拾取地点、状态 tag(复用 ITEM_STATUS_MAP 配色)
- 筛选栏:保留 el-form + el-input/el-select 功能,重塑为单行 pill 风格(去掉 el-card 包裹,改为白色圆角条 + 阴影 `--lf-shadow-1`)
- 分页:保留 el-pagination,移到卡片墙下方居中

## 5. 物品详情 ItemDetailView

- 大图优先:左侧主图区(圆角 `--lf-radius-lg`,占位时用兜底瓦片),右侧信息栏(标题、状态、拾取时间地点、投放点、认领按钮)
- 现有信息行/认领流程逻辑不变,只重排模板与样式

## 6. 导航栏 LayoutView

- `layout-header` 背景 → `backdrop-filter: blur(12px)` + 半透明白 `rgb(255 255 255 / 78%)`
- logo 图标升级为圆角容器 + 品牌绿渐变;下拉菜单加圆角与阴影令牌
- 菜单项/下拉交互逻辑不动

## 错误处理

- 图片加载失败:el-image `#error` 插槽 → 兜底瓦片(现有逻辑泛化)
- 物品数据为空:瀑布流区显示现有 el-empty
- GSAP 加载失败或 rAF 异常:try/catch 包裹动画启动,失败时静默降级为无动画(图片直接隐藏,不阻塞页面)

## 测试与验证

本项目无单测框架,验证方式:

1. `vue-tsc && vite build` 零错误
2. dev server 浏览器实测:
   - 首页:拖尾动画三种变体切换、触摸屏模拟、无图数据下的 SVG 兜底
   - 列表:瀑布流 3/2/1 列断点、筛选、分页、卡片跳转
   - 详情:大图布局、认领入口
   - 导航:毛玻璃效果、登录/未登录两种状态
3. 回归:未改造页面(登录/注册等)抽查样式无异常
