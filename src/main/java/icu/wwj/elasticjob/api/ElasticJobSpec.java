package icu.wwj.elasticjob.api;

import io.fabric8.generator.annotation.Min;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ElasticJobSpec {
    
    private PodTemplateSpec template;
    
    private JobExecutionType jobExecutionType;
    
    @Min(1)
    private int shardingTotalCount;
    
    private Map<String, String> shardingItemParameters;
    
    private String cron;
    
    private boolean misfire;
    
    private Map<String, String> props;
    
    private String description;
    
    private boolean disabled;
}
