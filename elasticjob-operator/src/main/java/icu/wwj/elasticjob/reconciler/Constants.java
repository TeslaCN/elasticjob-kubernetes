package icu.wwj.elasticjob.reconciler;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {
    
    public static final String ELASTICJOB_PREFIX = "elasticjob-";
    
    public static final String ELASTICJOB_DOMAIN = "icu.wwj.elasticjob/";
    
    public static final String ELASTICJOB_LABEL_APP = ELASTICJOB_DOMAIN + "app";
    
    public static final String ELASTICJOB_ANNOTATION_CONFIG = ELASTICJOB_DOMAIN + "config";
    
    public static final String ELASTICJOB_SHARDING_CONTEXT_PREFIX = "sharding-context-";
}
