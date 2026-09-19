// ============================================================
// 前台关键路径冒烟（C1，v1.4 §9.3）
// 覆盖：访客浏览 → 注册 → 登录 → 加购 → 购物车结算 → 提交订单
//       → 立即付款 → 模拟支付 → 支付成功；管理端登录冒烟
// 前置：后端运行于 8080（dev profile，MySQL/Redis 就绪，支付未配置凭证
//       自动回退模拟通道——真实凭证环境下本用例仍走模拟支付按钮）
// ============================================================
import { test, expect } from '@playwright/test'

const runId = Date.now()
const e2eUser = `e2e_${runId}`
const e2ePwd = 'e2e-pass-123'

test.describe.configure({ mode: 'serial' })

test('顾客关键路径：注册 → 登录 → 加购 → 下单 → 模拟支付', async ({ page }) => {
  // 1. 访客浏览首页：商品卡片可见
  await page.goto('/')
  await expect(page.locator('.product-card').first()).toBeVisible()

  // 2. 注册顾客（role-option 第一项为「顾客」）
  await page.goto('/register')
  await page.locator('.role-option').first().click()
  await page.getByPlaceholder('请输入用户名').fill(e2eUser)
  await page.getByPlaceholder(/请输入密码/).first().fill(e2ePwd)
  await page.getByPlaceholder('请再次输入密码').fill(e2ePwd)
  await page.getByRole('button', { name: '注册', exact: true }).click()
  await page.waitForURL('**/login')

  // 3. 登录（注册成功跳登录页；等待 Login 组件挂载完成，避免 fill 打在残留 DOM 上）
  await page.getByRole('heading', { name: '欢迎回来' }).waitFor()
  await page.getByPlaceholder('请输入用户名').fill(e2eUser)
  await page.getByPlaceholder('请输入密码').fill(e2ePwd)
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL((u) => u.pathname !== '/login')

  // 4. 商品详情加入购物车
  await page.locator('.product-card').first().click()
  await page.waitForURL(/\/product\//)
  await page.getByRole('button', { name: '加入购物车' }).click()
  await expect(page.getByText('已加入购物车')).toBeVisible()

  // 5. 购物车：全选 → 结算
  await page.goto('/cart')
  await expect(page.getByRole('button', { name: /结算/ })).toBeVisible()
  // 勾选全部未选中的条目（含底部全选栏）；全选后重复项跳过
  const boxes = page.locator('.checkbox:not(.checked)')
  const count = await boxes.count()
  for (let i = 0; i < count; i++) {
    await page.locator('.checkbox:not(.checked)').first().click()
  }
  await page.getByRole('button', { name: /结算/ }).click()
  await page.waitForURL('**/checkout')

  // 6. 结算页：填写收货信息 → 提交订单
  await page.getByPlaceholder('请输入收货人姓名').fill('E2E测试')
  await page.getByPlaceholder('请输入手机号').fill('13800001234')
  await page.getByPlaceholder('请输入详细收货地址（省市区 + 街道门牌）').fill('北京市海淀区中关村大街1号')
  await page.getByRole('button', { name: '提交订单' }).click()
  await page.waitForURL('**/orders')

  // 7. 订单中心：待付款订单 → 立即付款
  const payButton = page.getByRole('button', { name: '立即付款' }).first()
  await expect(payButton).toBeVisible()
  await payButton.click()
  await page.waitForURL(/\/pay\?orderNumber=/)

  // 8. 支付页：模拟支付（未配置商户凭证时的默认通道）
  await page.getByRole('button', { name: '模拟支付', exact: true }).click()
  await page.waitForURL(/pay-success/)

  // 9. 支付成功页
  await expect(page.getByText('支付成功')).toBeVisible()
})

test('管理端冒烟：admin 登录 → 仪表盘可见', async ({ page }) => {
  await page.goto('/login')
  await page.getByPlaceholder('请输入用户名').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('123456')
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL((u) => u.pathname !== '/login')

  await page.goto('/admin')
  // 管理端布局与仪表盘统计可见（种子管理员 admin/123456）
  await expect(page.getByText('仪表盘').first()).toBeVisible()
})
