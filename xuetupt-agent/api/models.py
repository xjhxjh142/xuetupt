"""
Pydantic 请求/响应模型
"""
from typing import Optional, List

from pydantic import BaseModel


class Message(BaseModel):
    role: str
    content: str


class ChatCompletionRequest(BaseModel):
    messages: List[Message]
    stream: Optional[bool] = False


class StartDialogRequest(BaseModel):
    problem: str
    dialog_id: Optional[str] = ""


class ContinueDialogRequest(BaseModel):
    context: dict
    answer: str
