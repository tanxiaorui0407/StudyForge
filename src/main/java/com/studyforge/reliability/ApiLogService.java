package com.studyforge.reliability;

import com.studyforge.entity.ApiCallLog;
import com.studyforge.mapper.ApiCallLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * API 调用日志：记录每次 LLM 调用的 token / 耗时 / 成功与否与失败原因。
 * 这是“可靠性套件”的可观测性基础，也是成本可见性（不烧冤枉 token）的依据。
 */
@Service
public class ApiLogService {

    private final ApiCallLogMapper mapper;

    public ApiLogService(ApiCallLogMapper mapper) {
        this.mapper = mapper;
    }

    public void record(String operation, String model, int inputTokens, int outputTokens,
                       long latencyMs, boolean success, String errorType, String errorMessage) {
        ApiCallLog log = new ApiCallLog();
        log.setOperation(operation);
        log.setModel(model);
        log.setInputTokens(inputTokens);
        log.setOutputTokens(outputTokens);
        log.setLatencyMs(latencyMs);
        log.setSuccess(success);
        log.setErrorType(errorType);
        log.setErrorMessage(errorMessage);
        mapper.insert(log);
    }

    public List<ApiCallLog> recent(int limit) {
        return mapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<ApiCallLog>lambdaQuery()
                        .orderByDesc(ApiCallLog::getId)
                        .last("LIMIT " + Math.max(1, Math.min(limit, 200)))
        );
    }

    /** 汇总：总调用、总 token、失败数 */
    public Summary summarize() {
        Summary s = new Summary();
        List<ApiCallLog> all = mapper.selectList(null);
        s.totalCalls = all.size();
        s.totalTokens = all.stream()
                .mapToLong(l -> (l.getInputTokens() == null ? 0 : l.getInputTokens())
                        + (l.getOutputTokens() == null ? 0 : l.getOutputTokens()))
                .sum();
        s.failures = all.stream().filter(l -> Boolean.FALSE.equals(l.getSuccess())).count();
        return s;
    }

    public static class Summary {
        public long totalCalls;
        public long totalTokens;
        public long failures;
    }
}
