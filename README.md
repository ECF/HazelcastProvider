# Hazelcast OSGi R7+ Distribution and Discovery Provider

ECF OSGi R7 Remote Services Distribution and Discovery Providers based upon [Hazelcast](http://www.hazelcast.com).

ECF is an [Eclipse Foundation](http://www.eclipse.org) open source project that implements the OSGi R7 version of the [Remote Services](https://osgi.org/specification/osgi.cmpn/7.0.0/service.remoteservices.html) and [Remote Service Admin](https://osgi.org/specification/osgi.cmpn/7.0.0/service.remoteserviceadmin.html) specifications. 

This repo holds both a Distribution and Discovery provider that can be plugged in underneath ECF's Remote Services implementation.  The following links provide access to resources about the Remote Services and Remote Service Admin (RSA) implementations themselves, along with linkt to other available Distribution and Discovery providers.

[ECF Home page](http://www.eclipse.org/ecf)<br>
[ECF Wiki](https://wiki.eclipse.org/ECF)<br>
[ECF Download page](http://www.eclipse.org/ecf/downloads.php)<br>
[ECF Distribution Providers](https://wiki.eclipse.org/Distribution_Providers)<br>
[ECF Discovery Providers](https://wiki.eclipse.org/Discovery_Providers) 

## Installing and Running an Example Remote Service in Apache Karaf

[Apache Karaf](http://karaf.apache.org) is an OSGi-based server.   The following shows how to use the Hazelcast Discovery and Distribution providers to export and use an example remote service.   The source for the example remote service projects is located:

[TimeService API](https://git.eclipse.org/c/ecf/org.eclipse.ecf.git/tree/examples/bundles/com.mycorp.examples.timeservice.async) Project - depended upon by both the Host and the Consumer projects.  

[TimeService Host](https://git.eclipse.org/c/ecf/org.eclipse.ecf.git/tree/examples/bundles/com.mycorp.examples.timeservice.host) Project - exports an implementation or 'host' of the TimeService API.

[TimeService Consumer](https://git.eclipse.org/c/ecf/org.eclipse.ecf.git/tree/examples/bundles/com.mycorp.examples.timeservice.consumer.ds.async) Project - discovers and imports and then calls the remote services.

### TimeService Host

To install the ECF Remote Services implementation, and the Hazelcast Discovery and Distribution providers:

In one instance of Karaf, add these two repos at the console

    karaf@root()> feature:repo-add https://download.eclipse.org/rt/ecf/latest/karaf-features.xml
    Adding feature url https://download.eclipse.org/rt/ecf/latest/karaf-features.xml
    karaf@root()> feature:repo-add https://raw.githubusercontent.com/ECF/HazelcastProvider/master/build/karaf-features.xml
    Adding feature url https://raw.githubusercontent.com/ECF/HazelcastProvider/master/build/karaf-features.xml
    
Then type this command to install ECF Remote Services, and the Hazelcast providers

    karaf@root()> feature:install ecf-rs-hazelcast

Type this command to configure the Timeservice Host to use the Hazelcast manager distribution provider

    karaf@root()> system:property -p service.exported.configs ecf.jms.hazelcast.manager
    
To install, start, export the TimeService host 

```feature:install ecf-rs-examples-timeservice-host 
karaf@root()> feature:install ecf-rs-examples-timeservice-host
19:41:09.134;EXPORT_REGISTRATION;exportedSR=[com.mycorp.examples.timeservice.ITimeService];cID=JMSID[hazelcast://localhost/defaultRemoteServicesTopic];rsId=1
--Endpoint Description---
<endpoint-descriptions xmlns="http://www.osgi.org/xmlns/rsa/v1.0.0">
  <endpoint-description>
    <property name="ecf.endpoint.id" value-type="String" value="hazelcast://localhost/defaultRemoteServicesTopic"/>
    <property name="ecf.endpoint.id.ns" value-type="String" value="ecf.namespace.jmsid"/>
    <property name="ecf.endpoint.ts" value-type="Long" value="1569811269048"/>
    <property name="ecf.exported.async.interfaces" value-type="String" value="*"/>
    <property name="ecf.rsvc.id" value-type="Long" value="1"/>
    <property name="endpoint.framework.uuid" value-type="String" value="1c5bf449-700a-4322-bf2b-850724f3a65c"/>
    <property name="endpoint.id" value-type="String" value="eb4fb828-5dd7-4e22-85aa-906f3dad1d5e"/>
    <property name="endpoint.package.version.com.mycorp.examples.timeservice" value-type="String" value="2.0.0"/>
    <property name="endpoint.service.id" value-type="Long" value="136"/>
    <property name="objectClass" value-type="String">
      <array>
        <value>com.mycorp.examples.timeservice.ITimeService</value>
      </array>
    </property>
    <property name="remote.configs.supported" value-type="String">
      <array>
        <value>ecf.jms.hazelcast.manager</value>
      </array>
    </property>
    <property name="remote.intents.supported" value-type="String">
      <array>
        <value>osgi.basic</value>
        <value>passByValue</value>
        <value>exactlyOnce</value>
        <value>ordered</value>
        <value>osgi.async</value>
        <value>hazelcast</value>
      </array>
    </property>
    <property name="service.imported" value-type="String" value="true"/>
    <property name="service.imported.configs" value-type="String">
      <array>
        <value>ecf.jms.hazelcast.manager</value>
      </array>
    </property>
  </endpoint-description>
</endpoint-descriptions>
---End Endpoint Description
TimeService host registered with registration=org.apache.felix.framework.ServiceRegistrationImpl@19fe5be4
karaf@root()>     
```
The above output indicates that the TimeService has been exported.  Also the Hazelcast discovery has advertised this remote service via a multicast group on the current LAN.

## TimeService Consumer

In a *second* Karaf instance (not the same process as the TimeService host) give the following Karaf commands:

    karaf@root()> feature:repo-add https://download.eclipse.org/rt/ecf/latest/karaf-features.xml
    Adding feature url https://download.eclipse.org/rt/ecf/latest/karaf-features.xml
    karaf@root()> feature:repo-add https://raw.githubusercontent.com/ECF/HazelcastProvider/master/build/karaf-features.xml
    Adding feature url https://raw.githubusercontent.com/ECF/HazelcastProvider/master/build/karaf-features.xml
    karaf@root()> feature:install ecf-rs-examples-timeservice-consumer
    karaf@root()> feature:install ecf-rs-hazelcast

After a few seconds to download, install and start bundles, and a few more seconds to discover the exported TimeService in the Hazelcast group, you should see output like this on the consumer, indicating that the remote service was discovered, imported, injected into the example consumer code, which calls the remote service and prints out a result along with the IMPORT_REGISTRATION and imported endpoint-description.

```karaf@root()> feature:repo-add https://raw.githubusercontent.com/ECF/HazelcastProvider/master/build/karaf-features.xml
Adding feature url https://raw.githubusercontent.com/ECF/HazelcastProvider/master/build/karaf-features.xml
karaf@root()> feature:install ecf-rs-examples-timeservice-consumer
karaf@root()> feature:install ecf-rs-hazelcast
karaf@root()> Current time on remote is: 1569811897139
19:51:37.153;IMPORT_REGISTRATION;importedSR=[com.mycorp.examples.timeservice.ITimeService, com.mycorp.examples.timeservice.ITimeServiceAsync];cID=JMSID[hazelcast://localhost/defaultRemoteServicesTopic];rsId=1
--Endpoint Description---
<endpoint-descriptions xmlns="http://www.osgi.org/xmlns/rsa/v1.0.0">
  <endpoint-description>
    <property name="ecf.endpoint.id" value-type="String" value="hazelcast://localhost/defaultRemoteServicesTopic"/>
    <property name="ecf.endpoint.id.ns" value-type="String" value="ecf.namespace.jmsid"/>
    <property name="ecf.endpoint.ts" value-type="Long" value="1569811269048"/>
    <property name="ecf.exported.async.interfaces" value-type="String" value="*"/>
    <property name="ecf.rsvc.id" value-type="Long" value="1"/>
    <property name="endpoint.framework.uuid" value-type="String" value="1c5bf449-700a-4322-bf2b-850724f3a65c"/>
    <property name="endpoint.id" value-type="String" value="eb4fb828-5dd7-4e22-85aa-906f3dad1d5e"/>
    <property name="endpoint.package.version.com.mycorp.examples.timeservice" value-type="String" value="2.0.0"/>
    <property name="endpoint.service.id" value-type="Long" value="136"/>
    <property name="objectClass" value-type="String">
      <array>
        <value>com.mycorp.examples.timeservice.ITimeService</value>
      </array>
    </property>
    <property name="remote.configs.supported" value-type="String">
      <array>
        <value>ecf.jms.hazelcast.manager</value>
      </array>
    </property>
    <property name="remote.intents.supported" value-type="String">
      <array>
        <value>osgi.basic</value>
        <value>passByValue</value>
        <value>exactlyOnce</value>
        <value>ordered</value>
        <value>osgi.async</value>
        <value>hazelcast</value>
      </array>
    </property>
    <property name="service.imported.configs" value-type="String">
      <array>
        <value>ecf.jms.hazelcast.member</value>
      </array>
    </property>
  </endpoint-description>
</endpoint-descriptions>
---End Endpoint Description
```

Notice the line

    karaf@root()> Current time on remote is: 1569811897139

This indicated that the consumer called the remote TimeService and received a result of the current time.  The code for the consumer class is [here](https://git.eclipse.org/c/ecf/org.eclipse.ecf.git/tree/examples/bundles/com.mycorp.examples.timeservice.consumer.ds/src/com/mycorp/examples/timeservice/consumer/ds/TimeServiceComponent.java#n21)

On the host console, you should see output like this:

    karaf@root()> TimeServiceImpl:  Received call to getCurrentTime()
   
The code for the TimeServiceImpl class is [here](https://git.eclipse.org/c/ecf/org.eclipse.ecf.git/tree/examples/bundles/com.mycorp.examples.timeservice.host/src/com/mycorp/examples/timeservice/host/TimeServiceImpl.java#n18)


