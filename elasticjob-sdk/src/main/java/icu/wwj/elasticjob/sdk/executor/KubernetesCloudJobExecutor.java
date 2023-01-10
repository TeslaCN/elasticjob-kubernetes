package icu.wwj.elasticjob.sdk.executor;

import org.apache.shardingsphere.elasticjob.api.ElasticJob;
import org.apache.shardingsphere.elasticjob.executor.ElasticJobExecutor;

public final class KubernetesCloudJobExecutor {
    
    private final ElasticJobExecutor elasticJobExecutor;
    
    public KubernetesCloudJobExecutor(ElasticJob elasticJob) {
        elasticJobExecutor = new ElasticJobExecutor(elasticJob, null, new KubernetesCloudJobFacade(null, null, null));
    }
    
    public KubernetesCloudJobExecutor(String jobType) {
        elasticJobExecutor = new ElasticJobExecutor(jobType, null, new KubernetesCloudJobFacade(null, null, null));
    }
    
    public void execute() {
    }
}
