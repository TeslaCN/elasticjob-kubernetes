package icu.wwj.elasticjob;

import io.javaoperatorsdk.operator.Operator;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class ElasticJobOperator {
    
    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
    
    public static void main(String[] args) {
        Operator operator = new Operator();
        operator.register(new ElasticJobReconciler());
        operator.start();
    }
}
