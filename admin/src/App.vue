<template>
  <div class="app-shell">
    <Sidebar />
    <div class="workspace">
      <Header @new-appointment="appointmentOpen = true" />
      <main>
        <div v-if="adminState.error" class="alert">
          <strong>接口异常</strong>
          <span>{{ adminState.error }}</span>
          <button type="button" @click="adminActions.bootstrap">重试</button>
        </div>
        <RouterView />
      </main>
    </div>
  </div>

  <div v-if="appointmentOpen" class="modal-backdrop" @click.self="appointmentOpen = false">
    <section class="modal">
      <header>
        <div>
          <span>FORM</span>
          <h2>新建预约</h2>
          <p>创建新预约并安排拍摄档期</p>
        </div>
        <button class="icon-button" type="button" aria-label="关闭" @click="appointmentOpen = false">
          <X :size="18" />
        </button>
      </header>
      <div class="form-grid">
        <label>
          <span>客户姓名</span>
          <input type="text" placeholder="例：周予安" />
        </label>
        <label>
          <span>手机号</span>
          <input type="text" placeholder="例：152****6612" />
        </label>
        <label>
          <span>门店</span>
          <select>
            <option v-for="store in adminState.stores" :key="store.id">{{ store.name }}</option>
          </select>
        </label>
        <label>
          <span>拍摄套系</span>
          <select>
            <option v-for="service in adminDerived.activeServices.value" :key="service.id">{{ service.name }}</option>
          </select>
        </label>
      </div>
      <footer>
        <small>当前后端管理接口尚未提供新建订单能力。</small>
        <button type="button" class="primary-button" disabled>创建预约</button>
      </footer>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { X } from 'lucide-vue-next'
import Header from './components/Header.vue'
import Sidebar from './components/Sidebar.vue'
import { adminActions, adminDerived, adminState } from './store'

const appointmentOpen = ref(false)

onMounted(() => {
  void adminActions.bootstrap()
})
</script>
