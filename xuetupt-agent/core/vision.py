"""视觉识别：拍照 → Ollama 视觉模型 → 数学题目文本"""
import base64
import logging

import requests

logger = logging.getLogger("xuetupt")
VISION_MODEL = "minicpm-v"


def recognize_math(image_data: bytes) -> str:
    """发送图片到视觉模型，返回识别出的数学题目文本"""
    try:
        img_b64 = base64.b64encode(image_data).decode("utf-8")
        resp = requests.post(
            "http://localhost:11434/api/chat",
            json={
                "model": VISION_MODEL,
                "messages": [{
                    "role": "user",
                    "content": "图中是什么数学题目？只输出题目文本，不要额外说明。如果不是数学题输出 NOT_MATH",
                    "images": [img_b64],
                }],
                "stream": False,
                "options": {"temperature": 0.0},
            },
            timeout=180,
        )
        text = resp.json()["message"]["content"].strip()
        logger.info(f"vision 识别结果: {text[:80]}")
        return text
    except Exception as e:
        logger.error(f"视觉识别失败: {e}")
        return "NOT_MATH"
