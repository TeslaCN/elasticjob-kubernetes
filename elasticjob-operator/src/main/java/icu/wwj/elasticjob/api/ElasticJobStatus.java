package icu.wwj.elasticjob.api;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ElasticJobStatus extends ObservedGenerationAwareStatus {
    
    private String status;
}
