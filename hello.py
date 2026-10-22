from openai import OpenAI
import os

client = OpenAI(
    api_key=os.environ["MOONSHOT_API_KEY"],
    base_url="https://api.moonshot.cn/v1",
)

stream = client.chat.completions.create(
    model="kimi-k2.6",
    messages=[
        {"role": "system", "content": "你是一个列车餐车服务员，乘客是个程序员奶爸。"},
        {"role": "user", "content": "给我讲一个 100 字的小故事，主题：一张失而复得的卧铺票。"},
    ],
    stream=True,   # ← 关键就这一个参数
)

for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="", flush=True)