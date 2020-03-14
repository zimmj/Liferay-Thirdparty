
# Liferay import a use Third Party Libraries  
In this repository, I collected some ways to import and use Third party Libraries. I hope to make people understand, how to use Third Party Libraries in Liferay, without the pain, I had to figure it out. 
# The issue  
To use third party libraries in bigger project is common. Often we even need to use custom middleware libraries to connect the different part together. 
Liferay uses OSGi container, which is a new virtual environment with specific shared packages. To use this packages, they need to be exposed to the container. To start a module, jar, all the needed packages need to be available. Therefore, we need to provide the used third party library to our OSGi container.
  
## Understand if library is OSGi compatible  
Today, most libraries are compatible with OSGi and can simple be added to our environment. For example, we can have a look at Google Guava. Its jar can be started in OSGi.
 We simply download the jar and deploy it in Liferay. Then we should see following logs:  
```less  
[com.liferay.portal.kernel.deploy.auto.AutoDeployScanner][AutoDeployDir:259] Processing guava-19.0.jar  
[Refresh Thread: Equinox Container: 29094b68-db73-4547-977d-109c13256dc1][BundleStartStopLogger:39] STARTED com.google.guava_19.0.0 [10651]  
```  
This logs means, that the jar is running in OSGi, and we can use its packages in our modules. We don't need to do anything more.
If only the first entry appears, we got unlucky. The library is not OSGi compatible. Therefore, we need to some extra work.
Another way to see, if it is an OSGi module, is to open the manifest of the jar. If we have something like below. We can assume, that it is running in OSGi.  
```less  
Bundle-Name: Guava: Google Core Libraries for Java  
Export-Package: com.google.common.net;uses:="javax.annotation,com.google  
 .common.base,com.google.common.hashImport-Package: javax.annotation;resolution:=optional,sun.misc;resolutio  
 n:=optionalTool: Bnd-1.50.0  
```  
* The export-statement shows, which packages are provided.   
* The import-statement shows, which packages are needed  

