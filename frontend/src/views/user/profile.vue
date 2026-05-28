<template>
  <div class="user-profile-page">
    <div class="profile-header">
      <div class="avatar-section">
        <el-avatar :size="80" :src="userInfo.avatar || defaultAvatar" />
        <el-button text @click="handleEditAvatar">更换头像</el-button>
      </div>
      <div class="user-info">
        <h2 class="username">{{ userInfo.nickname || userInfo.username }}</h2>
        <p class="user-id">ID: {{ userInfo.id }}</p>
      </div>
    </div>

    <div class="profile-content">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="profile-form"
      >
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="20" />
        </el-form-item>

        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :label="0">保密</el-radio>
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="生日" prop="birthday">
          <el-date-picker
            v-model="form.birthday"
            type="date"
            placeholder="选择生日"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存修改</el-button>
        </el-form-item>
      </el-form>

      <div class="password-section">
        <h3 class="section-title">修改密码</h3>
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="100px"
        >
          <el-form-item label="原密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="请输入原密码"
              show-password
            />
          </el-form-item>

          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码"
              show-password
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleChangePassword" :loading="changing">
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUser } from '@/composables/useUser'

const { userInfo, updateUserInfo } = useUser()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1png.png'

const formRef = ref()
const passwordFormRef = ref()
const saving = ref(false)
const changing = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  gender: 0,
  birthday: '',
  phone: '',
  email: ''
})

const rules = {
  nickname: [
    { max: 20, message: '昵称不能超过20个字符', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }
  ]
}

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: any) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const handleEditAvatar = () => {
  ElMessage.info('头像上传功能开发中')
}

const handleSave = async () => {
  try {
    await formRef.value?.validate()
    saving.value = true
    await new Promise(resolve => setTimeout(resolve, 1000))
    updateUserInfo(form)
    ElMessage.success('保存成功')
  } catch (error) {
    // 验证失败
  } finally {
    saving.value = false
  }
}

const handleChangePassword = async () => {
  try {
    await passwordFormRef.value?.validate()
    changing.value = true
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('密码修改成功')
    passwordFormRef.value?.resetFields()
  } catch (error) {
    // 验证失败
  } finally {
    changing.value = false
  }
}

onMounted(() => {
  if (userInfo.value) {
    form.username = userInfo.value.username || ''
    form.nickname = userInfo.value.nickname || ''
    form.gender = userInfo.value.gender || 0
    form.birthday = userInfo.value.birthday || ''
    form.phone = userInfo.value.phone || ''
    form.email = userInfo.value.email || ''
  }
})
</script>

<style lang="scss" scoped>
.user-profile-page {
  .profile-header {
    display: flex;
    align-items: center;
    gap: 24px;
    padding-bottom: 30px;
    border-bottom: 1px solid #f0f2f5;
    margin-bottom: 30px;
  }

  .avatar-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }

  .user-info {
    .username {
      font-size: 22px;
      font-weight: bold;
      color: #303133;
      margin-bottom: 8px;
    }

    .user-id {
      font-size: 13px;
      color: #909399;
    }
  }

  .profile-content {
    max-width: 600px;
  }

  .section-title {
    font-size: 16px;
    font-weight: bold;
    color: #303133;
    margin: 30px 0 20px;
    padding-top: 20px;
    border-top: 1px solid #f0f2f5;
  }
}
</style>
