<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <b>AI 服务配置与连接测试</b>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="本页面只进行连接测试，不保存 API Key、不修改运行配置。正式启用外部模型，请通过环境变量或 application.yml 配置（ICH_AI_ENABLED / ICH_AI_API_KEY 等）。"
        style="margin-bottom: 16px"
      />

      <el-form :model="form" label-width="110px" style="max-width: 620px">
        <el-form-item label="服务商">
          <el-select v-model="form.provider" style="width: 100%" @change="onProviderChange">
            <el-option
              v-for="p in providers"
              :key="p.code"
              :label="p.label"
              :value="p.code"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="接口地址">
          <el-input :model-value="currentBaseUrl" disabled />
        </el-form-item>

        <el-form-item label="模型名称">
          <el-input
            v-model="form.model"
            :placeholder="`留空使用默认模型：${currentDefaultModel}`"
          />
        </el-form-item>

        <el-form-item label="API Key">
          <el-input
            v-model="form.apiKey"
            type="password"
            show-password
            placeholder="仅用于本次测试，不会被保存"
            autocomplete="off"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="testing"
            @click="runTest"
          >测试连接</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="result" shadow="never" style="margin-top: 16px">
      <template #header><b>测试结果</b></template>
      <el-result
        :icon="result.ok ? 'success' : 'error'"
        :title="result.ok ? '连接成功' : '连接失败'"
        :sub-title="result.message"
      >
        <template #extra>
          <el-descriptions :column="2" border size="small" style="max-width: 560px">
            <el-descriptions-item label="结果分类">
              {{ stageText(result.stage) }}
            </el-descriptions-item>
            <el-descriptions-item label="HTTP 状态">
              {{ result.httpStatus ?? '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="耗时">
              {{ result.elapsedMs }} ms
            </el-descriptions-item>
            <el-descriptions-item label="使用模型">
              {{ result.model || '—' }}
            </el-descriptions-item>
          </el-descriptions>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { testAiConnection } from '@/api/admin'

// 与后端白名单枚举保持一致；接口地址由后端校验，前端仅作展示，不可编辑
const providers = [
  {
    code: 'glm',
    label: '智谱 GLM',
    baseUrl: 'https://open.bigmodel.cn/api/paas/v4',
    defaultModel: 'glm-4-flash'
  },
  {
    code: 'siliconflow',
    label: '硅基流动',
    baseUrl: 'https://api.siliconflow.cn/v1',
    defaultModel: 'Qwen/Qwen2.5-7B-Instruct'
  }
]

const stageMap = {
  SUCCESS: '成功',
  AUTH_FAILED: '认证失败（Key 无效）',
  FORBIDDEN: '访问被拒绝',
  NOT_FOUND: '接口或模型不存在',
  BAD_REQUEST: '请求参数错误',
  RATE_LIMITED: '限流或额度不足',
  UPSTREAM_ERROR: '服务商服务端错误',
  NETWORK_ERROR: '网络/连接失败',
  BAD_RESPONSE: '响应格式异常',
  INVALID_PROVIDER: '服务商无效'
}

const form = reactive({ provider: 'glm', model: '', apiKey: '' })
const testing = ref(false)
const result = ref(null)

const current = computed(() => providers.find(p => p.code === form.provider) || providers[0])
const currentBaseUrl = computed(() => current.value.baseUrl)
const currentDefaultModel = computed(() => current.value.defaultModel)

function onProviderChange() {
  // 切换服务商时清空模型与 Key，避免沿用旧值
  form.model = ''
  form.apiKey = ''
  result.value = null
}

function stageText(stage) {
  return stageMap[stage] || stage || '—'
}

async function runTest() {
  if (!form.provider) {
    ElMessage.warning('请选择服务商')
    return
  }
  if (!form.apiKey || !form.apiKey.trim()) {
    ElMessage.warning('请输入 API Key')
    return
  }
  testing.value = true
  result.value = null
  try {
    const payload = { provider: form.provider, apiKey: form.apiKey.trim() }
    if (form.model && form.model.trim()) {
      payload.model = form.model.trim()
    }
    const res = await testAiConnection(payload)
    result.value = res.data || null
    if (result.value && result.value.ok) {
      ElMessage.success('连接测试成功')
    } else {
      ElMessage.warning('连接测试未通过，请查看结果说明')
    }
  } catch (e) {
    // 不打印 Key；仅展示后端返回的安全提示
    ElMessage.error(e.message || '测试请求失败')
  } finally {
    // 测试结束立即清空 Key，不保留在组件内存以外的任何位置
    form.apiKey = ''
    testing.value = false
  }
}

function resetForm() {
  form.provider = 'glm'
  form.model = ''
  form.apiKey = ''
  result.value = null
}

// 页面卸载时彻底清空 Key，避免残留在内存引用
onUnmounted(() => {
  form.apiKey = ''
})
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
