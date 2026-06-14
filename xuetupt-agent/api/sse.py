"""SSE 流式响应工具（OpenAI 兼容格式）"""
import asyncio
import json


async def stream_llm_response(text: str, delay: float = 0.02, dialog_id: str = "",
                              image_base64: str = ""):
    """SSE 输出文本，末尾附带 dialog_id 和可选图片"""
    for char in text:
        chunk = json.dumps({"choices": [{"delta": {"content": char}}]})
        yield f"data: {chunk}\n\n"
        await asyncio.sleep(delay)
    meta = {"dialog_id": dialog_id, "finish_reason": "stop"}
    if image_base64:
        meta["image_base64"] = image_base64
    yield f"data: {json.dumps(meta)}\n\n"
    yield "data: [DONE]\n\n"
