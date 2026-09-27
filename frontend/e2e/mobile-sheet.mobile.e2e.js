// ============================================================
// 移动端视口冒烟（D1，v1.9 §9.4）
// 在 iPhone 13 视口下验证弹窗「底部 sheet」口径：
//   全宽 + 贴底 + 顶部圆角 + 限高内滚（qinghe-skins.css --sheet-* 令牌）
// 前置：后端运行于 8080（公开秒杀列表 + 种子演示活动，无需登录即可打开弹窗）
// 仅由 mobile-chromium 项目执行（见 playwright.config.js testMatch）
// ============================================================
import { test, expect } from '@playwright/test'

test('移动端弹窗 sheet 化：秒杀详情弹窗全宽贴底 + 顶部圆角 + 限高内滚', async ({ page }) => {
  await page.goto('/seckill')

  // 公开秒杀列表存在进行中活动（index.sql 种子 skseed001/002 或运行期活动）
  const card = page.locator('.seckill-card').first()
  await expect(card).toBeVisible()
  // openDetail 绑定在「立即抢购」按钮上（弹窗打开无需登录）
  await card.locator('.btn-grab').click()

  const modal = page.locator('.modal')
  await expect(modal).toBeVisible()
  // 弹窗为 fixed 定位：回页顶消除页面滚动对 boundingBox 的影响
  await page.evaluate(() => window.scrollTo(0, 0))

  const box = await modal.boundingBox()
  const vp = page.viewportSize()
  expect(vp.width).toBeLessThan(500) // 确认跑在移动视口而非桌面
  // 全宽贴底：sheet 形态的核心特征
  expect(box.x).toBeLessThan(2)
  expect(Math.abs(box.width - vp.width)).toBeLessThan(2)
  expect(Math.abs(box.y + box.height - vp.height)).toBeLessThan(2)
  // 顶部圆角（底部 sheet 视觉口径）
  const radius = await modal.evaluate((el) => getComputedStyle(el).borderTopLeftRadius)
  expect(parseFloat(radius)).toBeGreaterThan(8)
  // 限高内滚
  expect(box.height).toBeLessThanOrEqual(vp.height * 0.9)
})
