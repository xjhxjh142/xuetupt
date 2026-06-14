# 学途AI — 苏格拉底式数学辅导系统

基于 LangGraph 构建的多 Agent 并行推理系统，为初中数学提供苏格拉底式多轮引导辅导，支持多工具自主调度与混合检索增强生成。

## 架构

```
用户输入 → 规则过滤 → 多 Agent 并行分析 → 苏格拉底辅导 → 输出
                │          ├─ 意图分析 LLM
                │          ├─ 降级判断 LLM
                │          └─ 查询改写 LLM
                          ↓
                    工具调度循环 ← 计算器/绘图/公式/知识库
                     (Function Calling)
```

- **多 Agent 并行编排**：分析阶段拆为三个独立 LLM 调用，`ThreadPoolExecutor` 并行执行，延迟降低 60%
- **自适应教学策略**：LLM 自动判断学生状态，在苏格拉底引导与直接讲解模式间切换
- **多工具协同**：通过 Function Calling 自主调度计算器、绘图、公式查询、知识库检索，正则规则兜底异常

## 技术栈

| 模块 | 技术 |
|------|------|
| Agent 框架 | LangGraph (StateGraph) |
| LLM | 智谱 GLM-4-Flash（默认）/ Ollama 本地模型 |
| 检索 | BM25 + bge-m3 向量 + RRF 融合 + Cross-Encoder 重排序 |
| 视觉 | minicpm-v（拍照识别数学题目） |
| 追踪 | LangSmith 全链路可观测 |
| 后端 | FastAPI |
| 前端 | Vue 3 + Vite |

## 快速开始

### 1. 环境要求

- Python 3.10+
- Ollama（可选，用于视觉识别和本地模型）

### 2. 安装

```bash
cd xuetupt-agent
pip install -r requirements.txt  # 或手动安装依赖
```

### 3. 配置

复制配置文件并填入 API Key：

```bash
cp config.py config.local.py
# 编辑 config.py，设置 LLM_API_KEY（智谱 GLM）
```

或通过环境变量传入：

```bash
export LLM_API_KEY="your_zhipu_api_key"
export LANGSMITH_API_KEY="your_langsmith_key"  # 可选，用于追踪
```

### 4. 启动后端

```bash
cd xuetupt-agent
python main.py
# 服务运行在 http://localhost:8013
```

### 5. 启动前端（可选）

```bash
cd xuetupt-web
npm install
npm run dev
```

## 项目结构

```
xuetupt-agent/
├── core/
│   ├── agent.py          # Prompt 模板 + LLM 调用（OpenAI SDK）
│   ├── graph.py          # LangGraph StateGraph 定义
│   ├── nodes.py          # LangGraph 节点（多 Agent 并行）
│   ├── routers.py        # 路由逻辑
│   ├── rule_filter.py    # 规则过滤器
│   ├── retriever.py      # 双路召回 RAG 引擎
│   ├── tool.py           # 工具函数（计算器/绘图/公式）
│   └── vision.py         # 拍照识别
├── api/
│   ├── routes/socratic.py # FastAPI 路由
│   └── sse.py            # SSE 流式输出
├── models/
│   ├── schemas.py        # 数据模型
│   └── state_types.py    # LangGraph State 定义
├── knowledge_base/       # 初中数学知识库
└── config.py             # 配置文件
```

## License

MIT
