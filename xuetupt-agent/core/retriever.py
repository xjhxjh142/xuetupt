"""
RAG 检索器：双路召回 + RRF 融合 + Cross-Encoder 重排序
"""

from typing import Optional

from knowledge_base import KnowledgeBase
from models.schemas import AnalysisResult, RetrievalResult, KnowledgeChunk
from config import config


class Retriever:
    """知识检索器（BM25 + 向量双路召回 → RRF 融合 → CE 重排序）"""

    RRF_K = 60

    def __init__(self, kb: Optional[KnowledgeBase] = None):
        self._kb = kb or KnowledgeBase()

    def load(self):
        self._kb.load()

    def retrieve(self, intent: AnalysisResult, top_k: int = 0) -> RetrievalResult:
        """双路召回 + RRF + 重排序"""
        if not self._kb.chunks:
            self.load()

        top_k = top_k or config.retriever_top_k
        query = intent.rewritten_query or intent.math_topic or ""
        if not query.strip() and intent.key_concepts:
            query = " ".join(intent.key_concepts)
        if not query.strip():
            return RetrievalResult(chunks=[], total_chunks=0)

        # BM25 关键词
        bm25_chunks = self._kb.bm25_retrieve(query, top_k=top_k * 2)

        # 向量语义
        vector_chunks = self._kb.retrieve(query, top_k=top_k * 2)

        # RRF 融合
        fused = self._rrf_fuse(bm25_chunks, vector_chunks, top_k=top_k * 2)

        reranked = fused[:top_k]

        if not reranked:
            reranked = [
                KnowledgeChunk(
                    title=f"关于 {intent.math_topic or '数学'}",
                    content="未在知识库中找到相关条目。",
                    similarity_score=0.0,
                )
            ]

        return RetrievalResult(
            chunks=reranked, total_chunks=len(bm25_chunks) + len(vector_chunks)
        )

    def _rrf_fuse(self, list_a, list_b, top_k):
        """RRF 融合"""
        seen = {}
        for i, c in enumerate(list_a):
            seen[id(c)] = 1.0 / (self.RRF_K + i + 1)
        for i, c in enumerate(list_b):
            cid = id(c)
            seen[cid] = seen.get(cid, 0) + 1.0 / (self.RRF_K + i + 1)
        deduped = {}
        for c in list_a + list_b:
            if id(c) not in deduped:
                deduped[id(c)] = c
        merged = sorted(deduped.values(), key=lambda c: -seen.get(id(c), 0))
        return merged[:top_k]
