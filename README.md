# Liferay Thirdparty
This is an example repository to show how the resolve Thirdparty Libraries in Liferay OSGi.
I hope to help feature developer to understand how we can easily add new Third party Libraries to Liferay and use them.

## Simplest Way
There are already many libraries which are running in OSGi. For example the google guava project already runs in OSGi.
Therefore we always should check if the desired library is already OSGi runnable. For this we simple deploy it in OSGi
and it might run in it. 
We should get logs like:
```less
[com.liferay.portal.kernel.deploy.auto.AutoDeployScanner][AutoDeployDir:259] Processing guava-19.0.jar
[Refresh Thread: Equinox Container: 29094b68-db73-4547-977d-109c13256dc1][BundleStartStopLogger:39] STARTED com.google.guava_19.0.0 [10651]
```
If we only see the first log entry, we know it is not OSGi able. Another way to see it, it to open the jar with an zip-tool
and open the manifest file in META-INF. If it's almost empty, it is usually no OSGi bundle. An OSGi should have following entries.
```less
Bundle-Name: Guava: Google Core Libraries for Java
Export-Package: com.google.common.net;uses:="javax.annotation,com.google
 .common.base,com.google.common.hash
Import-Package: javax.annotation;resolution:=optional,sun.misc;resolutio
 n:=optional
Tool: Bnd-1.50.0
```
The export-statement shows, which packages are provided. 
The import-statement shows, which packages are needed
More about it later, but if you wanna know more, you can read about it here:
[More Information](https://blog.christianposta.com/osgi/understanding-how-osgi-bundles-get-resolved-part-i/)

## Basic of Wrapping
To use a not OSGi able thirdparty library we wrap them into a OSGi bundle. This can be done With liferay:
See module: Intercom-Include
We simple tell Gradle, more exactly the bnd.bnd tools to include this library to our bundle. This tool resolves the
dependencies. In the end, the tools makes almost the same as in module intercom-include-by-hand. But when we try to
start it we get the following log:
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
As you can see, we have an error, which tells us, that the module is missing a specific package. This package is found
by the bnd.bnd tool and written into the previous mentioned Import-Packages.
These packages must be provided on the startup from the bundle via the export statement (Export-Package) from another module.
To solve this, we have different option:
* remove the import from this package from the Manifest file
* add the package with compile-include
* find a OSGi module, which is providing this packages
With practice you will know, which packages need to be included, which have OSGi bundles and which we can exclude from the import.

### Find on OSGi Module
The most modern Libraries have an OSGi solution. Therefore it is often helpful to search for the jar, download it. And check ther dependencies.
Either with trying to start them, or to look into the manifest file.
Log when tried to start in OSGi.
```less
Unresolved requirement: Import-Package: org.slf4j.impl_    -> Export-Package: org.slf4j.impl; bundle-symbolic-name="slf4j.log4j12";
 bundle-version="1.7.21"; version="1.7.21"_       slf4j.log4j12 [2159]_         
Unresolved requirement: Fragment-Host: slf4j.api_         
Unresolved requirement: Import-Package: org.slf4j; version="1.7.21"_ [Sanitized]
        at org.eclipse.osgi.container.Module.start(Module.java:444)
```
If we compare it with the manifest file:
```less
Import-Package: org.slf4j;version=1.6.0,org.slf4j.helpers;version=1.6.0,org.slf4j.spi;version=1.6.0
```
We see that we need another slf4j package, but not exactly which one. If we would have checked the listed compile dependencies on maven central.
We would have seen, that we need as well the slf4j-api. If we deploy this as well, both module will start without trouble.
This shows as, that the dependencies listen on maven central or in gradle under source sets are important information to 
solve dependencies issue for us.

#### Older project
For older project, where OSGi wasn't so common. There are so called Service-Packs, which are wrapping common jar as OSGi modules.
You can search for them as following:
```less
  servicemix::Bundles::jar-name  
```
This can be very usefull when working with older packages like axis-2

### Add the jar with the missing package
A really simple way, is to add all jar with missing packages to the same module. This will solve our most dependencies issues.
But lead the a fat-jar. The fat-jar is not really common in OSGi, because we wanna share the jar under each other. If we
compile include them all in one Module, only this module has access to them.
One way would be to wrap them in there own module, and export the needed packages to the OSGi environment.
This can be down with the following statement in a bnd.bnd fiel:
```less
Export-Package:org.slf4j.*
```
Luckily we can use wildcards in bnd.bnd files. We can simple expose all package from the root and there export the needed package as well.
This we also need to know, if we develop different services in Liferay, if we wanna separate our code.
### Remove the package from the import
Some jar have dependencies to really old packages, which are not really used. But OSGi wanna import them.
This is as well the case in the example of the fop wrapper.
In its bnd.bnd file, we see following import statement:
```less
Import-Package: \
    !com.sun.*,\
    !gnu.gcj*,\
    !junit.*,\
    !kaffe.util.*,\
    !org.apache.avalon.framework.logger,\
    !org.apache.env,\
    !org.apache.harmony.luni.util,\
    !org.apache.log,\
    !org.mozilla.javascript,\
    !org.python.*,\
    !sun.*,\
    !weblogic,\
    !org.json,\
    !org.apache.*,\
    *
```
With the syntax: !(not) package.name.Wildcard, we can exclude all this package from the import statement of the manifest.
If there are no more missing packages, we can start the module and try to use it. You need to be aware, that this can 
cause class-def not found issues, which are occurring on runtime. Therefore are the previous method better to solve
missing package issue, but for old packages we need this. And experience shows, that this packages (classes) are really rarely used att all.

If we use the Import-Package statement for anything, we need to follow this shown syntax. Most importantly we must keep the
(*) at the end of the statement. Otherwise no packages will be imported on runtime, and we will get a class not def error.

#Conclusion
To add third Party Library to Liferay is usually not really complicated, because most of them are already OSGi modules.
If they are not, we can wrap them with compile-include in one and expose the needed packages to the outside. Only for old
jar's we need to exclude specific packages from the import statement, to make them run.

I hope this example helps you to understand the way, how dependencies are solved in Liferay, and explains it better than the documentation of Liferay itself.
Feel free to make changes on this repository.