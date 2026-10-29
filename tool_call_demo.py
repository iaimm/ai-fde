import json, requests
from rag_min import load_chunks, retrieve, API_URL, API_KEY, MODEL, NOTES_DIR

chunks = load_chunks(NOTES_DIR)

def search_notes(query):    # ← 工具本体：在本地执行，模型只是"决定"调用它
    hits = retrieve(query, chunks)
    return "\n".join(f"[{r['src']}] {r['text']}" for r in hits)

TOOLS = [{
    "type": "function",
    "function": {
        "name": "search_notes",
        "description": "在用户的本地技术笔记中搜索。回答Java并发、线程池等问题前必须先调用",
        "parameters": {
            "type": "object",
            "properties": {"query": {"type": "string", "description": "搜索关键词"}},
            "required": ["query"]
        }
    }
}]

messages = [
    {"role": "system", "content": "你是笔记助手，需要笔记内容时必须调用工具，禁止凭记忆编造"},
    {"role": "user", "content": "线程池的四种拒绝策略是什么？"}
]

# 第一轮：模型看到 tools，自己决定要不要调用
r = requests.post(API_URL, headers={"Authorization": f"Bearer {API_KEY}"},
                  json={"model": MODEL, "messages": messages, "tools": TOOLS}, timeout=60).json()
msg = r["choices"][0]["message"]
print("第一轮 finish_reason:", r["choices"][0].get("finish_reason"))
print("tool_calls:", json.dumps(msg.get("tool_calls"), ensure_ascii=False)[:300])

# 你本地执行工具，把结果交回去
call = msg["tool_calls"][0]
result = search_notes(json.loads(call["function"]["arguments"])["query"])
print("工具返回前80字:", result[:80], "...")

messages += [msg, {"role": "tool", "tool_call_id": call["id"], "content": result}]

# 第二轮：模型基于工具结果给出最终回答
r2 = requests.post(API_URL, headers={"Authorization": f"Bearer {API_KEY}"},
                   json={"model": MODEL, "messages": messages, "tools": TOOLS}, timeout=60).json()
print("最终回答:", r2["choices"][0]["message"]["content"])