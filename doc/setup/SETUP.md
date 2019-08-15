## Setup Guide for Artemis

In this guide you learn how to setup the development environment of Artemis.
Artemis is based on [JHipster](https://jhipster.github.io), i.e. [Spring Boot](http://projects.spring.io/spring-boot) development on the application server using Java 12, and TypeScript development on the application client in the browser using [Angular 8](https://angular.io) and Webpack. To get an overview of the used technology, have a look at [https://jhipster.github.io/tech-stack](https://jhipster.github.io/tech-stack) and other tutorials on the JHipster homepage.  

You can find tutorials how to setup JHipster in an IDE ([IntelliJ](https://www.jetbrains.com/idea) is recommended, but it also runs in other IDEs as well) on [https://jhipster.github.io/configuring-ide](https://jhipster.github.io/configuring-ide).
Before you can build Artemis, you must install and configure the following dependencies/tools on your machine:

1. [Java 12 JDK](https://www.oracle.com/technetwork/java/javase/downloads/jdk12-downloads-5295953.html): Java is the main development language for the server application of Artemis.
2. [MySQL Database Server 5.7.x](https://dev.mysql.com/downloads/mysql): Artemis uses Hibernate to store entities in a MySQL database. Download and install the MySQL Community Server (5.7.x) and configure the 'root' user with an empty password. (In case you want to use a different password, make sure to change the value in application-dev.yml and in liquibase.gradle). The required Artemis scheme will be created / updated automatically at startup time of the server application. (Please note that MySQL 8 is not yet supported)
3. [Node.js](https://nodejs.org): We use Node (>=12.3.1) to run a development web server and build the project. Depending on your system, you can install Node either from source or as a pre-packaged bundle. 
4. [Yarn](https://yarnpkg.com): We use Yarn (>=1.15.2) to manage Node dependencies.
Depending on your system, you can install Yarn either from source or as a pre-packaged bundle.

### Server Setup

To start the Artemis application server from the development environment, first import the project into IntelliJ and then make sure to install the Spring Boot plugins to run the main class de.tum.in.www1.artemis.ArtemisApp. Before the application runs, you have to configure the file `application-artemis.yml` in the folder `src/main/resources/config`. 

```yaml
artemis:
  repo-clone-path: ./repos/
  encryption-password: <encrypt-password>
  jira:
    url: https://jirabruegge.in.tum.de
    user: <username>
    password: <password>
    admin-group-name: tumuser
  version-control:
    url: https://repobruegge.in.tum.de
    user: <username>
    secret: <password>
  bamboo:
    url: https://bamboobruegge.in.tum.de
    bitbucket-application-link-id: de1bf2e0-eb40-3a2d-9494-93cbe2e22d08
    user: <username>
    password: <password>
    empty-commit-necessary: true
    authentication-token: <token>
  lti:
    id: artemis_lti
    oauth-key: artemis_lti_key
    oauth-secret: <secret>
    user-prefix_edx: edx_
    user-prefix_u4i: u4i_
    user-group-name_edx: edx
    user-group-name_u4i: u4i
  git:
    name: artemis
    email: artemis@in.tum.de
```
Change all entries with ```<...>``` with proper values, e.g. your TUM Online account credentials to connect to the given instances of JIRA, Bitbucket and Bamboo. Alternatively, you can connect to your local JIRA, Bitbucket and Bamboo instances.
Be careful that you don't commit changes in this file. Best practice is to specify that your local git repository ignores this file or assumes that this file is unchanged. 

The Artemis server should startup by running the main class ```de.tum.in.www1.artemis.ArtemisApp``` using Spring Boot.

One typical problem in the development setup is that an exception occurs during the database initialization. Artemis uses [Liquibase](https://www.liquibase.org) to automatically upgrade the database scheme after changes to the data model. This ensures that the changes can also be applied to the production server. In some development environments, it can be the case that the liquibase migration from an empty database scheme to the current version of the database scheme fails, e.g. due to the fact that the asynchronous migration is too slow. In these cases, it can help to manually import an existing database scheme using e.g. MySQL Workbench or Sequel Pro into the `Artemis` database scheme in your MySQL server. You can find a recent scheme in the `data` folder in this git repository. If you then start the application server, liquibase will recognize that all migration steps have already been executed. In case you encounter errors with liquibase checksum values, run the following command in your terminal / command line:

```
java -jar liquibase-core-3.5.3.jar --url=jdbc:mysql://localhost:3306/ArTEMiS --username=root --password='' --classpath=mysql-connector-java-5.1.43.jar  clearCheckSums
```
You can download the required jar files here:

* [liquibase-core-3.5.3.jar](http://central.maven.org/maven2/org/liquibase/liquibase-core/3.5.3/liquibase-core-3.5.3.jar)
* [mysql-connector-java-5.1.43.jar](http://central.maven.org/maven2/mysql/mysql-connector-java/5.1.43/mysql-connector-java-5.1.43.jar)

As an alternative you can use this gradle command:

```
./gradlew liquibaseClearChecksums
```

If you use a password, you need to adapt it in Artemis/gradle/liquibase.gradle.

**Please note:** Artemis uses Spring profiles to segregate parts of the application configuration and make it only available in certain environments. For development purposes, the following program arguments can be used to enable the `dev` profile and the profiles for JIRA, Bitbucket and Bamboo:

    --spring.profiles.active=dev,bamboo,bitbucket,jira,artemis

### Client Setup

After installing Node, you should be able to run the following command to install development tools. You will only need to run this command when dependencies change in [package.json](package.json).

```
yarn install
```

To start the client application in the browser, use the following command:

```
yarn start
```

This compiles TypeScript code to JavaScript code, starts the hot module replacement feature in Webpack (i.e. whenever you change a TypeScript file and save, the client is automatically reloaded with the new code) and will start the client application in your browser on `http://localhost:9000`. If you have activated the JIRA profile (see above in Server Setup) and if you have configured `application-artemis.yml` correctly, then you should be able to login with your TUM Online account.

For more information, review [Working with Angular](https://www.jhipster.tech/development/#working-with-angular). For further instructions on how to develop with JHipster, have a look at [Using JHipster in development](http://www.jhipster.tech/development).

### Using docker-compose

A full functioning development environment can also be set up using docker-compose: 

1. Install [docker](https://docs.docker.com/install/) and [docker-compose](https://docs.docker.com/compose/install/)
2. Configure the credentials in `application-artemis.yml` in the folder `src/main/resources/config` as described above
3. Run `docker-compose up`
4. Go to [http://localhost:9000](http://localhost:9000)

The client and the server will run in different containers. As yarn is used with its live reload mode to build and run the client, any change in the client's codebase will trigger a rebuild automatically. In case of changes in the codebase of the server one has to restart the `artemis-server` container via `docker-compose restart artemis-server`.

(Native) Running and Debugging from IDEs is currently not supported.

**Get a shell into the containers:**

* app container: `docker exec -it $(docker-compose ps -q artemis-app) sh`
* mysql container: `docker exec -it $(docker-compose ps -q artemis-mysql) mysql`

**Other useful commands:**

* Stop the server: `docker-compose stop artemis-server` (restart via `docker-compose start artemis-server`)
* Stop the client: `docker-compose stop artemis-client` (restart via `docker-compose start artemis-client`)

### Text Assessment Clustering Service

The semi-automatic text assessment relies on the text assessment clustering (TAC) service, which is currently closed-source. (Contact @jpbernius if you require access.)

To enable automatic text assessments, special configuration is required:

**Enable the `automaticText` Spring profile:**

```
--spring.profiles.active=dev,bamboo,bitbucket,jira,artemis,automaticText
```

**Configure API Endpoints**:

The TAC service is running on a dedicated machine and is adressed via HTTP. We need to extend the configuration in the file `src/main/resources/config/application-artemis.yml` like so:

```yaml
artemis:
  # ...
  automatic-text:
    embedding-url: http://localhost:8000/embed
    embedding-chunk-size: 50
    clustering-url: http://localhost:8000/cluster
    secret: null
```

