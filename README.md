# TeamCity Automated Tests

This repository is a part of the solution for the test assignment for the TeamCity recruitment process for the
position of QA Automation Engineer. The full report can be found in the [TeamCity Test Tasks Overview](TeamCity_Test_Tasks.pdf).

## Prerequisites

- run a TeamCity server instance version 2022.10.2 (build 117025)
- have access to a administrator user on that instance
- sample git project using Maven, without a predefined Kotlin based pipeline
- install Chromedriver and Google Chrome
- install Maven 3 (version 3.8.1) and OpenJDK 19

## Setup

Copy the .env.example file and rename it as .env and adjust the environment variables accordingly

```bash
cp .env.example .env
```

## Run the tests

Execute the test maven goal

```bash
./mvnw test
```