# COMPONENT-GUIDELINES.md — 组件开发、样式、依赖与无障碍规范

> 派生视图（权威源：[青禾商城手册.md](../青禾商城手册.md) v8.0 §5.12/§9-D1；样式细节见 [DESIGN.md](../DESIGN.md)）。

## 1. 组件分类与现状

| 类别 | 位置 | 清单 |
| --- | --- | --- |
| 全局组件 | `src/components/` | AppHeader（sticky 头部 + 移动紧凑化）、AppFooter、ProductCard |
| 控制台公共组件 | `src/components/common/` | ConsoleSidebar（侧栏 + 遮罩）、AppToast（全局提示容器）、LogisticsTimeline（物流时间轴） |
| 页面组件 | `src/views/**`（40 个） | 前台 15 + admin 9 + merchant 8 等 |

**门禁基线：每个 `.vue` 必须有同名 `.spec.js`（当前 63 文件全覆盖，新增组件不得破坏）。**

## 2. 组件开发规范

1. **`<script setup>`**：组合式 API；`ref/computed/onMounted` 管理状态；对外事件 `defineEmits`、契约 props `defineProps`（required 显式声明）。
2. **数据获取**：一律经 `src/api/*` 封装（axios Result 包络：`res.data` 为业务数据，失败 reject Error 由 `apiError` 统一转提示）。组件内禁止直接 import axios。
3. **反馈**：`import { toast, apiError } from '../utils/toast'`——成功 `toast.success`、校验拦截 `toast.warning`、失败 `apiError(e, '前缀')`；断言用真实 toasts 队列（参考 `Checkout.spec`）。
4. **长流程表单**：弹窗承载；≤640px 底部 sheet（复制 `qinghe-skins.css` 口径的媒体查询块）。
5. **金额**：展示用 `formatPrice`；计算用 `utils/coupon` 现有规则函数，禁止在组件内重写优惠公式。

## 3. 样式规范

1. **只消费语义令牌**：`var(--color-*) / --radius-* / --shadow-* / --transition-* / --skin-*`；禁止硬编码角色色（三端皮肤由 `data-skin` 驱动，见 DESIGN.md §2）。
2. **scoped 优先**：组件样式 `<style scoped>`；控制台公共样式例外（`console-*` 前缀全局，防冲突靠前缀）。
3. **响应式断点**：768px（导航图标化/侧栏抽屉）与 640px（头部紧凑化/表格横滚/弹窗 sheet）两级；新增断点需入 DESIGN.md。
4. **暗色主题**：颜色全部走令牌即自动适配；不得出现裸 `#fff/#000` 文字色。

## 4. 依赖规范

- 运行时依赖白名单：`vue / vue-router / pinia / axios`（新增依赖需在手册变更注说明理由）。
- 开发依赖：`vitest / @vue/test-utils / @playwright/test / eslint / prettier / vite`。
- 禁止引入 UI 组件库（样式体系为自研令牌）；图标内联 SVG。
- 依赖升级走 `npm audit`（CI 门禁 audit-level=high）+ 全量回归。

## 5. 测试规范（spec 编写）

1. 页面/组件 spec 与组件同目录同名；mock 局部：`vi.mock('../../api/xxx')` + `vi.hoisted` 提取 spy。
2. **toast 断言不 mock**：导入真实 `toasts` 队列 + `beforeEach` 清空（v1.14 教训：mock 掉 utils/toast 会失去联动行为）。
3. **包络契约**：`res.data` 为业务数据（拦截器已解包）；分页 `Paging.data`；纯列表接口直接是数组——mock 数据形状必须与真实响应一致（v1.7 教训）。
4. PageHelper 分页路径纯 mock 恒空（§8.1 局限），数据填充逻辑抽 static/协作者直测 + 集成兜底。
5. 覆盖率门禁：statements/lines ≥85、branches ≥80、functions ≥75（CI 强制）。

## 6. 无障碍规范

1. 交互控件用原生语义元素；图标按钮必须 `aria-label`（侧栏关闭/主题切换/轮播指示点等既有示例）。
2. 动态通知区域 `role="status" aria-live="polite"`（AppToast 既有实现）。
3. 遮罩类弹层支持点击自身关闭；表单必填以 label + `*` 标注。
4. 新增交互组件提交前自检：键盘可达、焦点可见、读屏语义三问。
