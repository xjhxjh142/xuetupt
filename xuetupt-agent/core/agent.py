"""Prompt 模板构建 + LLM 调用（OpenAI SDK）"""

import json
import logging

from openai import OpenAI

from config import config
from models.schemas import TutorOutput

logger = logging.getLogger("xuetupt")

# ── Function Calling 工具定义 ──
TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "rag",
            "description": "检索知识库获取精确概念信息",
            "parameters": {
                "type": "object",
                "properties": {"query": {"type": "string"}},
                "required": ["query"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "calculator",
            "description": "数学计算或解方程，如 15*2+3*4、2x+3=7",
            "parameters": {
                "type": "object",
                "properties": {"expression": {"type": "string"}},
                "required": ["expression"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "plotter",
            "description": "绘制函数图像，如 y=x**2",
            "parameters": {
                "type": "object",
                "properties": {"function": {"type": "string"}},
                "required": ["function"],
            },
        },
    },
]

MATH_SIGNALS = {
    "方程",
    "函数",
    "几何",
    "三角",
    "公式",
    "解题",
    "计算",
    "证明",
    "求解",
    "等于",
    "不等式",
    "概率",
    "统计",
    "勾股",
    "定理",
    "平方",
    "根号",
    "面积",
    "体积",
    "周长",
    "平行",
    "垂直",
    "对称",
    "坐标",
    "因式分解",
    "分式",
    "实数",
    "有理数",
    "无理数",
    "整式",
    "幂",
    "指数",
    "对数",
    "数列",
    "向量",
    "矩阵",
    "导数",
    "积分",
}


def _fmt_history(history: list[dict], max_lines: int = 6) -> str:
    """格式化对话历史，保留最近几轮，截断过长文本"""
    if not history:
        return ""
    lines = []
    for t in history[-max_lines:]:
        role = "学生" if t["role"] == "user" else "老师"
        content = t.get("content", "")[:150]
        lines.append(f"{role}: {content}")
    return "\n".join(lines)


def call_intent_analysis(
    user_input: str, history: list[dict], client: OpenAI
) -> tuple[bool, str]:
    """判断是否为数学问题 → (math_verify, reason)"""
    system = (
        "你是一个初中数学问题判断专家。只看用户输入是否为初中数学问题。\n"
        '输出 JSON：{"is_math": true/false, "reason": "判断依据"}'
    )
    parts = [f"## 当前问题\n{user_input}"]
    h = _fmt_history(history)
    if h:
        parts.append(f"## 对话历史\n{h}")
    try:
        resp = client.chat.completions.create(
            model=config.llm_model,
            messages=[
                {"role": "system", "content": system},
                {"role": "user", "content": "\n\n".join(parts)},
            ],
            temperature=0.3,
            max_tokens=256,
        )
        raw = _extract_json(resp.choices[0].message.content or "")
        return raw.get("is_math", True), raw.get("reason", "")
    except Exception as e:
        logger.error(f"intent_analysis 失败: {e}")
        return True, ""


def call_degrade_judgment(user_input: str, history: list[dict], client: OpenAI) -> bool:
    """判断是否需要直接讲解 → needs_explain"""
    system = (
        "判断学生是否需要老师直接讲解而不是被提问。\n"
        "以下情况 needs_explain 设为 true：\n"
        "- 学生明确表示听不懂、卡住了（如「听不懂」「不明白」「太难」「教教我」）\n"
        "- 学生明确要求直接告诉答案（如「告诉我答案」「直接说」）\n"
        "- 学生的问题不含任何数学内容，只是在求助\n"
        "普通的提问（如「什么是X」「解释X」「X是什么」）不是 degrade 信号。\n"
        "否则设为 false。\n"
        '输出 JSON：{"needs_explain": true/false}'
    )
    parts = [f"## 当前问题\n{user_input}"]
    h = _fmt_history(history)
    if h:
        parts.append(f"## 对话历史\n{h}")
    try:
        resp = client.chat.completions.create(
            model=config.llm_model,
            messages=[
                {"role": "system", "content": system},
                {"role": "user", "content": "\n\n".join(parts)},
            ],
            temperature=0.3,
            max_tokens=128,
        )
        raw = _extract_json(resp.choices[0].message.content or "")
        return raw.get("needs_explain", False)
    except Exception as e:
        logger.error(f"degrade_judgment 失败: {e}")
        return False


def call_query_rewriting(
    user_input: str,
    history: list[dict],
    client: OpenAI,
    fallback_topic: str = "",
) -> tuple[str, str, list[str]]:
    """话题提取 + 查询改写 → (math_topic, rewritten_query, key_concepts)"""
    inherit = (
        f"如果当前问题是上一话题的延续（回答、追问、直接解释），math_topic 必须与「{fallback_topic}」相同。"
        if fallback_topic
        else ""
    )
    system = (
        "提取当前问题的数学话题和检索关键词。\n"
        f"{inherit}\n"
        '输出 JSON：{"math_topic": "标准化话题名", "rewritten_query": "检索关键词，多个用空格分隔", "key_concepts": ["概念1", "概念2"]}'
    )
    parts = [f"## 当前问题\n{user_input}"]
    h = _fmt_history(history)
    if h:
        parts.append(f"## 对话历史\n{h}")
    parts.append(
        "注意：如果当前问题不含数学术语（如「继续」「然后呢」「直接解释」），rewritten_query 要用上一轮话题。"
    )
    try:
        resp = client.chat.completions.create(
            model=config.llm_model,
            messages=[
                {"role": "system", "content": system},
                {"role": "user", "content": "\n\n".join(parts)},
            ],
            temperature=0.3,
            max_tokens=512,
        )
        raw = _extract_json(resp.choices[0].message.content or "")
        topic = raw.get("math_topic", "") or ""
        query = raw.get("rewritten_query", "") or ""
        concepts = raw.get("key_concepts", []) or []

        # topic 降级指令兜底
        if not topic or topic in ("无", "直接解释", "详细讲解", "讲解"):
            topic = fallback_topic
        # query 退化为 topic
        if not query or not any(kw in query for kw in MATH_SIGNALS):
            query = fallback_topic or query or user_input
        return topic, query, concepts
    except Exception as e:
        logger.error(f"query_rewriting 失败: {e}")
        return fallback_topic, fallback_topic or user_input, []


def build_tutor_prompt(
    user_input: str,
    history: list[dict],
    knowledge_text: str,
    needs_explain: bool = False,
    tool_result: str = "",
    current_topic: str = "",
) -> tuple[str, str]:
    # scaffold 模式：直接讲解，不输出 JSON，不需要工具
    if needs_explain:
        topic_line = (
            f"当前话题是「{current_topic}」，请围绕此话题讲解。"
            if current_topic
            else "围绕学生的问题直接讲解。"
        )
        system = (
            "你是初中数学辅导老师。按以下流程执行。\n\n"
            "## 【流程】直接讲解模式\n"
            "1. 禁止反问学生任何问题，禁止出题\n"
            "2. 用「以下是解释：」开头，直接讲解当前知识点\n"
            f"3. {topic_line}\n"
            "4. 直接输出讲解文本，不要 JSON 格式。"
        )
        user_parts = [
            f"## 学生最新问题\n{user_input}",
            f"## 对话历史\n{_format_history(history)}",
            f"## 相关知识\n{knowledge_text or '无'}",
        ]
        return system, "\n\n".join(user_parts)

    # normal 模式：苏格拉底引导，文本输出（工具调用走 API tool_calls，无需 JSON）
    system = (
        "你是苏格拉底式初中数学辅导老师。\n"
        "- 如果上一轮有工具返回了结果（_tool_result），先解释那个结果\n"
        "- 采用苏格拉底引导法：通过提问让学生自己发现结论，不直接给答案\n"
        "- 需要工具时请调用函数，否则直接输出你的引导问题。"
    )

    user_parts = [
        f"## 学生最新问题\n{user_input}",
        f"## 对话历史\n{_format_history(history)}",
        f"## 相关知识\n{knowledge_text or '无'}",
    ]
    if tool_result:
        user_parts.append(
            f"## 工具返回结果\n{tool_result}\n请向学生解释以上结果，不要再次调用工具。"
        )
    user = "\n\n".join(user_parts)

    return system, user


def _format_history(history: list[dict]) -> str:
    if not history:
        return "无"
    lines = [
        f"{'学生' if t['role']=='user' else '老师'}: {t.get('content', '')[:200]}"
        for t in history[-8:]
    ]
    return "\n".join(lines)


def call_generate_tutoring(
    user_input: str,
    history: list[dict],
    knowledge_chunks: list,
    client: OpenAI,
    needs_explain: bool = False,
    tool_result: str = "",
    current_topic: str = "",
) -> TutorOutput:
    """苏格拉底导引"""
    knowledge_text = _format_knowledge(knowledge_chunks)
    system, user = build_tutor_prompt(
        user_input,
        history,
        knowledge_text,
        needs_explain=needs_explain,
        tool_result=tool_result,
        current_topic=current_topic,
    )

    # scaffold 模式：直接讲解，原始文本输出（不解析 JSON）
    if needs_explain:
        try:
            resp = client.chat.completions.create(
                model=config.llm_model,
                messages=[
                    {"role": "system", "content": system},
                    {"role": "user", "content": user},
                ],
                temperature=0.7,
                max_tokens=1024,
            )
            raw = resp.choices[0].message.content or ""
        except Exception as e:
            logger.error(f"scaffold chat 失败: {e}")
            raw = "以下是解释：\n" + user_input
        return TutorOutput(socratic_question=raw)

    # normal 模式：JSON 输出 + tools
    try:
        kwargs = dict(
            model=config.llm_model,
            messages=[
                {"role": "system", "content": system},
                {"role": "user", "content": user},
            ],
            temperature=0.7,
            max_tokens=2048,
        )
        if TOOLS:
            kwargs["tools"] = TOOLS

        resp = client.chat.completions.create(**kwargs)
        msg = resp.choices[0].message

        # tool_calls
        if msg.tool_calls:
            tc = msg.tool_calls[0].function
            return TutorOutput(
                socratic_question=msg.content or f"正在调用{tc.name}...",
                tool_call=tc.name,
            )

        # 直接文本输出
        return TutorOutput(
            socratic_question=msg.content or "",
            tool_call="",
        )
    except Exception as e:
        logger.error(f"call_generate_tutoring 失败: {e}")
        return TutorOutput(
            socratic_question="你能告诉我你目前对这个问题的理解吗？",
        )


def _format_knowledge(chunks: list) -> str:
    """格式化知识块为文本"""
    parts = []
    total = 0
    for c in chunks:
        text = f"## {getattr(c, 'title', '')}\n{getattr(c, 'content', str(c))[:300]}"
        if total + len(text) > 800:
            break
        parts.append(text)
        total += len(text)
    return "\n\n".join(parts) if parts else "无"


def _extract_json(text: str) -> dict:
    """从 LLM 回复中提取 JSON，处理代码块包裹、prompt 内容混杂等不规范的格式"""
    text = text.strip()
    if text.startswith("{"):
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            pass
    if "```json" in text:
        text = text.split("```json")[1].split("```")[0].strip()
    elif "```" in text:
        text = text.split("```")[1].strip()
    else:
        start = text.find("{")
        end = text.rfind("}")
        if start != -1 and end != -1 and end > start:
            text = text[start : end + 1]
    try:
        return json.loads(text)
    except json.JSONDecodeError as e:
        raise ValueError(f"无法从 LLM 回复中提取 JSON:\n{text}") from e
