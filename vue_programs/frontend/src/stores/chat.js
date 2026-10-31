import { ref, watch } from 'vue'
import { defineStore } from 'pinia'
import axios from 'axios'

export const useChatStore = defineStore('chat', () => {
  const messages = ref([])      // ← 缺这行就是 undefined
  const isSending = ref(false)    // ← Day 7 新增

//   async function send(question) {
//     messages.value.push({ role: 'user', content: question })
//     isSending.value = true
//     try {
//       const resp = await axios.post('/api/chat', { question })
//       messages.value.push({ role: 'ai', content: resp.data })
//     } catch (e) {
//       messages.value.push({ role: 'ai', content: '出错了: ' + e.message })
//     } finally {
//       isSending.value = false
//     }
//   }

    function save() {
        localStorage.setItem('notesgpt-messages', JSON.stringify(messages.value))
    }

    function load() {
        const saved = localStorage.getItem('notesgpt-messages')
        if (saved) messages.value = JSON.parse(saved)
    }

    watch(messages, save, { deep: true })

    async function send(question) {
        messages.value.push({ role: 'user', content: question })
        messages.value.push({ role: 'ai', content: '' })   // 先占个空位，流式往里填
        isSending.value = true
        let started = false                                 // 首字检测旗标
        try {
            // encodeURIComponent：curl 中文 400 的学费不能白交
            const resp = await fetch('/api/chat/stream?question=' + encodeURIComponent(question))
            const reader = resp.body.getReader()
            const decoder = new TextDecoder()
            let buffer = ''

            while (true) {
            const { done, value } = await reader.read()
            if (done) break
            // stream:true 处理中文字符被切块的情况（一个汉字可能分两个网络包到）
            buffer += decoder.decode(value, { stream: true })

            const frames = buffer.split('\n\n')   // SSE 帧以空行分隔（Day 1 截图里的格式）
            buffer = frames.pop()                  // 末尾可能是不完整的半帧，留到下轮
            for (const frame of frames) {
                const line = frame.trim()
                if (!line.startsWith('data:')) continue
                const data = JSON.parse(line.slice(5).trim());
                if (data === '[DONE]') continue
                messages.value[messages.value.length - 1].content += data
                if (!started) { started = true; isSending.value = false }   // 首字到达，"思考中"退场
            }
            }
        } catch (e) {
            messages.value[messages.value.length - 1].content += ' [出错了: ' + e.message + ']'
        } finally {
            isSending.value = false
        }
    }

  return { messages, send, isSending, load }   // ← 漏 return 也是 undefined
})