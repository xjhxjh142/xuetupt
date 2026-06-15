"""LangGraph Node 函数工厂"""

import logging
from concurrent.futures import ThreadPoolExecutor

from openai import OpenAI

from models.schemas import AnalysisResult, RetrievalResult, TutorOutput
from models.state_types import TutorState
from core.rule_filter import RuleFilter
from core.retriever import Retriever
from core.agent import (
    call_generate_tutoring,
    call_intent_analysis,
    call_degrade_judgment,
    call_query_rewriting,
)
from core.tool import (
    call_calculator,
    call_plotter,
    call_formula,
    call_escalate,
)

logger = logging.getLogger("xuetupt")


def create_nodes(client: OpenAI):
    rule_filter = RuleFilter()
    retriever = Retriever()

    def entry_node(state: TutorState) -> dict:
        user_msg = {
            "role": "user",
            "content": state["user_input"],
        }
        logger.info(f"entry_node: '{state['user_input']}'")
        return {
            "history": [user_msg],
            "final_output": "",
            "tool_result": "",
        }

    def rule_node(state: TutorState) -> dict:
        rule_result = rule_filter.filter(state["user_input"])
        logger.info(f"rule_node: {rule_result}")
        return {"rule_result": rule_result}

    def analyze_node(state: TutorState) -> dict:
        user_input = state["user_input"]
        history = state.get("history", [])
        fallback_topic = state.get("current_topic", "")

        # 3个分析节点并行执行
        with ThreadPoolExecutor(max_workers=3) as pool:
            # 意图分析、降级判断、查询重写三个任务同时提交
            fut_intent = pool.submit(call_intent_analysis, user_input, history, client)
            fut_degrade = pool.submit(
                call_degrade_judgment, user_input, history, client
            )
            fut_rewrite = pool.submit(
                call_query_rewriting, user_input, history, client, fallback_topic
            )
            math_verify, verify_reason = fut_intent.result()
            needs_explain = fut_degrade.result()
            math_topic, rewritten_query, key_concepts = fut_rewrite.result()

        # degrade 时强制是数学问题
        if needs_explain:
            math_verify = True

        intent = AnalysisResult(
            math_verify=math_verify,
            verify_reason=verify_reason,
            needs_explain=needs_explain,
            math_topic=math_topic,
            rewritten_query=rewritten_query,
            key_concepts=key_concepts,
        )

        logger.info(f"analyze_node: {intent}")
        return {
            "analyze_result": intent,
            "current_topic": intent.math_topic,
        }

    def tutor_node(state: TutorState) -> dict:
        retrieval = state.get("retrieval")
        chunks = retrieval.chunks if retrieval else []
        intent = state.get("analyze_result")
        output = call_generate_tutoring(
            user_input=state["user_input"],
            history=state.get("history", []),
            knowledge_chunks=chunks,
            needs_explain=intent.needs_explain if intent else False,
            tool_result=state.get("tool_result", ""),
            client=client,
            current_topic=state.get("current_topic", ""),
        )
        logger.info(f"tutor_node: {output}")
        return {"tutor_output": output}

    def reject_node(state: TutorState) -> dict:
        fr = state.get("rule_result")
        reason = fr.reason if fr else "非数学问题"
        text = (
            "我是初中数学辅导老师，只能解答数学相关的问题。\n"
            f"（{reason}）\n\n要不要换个数学问题来探讨？比如关于方程、几何、函数等。"
        )
        return {"final_output": text}

    """工具函数"""

    def retrieve_node(state: TutorState) -> dict:
        intent = state.get("analyze_result")
        retrieval = retriever.retrieve(intent) if intent else RetrievalResult()
        logger.info(f"retrieve_node: {retrieval}")
        result_text = ""
        for c in retrieval.chunks[:3]:
            result_text += (
                f"\n■ {getattr(c, 'title', '')}：{getattr(c, 'content', '')[:200]}"
            )
        logger.info(f"retrieve_node: {result_text}")
        return {"retrieval": retrieval, "tool_result": result_text}

    def calculator_node(state: TutorState) -> dict:
        text = call_calculator(state.get("user_input", ""))
        logger.info(f"calculator_node: {text}")
        return {
            "tutor_output": TutorOutput(socratic_question=text),
            "tool_result": text,
        }

    def plotter_node(state: TutorState) -> dict:
        func = state.get("user_input", "")
        text, img = call_plotter(func)
        result = {
            "tutor_output": TutorOutput(socratic_question=text),
            "tool_result": text,
        }
        if img:
            result["image_base64"] = img
        logger.info(f"plotter_node: {result}")
        return result

    def formula_node(state: TutorState) -> dict:
        text = call_formula(state.get("user_input", ""))
        logger.info(f"formula_node: {text}")
        return {
            "tutor_output": TutorOutput(socratic_question=text),
            "tool_result": text,
        }

    def finalize_node(state: TutorState) -> dict:
        text = state.get("final_output", "")
        output = state.get("tutor_output")
        if not text and output and getattr(output, "socratic_question", ""):
            text = output.socratic_question
        if not text:
            text = "请继续你的思考，告诉我你的想法。"
        logger.info(f"finalize_node: {text}")
        return {
            "final_output": text,
            "history": [{"role": "assistant", "content": text}],
        }

    return {
        "entry": entry_node,
        "rule": rule_node,
        "analyze": analyze_node,
        "retrieve": retrieve_node,
        "tutor": tutor_node,
        "reject": reject_node,
        "calculator": calculator_node,
        "plotter": plotter_node,
        "formula": formula_node,
        "finalize": finalize_node,
    }
