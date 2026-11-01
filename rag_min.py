# F2 最小可跑版：加载笔记 → 分块 → 关键词检索 → LLM 流式回答
import os, glob, json, requests

# ==== 改成你跑通 FDE 练习时的配置 ====
API_URL = "https://api.moonshot.cn/v1/chat/completions"
API_KEY = os.environ["MOONSHOT_API_KEY"]
MODEL   = "kimi-k2.6"
NOTES_DIR = "./notes"          # 建个目录，扔 2~3 篇你的 .md 笔记进去

def load_chunks(folder, size=300, overlap=50):
    chunks = []
    for f in glob.glob(os.path.join(folder, "*.md")):
        text = open(f, encoding="utf-8").read()
        for i in range(0, len(text), size - overlap):
            chunks.append({"src": os.path.basename(f), "text": text[i:i+size]})
    return chunks

def retrieve(query, chunks, k=3):
    qs = set(query.lower())
    return sorted(chunks, key=lambda c: len(qs & set(c["text"].lower())), reverse=True)[:k]

def ask(question, chunks):
    refs = retrieve(question, chunks)
    ctx = "\n---\n".join(f"[{i+1}] {r['src']}: {r['text']}" for i, r in enumerate(refs))
    prompt = f"基于以下笔记回答问题，回答中用[编号]标注来源：\n{ctx}\n\n问题：{question}"
    resp = requests.post(API_URL,
        headers={"Authorization": f"Bearer {API_KEY}"},
        json={"model": MODEL,
              "messages": [{"role": "user", "content": prompt}],
              "stream": True},
        stream=True, timeout=(5, 60))
    if resp.status_code != 200:
        print("请求失败:", resp.status_code, resp.text[:200])
        return
    print("\n答案: ", end="", flush=True)
    for line in resp.iter_lines():
        line = line.strip()
        if not line.startswith(b"data:"):
            continue
        payload = line[5:].strip()
        if payload == b"[DONE]":
            break
        delta = json.loads(payload)["choices"][0]["delta"].get("content", "")
        print(delta, end="", flush=True)
    print("\n来源:", [r["src"] for r in refs])

if __name__ == "__main__":
    chunks = load_chunks(NOTES_DIR)
    print(f"已加载 {len(chunks)} 个分块")
    while True:
        q = input("\n问点什么 (q 退出): ").strip()
        if q == "q":
            break
        ask(q, chunks)