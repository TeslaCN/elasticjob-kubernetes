package icu.wwj.elasticjob;

import icu.wwj.elasticjob.api.ElasticJob;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import lombok.extern.slf4j.Slf4j;

@ControllerConfiguration
@Slf4j
public class ElasticJobReconciler implements Reconciler<ElasticJob>, ErrorStatusHandler<ElasticJob> {
    
    @Override
    public UpdateControl<ElasticJob> reconcile(final ElasticJob resource, final Context<ElasticJob> context) throws Exception {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public ErrorStatusUpdateControl<ElasticJob> updateErrorStatus(final ElasticJob resource, final Context<ElasticJob> context, final Exception e) {
        throw new UnsupportedOperationException();
    }
}
