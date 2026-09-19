// ============================================================
// Playwright E2E 配置（C1，v1.4 §9.3）
// 关键路径冒烟：访客浏览 → 注册/登录 → 加购 → 下单 → 模拟支付 → 管理端
//
// 前置：后端已运行于 http://localhost:8080（本机 dev / CI 由 job 先起后端）
// webServer 自动拉起 vite dev（/api 与 /uploads 代理至 8080，见 vite.config.js）
// 与 Vitest 单测隔离：e2e 用例命名 *.e2e.js，vitest 默认 include 不匹配
// ============================================================
import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.js',
  timeout: 90_000,
  expect: { timeout: 15_000 },
  fullyParallel: false, // 冒烟含交易写操作，串行执行避免库存/订单相互干扰
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : [['list']],
  use: {
    baseURL: 'http://localhost:5199',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'npm run dev -- --port 5199 --strictPort',
    url: 'http://localhost:5199',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
})
