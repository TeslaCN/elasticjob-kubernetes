package icu.wwj.elasticjob.api;

import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ElasticJobStatus extends ObservedGenerationAwareStatus {
    
    @PrinterColumn
    private String status;
    
    @PrinterColumn(name = "LAST SCHEDULE TIME")
    private String lastScheduleTime;
}
