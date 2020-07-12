# big-store-wti
## Intro
### Abstract
Big Store is a highly efficient system for storing large amounts of data with warranted GET time.

### Table of contents
  - [Intro](#intro)
    - [Abstract](#abstract)
    - [Table of contents](#table-of-contents)
  - [About](#about)
    - [Motivation](#motivation)
    - [General assumptions](#general-assumptions)
    - [Proposed solution](#proposed-solution)
    - [Technologies](#technologies)
    - [Sample deployment](#sample-deployment)
    - [Data flows](#data-flows)
      - [Data inflow](#data-inflow)
      - [Data outflow](#data-outflow)
    - [Future improvements](#future-improvements)
  - [Implementation state](#implementation-state)
  - [Deployment](#deployment)
    - [Preliminary](#preliminary)
    - [Build](#build)
    - [Run](#run)
  - [License](#license)
  - [Credits](#credits)

## About
### Motivation
Modern systems often require strict time constraints to be met in order to assure desired quality of service or SLA. This specificly applies to many distributed systems, online machine learning and end user solutions. With a great scale come immersive amounts of data. Storage access can be a bottleneck for every application. A reliable resolution of this problem may lead to a giantic improvement of preformance in all kind of large scale solutions. Big Store aims to be a POC in a way to fighting these problems.

### General assumptions
Several assumptions about data can be made, that applies to a great part of modern use cases:
1. Data should be saved and accessed in a concurrent safe process.
2. Storage should be redundant and scalable.
3. Data should be accesible fast enough to use it for online actions.
4. Retrieved data must be as fresh as possible, but not neceesarly the most fresh available.

### Proposed solution
Big Store is a data storage system. It is desinged to handle high voulume of income and outcome traffic, side-by-side enabling users to maintain stable data retrieve time. BS is based on two data levels. The data is stored in the main high volume store, which is managed by several _companions_, that are bounding the _store_ with cache storage. 

Each request for retrieving data is directed by the _Hub_ to the _companion_ assigned to desired part of store. The _companion_ then is trying to provide the client with the most fresh data available. It achievies so, by quering cache first and if the data is fresh enough it returns it. In case the data is present in cache, but to old to be considered as possible most fresh, the _companion_ ask the _store_ to retrieve the data. If the _store_ won't manage to handle the requst on time, the entity from cache is returned to the client. Late queries to the _store_ are not cancelled, the are used  by a backgroud process to update the cache.

Saving data into the _store_ holds asynchronously. After entering the data, the client is provided with a promise, the data will be evantually saved in the _store_. All requests are being directed by the _Hub_ to the _companion_ assigned to desired part of store. A background process is then queueing the data to ba saved and is saving it maintaining the order.

Access to the system can be achieved by REST API and async _RabbitMQ_ queue. REST provides the ability to update and retrieve the data. Asyncronous API is used only for updating data in the system.

### Technologies
Such use case requires, that the technology will reliably handle long time runs with countnuos heavy load. One of the best available solution, that meets these constraints, is JVM. Java Virtual Machine was designed speciffically to be run under high traffic conditions. That is making JVM natural choice for Big Store.

_Hub_ is implemented in _Scala_ using _akka HTTP_. As main role of it is to direct the traffic to correct _companions_, _akka_ actors makes it perfect tool for that. 

_Companions_ uses _Kotlin_ with _courutines_. That allows it to handle concurrent jobs (like incoming GETs or background queries), requiring very few resources. Which is crutial, since BS will spawn multiple _companions_ at once.

Data is stored in _Cassandra_, because of its ability od fast inserts and high scalabitlity. For cache _Redis_ was chosen.

### Sample deployment
Figure below shows Big Store deployed with 3 companions and both REST and async API enabled.

![Schema of sample deployment with 3 companions](docs/schema/big-store-schema.png)

### Data flows
#### Data inflow
Figure below shows the flow of input data in Big Store.

![Schema of data inflow](docs/schema/big-store-inflow-schema.png)

#### Data outflow
Figure below shows the flow of output data in Big Store.

![Schema of data outflow](docs/schema/big-store-outflow-schema.png)

### Future improvements
tba

## Implementation state
tba

## Deployment
### Preliminary
tba

### Build
tba

### Run
tba

## License
tba

## Credits
Big Store was made by Jakub Riegel and supervised by Andrzej Szwabe, PhD as a project for Selected Internet Technologies Course on Poznań University of Technology.