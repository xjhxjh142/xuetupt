"""
苏格拉底辅导对话 API 路由 — LangGraph 版
"""

import uuid

from fastapi import APIRouter, HTTPException, Request, UploadFile, File
from fastapi.responses import JSONResponse, StreamingResponse

from api.models import StartDialogRequest, ContinueDialogRequest, ChatCompletionRequest
from api.sse import stream_llm_response

router = APIRouter(prefix="/api/agent", tags=["socratic"])


def _extract_reply(result: dict) -> str:
    """从 LangGraph 结果中提取回复文本"""
    tutor = result.get("tutor_output")
    if tutor and hasattr(tutor, "socratic_question") and tutor.socratic_question:
        return tutor.socratic_question
    return result.get("final_output", "")


def _invoke(app, user_input: str, dialog_id: str) -> dict:
    """调用 LangGraph app 并返回结果"""
    result = app.invoke(
        {"user_input": user_input},
        {"configurable": {"thread_id": dialog_id}},
    )
    resp = {
        "success": True,
        "response": _extract_reply(result),
        "is_completed": False,
        "dialog_id": dialog_id,
        "context": {"dialog_id": dialog_id},
    }
    img = result.get("image_base64", "")
    if img:
        resp["image_base64"] = img
    return resp


@router.post("/start")
async def start_dialog(request: Request, req: StartDialogRequest):
    # 每次新对话都生成一个唯一 dialog_id，前端可通过该 ID 追踪对话状态，无需额外请求获取
    app = request.app.state.langgraph_app
    dialog_id = req.dialog_id or f"dialog_{uuid.uuid4().hex[:12]}"
    try:
        return JSONResponse(content=_invoke(app, req.problem, dialog_id))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/continue")
async def continue_dialog(request: Request, req: ContinueDialogRequest):
    # 继续对话时从请求上下文中获取 dialog_id，确保对话状态连续，无需前端额外传递
    app = request.app.state.langgraph_app
    dialog_id = (
        req.context.get("dialog_id", "") if isinstance(req.context, dict) else ""
    )
    try:
        return JSONResponse(content=_invoke(app, req.answer, dialog_id))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/start/stream")
async def start_dialog_stream(request: Request, req: StartDialogRequest):
    # 流式接口同样生成唯一 dialog_id，并在流结束时发送给前端，前端可直接使用该 ID 进行后续交互，无需额外请求获取
    app = request.app.state.langgraph_app
    dialog_id = req.dialog_id or f"dialog_{uuid.uuid4().hex[:12]}"
    try:
        result = _invoke(app, req.problem, dialog_id)
        return StreamingResponse(
            stream_llm_response(
                result["response"],
                dialog_id=dialog_id,
                image_base64=result.get("image_base64", ""),
            ),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache",
                "Connection": "keep-alive",
                "X-Accel-Buffering": "no",
            },
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/continue/stream")
async def continue_dialog_stream(request: Request, req: ContinueDialogRequest):
    # 继续对话（流式）时从请求上下文中获取 dialog_id，确保对话状态连续，无需前端额外传递
    app = request.app.state.langgraph_app
    dialog_id = (
        req.context.get("dialog_id", "") if isinstance(req.context, dict) else ""
    )
    try:
        result = _invoke(app, req.answer, dialog_id)
        return StreamingResponse(
            stream_llm_response(
                result["response"],
                dialog_id=dialog_id,
                image_base64=result.get("image_base64", ""),
            ),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache",
                "Connection": "keep-alive",
                "X-Accel-Buffering": "no",
            },
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/chat")
async def chat_completion(request: Request, req: ChatCompletionRequest):
    """兼容 OpenAI API 格式"""
    user_message = req.messages[-1].content if req.messages else ""
    if not user_message:
        return JSONResponse(content={"choices": [{"message": {"content": ""}}]})

    app = request.app.state.langgraph_app
    dialog_id = f"chat_{uuid.uuid4().hex[:12]}"
    result = _invoke(app, user_message, dialog_id)

    if req.stream:
        return StreamingResponse(
            stream_llm_response(
                result["response"],
                dialog_id=dialog_id,
                image_base64=result.get("image_base64", ""),
            ),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache",
                "Connection": "keep-alive",
                "X-Accel-Buffering": "no",
            },
        )

    return JSONResponse(
        content={
            "choices": [
                {
                    "index": 0,
                    "message": {"role": "assistant", "content": result["response"]},
                }
            ]
        }
    )


@router.post("/recognize")
async def recognize_math_problem(request: Request, file: UploadFile = File()):
    """拍照识别数学题目"""
    from core.vision import recognize_math

    image_data = await file.read()
    raw = recognize_math(image_data)

    # 清理识别结果中的 NOT_MATH 残留
    problem = raw.replace("NOT_MATH", "").strip()
    if not problem:
        return JSONResponse(
            content={
                "success": False,
                "response": "图片中没有识别到数学题目，请重新拍照。",
            }
        )

    # 进入辅导流程
    app = request.app.state.langgraph_app
    dialog_id = f"dialog_{uuid.uuid4().hex[:12]}"
    result = app.invoke(
        {"user_input": problem},
        {"configurable": {"thread_id": dialog_id}},
    )
    tutor_out = result.get("tutor_output")
    return JSONResponse(
        content={
            "success": True,
            "problem": problem,
            "response": (
                getattr(tutor_out, "socratic_question", "") if tutor_out else ""
            ),
            "dialog_id": dialog_id,
        }
    )
