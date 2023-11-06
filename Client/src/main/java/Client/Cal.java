package Client;

import java.util.Collections;
import java.util.List;

public class Cal {
    private final List<Long> latencies;

    public Cal(List<Long> latencies) {
        Collections.sort(latencies);
        this.latencies = latencies;
    }

    // 计算平均响应时间
    public double getMeanResponseTime() {
        return this.latencies.stream().mapToDouble(latency -> latency).average().orElse(0.0);
    }

    // 计算中位数响应时间
    public double getMedianResponseTime() {
        int size = this.latencies.size();
        if (size % 2 == 0) {
            return (this.latencies.get(size / 2) + this.latencies.get(size / 2)) / 2.0;
        }

        return this.latencies.get(size / 2);
    }

    // 获取最大响应时间
    public double getMax() {
        return this.latencies.get(this.latencies.size() - 1);
    }

    // 获取最小响应时间
    public double getMin() {
        return this.latencies.get(0);
    }

    // 获取指定百分位的响应时间
    public long getPercentile(int percentile) {
        int size = this.latencies.size();
        int percentileIndex = (int) Math.ceil((percentile / 100.0) * size);

        return this.latencies.get(percentileIndex);
    }
}
