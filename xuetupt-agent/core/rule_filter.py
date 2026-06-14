"""规则过滤器：在调用 LLM 之前快速拦截非数学问题"""

import re

from models.schemas import RuleResult

# ── 数学关键词白名单（初中 7-9 年级）──
MATH_WHITELIST = {
    # 代数
    "方程",
    "不等式",
    "函数",
    "代数",
    "未知数",
    "变量",
    "系数",
    "一次",
    "二次",
    "正比例",
    "反比例",
    "抛物线",
    "顶点",
    "对称轴",
    "平方根",
    "算术平方根",
    "立方根",
    "实数",
    "有理数",
    "无理数",
    "质数",
    "素数",
    "合数",
    "整式",
    "分式",
    "因式分解",
    "提公因式",
    "公式法",
    "十字相乘法",
    "幂",
    "指数",
    "科学记数法",
    "近似数",
    # 几何
    "三角形",
    "四边形",
    "平行四边形",
    "矩形",
    "菱形",
    "正方形",
    "梯形",
    "圆",
    "圆心",
    "半径",
    "直径",
    "弦",
    "弧",
    "扇形",
    "内角和",
    "外角",
    "对顶角",
    "邻补角",
    "同位角",
    "内错角",
    "同旁内角",
    "全等",
    "相似",
    "位似",
    "轴对称",
    "中心对称",
    "平移",
    "旋转",
    "勾股定理",
    "勾股",
    "直角",
    "锐角",
    "钝角",
    "三角函数",
    "中线",
    "高线",
    "角平分线",
    "中位线",
    "垂直平分线",
    "面积",
    "体积",
    "表面积",
    "周长",
    "容积",
    # 概率统计
    "概率",
    "频率",
    "统计",
    "平均数",
    "中位数",
    "众数",
    "方差",
    "极差",
    "条形图",
    "扇形图",
    "折线图",
    "直方图",
    # 综合
    "运算",
    "计算",
    "化简",
    "求解",
    "证明",
    "验证",
    "比",
    "比例",
    "百分比",
    "正数",
    "负数",
    "绝对值",
    "数轴",
    "坐标系",
    "命题",
    "定理",
    "定义",
    "公理",
    "相交线",
    "平行线",
    "垂线",
    "垂足",
    "应用题",
    "行程",
    "工程",
    "利润",
    "浓度",
    "年龄",
}

# ── 问候/闲聊关键词（直接拦截，不走 LLM）──
GREETING_KEYWORDS = {
    "你好",
    "您好",
    "hello",
    "hi",
    "hey",
    "嗨",
    "早上好",
    "中午好",
    "晚上好",
    "晚安",
    "你是谁",
    "你叫什么",
    "谢谢",
    "感谢",
    "谢谢你",
    "再见",
    "拜拜",
    "在吗",
    "在不在",
    "有空吗",
}

# ── 正则模式 ──
URL_PATTERN = re.compile(r"https?://\S+|www\.\S+")
CODE_BLOCK_PATTERN = re.compile(r"```[\s\S]*?```")
FILE_PATH_PATTERN = re.compile(r"[A-Za-z]:\\[^\s]+")


# ── 降级关键词（L1 规则拦截，不走 LLM）──
DEGRADE_KEYWORDS = {
    "直接解释",
    "讲给我听",
    "告诉我答案",
    "教教我",
    "听不懂",
    "什么意思",
    "解释一下",
    "直接说",
    "怎么算",
    "不明白",
    "不会",
    "太难",
    "答案是什么",
    "怎么做",
    "给我讲",
    "说明一下",
}


class RuleFilter:
    """基于规则的输入过滤器"""

    def filter(self, text: str) -> RuleResult:
        """过滤输入，判断是否为数学问题"""
        text = text.strip()

        # 1. 正则拦截
        for pattern, name in [
            (URL_PATTERN, "URL检测"),
            (CODE_BLOCK_PATTERN, "代码块检测"),
            (FILE_PATH_PATTERN, "文件路径检测"),
        ]:
            if pattern.search(text):
                return RuleResult(
                    passed=False,
                    reason=f"匹配到{name}，非数学问题",
                    rules=[name],
                )

        # 2. 降级关键词检测
        degrade_hit = any(kw in text for kw in DEGRADE_KEYWORDS)
        if degrade_hit:
            return RuleResult(
                passed=True,
                reason="命中降级关键词",
                rules=["降级关键词"] + [kw for kw in DEGRADE_KEYWORDS if kw in text],
            )

        # 3. 白名单检测
        words = set(_tokenize(text))
        white_hits = words & MATH_WHITELIST
        if white_hits:
            return RuleResult(
                passed=True,
                reason=f"命中数学关键词：{', '.join(sorted(white_hits)[:5])}",
                rules=["白名单"] + list(white_hits),
            )

        # 4. 问候检测
        text_lower = text.lower()
        for kw in GREETING_KEYWORDS:
            if kw in text_lower:
                return RuleResult(
                    passed=False,
                    reason=f"检测到问候/闲聊关键词：{kw}",
                    rules=["问候检测", kw],
                )

        # 5. 兜底：未识别但含数字/字母，放行让 LLM 判断
        if re.search(r"[a-zA-Z0-9+\-=]", text):
            return RuleResult(
                passed=True,
                reason="包含数学符号",
                rules=["符号兜底"],
            )
        return RuleResult(
            passed=True,
            reason="交由 LLM 判断",
            rules=["未识别兜底"],
        )


def _tokenize(text: str) -> list[str]:
    """简易中文分词：按字符和常见双字词拆分"""
    tokens = set()
    # 双字滑动窗口
    for i in range(len(text) - 1):
        bigram = text[i : i + 2]
        if "一" <= bigram[0] <= "鿿" or "一" <= bigram[1] <= "鿿":
            tokens.add(bigram)
    # 单字
    for ch in text:
        if "一" <= ch <= "鿿":
            tokens.add(ch)
    # 英文单词（简单按空格和标点拆分）
    for word in re.findall(r"[a-zA-Z_]\w*", text):
        tokens.add(word.lower())
    return list(tokens)