I will talk about the different statement later, but the read more about it, see the link:  
[Explanation how osgi bundles get resolved](https://blog.christianposta.com/osgi/understanding-how-osgi-bundles-get-resolved-part-i/)  
  
## Basic of Wrapping
If the preferable third party library is not OSGi compatible, we need to make it OSGi compatible. And the easiest way to do so, is to include this library in our module.  
They way I try to explain here is shown in the module: **Intercom-Include**. The method is rather simple and possible the best way to include a library.
We just tell Gradle, more exactly the bnd.bnd tools, to include this library into our module. This tool resolves most of the dependencies and its transitive dependencies.  In the example module: **Intercom-include-by-hand**. I show, what the tool makes behind the curtain, so we even could do it by hand.
There is a big but, when we try to deploy this module, we should get following log:
```less  
ERROR [fileinstall- Error while starting bundle: Liferay-Thirdparty/bundles/osgi/modules/intercom-include.jar  
org.osgi.framework.BundleException: Could not resolve module: intercom-include [2158]_  Unresolved requirement: Import-Package: org.slf4j.impl_ [Sanitized]  
 at org.eclipse.osgi.container.Module.start(Module.java:444)  
 at org.eclipse.osgi.internal.framework.EquinoxBundle.start(EquinoxBundle.java:428)  
 at org.apache.felix.fileinstall.internal.DirectoryWatcher.startBundle(DirectoryWatcher.java:1264)  
 at org.apache.felix.fileinstall.internal.DirectoryWatcher.startBundles(DirectoryWatcher.java:1237)  
 at org.apache.felix.fileinstall.internal.DirectoryWatcher.startAllBundles(DirectoryWatcher.java:1226)  
 at org.apache.felix.fileinstall.internal.DirectoryWatcher.doProcess(DirectoryWatcher.java:515)  
 at org.apache.felix.fileinstall.internal.DirectoryWatcher.process(DirectoryWatcher.java:365)  
 at org.apache.felix.fileinstall.internal.DirectoryWatcher.run(DirectoryWatcher.java:316)  
```  
As you can see, we have an error. The stack trace tills us which module is missing a specific package. This package was found by the bnd.bnd tool and written into the previous mentioned Import-Packages in the manifest file. These packages mentioned in Import-Packages must be provided on the startup of the bundle via an export statement (Export-Package) from another module. To solve this, we have different option:  
* remove the import of the missing package in the manifest 
* add the package with compile-include  
* find a OSGi module, which is providing this packages  

With practice, you will know, which packages need to be included, which exist as OSGi bundles and which we can be exclude from the import.  
  
### Find an OSGi Module  
As mention before, the most common libraries are OSGi compatible. Therefore, we can simply download them and try to start them in our OSGi container.
In my example, we need slf4j.imple. As it only shows which package is needed we need to figure out, which jar is providing it.  To do so, we have following options:

 - Check Gradle dependencies of this module (Same as we check dependencies on [Maven Central](https://mvnrepository.com/artifact/io.intercom/intercom-java/2.8.1))
 - Search in google about it
 - Search for the jar with [findjar.com](findjar.com)
 
In the most cases we will find out, that we need the jar:
 - slf4j-api-1.7.25.jar

So we download it and try to deploy it. Unfortunately we get following Log:
```less  
Unresolved requirement: Import-Package: org.slf4j.impl_ -> Export-Package: org.slf4j.impl; bundle-symbolic-name="slf4j.log4j12";  
 bundle-version="1.7.21"; version="1.7.21"_       slf4j.log4j12 [2159]_ Unresolved requirement: Fragment-Host: slf4j.api_ Unresolved requirement: Import-Package: org.slf4j; version="1.7.21"_ [Sanitized]  
 at org.eclipse.osgi.container.Module.start(Module.java:444)  
```  
If we compare it with the manifest file:  
```less  
Import-Package: org.slf4j;version=1.6.0,org.slf4j.helpers;version=1.6.0,org.slf4j.spi;version=1.6.0  
```  
Unfortunately this seams a bit harder, because on Maven Central are non dependencies listen. So a google search it is. Unfortunately log4j has many different implementation, and we don't know exactly which one was used. But a common one is:

 - slf4j-log4j12-1.7.25.jar

 When we download it and deploy it as well, we see that all packages are started as well, and therefore we can use it. See the logs
```less
2020-03-14 20:35:22.432 INFO  [Refresh Thread: Equinox Container: 8abbedbb-d706-4246-a2ab-9ea35c6abea2][BundleStartStopLogger:39] STARTED intercom-include_1.0.0 [3224]
2020-03-14 20:35:22.432 INFO  [Refresh Thread: Equinox Container: 8abbedbb-d706-4246-a2ab-9ea35c6abea2][BundleStartStopLogger:39] STARTED slf4j.api_1.7.25 [3227]
```
#### Older project  
For older project/jar, developers often used many packages, especially distributed from sun in different java virtual environment, they can be tedious to include in a module. If we are lucky, we can find them in so-called service mix. The Best way to find them, is to search like following shown.
```less  
 servicemix::Bundles::jar-name
 ```  
For my example project FOP, there is a service mix avaible. But strangely, it is quite hard to get it running in OSGi of Liferay. You need to now, that these bundles are explicitly made for the OSGi runtime of [service-mix](https://servicemix.apache.org/index.html). But if you try it by yourself, you learn one important principle of OSGi. Each package can be provided trough another bundle in its export statement. So we sometimes need to add several service-mixes together till it is running.

This service-mix have the Idea to wrap older libraries in a module in export the needed package. Exactly this we can do as well with liferay. I show it with the library FOP. See chapter: How to make a wrapper bundle with export statement
## How to make a wrapper bundle with export statement
As for now, we included the jars into one bundle but we are not able to use the jar in other bundles as well. To expose the included package we simply need to add following statement to our bnd.bnd file:
```less  
Export-Package:org.slf4j.*  
```  
We need to export all the packages we want to import in other bundles. This example exposes all packages coming after org.slf4j. You can as well expose only a specific package.
The export statement is very useful and often used, to separate code and concern in OSGi. 
### Specific example with FOP
In this project, I mad a specific example with the Library FOP.
FOP is used to generate PDF. With this sample I use almost all the different tools I explained here. Therefore, it's the perfect example.
The first thing I do is to include the FOP Jar with:
```less
dependencies {  
    compileInclude "org.apache.xmlgraphics:fop:2.4"  
}
```
Let's see what the Liferay things about it.
```less
org.osgi.framework.BundleException: Could not resolve module: fop.wrapper [3234]_  Unresolved requirement: Import-Package: com.sun.image.codec.jpeg_
```
As we see, we are missing a package from Java core. These packages are usually not needed, but somehow transitive dependencies. We usually deal with them, when we remove them from the import, as following showed.
If redo this process, until FOP wrapper example is running, we're going to get the below mentioned Import statement. 
#### Remove the package from the import  
As mentioned, above, some packages, better there classes, are never used. Therefore, we need to exclude them from our import Statement. For the FOP example the excludes looks like:  
```less  
Import-Package: \  
 !com.sun.*,\  
 !gnu.gcj*,\  
 !junit.*,\ !kaffe.util.*,\  
 !org.apache.avalon.framework.logger,\  
 !org.apache.env,\  
 !org.apache.harmony.luni.util,\  
 !org.apache.log,\  
 !org.mozilla.javascript,\  
 !org.python.*,\  
 !sun.*,\ !weblogic,\ !org.json,\  
 !org.apache.*,\  
 *
 ```  
With the syntax: !(not) package.name.wildcard, we can exclude all the sup package from the import statement of the manifest. If we exclude all the missing packages, the module will start. You need to be aware, that this can cause class-def not found issues, which are occurring on runtime. So sometimes we need to change that, and include the packages, which have thrown a class not found exception.
If we use the Import-Package statement for anything, we need to follow this shown syntax. Most importantly we must keep the (*) at the end of the statement. Otherwise, no packages will be imported on runtime, and we will get a class not def error.  
### How to use included packages
We were talking how to include and export specific packages. But how do we use them. First we need to import the packages we want to import. For the FOP it's following statement:
```less
Export-Package: org.apache.fop.*
```
This will export more packages than we need, but it makes it as well simpler to read. Now to show how to use it, I made the module: fop-sample. We simply import the FOP packages with the gralde compileOnly directive. This tells bnd.bnd to put all the used packages from the code, which belongs to this jar, in the import statement.
It looks like:
```less
dependencies {  
  compileOnly "org.osgi:org.osgi.service.component.annotations:1.3.0"  
  compileOnly "org.apache.xmlgraphics:fop:2.4"  
}
```
The annotations I need to make a OSGi component, so I can generate a PDF on bundle start.
If we try to make a PDF with only this includes we get following error:
```less
The activate method has thrown an exception
java.lang.IncompatibleClassChangeError: Class org.apache.fop.fo.FOTreeBuilder does not implement the requested interface org.xml.sax.ContentHandler
```
That means, we don't find a class or interface on runtime. First we will check, if this mention packages is written in the import statement of the module fop.sample. We open the jar with a zip program and open the manifest file. There we see it is included. So we know, it only not presented to the OSGi, environment. Now we can check, if the package exist in the wrapper Manifest or just try it out. But either way, we need to include this package to the export-statement, to make it available in OSGi. Therefore, the statement looks like:
```less
Export-Package: org.apache.fop.*, org.xml.sax
```
#### Where to put found OSGi modules  
All the jar we are using in our modules, need to be deployed on startup of the server. Liferay offer two ways to do so:
* Add the jars to the config folder
* Add the jars to the static resolver. 
##### Add jar to config folder  
We can add all our needed Third-Party jar to one of the following folder:  
* configs/common/deploy  
* configs/common/osgi/modules 

On the init-bundle task, the jars get put into the corresponding folders. From this folder, all jars will be deployed in the OSGi Container. And therefore their packages will be available. 
With this option, the jar need to be checked into GIT, so that they are loaded as well on the build server. 
  
#### Add jar to the static resolver  
Liferay uses a [static resolver](https://portal.liferay.dev/docs/7-1/tutorials/-/knowledge_base/t/modifying-the-target-platforms-capabilities) on the init-bundle task. We can add libraries to this resolver. To do so we add following statement to the root build.gradle file:  
```less  
dependencies {  
 providedModules group: "com.google.guava", name: "guava", version: "23.0"}  
```  
With this statement, Liferay will download and add this jar in the init-bundle step to the folder: bundles/osgi/modules and the jar will be deployed on startup.
With this way, we don't need to add full jars to GIT, and we easily can change the version of the jar. Because of it, I personally think, that this way should be used to add third party Libraries to Liferay.
    
# Conclusion  
To add third Party Library to Liferay is usually not really complicated, because most of them are already OSGi modules. If they are not, we can wrap them with compile-include in one and expose the needed packages to the outside. Only for old jar's we need to exclude specific packages from the import statement, to make them run.  
I hope this example helps you to understand the way, how dependencies are solved in Liferay, and explains it better than the documentation of Liferay itself. Feel free to make changes on this repository.  
  
# Special cases  
As I was resolving a jar with generated code. Generated SOAP client using axis-2. I came along an issue with classloaders and binary classes.  Axis-2 generates binary classes, which are loaded on runtime into the classloader of the module, axis-2 is in.  But this classes are needed in another module (with another classloader) as well. To make this possible. I can use the statement:  
 ```less
 DynamicImport-Package: com.package.*  
```  
This Import is not well known, but can help in specific cases.  
Be aware, that this kind of import, should not be used commonly, please try to stick at the above ways.
