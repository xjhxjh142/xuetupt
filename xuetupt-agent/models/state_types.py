from typing import TypedDict
from models.schemas import (
    RuleResult,
    AnalysisResult,
    RetrievalResult,
    TutorOutput,
)


class TutorState(TypedDict, total=False):
    user_input: str
    current_topic: str
    history: list

    rule_result: RuleResult
    analyze_result: AnalysisResult
    retrieval: RetrievalResult
    tutor_output: TutorOutput

    tool_result: str
    image_base64: str
    final_output: str
