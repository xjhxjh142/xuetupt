"""LangGraph Router 函数"""
import re

from models.state_types import TutorState


def entry_router(state: TutorState) -> str:
    return "rule"


def rule_router(state: TutorState) -> str:
    rr = state.get("rule_result")
    if rr and not rr.passed:
        return "reject"
    return "analyze"


def analyze_router(state: TutorState) -> str:
    intent = state.get("analyze_result")
    if not intent or not intent.math_verify:
        return "reject"
    return "tutor"


def tutor_router(state: TutorState) -> str:
    if state.get("tool_result"):
        return "finalize"
    tutor_out = state.get("tutor_output")
    if tutor_out:
        tc = getattr(tutor_out, "tool_call", "")
        if tc in ("calculator", "plotter", "formula"):
            return tc
        if tc == "rag":
            return "retrieve"
    text = state.get("user_input", "")
    if re.search(r"[画绘作].*[图像图形曲线]", text):
        return "plotter"
    if re.search(r"公式|定理|面积|体积|周长", text):
        return "formula"
    if not re.search(r"[一-鿿]", text):
        if re.search(r"[\d\s\+\-\*\/\(\)\.\^xX=]{4,}", text) and re.search(r"[\+\-\*\/=]", text):
            return "calculator"
    return "finalize"
