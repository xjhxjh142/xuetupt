"""工具调用纯函数"""

import re
import io
import base64
import logging

import sympy
import numpy as np
from sympy.parsing.sympy_parser import (
    parse_expr,
    standard_transformations,
    implicit_multiplication_application,
)

logger = logging.getLogger("xuetupt")


# ── 公式库 ──
_FORMULAS = {
    "平方差公式": "(a+b)(a-b) = a²-b²",
    "完全平方公式": "(a±b)² = a² ± 2ab + b²",
    "勾股定理": "a² + b² = c²（直角边 a、b，斜边 c）",
    "求根公式": "x = [-b ± √(b²-4ac)] / 2a（一元二次方程 ax²+bx+c=0）",
    "判别式": "Δ = b²-4ac（Δ>0 两不等实根，Δ=0 两相等实根，Δ<0 无实根）",
    "三角形面积": "S = ½ × 底 × 高；S = √[s(s-a)(s-b)(s-c)]（海伦公式）",
    "圆周长": "C = 2πr",
    "圆面积": "S = πr²",
    "平行四边形面积": "S = 底 × 高",
    "梯形面积": "S = ½ × (上底+下底) × 高",
    "扇形面积": "S = (n/360) × πr²（n 为圆心角度数）",
    "弧长": "L = (n/180) × πr",
    "正弦": "sinA = 对边/斜边",
    "余弦": "cosA = 邻边/斜边",
    "正切": "tanA = 对边/邻边",
    "方差": "s² = (1/n)Σ(xi-x̄)²",
    "等差数列求和": "Sn = n(a₁+an)/2",
    "等比数列求和": "Sn = a₁(1-qⁿ)/(1-q)（q≠1）",
}


# ── 工具调用纯函数 ──


def call_calculator(text: str) -> str:
    """从输入中提取表达式并用 sympy 精确计算"""
    try:
        # 提取纯数学表达式
        m = re.search(r"[\d\+\-\*\/\(\)\.\^xX\=\s]{3,}", text)
        expr = m.group(0).strip() if m else text
        expr = expr.replace("×", "*").replace("÷", "/").replace("＝", "=")
        trans = standard_transformations + (implicit_multiplication_application,)
        if "=" in expr:
            left, right = expr.split("=", 1)
            eq = sympy.Eq(parse_expr(left, transformations=trans),
                          parse_expr(right, transformations=trans))
            result = sympy.solve(eq)
            return f"解方程 {expr}：\n{', '.join(str(r) for r in result)}"
        parsed = parse_expr(expr, transformations=trans)
        result = sympy.simplify(parsed)
        return f"计算 {expr} 的结果：\n{expr} = {result}"
    except Exception:
        return "无法计算该表达式，请确认输入是否正确。"


def call_plotter(text: str) -> tuple[str, str]:
    """从用户输入中提取函数式并绘图，返回(文本, base64图片)"""
    try:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
        expr_str = ""
        # 1. 匹配 y=... 格式
        m = re.search(r"[yY]\s*=\s*([a-zA-Z0-9_\*\^\/\+\-\(\)]+)", text)
        if m:
            expr_str = m.group(1).strip()
        else:
            # 2. 兜底：取含运算符的最长连续 token
            parts = re.findall(r"[a-zA-Z0-9_\*\^\/\+\-\(\)]+", text)
            expr_str = max(parts, key=len) if parts else text

        expr_str = expr_str.replace("^", "**")
        x = sympy.symbols("x")
        f = sympy.lambdify(x, sympy.sympify(expr_str), "numpy")
        xv = np.linspace(-10, 10, 400)
        yv = np.where(np.abs(f(xv)) > 100, np.nan, f(xv))

        fig, ax = plt.subplots(figsize=(6, 4))
        ax.plot(xv, yv, "b-", linewidth=2)
        ax.axhline(0, color="gray", linewidth=0.5)
        ax.axvline(0, color="gray", linewidth=0.5)
        ax.grid(True, alpha=0.3)
        ax.set_title(f"y = {expr_str}")

        buf = io.BytesIO()
        fig.savefig(buf, format="png", dpi=100, bbox_inches="tight")
        raw = buf.getvalue()
        plt.close(fig)
        b64 = base64.b64encode(raw).decode()
        return f"函数 y = {expr_str} 的图像如下：", f"data:image/png;base64,{b64}"
    except Exception as e:
        logger.warning(f"call_plotter 失败: {e}")
        return f"无法绘制函数图像：{text[:30]}", ""


def call_formula(query: str) -> str:
    """公式库匹配"""
    matched = [(n, f) for n, f in _FORMULAS.items() if any(kw in query for kw in n)]
    if matched:
        return "找到以下相关公式：\n\n" + "\n".join(
            f"■ {n}\n  {f}\n" for n, f in matched
        )
    return "未找到相关公式。可尝试：平方差公式、完全平方公式、勾股定理、求根公式、三角形面积等。"


def call_escalate() -> str:
    """转人工"""
    return (
        "我理解你遇到了困难。让我来直接给你讲解一下当前问题。\n\n"
        "如果你还是觉得不太明白，建议请教你的数学老师或同学，"
        "他们可以给你更有针对性的面对面辅导。"
    )
