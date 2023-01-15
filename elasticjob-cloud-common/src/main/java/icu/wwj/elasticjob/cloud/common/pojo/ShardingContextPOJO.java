package icu.wwj.elasticjob.cloud.common.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.shardingsphere.elasticjob.api.ShardingContext;

@Getter
@Setter
@NoArgsConstructor
public class ShardingContextPOJO {
    
    private String jobName;
    
    private String taskId;
    
    private int shardingTotalCount;
    
    private String jobParameter;
    
    private int shardingItem;
    
    private String shardingParameter;
    
    public ShardingContextPOJO(ShardingContext shardingContext) {
        jobName = shardingContext.getJobName();
        taskId = shardingContext.getTaskId();
        shardingTotalCount = shardingContext.getShardingTotalCount();
        jobParameter = shardingContext.getJobParameter();
        shardingItem = shardingContext.getShardingItem();
        shardingParameter = shardingContext.getShardingParameter();
    }
    
    public ShardingContext toShardingContext() {
        return new ShardingContext(jobName, taskId, shardingTotalCount, jobParameter, shardingItem, shardingParameter);
    }
}
