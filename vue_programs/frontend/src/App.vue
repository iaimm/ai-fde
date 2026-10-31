<script setup>
import { ref, onMounted } from 'vue'
import { useChatStore } from './stores/chat'


const chat = useChatStore()
const question = ref('')

onMounted(() => chat.load())
async function ask() {
  const q = question.value
  question.value = ''
  await chat.send(q)
}
</script>

<template>
  <div v-if="chat?.messages.length === 0" style="color: #999;">
    还没有对话，问点什么吧
  </div>
  <div style="padding: 2rem; font-family: sans-serif; max-width: 640px;">
    <h1>NotesGPT</h1>
    <div v-for="(m, i) in chat.messages" :key="i"
         :style="{ textAlign: m.role === 'user' ? 'right' : 'left', margin: '8px 0' }">
      <span :style="{
        display: 'inline-block', padding: '8px 12px', borderRadius: '8px',
        background: m.role === 'user' ? '#d3e3fd' : '#f0f0f0'
      }">{{ m.content }}</span>
    </div>
    <div v-if="chat.isSending" style="color: #999;">思考中...</div>
    
    <div style="margin-top: 1rem;">
      <input v-model="question" placeholder="输入你的问题" style="padding: 6px; width: 300px;" />
      <button @click="ask" :disabled="chat.isSending" style="margin-left: 8px; padding: 6px 16px;">
        {{ chat.isSending ? '发送中' : '发送' }}
      </button>
    </div>
  </div>
</template>