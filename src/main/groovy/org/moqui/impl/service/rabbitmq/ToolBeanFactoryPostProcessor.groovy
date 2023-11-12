package org.moqui.impl.service.rabbitmq

import org.moqui.context.ExecutionContextFactory
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory

class ToolBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    protected ExecutionContextFactory ecf = null
    void init(protected ExecutionContextFactory ecf = null){
        this.ecf = ecf
    }
    @Override
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Map messageListenerMap =  beanFactory.getBeansOfType(ToolMessageListener.class)
        if(messageListenerMap == null){
            ecf.getExecutionContext().logger.warn("No any message listener are defined.")
            return
        }

        for(item in messageListenerMap){
            item.value.bindExecutionContextFactory(ecf)
        }

    }
}
