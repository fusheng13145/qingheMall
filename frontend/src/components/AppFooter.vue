<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <div class="footer-top">
        <div class="footer-brand">
          <div class="footer-logo">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
              <polyline points="9 22 9 12 15 12 15 22"/>
            </svg>
            <span>青禾商城</span>
          </div>
          <p class="footer-desc">品质生活，尽在青禾</p>
        </div>
        <div class="footer-links-group">
          <div class="footer-col">
            <h4 class="footer-col-title">服务</h4>
            <a href="javascript:void(0)" class="footer-link">售后服务</a>
            <a href="javascript:void(0)" class="footer-link">配送说明</a>
            <a href="javascript:void(0)" class="footer-link">退换政策</a>
          </div>
          <div class="footer-col">
            <h4 class="footer-col-title">关于</h4>
            <a href="javascript:void(0)" class="footer-link">关于我们</a>
            <a href="javascript:void(0)" class="footer-link">联系我们</a>
            <a href="javascript:void(0)" class="footer-link">加入我们</a>
          </div>
          <div class="footer-col">
            <h4 class="footer-col-title">帮助</h4>
            <a href="javascript:void(0)" class="footer-link">常见问题</a>
            <a href="javascript:void(0)" class="footer-link">隐私政策</a>
            <a href="javascript:void(0)" class="footer-link">用户协议</a>
          </div>
        </div>
      </div>
      <div class="footer-bottom">
        <p class="footer-copyright">&copy; 2024 青禾商城. 保留所有权利.</p>
      </div>
    </div>
  </footer>
</template>

<script setup>
</script>

<style scoped>
.app-footer {
  background: var(--color-bg-elevated);
  border-top: 1px solid var(--color-border);
  margin-top: 60px;
  transition: background-color var(--transition-base), border-color var(--transition-base);
}

.footer-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 20px;
}

.footer-top {
  display: flex;
  justify-content: space-between;
  padding: 48px 0 36px;
  gap: 40px;
}

.footer-brand {
  max-width: 280px;
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-primary);
  margin-bottom: 12px;
}

.footer-logo span {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
}

.footer-desc {
  font-size: 14px;
  color: var(--color-text-tertiary);
  line-height: 1.6;
}

.footer-links-group {
  display: flex;
  gap: 64px;
}

.footer-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.footer-col-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.footer-link {
  font-size: 14px;
  color: var(--color-text-tertiary);
  transition: color var(--transition-fast);
}

.footer-link:hover {
  color: var(--color-primary);
}

.footer-bottom {
  border-top: 1px solid var(--color-border);
  padding: 20px 0;
}

.footer-copyright {
  font-size: 13px;
  color: var(--color-text-tertiary);
  text-align: center;
}

@media (max-width: 768px) {
  .footer-top {
    flex-direction: column;
    gap: 32px;
    padding: 36px 0 28px;
  }

  .footer-links-group {
    gap: 40px;
  }
}

@media (max-width: 480px) {
  .footer-links-group {
    flex-direction: column;
    gap: 24px;
  }
}
</style>
