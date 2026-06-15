"""系统配置"""

import os
from dataclasses import dataclass, field
from pathlib import Path


@dataclass
class _Config:
    # LLM 配置（默认智谱AI GLM-4-Flash，永久免费）
    llm_provider: str = os.getenv("LLM_PROVIDER", "zhipu")
    llm_api_key: str = os.getenv("LLM_API_KEY", "")
    llm_model: str = os.getenv("LLM_MODEL", "glm-4-flash")
    llm_base_url: str = os.getenv(
        "LLM_BASE_URL", "https://open.bigmodel.cn/api/paas/v4/"
    )

    # 路径
    project_root: Path = Path(__file__).parent
    kb_dir: Path = field(
        default_factory=lambda: Path(__file__).parent / "knowledge_base" / "data"
    )
    embedding_cache_dir: Path = field(
        default_factory=lambda: Path(__file__).parent / "knowledge_base" / "embeddings"
    )

    # RAG
    embedding_base_url: str = os.getenv("EMBEDDING_BASE_URL", "http://localhost:11434")
    embedding_model: str = os.getenv("EMBEDDING_MODEL", "bge-m3:latest")
    retriever_top_k: int = 3

    # 向量数据库（Milvus）
    milvus_host: str = os.getenv("MILVUS_HOST", "192.168.98.100")
    milvus_port: str = os.getenv("MILVUS_PORT", "19530")
    milvus_collection: str = "math_kb"
    embedding_dim: int = 1024

    # 对话
    max_turns_per_session: int = 30
    stuck_threshold: int = 3

    # LangSmith 追踪
    langsmith_key: str = os.getenv("LANGSMITH_API_KEY", "")
    langsmith_project: str = os.getenv("LANGSMITH_PROJECT", "xuetupt")


config = _Config()
