package icu.wwj.elasticjob.api.trigger;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("icu.wwj.elasticjob")
@Version("v1alpha1")
public class ElasticJobTrigger extends CustomResource<ElasticJobTriggerSpec, ElasticJobTriggerStatus> implements Namespaced {
}
