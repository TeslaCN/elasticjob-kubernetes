package icu.wwj.elasticjob.api.trigger;

import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ElasticJobTriggerStatus extends ObservedGenerationAwareStatus {
    
    @PrinterColumn
    private String status;
}
