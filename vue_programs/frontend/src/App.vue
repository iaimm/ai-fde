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
       <div v-if="m.refs" style="margin-top: 4px; text-align: left;">
      <span v-for="(r, j) in m.refs" :key="j"
            @click="m.activeRef = (m.activeRef === j ? null : j)"
            style="cursor: pointer; font-size: 12px; color: #1a73e8; margin-right: 10px;">
        [{{ j + 1 }}] {{ r.src }}
      </span>
      <div v-if="m.activeRef !== null && m.activeRef !== undefined"
          style="margin-top: 4px; padding: 8px; background: #f8f9fa; border-radius: 6px;
                  font-size: 13px; color: #555; white-space: pre-wrap;">
        {{ m.refs[m.activeRef].text }}
      </div>
    </div>
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