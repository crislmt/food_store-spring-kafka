# Apache Kafka Food Delivery Application

## Overview
This project is a microservices-based **food delivery application** built using **Spring Boot** and **Apache Kafka** for asynchronous communication between services. The frontend is a simple **Python command-line interface** that interacts with the backend via HTTP REST APIs.

## Features
- **User Registration**: Customers can sign up and store their details.
- **Order Management**: Customers place orders, which are validated before shipping.
- **Shipping Management**: Orders are shipped, and delivery status is updated.
- **Kafka-Based Messaging**: Services interact asynchronously using Kafka topics.
- **Fault Tolerance**: Data is recovered from Kafka logs in case of failures.

## Architecture
The application consists of **three microservices**:

1. **CustomerService**
   - Handles user registration.
   - Stores user data as `User` objects.
   - Publishes user registration events to `CustomerTopic`.

2. **OrderService**
   - Manages order placement and validation.
   - Checks product availability and user subscription.
   - Publishes order events to `OrderTopic`.
   - Monitors `OrderTopic` for shipping updates.

3. **ShippingService**
   - Handles order shipping.
   - Assigns delivery status (`SHIPPED` or `ABORTED`).
   - Publishes shipping status updates to `OrderTopic`.

### Service Interaction with Kafka
- Messages exchanged between services are formatted as **strings** with attributes separated by `#`.
- In case of failures, data is recovered from **Kafka logs**.
- Kafka topics:
  - `CustomerTopic`: Stores customer-related messages.
  - `OrderTopic`: Stores order status updates.
  - `ProductTopic`: Stores product quantity updates.

## Installation & Setup
### Prerequisites
Ensure the following are installed:
- **Java 17+**
- **Maven**
- **Apache Kafka**
- **Python 3**
- **Python Requests library** (`pip install requests`)

### Steps
1. **Start Kafka**
```sh
zookeeper-server-start.sh config/zookeeper.properties
kafka-server-start.sh config/server.properties
```

2. **Configure Application Properties**
Modify `application.properties` located in `src/main/resources` to enable/disable specific services by setting the corresponding flag.

3. **Build the Backend Services** 
```sh
mvn clean
mvn package -DskipTests
```

4. **Run the Backend**
```sh
java -jar target/<jar-file>.jar
```

5. **Run the Frontend**
```sh
cd frontend
python food_delivery_application_frontend.py
```

## Deployment
- Services can be **deployed on virtual machines**.
- One VM acts as the **Kafka broker**.
- Frontend can run on any machine in the local network.

## References
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Documentation](https://docs.spring.io/spring-framework/reference/)

## Contributors
- Bruno Morelli
- Cristian Lo Muto
- Vincenzo Martelli

