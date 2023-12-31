# Java Coding Challenge - Task Management API Suite

## Background
I created this application as part of a job application for a position at an IT consultancy in Wellington. There was a small amount of out-dated code initially provided, with no API and a partial database implementation. There was also a request to keep to the same technologies and write the API to a 'production standard'.

Based on my experience with the company, the interview pannel do not review this material. 

## Getting started

### Building the application

From the project root folder: 

`mvn clean install`

### Running the application

From the maven target directory:

`java -jar task-api-jar-with-dependencies.jar`

### As built documentation

Once the application is built and running, as built documentation is available (using default configuration) at [http://localhost:8080](http://localhost:8080).

### Troubleshooting

The application will not build if the tests fail. There are two likely reasons why the tests might fail.

1) Port 8080 is already in use. If this is the case then it should be fairly clear from the log. The solution to this is to free up port 8080 so the application will build or configure the application to build on a different port.
2) The test cases do not match the previous snapshots. If this is the case then this should also be visible in the log output. The solution to this issue is to delete the previous test snapshots (or in the case of development fix whatever is causing the tests to fail). The snapshots are stored in the `src/test/java/nz/co/snapshot/api_test/__snapshots__` folder.
