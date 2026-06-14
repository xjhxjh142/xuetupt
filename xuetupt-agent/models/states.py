from enum import Enum

class IntentType(str, Enum):
    PROBLEM_SOLVE = "PROBLEM_SOLVE"
    CONCEPT_EXPLAIN = "CONCEPT_EXPLAIN"
    GREETING = "GREETING"
    CLARIFY = "CLARIFY"
    UNKNOWN = "UNKNOWN"
