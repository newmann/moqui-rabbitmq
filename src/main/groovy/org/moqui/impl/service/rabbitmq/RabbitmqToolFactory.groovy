/*
 * This software is in the public domain under CC0 1.0 Universal plus a 
 * Grant of Patent License.
 * 
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 * 
 * You should have received a copy of the CC0 Public Domain Dedication
 * along with this software (see the LICENSE.md file). If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package org.moqui.impl.service.rabbitmq

import org.moqui.resource.ResourceReference
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import groovy.transform.CompileStatic
import org.moqui.BaseArtifactException
import org.moqui.context.ExecutionContextFactory
import org.moqui.context.ToolFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.io.ClassPathResource

/** A ToolFactory for Rabbitmq:
 * 1、创建一个defaultChannel,方便生产者使用
 * 2、可能需要为flowable创建一个独立的connection，避免多线程情况下的问题
 *
 * */
@CompileStatic
class RabbitmqToolFactory implements ToolFactory<RabbitmqToolFactory> {
    protected final static Logger logger = LoggerFactory.getLogger(RabbitmqToolFactory.class)
    final static String TOOL_NAME = "Rabbitmq"


    protected ExecutionContextFactory ecf = null
    protected GenericApplicationContext ctx = null
//    protected ConnectionFactory connectionFactory = null
//    protected RabbitAdmin rabbitAdmin = null

    /** Default empty constructor */
    RabbitmqToolFactory() { }

    @Override
    String getName() { return TOOL_NAME }
    @Override
    void init(ExecutionContextFactory ecf) {
        logger.info("Starting Rabbitmq connection...")
        this.ecf = ecf

        ctx = new GenericApplicationContext()

        //through post processor binding ExecutionContexFactory, then message listener can call moqui service
        ToolBeanFactoryPostProcessor beanFactoryPostProcessor = new ToolBeanFactoryPostProcessor()
        beanFactoryPostProcessor.init(ecf)

        ctx.addBeanFactoryPostProcessor(beanFactoryPostProcessor)

        List<ResourceReference> confFileList = getRabbitmqConfigureFiles()
        if(confFileList && confFileList.size()>0){
            XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(ctx)

            for(confFile in confFileList){
                xmlBeanDefinitionReader.loadBeanDefinitions(confFile.getLocation())
            }
            ctx.refresh()
//            connectionFactory = (ConnectionFactory) ctx.getBean("connectionFactory")
//            rabbitAdmin = (RabbitAdmin) ctx.getBean('rabbitAdmin')
//
//            AmqpTemplate template = (AmqpTemplate) ctx.getBean("amqpTemplate")
//            template.convertAndSend("myqueue", "foo")
//            String foo = (String) template.receiveAndConvert("myqueue")
//            logger.info("get from first connection queue: ${foo}")
//
//            ConnectionFactory connect2 = (ConnectionFactory) ctx.getBean("flowableConnectionFactory")
//
//            logger.info("flowable's rabbitmq connection is established ")

        }else{
            logger.warn("Starting Rabbitmq Tool,but can't find any rabbitmq.conf.xml files..")
        }


    }
    @Override
    void preFacadeInit(ExecutionContextFactory ecf) {
        this.ecf = ecf

    }

    @Override
    RabbitmqToolFactory getInstance(Object... parameters) {
        if (ctx == null) throw new IllegalStateException("RabbitmqToolFactory not initialized")
        return this
    }

    @Override
    void destroy() {
        // stop rabbitmq connection
        Map<String,ConnectionFactory> connectionFactoryList = ctx.getBeansOfType(ConnectionFactory.class)
        if (connectionFactoryList && connectionFactoryList.size()>0) try {
            for(connectionFactoryEntry in connectionFactoryList){
                connectionFactoryEntry.value.resetConnection()
                logger.info("Rabbitmq connection ${connectionFactoryEntry.key} closed")

            }
        } catch (Throwable t) { logger.error("Error in Rabbitmq connection close", t) }
    }

    ExecutionContextFactory getEcf() { return ecf }

    private List<ResourceReference> getRabbitmqConfigureFiles(){
        LinkedHashMap<String, String> componentList = ecf.getComponentBaseLocations()
        List<ResourceReference> fileList = []
        for (component in componentList) {
            ResourceReference componentResource = ecf.resource.getLocationReference(component.value +"/rabbitmq")

            List<ResourceReference> childList = componentResource.getChildren()
            if (childList && childList.size() > 0) {
                for (child in childList) {
                    if(child.getLocation().endsWith("rabbitmq.conf.xml")){
                        fileList.add(child)
                    }
                }
            }
        }
        return fileList
    }

    Object getBean(String beanName){
        return ctx.getBean(beanName)
    }
}
