package icu.wwj.elasticjob.api.trigger;

import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode
public class ElasticJobTriggerSpec {
    
    // TODO Use or remove these fields
    @PrinterColumn
    private String jobParameter;
    
    private Map<String, String> shardingItemParameters = new LinkedHashMap<>();
}
