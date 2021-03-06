/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.management;

import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.ManagementStatisticsLevel;
import org.apache.camel.management.event.DefaultEventFactory;
import org.apache.camel.management.mbean.Statistic;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.spi.EventFactory;
import org.apache.camel.spi.EventNotifier;
import org.apache.camel.spi.ManagementAgent;
import org.apache.camel.spi.ManagementNamingStrategy;
import org.apache.camel.spi.ManagementObjectStrategy;
import org.apache.camel.spi.ManagementStrategy;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ServiceHelper;

/**
 * A default management strategy that does <b>not</b> manage.
 * <p/>
 * This is default only used if Camel detects that it cannot use the JMX capable
 * {@link org.apache.camel.management.ManagedManagementStrategy} strategy. Then Camel will
 * fallback to use this instead that is basically a simple and <tt>noop</tt> strategy.
 * <p/>
 * This class can also be used to extend your custom management implement. In fact the JMX capable
 * provided by Camel extends this class as well.
 *
 * @see ManagedManagementStrategy
 * @version 
 */
public class DefaultManagementStrategy implements ManagementStrategy, CamelContextAware {

    private List<EventNotifier> eventNotifiers = new CopyOnWriteArrayList<EventNotifier>();
    private EventFactory eventFactory = new DefaultEventFactory();
    private ManagementNamingStrategy managementNamingStrategy;
    private ManagementObjectStrategy managementObjectStrategy;
    private boolean onlyManageProcessorWithCustomId;
    private ManagementAgent managementAgent;
    private ManagementStatisticsLevel statisticsLevel = ManagementStatisticsLevel.All;
    private CamelContext camelContext;

    public DefaultManagementStrategy() {
    }

    public DefaultManagementStrategy(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    public List<EventNotifier> getEventNotifiers() {
        return eventNotifiers;
    }

    public void addEventNotifier(EventNotifier eventNotifier) {
        this.eventNotifiers.add(eventNotifier);
    }

    public boolean removeEventNotifier(EventNotifier eventNotifier) {
        return eventNotifiers.remove(eventNotifier);
    }

    public void setEventNotifiers(List<EventNotifier> eventNotifiers) {
        this.eventNotifiers = eventNotifiers;
    }

    public EventFactory getEventFactory() {
        return eventFactory;
    }

    public void setEventFactory(EventFactory eventFactory) {
        this.eventFactory = eventFactory;
    }

    public ManagementNamingStrategy getManagementNamingStrategy() {
        if (managementNamingStrategy == null) {
            managementNamingStrategy = new DefaultManagementNamingStrategy();
        }
        return managementNamingStrategy;
    }

    public void setManagementNamingStrategy(ManagementNamingStrategy managementNamingStrategy) {
        this.managementNamingStrategy = managementNamingStrategy;
    }

    public ManagementObjectStrategy getManagementObjectStrategy() {
        if (managementObjectStrategy == null) {
            managementObjectStrategy = new DefaultManagementObjectStrategy();
        }
        return managementObjectStrategy;
    }

    public void setManagementObjectStrategy(ManagementObjectStrategy managementObjectStrategy) {
        this.managementObjectStrategy = managementObjectStrategy;
    }

    public ManagementAgent getManagementAgent() {
        return managementAgent;
    }

    public void setManagementAgent(ManagementAgent managementAgent) {
        this.managementAgent = managementAgent;
    }

    public void onlyManageProcessorWithCustomId(boolean flag) {
        onlyManageProcessorWithCustomId = flag;
    }

    public boolean isOnlyManageProcessorWithCustomId() {
        return onlyManageProcessorWithCustomId;
    }

    public boolean manageProcessor(ProcessorDefinition<?> definition) {
        return false;
    }

    public void manageObject(Object managedObject) throws Exception {
        // noop
    }

    public void manageNamedObject(Object managedObject, Object preferredName) throws Exception {
        // noop
    }

    public <T> T getManagedObjectName(Object managedObject, String customName, Class<T> nameType) throws Exception {
        // noop
        return null;
    }

    public void unmanageObject(Object managedObject) throws Exception {
        // noop
    }

    public void unmanageNamedObject(Object name) throws Exception {
        // noop
    }

    public boolean isManaged(Object managedObject, Object name) {
        // noop
        return false;
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }

    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    public void notify(EventObject event) throws Exception {
        if (eventNotifiers != null && !eventNotifiers.isEmpty()) {
            for (EventNotifier notifier : eventNotifiers) {
                if (notifier.isEnabled(event)) {
                    notifier.notify(event);
                }
            }
        }
    }

    public Statistic createStatistic(String name, Object owner, Statistic.UpdateMode updateMode) {
        // noop
        return null;
    }

    public void setStatisticsLevel(ManagementStatisticsLevel level) {
        this.statisticsLevel = level;
    }

    public ManagementStatisticsLevel getStatisticsLevel() {
        return statisticsLevel;
    }

    public void start() throws Exception {
        ObjectHelper.notNull(camelContext, "CamelContext");

        if (eventNotifiers != null) {
            for (EventNotifier notifier : eventNotifiers) {

                // inject CamelContext if the service is aware
                if (notifier instanceof CamelContextAware) {
                    CamelContextAware aware = (CamelContextAware) notifier;
                    aware.setCamelContext(camelContext);
                }

                ServiceHelper.startService(notifier);
            }
        }

        if (managementAgent != null) {
            managementAgent.start();
            // set the naming strategy using the domain name from the agent
            if (managementNamingStrategy == null) {
                setManagementNamingStrategy(new DefaultManagementNamingStrategy(managementAgent.getMBeanObjectDomainName()));
            }
        }
    }

    public void stop() throws Exception {
        if (managementAgent != null) {
            managementAgent.stop();
        }
        if (eventNotifiers != null) {
            ServiceHelper.stopServices(eventNotifiers);
        }
    }

}
