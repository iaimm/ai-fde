import { ref } from 'vue'
import { defineStore } from 'pinia'
import axios from 'axios'

export const useChatStore = defineStore('chat', () => {
  const messages = ref([])      // ← 缺这行就是 undefined
  const isSending = ref(false)    // ← Day 7 新增

  async function send(question) {
    messages.value.push({ role: 'user', content: question })
    isSending.value = true
    try {
      const resp = await axios.post('/api/chat', { question })
      messages.value.push({ role: 'ai', content: resp.data })
    } catch (e) {
      messages.value.push({ role: 'ai', content: '出错了: ' + e.message })
    } finally {
      isSending.value = false
    }
  }

  return { messages, send, isSending }   // ← 漏 return 也是 undefined
})