"""LangGraph 图定义"""

import logging
import os
import sqlite3
from pathlib import Path

logging.getLogger("langgraph.checkpoint.serde.jsonplus").setLevel(logging.ERROR)

from openai import OpenAI
from langgraph.graph import StateGraph, END
from langgraph.checkpoint.sqlite import SqliteSaver

from config import config
from models.state_types import TutorState
from core.routers import entry_router, rule_router, analyze_router, tutor_router


def build_tutor_graph():
    _log = logging.getLogger("xuetupt")

    # LangSmith 追踪（若有 key）
    if config.langsmith_key:
        os.environ["LANGCHAIN_TRACING_V2"] = "true"
        os.environ["LANGCHAIN_API_KEY"] = config.langsmith_key
        os.environ["LANGCHAIN_PROJECT"] = config.langsmith_project
        _log.info(f"LangSmith 已启用，项目：{config.langsmith_project}")
    else:
        _log.info("LangSmith 未配置（设置 LANGSMITH_API_KEY 启用）")

    client = OpenAI(api_key=config.llm_api_key, base_url=config.llm_base_url)
    from core.nodes import create_nodes

    nodes = create_nodes(client)

    builder = StateGraph(TutorState)

    for name, func in nodes.items():
        builder.add_node(name, func)

    # 设置入口节点
    builder.set_entry_point("entry")

    # 定义状态转移逻辑
    builder.add_conditional_edges("entry", entry_router)
    builder.add_conditional_edges("rule", rule_router)
    builder.add_conditional_edges("analyze", analyze_router)
    builder.add_edge("reject", "finalize")

    # 核心循环：tutor 自主决定调用工具或输出
    builder.add_conditional_edges("tutor", tutor_router)
    # 工具节点执行完后回到 tutor 解释结果
    builder.add_edge("retrieve", "tutor")
    builder.add_edge("calculator", "tutor")
    builder.add_edge("plotter", "tutor")
    builder.add_edge("formula", "tutor")
    builder.add_edge("finalize", END)

    _db_path = str(Path(__file__).parent.parent / "checkpoints.db")
    # 创建 SQLite 连接，设置 check_same_thread=False 以允许多线程访问
    _conn = sqlite3.connect(_db_path, check_same_thread=False)
    _conn.execute("PRAGMA journal_mode=DELETE")
    return builder.compile(
        checkpointer=SqliteSaver(_conn),
        name="xuetupt-tutor",
    )
