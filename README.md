# Hazelcast OSGi R7 Distribution and Discovery Provider

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

In one instance of Karaf, type this at the console

    karaf@root()> feature:repo-add https://raw.githubusercontent.com/ECF/HazelcastProvider/master/build/karaf-features.xml
    
This will produce output like the following, showing the install of ECF Remote Services, and the Hazelcast providers

    Adding feature url https://raw.githubusercontent.com/ECF/HazelcastProvider/master/build/karaf-features.xml
    karaf@root()> feature:install -v ecf-rs-hazelcast
    Adding features: ecf-rs-hazelcast/[1.5.3,1.5.3]
        Changes to perform:
      Region: root
        Bundles to install:
        mvn:com.hazelcast/hazelcast/3.12.2
        mvn:javax.el/javax.el-api/3.0.0
        mvn:javax.enterprise/cdi-api/1.2
        mvn:javax.interceptor/javax.interceptor-api/1.2
        https://raw.githubusercontent.com/ECF/HazelcastProvider/master/build/plugins/javax.jms_1.1.0.v201205091237.jar
        mvn:javax.transaction/javax.transaction-api/1.2
        mvn:org.apache.felix/org.apache.felix.scr/2.1.16
        mvn:org.apache.karaf.scr/org.apache.karaf.scr.management/4.2.6
        mvn:org.apache.karaf.scr/org.apache.karaf.scr.state/4.2.6
        mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.javax-inject/1_2
        mvn:org.eclipse.platform/org.eclipse.core.jobs/3.9.3
        mvn:org.eclipse.ecf/org.eclipse.ecf/3.9.3
        mvn:org.eclipse.ecf/org.eclipse.ecf.console/1.3.0
        mvn:org.eclipse.ecf/org.eclipse.ecf.discovery/5.0.300
                                             
                                                 https://raw.githubusercontent.com/ECF/HazelcastProvider/master/build/plugins/org.eclipse.ecf.discovery.provider.hazelcast_1.0.4.201909291340.jar
      mvn:org.eclipse.ecf/org.eclipse.ecf.identity/3.9.1
      mvn:org.eclipse.ecf/org.eclipse.ecf.osgi.services.distribution/2.1.200
      mvn:org.eclipse.ecf/org.eclipse.ecf.osgi.services.remoteserviceadmin/4.6.800
      mvn:org.eclipse.ecf/org.eclipse.ecf.osgi.services.remoteserviceadmin.console/1.2.0
      mvn:org.eclipse.ecf/org.eclipse.ecf.osgi.services.remoteserviceadmin.proxy/1.0.100
      mvn:org.eclipse.ecf/org.eclipse.ecf.provider/4.8.0
      
          https://raw.githubusercontent.com/ECF/HazelcastProvider/master/build/plugins/org.eclipse.ecf.provider.jms_1.10.100.201806152009.jar
           https://raw.githubusercontent.com/ECF/HazelcastProvider/master/build/plugins/org.eclipse.ecf.provider.jms.hazelcast_1.2.3.201909291340.jar
      mvn:org.eclipse.ecf/org.eclipse.ecf.provider.remoteservice/4.4.100
      mvn:org.eclipse.ecf/org.eclipse.ecf.remoteservice/8.13.1
      mvn:org.eclipse.ecf/org.eclipse.ecf.remoteservice.asyncproxy/2.1.0
      mvn:org.eclipse.ecf/org.eclipse.ecf.sharedobject/2.6.0
      mvn:org.eclipse.platform/org.eclipse.equinox.common/3.9.0
      mvn:org.eclipse.platform/org.eclipse.equinox.concurrent/1.1.0
      mvn:org.eclipse.platform/org.eclipse.equinox.supplement/1.7.0
      mvn:org.eclipse.ecf/org.eclipse.osgi.services.remoteserviceadmin/1.6.200
      mvn:org.ops4j.pax.transx/pax-transx-tm-api/0.4.3
      mvn:org.osgi/org.osgi.util.function/1.0.0
      mvn:org.osgi/org.osgi.util.promise/1.0.0
