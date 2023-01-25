package icu.wwj.elasticjob.sdk.executor;

import icu.wwj.elasticjob.sdk.daemon.DaemonJobScheduler;
import icu.wwj.elasticjob.sdk.environment.KubernetesEnvironmentLoader;
import org.apache.shardingsphere.elasticjob.api.ElasticJob;
import org.apache.shardingsphere.elasticjob.api.JobConfiguration;
import org.apache.shardingsphere.elasticjob.executor.ElasticJobExecutor;
import org.apache.shardingsphere.elasticjob.infra.listener.ShardingContexts;
import org.apache.shardingsphere.elasticjob.tracing.JobTracingEventBus;

public final class KubernetesCloudJobExecutor {
    
    private final KubernetesEnvironmentLoader environmentLoader = new KubernetesEnvironmentLoader("/etc/elasticjob");
    
    public void execute(ElasticJob elasticJob) {
        JobConfiguration jobConfiguration = environmentLoader.loadJobConfiguration();
        ShardingContexts shardingContexts = environmentLoader.loadShardingContexts(jobConfiguration);
        KubernetesCloudJobFacade jobFacade = new KubernetesCloudJobFacade(shardingContexts, jobConfiguration, new JobTracingEventBus());
        if (environmentLoader.isTransient()) {
            new ElasticJobExecutor(elasticJob, jobConfiguration, jobFacade).execute();
        } else {
            new DaemonJobScheduler(elasticJob, null, jobConfiguration, jobFacade).schedule();
        }
    }
    
    public void execute(String elasticJobType) {
        JobConfiguration jobConfiguration = environmentLoader.loadJobConfiguration();
        ShardingContexts shardingContexts = environmentLoader.loadShardingContexts(jobConfiguration);
        KubernetesCloudJobFacade jobFacade = new KubernetesCloudJobFacade(shardingContexts, jobConfiguration, new JobTracingEventBus());
        if (environmentLoader.isTransient()) {
            new ElasticJobExecutor(elasticJobType, jobConfiguration, jobFacade).execute();
        } else {
            new DaemonJobScheduler(null, elasticJobType, jobConfiguration, jobFacade).schedule();
        }
    }
}
