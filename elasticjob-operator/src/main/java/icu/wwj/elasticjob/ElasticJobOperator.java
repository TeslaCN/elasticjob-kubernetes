package icu.wwj.elasticjob;

import icu.wwj.elasticjob.reconciler.ElasticJobReconciler;
import icu.wwj.elasticjob.reconciler.ElasticJobTriggerReconciler;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.Operator;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class ElasticJobOperator {
    
    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
    
    public static void main(String[] args) {
        KubernetesClient client = new KubernetesClientBuilder().build();
        Operator operator = new Operator(client);
        operator.register(new ElasticJobReconciler(client));
        operator.register(new ElasticJobTriggerReconciler(client));
        operator.start();
    }
}
