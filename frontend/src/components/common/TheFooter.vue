<template>
  <footer class="footer">
    <div class="container">
      <div class="footer-content">
        <div class="footer-section">
          <h4>关于我们</h4>
          <ul>
            <li><a href="#">公司简介</a></li>
            <li><a href="#">联系方式</a></li>
            <li><a href="#">招聘信息</a></li>
          </ul>
        </div>

        <div class="footer-section">
          <h4>客户服务</h4>
          <ul>
            <li><a href="#">售后服务</a></li>
            <li><a href="#">配送说明</a></li>
            <li><a href="#">退换货政策</a></li>
          </ul>
        </div>

        <div class="footer-section">
          <h4>帮助中心</h4>
          <ul>
            <li><a href="#">常见问题</a></li>
            <li><a href="#">用户指南</a></li>
            <li><a href="#">联系客服</a></li>
          </ul>
        </div>

        <div class="footer-section">
          <h4>关注我们</h4>
          <ul>
            <li><a href="#">官方微博</a></li>
            <li><a href="#">官方微信</a></li>
          </ul>
        </div>
      </div>

      <div class="footer-bottom">
        <p>&copy; 2024 青禾商城 版权所有 | ICP备案号：XXXXXXXX号</p>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
</script>

<style lang="scss" scoped>
.footer {
  background: #2c2c2c;
  color: #999;
  padding: 40px 0 20px;
  margin-top: 60px;
}

.container {
  width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}

.footer-content {
  display: flex;
  justify-content: space-between;
  padding-bottom: 30px;
  border-bottom: 1px solid #3a3a3a;
}

.footer-section {
  h4 {
    color: #fff;
    font-size: 16px;
    margin-bottom: 16px;
  }

  ul {
    li {
      margin-bottom: 10px;

      a {
        color: #999;
        text-decoration: none;
        transition: color 0.3s;

        &:hover {
          color: #409eff;
        }
      }
    }
  }
}

.footer-bottom {
  text-align: center;
  padding-top: 20px;
  font-size: 13px;
}
</style>
