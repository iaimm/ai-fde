import requests
import os

API_URL = "https://api.moonshot.cn/v1"
API_KEY = os.environ["MOONSHOT_API_KEY"]
MODEL   = "kimi-k2.6"

print(">>> 测非流式调用（有30秒超时）...")
try:
    r = requests.post(API_URL,
        headers={"Authorization": f"Bearer {API_KEY}"},
        json={"model": MODEL,
              "messages": [{"role": "user", "content": "只回复两个字：正常"}],
              "stream": False},
        timeout=30)
    print("状态码:", r.status_code)
    print("响应前300字:", r.text[:300])
except Exception as e:
    print("请求异常:", type(e).__name__, e)