package org.moqui.impl.service.rabbitmq

import org.moqui.context.ExecutionContextFactory

abstract class ToolMessageListener {
    protected ExecutionContextFactory ecf
    void bindExecutionContextFactory(ExecutionContextFactory ecf){
        this.ecf =ecf
    }
}