from openai import OpenAI
import json

client = OpenAI(
    api_key=os.environ["MOONSHOT_API_KEY"],
    base_url="https://api.moonshot.cn/v1",
)

completion = client.chat.completions.create(
    model="kimi-k2.6",
    messages=[
        {"role": "system", "content": "你是一个列车餐车服务员。不输出任何其他文字、解释或 markdown 标记。"},
        {"role": "user", "content": "用 JSON 回复我，格式：{\"安慰\": \"一句安慰我的话\", \"损人\": \"一句损我的话\", \"心情\": \"愉快\"}。把值替换成你自己的话，键不变。"},
    ],
)

text = completion.choices[0].message.content
print("原始输出：", text)

data = json.loads(text)
print("解析成功：", data["损人"])