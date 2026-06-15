"""Milvus 向量数据库封装"""

import logging

from pymilvus import (
    connections,
    Collection,
    CollectionSchema,
    FieldSchema,
    DataType,
    utility,
)

from config import config
from models.schemas import KnowledgeChunk

logger = logging.getLogger("xuetupt")


class MilvusStore:
    """Milvus 向量数据库封装，用于存储和检索知识块的向量表示"""

    def __init__(self):
        self.collection_name = config.milvus_collection
        self.dim = config.embedding_dim
        self.collection = None
        self._connect()

    def _connect(self):
        """连接 Milvus 数据库并初始化集合"""
        try:
            connections.connect(
                alias="default",
                host=config.milvus_host,
                port=config.milvus_port,
            )
            self._init_collection()
            logger.info(f"Milvus 已连接: {config.milvus_host}:{config.milvus_port}")
        except Exception as e:
            logger.error(f"Milvus 连接失败: {e}")
            raise

    def _init_collection(self):
        """检查集合是否存在，若不存在则创建"""
        if utility.has_collection(self.collection_name):
            self.collection = Collection(self.collection_name)
            self.collection.load()
            logger.info(
                f"Milvus 集合已存在: {self.collection_name}, 条目数: {self.collection.num_entities}"
            )
            return

        # 创建集合
        schema = CollectionSchema(
            [
                FieldSchema(
                    name="id", dtype=DataType.INT64, is_primary=True, auto_id=True
                ),
                FieldSchema(name="title", dtype=DataType.VARCHAR, max_length=256),
                FieldSchema(name="content", dtype=DataType.VARCHAR, max_length=2048),
                FieldSchema(name="source", dtype=DataType.VARCHAR, max_length=128),
                FieldSchema(name="vector", dtype=DataType.FLOAT_VECTOR, dim=self.dim),
            ]
        )
        self.collection = Collection(self.collection_name, schema)
        self.collection.create_index(
            "vector",
            {
                "index_type": "HNSW",
                "metric_type": "COSINE",
                "params": {"M": 16, "efConstruction": 200},
            },
        )
        self.collection.load()
        logger.info(f"Milvus 集合已创建: {self.collection_name}")

    def insert(self, chunks: list[KnowledgeChunk], embeddings: list[list[float]]):
        """将知识块和对应的向量插入 Milvus 集合"""
        if not chunks:
            return
        self.collection.insert(
            [
                [c.title for c in chunks],
                [c.content for c in chunks],
                [c.source for c in chunks],
                embeddings,
            ]
        )
        self.collection.flush()
        logger.info(f"Milvus 插入 {len(chunks)} 条")

    def search(self, query_vec: list[float], top_k: int = 3) -> list[KnowledgeChunk]:
        """根据查询向量搜索相似的知识块"""
        hits = self.collection.search(
            data=[query_vec],
            anns_field="vector",
            param={"metric_type": "COSINE", "params": {"ef": 64}},
            limit=top_k,
            output_fields=["title", "content", "source"],
        )[0]
        return [
            KnowledgeChunk(
                title=h.entity.get("title"),
                content=h.entity.get("content"),
                source=h.entity.get("source", ""),
                similarity_score=round(h.score, 4),
            )
            for h in hits
        ]

    def count(self) -> int:
        """返回集合中的实体数量"""
        return self.collection.num_entities

    def drop(self):
        """删除集合及其所有数据"""
        utility.drop_collection(self.collection_name)
        logger.info(f"Milvus 集合已删除: {self.collection_name}")
