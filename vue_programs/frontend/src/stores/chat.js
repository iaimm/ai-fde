import { ref } from 'vue'
import { defineStore } from 'pinia'
import axios from 'axios'

export const useChatStore = defineStore('chat', () => {
    const messages = ref([])   // 元素形如 { role: 'user'|'ai', content: '...' }

    const isSending = ref(false)

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
        return { messages, send, isSending } 
    }
})