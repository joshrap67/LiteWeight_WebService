# LiteWeight WebService

This web service exposes a REST API intended to be consumed by the Android Application [Liteweight](https://github.com/joshrap67/LiteWeight) (compatible with versions >= 2.2.x)

Refer to the [Wiki](https://github.com/joshrap67/LiteWeight_WebService/wiki) for details on the implementation of the service.

## Prerequisites

The bulk of the service is written in Java (using Java 11).

The auto confirm lambda code is written in python (Python 3.7.0)

The script for building the for auto confirm package requires pip.

[Maven](https://maven.apache.org/) is required to build the web service jar.

[7zip](https://www.7-zip.org/download.html) for building the auto confirm package.

For development Intellij IDEA Community Edition was used.

## Deployment

To build the web service Jar, run the following command in the src directory:

```
mvn clean install
```

The jar will be put in the build directory. Upload that jar to the Proxy Endpoint on lambda (using developer credentials of course).

To build the auto confirm users package just run the batch script in the auto_confirm_users directory.

## Authors

- Joshua Rapoport - *Creator and Lead Software Developer*

## Acknowledgments

Default Profile Picture made by [Smashicons](https://www.flaticon.com/free-icon/user_149071)
