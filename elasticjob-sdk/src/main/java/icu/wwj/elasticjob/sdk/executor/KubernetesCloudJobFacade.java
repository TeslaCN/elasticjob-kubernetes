package icu.wwj.elasticjob.sdk.executor;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.elasticjob.api.JobConfiguration;
import org.apache.shardingsphere.elasticjob.executor.JobFacade;
import org.apache.shardingsphere.elasticjob.infra.listener.ShardingContexts;
import org.apache.shardingsphere.elasticjob.tracing.JobTracingEventBus;
import org.apache.shardingsphere.elasticjob.tracing.event.JobExecutionEvent;
import org.apache.shardingsphere.elasticjob.tracing.event.JobStatusTraceEvent;

import java.util.Collection;

@RequiredArgsConstructor
public final class KubernetesCloudJobFacade implements JobFacade {
    
    private final ShardingContexts shardingContexts;
    
    private final JobConfiguration jobConfig;
    
    private final JobTracingEventBus jobTracingEventBus;
    
    @Override
    public JobConfiguration loadJobConfiguration(final boolean fromCache) {
        JobConfiguration result = JobConfiguration.newBuilder(jobConfig.getJobName(), jobConfig.getShardingTotalCount())
                .cron(jobConfig.getCron()).timeZone(jobConfig.getTimeZone()).shardingItemParameters(jobConfig.getShardingItemParameters()).jobParameter(jobConfig.getJobParameter())
                .failover(jobConfig.isFailover()).misfire(jobConfig.isMisfire()).description(jobConfig.getDescription())
                .jobExecutorServiceHandlerType(jobConfig.getJobExecutorServiceHandlerType())
                .jobErrorHandlerType(jobConfig.getJobErrorHandlerType()).build();
        result.getProps().putAll(jobConfig.getProps());
        return result;
    }
    
    @Override
    public void checkJobExecutionEnvironment() {
    }
    
    @Override
    public void failoverIfNecessary() {
    }
    
    @Override
    public void registerJobBegin(final ShardingContexts shardingContexts) {
    }
    
    @Override
    public void registerJobCompleted(final ShardingContexts shardingContexts) {
    }
    
    @Override
    public ShardingContexts getShardingContexts() {
        return shardingContexts;
    }
    
    @Override
    public boolean misfireIfRunning(final Collection<Integer> shardingItems) {
        return false;
    }
    
    @Override
    public void clearMisfire(final Collection<Integer> shardingItems) {
    }
    
    @Override
    public boolean isExecuteMisfired(final Collection<Integer> shardingItems) {
        return false;
    }
    
    @Override
    public boolean isNeedSharding() {
        return false;
    }
    
    @Override
    public void beforeJobExecuted(final ShardingContexts shardingContexts) {
    }
    
    @Override
    public void afterJobExecuted(final ShardingContexts shardingContexts) {
    }
    
    @Override
    public void postJobExecutionEvent(final JobExecutionEvent jobExecutionEvent) {
    }
    
    @Override
    public void postJobStatusTraceEvent(final String taskId, final JobStatusTraceEvent.State state, final String message) {
    }
}