Installing bundles:
  mvn:com.hazelcast/hazelcast/3.12.2
  mvn:javax.el/javax.el-api/3.0.0
  mvn:javax.enterprise/cdi-api/1.2
  mvn:javax.interceptor/javax.interceptor-api/1.2
  https://raw.githubusercontent.com/ECF/HazelcastProvider/master/build/plugins/javax.jms_1.1.0.v201205091237.jar
  mvn:javax.transaction/javax.transaction-api/1.2
  mvn:org.apache.felix/org.apache.felix.scr/2.1.16
  mvn:org.apache.karaf.scr/org.apache.karaf.scr.management/4.2.6
  mvn:org.apache.karaf.scr/org.apache.karaf.scr.state/4.2.6
  mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.javax-inject/1_2
  mvn:org.eclipse.platform/org.eclipse.core.jobs/3.9.3
  mvn:org.eclipse.ecf/org.eclipse.ecf/3.9.3
  mvn:org.eclipse.ecf/org.eclipse.ecf.console/1.3.0
  mvn:org.eclipse.ecf/org.eclipse.ecf.discovery/5.0.300
  https://raw.githubusercontent.com/ECF/HazelcastProvider/master/build/plugins/org.eclipse.ecf.discovery.provider.hazelcast_1.0.4.201909291340.jar
  mvn:org.eclipse.ecf/org.eclipse.ecf.identity/3.9.1
  mvn:org.eclipse.ecf/org.eclipse.ecf.osgi.services.distribution/2.1.200
  mvn:org.eclipse.ecf/org.eclipse.ecf.osgi.services.remoteserviceadmin/4.6.800
  mvn:org.eclipse.ecf/org.eclipse.ecf.osgi.services.remoteserviceadmin.console/1.2.0
  mvn:org.eclipse.ecf/org.eclipse.ecf.osgi.services.remoteserviceadmin.proxy/1.0.100
  mvn:org.eclipse.ecf/org.eclipse.ecf.provider/4.8.0
  https://raw.githubusercontent.com/ECF/HazelcastProvider/master/build/plugins/org.eclipse.ecf.provider.jms_1.10.100.201806152009.jar
  https://raw.githubusercontent.com/ECF/HazelcastProvider/master/build/plugins/org.eclipse.ecf.provider.jms.hazelcast_1.2.3.201909291340.jar
  mvn:org.eclipse.ecf/org.eclipse.ecf.provider.remoteservice/4.4.100
  mvn:org.eclipse.ecf/org.eclipse.ecf.remoteservice/8.13.1
  mvn:org.eclipse.ecf/org.eclipse.ecf.remoteservice.asyncproxy/2.1.0
  mvn:org.eclipse.ecf/org.eclipse.ecf.sharedobject/2.6.0
  mvn:org.eclipse.platform/org.eclipse.equinox.common/3.9.0
  mvn:org.eclipse.platform/org.eclipse.equinox.concurrent/1.1.0
  mvn:org.eclipse.platform/org.eclipse.equinox.supplement/1.7.0
  mvn:org.eclipse.ecf/org.eclipse.osgi.services.remoteserviceadmin/1.6.200
  mvn:org.ops4j.pax.transx/pax-transx-tm-api/0.4.3
  mvn:org.osgi/org.osgi.util.function/1.0.0
  mvn:org.osgi/org.osgi.util.promise/1.0.0
Starting bundles:
  com.hazelcast/3.12.2
  org.osgi.util.function/1.0.0.201505202023
  org.osgi.util.promise/1.0.0.201505202023
  org.apache.felix.scr/2.1.16
  org.apache.karaf.scr.management/4.2.6
  org.apache.karaf.scr.state/4.2.6
  javax.el-api/3.0.0
  org.apache.servicemix.bundles.javax-inject/1.0.0.2
  javax.interceptor-api/1.2.0
  javax.enterprise.cdi-api/1.2.0
  javax.transaction-api/1.2.0
  javax.jms/1.1.0.v201205091237
  org.eclipse.equinox.supplement/1.7.0.v20170329-1416
  org.eclipse.equinox.common/3.9.0.v20170207-1454
  org.eclipse.equinox.concurrent/1.1.0.v20130327-1442
  org.eclipse.ecf.remoteservice.asyncproxy/2.1.0.v20180409-2248
  org.eclipse.core.jobs/3.9.3.v20180115-1757
  org.eclipse.ecf.identity/3.9.1.v20180810-0833
  org.eclipse.ecf.discovery/5.0.300.v20180306-0211
  org.eclipse.osgi.services.remoteserviceadmin/1.6.200.v20180301-0016
  org.eclipse.ecf.osgi.services.remoteserviceadmin.proxy/1.0.100.v20180301-0016
  org.eclipse.ecf.provider.jms/1.10.100.201806152009
  org.ops4j.pax.transx.pax-transx-tm-api/0.4.3
  org.eclipse.ecf.console/1.3.0.v20180713-1805
  org.eclipse.ecf.discovery.provider.hazelcast/1.0.4.201909291340
  org.eclipse.ecf.osgi.services.remoteserviceadmin/4.6.800.v20180518-0149
  org.eclipse.ecf.osgi.services.distribution/2.1.200.v20180301-0016
  org.eclipse.ecf.remoteservice/8.13.1.v20180801-1752
  org.eclipse.ecf.provider.remoteservice/4.4.100.v20180516-2213
  org.eclipse.ecf.provider/4.8.0.v20180402-2103
  org.eclipse.ecf/3.9.3.v20181012-2016
  org.eclipse.ecf.sharedobject/2.6.0.v20180404-2345
  org.eclipse.ecf.provider.jms.hazelcast/1.2.3.201909291340
  org.eclipse.ecf.osgi.services.remoteserviceadmin.console/1.2.0.v20180713-1805
Done.





