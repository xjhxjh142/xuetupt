from dataclasses import dataclass, field


@dataclass
class KnowledgeChunk:
    title: str = ""
    content: str = ""
    source: str = ""
    similarity_score: float = 0.0
    socratic_clues: list = field(default_factory=list)


@dataclass
class RuleResult:
    passed: bool = True
    reason: str = ""
    rules: list = field(default_factory=list)


@dataclass
class AnalysisResult:
    math_verify: bool = True
    verify_reason: str = ""
    needs_explain: bool = False
    math_topic: str = ""
    rewritten_query: str = ""
    key_concepts: list = field(default_factory=list)


@dataclass
class RetrievalResult:
    chunks: list = field(default_factory=list)
    total_chunks: int = 0


@dataclass
class TutorOutput:
    socratic_question: str = ""
    tool_call: str = ""
