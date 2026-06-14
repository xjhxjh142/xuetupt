"""知识库：加载 + embedding 生成 + 检索（支持 BM25、向量、关键词）"""

import hashlib
import json
import math
import re
from pathlib import Path
from typing import Optional

import requests
from config import config
from models.schemas import KnowledgeChunk


class KnowledgeBase:
    def __init__(self, kb_dir: Optional[Path] = None):
        self.kb_dir = kb_dir or config.kb_dir
        self.cache_dir = config.embedding_cache_dir
        self.cache_dir.mkdir(parents=True, exist_ok=True)
        self.chunks: list[KnowledgeChunk] = []
        self.embeddings: list[list[float]] = []
        self._loaded = False
        # BM25 预计算数据
        self._bm25_data = None

    def load(self, force_reload: bool = False):
        if self._loaded and not force_reload:
            return
        self.chunks = self._load_documents()
        self.embeddings = self._load_or_compute_embeddings(force_reload)
        self._bm25_data = self._build_bm25_index()
        self._loaded = True

    def _load_documents(self) -> list[KnowledgeChunk]:
        chunks = []
        if not self.kb_dir.exists():
            return chunks
        for md_file in sorted(self.kb_dir.glob("*.md")):
            text = md_file.read_text(encoding="utf-8")
            sections = re.split(r"\n##\s+", text)
            title_base = md_file.stem
            for i, sec in enumerate(sections):
                if not sec.strip():
                    continue
                lines = sec.strip().split("\n")
                sec_title = lines[0].strip() if i > 0 else title_base
                content = "\n".join(lines[1:]) if i > 0 else sec.strip()
                clues = []
                for line in lines:
                    if line.startswith("- 引导") or line.startswith("- Q:"):
                        clues.append(line.lstrip("- "))
                chunk = KnowledgeChunk(
                    title=f"{title_base} - {sec_title}",
                    content=content[:500],
                    source=md_file.name,
                    socratic_clues=clues,
                )
                chunks.append(chunk)
        return chunks

    def _ollama_embed(self, texts: list[str]) -> list[list[float]]:
        """调用 Ollama API 获取文本向量表示"""
        resp = requests.post(
            f"{config.llm_base_url}/api/embed",
            json={"model": config.embedding_model, "input": texts},
            timeout=120,
        )
        resp.raise_for_status()
        return resp.json()["embeddings"]

    def _content_hash(self) -> str:
        combined = "".join(c.content for c in self.chunks)
        return hashlib.md5(combined.encode()).hexdigest()[:12]

    def _load_or_compute_embeddings(self, force: bool = False) -> list[list[float]]:
        if not self.chunks:
            return []
        cache_path = self.cache_dir / f"emb_{self._content_hash()}.json"
        if cache_path.exists() and not force:
            with open(cache_path, "r", encoding="utf-8") as f:
                return json.load(f)
        try:
            texts = [f"{c.title}: {c.content}" for c in self.chunks]
            emb = self._ollama_embed(texts)
            with open(cache_path, "w", encoding="utf-8") as f:
                json.dump(emb, f)
            return emb
        except Exception as e:
            return []

    def retrieve(self, query: str, top_k: int = 0) -> list[KnowledgeChunk]:
        """向量语义检索"""
        if not self.chunks:
            return []
        top_k = top_k or config.retriever_top_k
        if not self.embeddings:
            return self._keyword_retrieve(query, top_k)
        try:
            query_vec = self._ollama_embed([query])[0]
        except Exception:
            return self._keyword_retrieve(query, top_k)
        scores = []
        for emb in self.embeddings:
            dot = sum(a * b for a, b in zip(query_vec, emb))
            norm = (sum(a * a for a in query_vec) ** 0.5) * (
                sum(b * b for b in emb) ** 0.5
            ) + 1e-10
            scores.append(dot / norm)
        top_indices = sorted(range(len(scores)), key=lambda i: scores[i], reverse=True)[
            :top_k
        ]
        results = []
        for idx in top_indices:
            chunk = self.chunks[idx]
            chunk.similarity_score = round(float(scores[idx]), 4)
            results.append(chunk)
        return results

    # ── BM25 相关 ──

    def _tokenize(self, text: str) -> list[str]:
        """分词：中文双字滑动窗口 + 英文单词 + 数学符号"""
        tokens = []
        # 中文连续字
        for match in re.finditer(r"[一-鿿]+", text):
            s = match.group()
            for i in range(len(s)):
                tokens.append(s[i])  # 单字
            for i in range(len(s) - 1):
                tokens.append(s[i : i + 2])  # 双字
        # 英文单词和数字
        for match in re.finditer(r"[a-zA-Z0-9\+\-\*/=<>^²³√∑∫∠△⊥π]+", text):
            tokens.append(match.group().lower())
        return tokens

    def _build_bm25_index(self) -> dict:
        """构建 BM25 倒排索引"""
        if not self.chunks:
            return {}
        k1, b = 1.5, 0.75
        doc_texts = [
            self._tokenize(f"{c.title} {c.content} {c.source}") for c in self.chunks
        ]
        doc_lens = [len(t) for t in doc_texts]
        avgdl = sum(doc_lens) / len(doc_lens) if doc_lens else 1

        # 倒排索引：term → {doc_id: tf}
        inverted: dict[str, dict[int, int]] = {}
        for i, tokens in enumerate(doc_texts):
            for t in set(tokens):
                if t not in inverted:
                    inverted[t] = {}
                inverted[t][i] = inverted[t].get(i, 0) + 1

        # 预计算 IDF
        n_docs = len(self.chunks)
        idf: dict[str, float] = {}
        for term, postings in inverted.items():
            df = len(postings)
            idf[term] = math.log((n_docs - df + 0.5) / (df + 0.5) + 1.0)

        return {
            "k1": k1,
            "b": b,
            "avgdl": avgdl,
            "doc_lens": doc_lens,
            "inverted": inverted,
            "idf": idf,
        }

    def bm25_retrieve(self, query: str, top_k: int = 3) -> list[KnowledgeChunk]:
        """BM25 关键词检索"""
        if not self._bm25_data:
            return self._keyword_retrieve(query, top_k)
        q_tokens = self._tokenize(query)
        if not q_tokens:
            return self._keyword_retrieve(query, top_k)

        data = self._bm25_data
        k1, b = data["k1"], data["b"]
        avgdl = data["avgdl"]
        doc_lens = data["doc_lens"]
        inverted = data["inverted"]
        idf = data["idf"]
        n_docs = len(self.chunks)

        scores = [0.0] * n_docs
        for term in set(q_tokens):
            if term not in idf:
                continue
            w_idf = idf[term]
            postings = inverted.get(term, {})
            for doc_id, tf in postings.items():
                dl = doc_lens[doc_id]
                scores[doc_id] += (
                    w_idf * (tf * (k1 + 1)) / (tf + k1 * (1 - b + b * dl / avgdl))
                )

        top_indices = sorted(range(n_docs), key=lambda i: scores[i], reverse=True)[
            :top_k
        ]
        results = []
        for idx in top_indices:
            chunk = self.chunks[idx]
            chunk.similarity_score = round(float(scores[idx]), 4)
            results.append(chunk)
        return results

    def _keyword_retrieve(self, query: str, top_k: int) -> list[KnowledgeChunk]:
        words = set(re.findall(r"[\w一-鿿]+", query.lower()))
        scored = []
        for chunk in self.chunks:
            text = (chunk.title + chunk.content).lower()
            matches = sum(1 for w in words if w in text)
            if matches > 0:
                scored.append((matches, chunk))
        scored.sort(key=lambda x: x[0], reverse=True)
        for _, chunk in scored[:top_k]:
            chunk.similarity_score = 1.0
        return [c for _, c in scored[:top_k]]
