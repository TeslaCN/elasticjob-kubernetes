package icu.wwj.elasticjob.api;

import io.fabric8.generator.annotation.Min;
import io.fabric8.generator.annotation.Required;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import io.fabric8.kubernetes.model.annotation.SpecReplicas;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode
public class ElasticJobSpec {
    
    @Required
    private PodTemplateSpec template;
    
    @Required
    @PrinterColumn(name = "JOB EXECUTION TYPE")
    private JobExecutionType jobExecutionType;
    
    @Required
    @Min(1)
    @PrinterColumn(name = "SHARDING TOTAL COUNT")
    @SpecReplicas
    private int shardingTotalCount;
    
    @PrinterColumn
    private String cron;
    
    private Map<String, String> shardingItemParameters = new LinkedHashMap<>();
    
    private String jobParameter;
    
    private boolean failover;
    
    private boolean misfire;
    
    private String jobErrorHandlerType;
    
    private String description;
    
    private Map<String, String> props = new LinkedHashMap<>();
    
    @PrinterColumn
    private boolean disabled;
}
