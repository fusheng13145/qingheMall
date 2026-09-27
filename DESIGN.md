# DESIGN.md — 视觉风格、布局与交互设计规范

> 派生视图（权威源：[青禾商城手册.md](青禾商城手册.md) v8.0 §5.12/§9-D1）。适用前端：Vue 3.3 + Vite 6，`frontend/src/styles/` 为唯一样式源。

## 1. 设计语言总纲

- **设计气质**：清爽电商风——大留白、卡片化内容、圆角（`--radius-*`）与柔和阴影（`--shadow-*`）分层。
- **三端三色**：同一内核令牌 + 按角色覆盖的皮肤令牌（`data-skin` 驱动），禁止组件内硬编码角色色。
- **亮/暗双主题**：全部颜色经由语义令牌（`--color-*`）消费，暗色由主题 store 切换 `dark` 类驱动。

## 2. 三端皮肤令牌

| 端 | `data-skin` | 主色（--color-primary） | 辅助（light/dark） | 50 色 | 侧栏 chrome |
| --- | --- | --- | --- | --- | --- |
| 用户前台 | `user`（App.vue 根） | `#10b981` 翠绿 | `#34d399` / `#059669` | 翠绿系 | 白底 |
| 商家工作台 | `merchant`（MerchantLayout 根） | `#d97706` 琥珀 | `#f59e0b` / `#b45309` | `#fef3c7` 琥珀浅 | 琥珀暗 chrome |
| 管理后台 | `admin`（AdminLayout 根） | `#3b82f6` 钢蓝 | 钢蓝系 | 钢蓝浅 | 墨石暗侧栏 |

- 皮肤同时覆盖 `--skin-chrome-*`（侧栏背景/前景/边框）与向后兼容的 `--color-primary` 系。
- 新组件**只允许**消费 `var(--color-*)` / `var(--skin-*)` / `var(--radius-*)` / `var(--shadow-*)` / `var(--transition-*)`。

## 3. 布局体系

| 场景 | 布局 | 说明 |
| --- | --- | --- |
| 用户前台 | AppHeader（sticky）+ 路由视图 + AppFooter | 首页 Hero + 特性四宫格 + 运营位轮播（5s 自动切换、悬停暂停、指示点）+ 推荐/新品双区块 |
| 控制台（admin/merchant） | `ConsoleSidebar`（240px 固定侧栏）+ 顶栏 + 内容区 | ≤768px 汉堡展开：遮罩 + 侧栏滑入（`.open`），导航项点击自动收起 |
| 移动端头部 | ≤640px 紧凑化：logo 仅图标、导航条自身横向滑动（隐藏滚动条）、认证按钮紧凑 | 消除横向溢出（v1.9 修复 551px 溢出 bug 的规范沉淀） |

## 4. 交互设计规范

1. **弹窗（长流程）**：≤640px 统一「底部 sheet」形态——遮罩 `align-items: flex-end`、`max-height: 88vh` 内滚、顶部圆角（令牌 `--sheet-breakpoint/--sheet-max-height/--sheet-radius-top`，见 `qinghe-skins.css`）。v1.6 订单三弹窗 + v1.9 Profile/Seckill/商品弹窗均已接入。
2. **控制台表格**：≤640px `display: block + overflow-x: auto` 横向滚动（`data-table/coupon-table/seckill-table`）。
3. **反馈**：全局 Toast（`AppToast` 容器 + `utils/toast` 队列；成功绿/失败红/警告琥珀/信息蓝；点击可提前关闭；容器 `role="status" aria-live="polite"`）。页面内禁止再自造弹提示。
4. **运营位轮播**：5s 自动切换、悬停暂停、指示点可点、`/` 前缀链接走站内路由、其余外链新窗（`rel="noopener"`）。
5. **状态兜底**：列表必有加载（spinner）/空态（图标+文案）/失败三态；404 兜底页提供双出口（返回首页/浏览商品）。

## 5. 可访问性基线

- 交互控件使用原生语义元素（button/a/input）；图标按钮带 `aria-label`（关闭菜单/主题切换/轮播指示点）。
- Toast 容器 `role="status" aria-live="polite"`；遮罩点击自身可关闭（`@click.self`）。
- 表单必填项以 `<span class="req">*</span>` 标注并配合 label。

## 6. 新增页面/组件的落地清单

1. 消费皮肤令牌而非硬编码色；新令牌先入 `qinghe-skins.css` 基础层。
2. 长流程表单弹窗 → 复制 §4.1 的 sheet 媒体查询块。
3. 控制台新表格 → 追加对应表格类的 640px 横滚块。
4. 配套 `.spec.js`（全 .vue 覆盖是门禁基线），交互含 toast 的用真实 toast 队列断言（参考 `Checkout.spec`）。
5. `npm run lint && npm run test && npm run build` 三绿后提交。
