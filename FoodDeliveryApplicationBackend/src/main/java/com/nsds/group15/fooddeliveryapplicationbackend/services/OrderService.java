package com.nsds.group15.fooddeliveryapplicationbackend.services;

import com.nsds.group15.fooddeliveryapplicationbackend.entity.Customer;
import com.nsds.group15.fooddeliveryapplicationbackend.entity.Order;
import com.nsds.group15.fooddeliveryapplicationbackend.entity.OrderStatus;
import com.nsds.group15.fooddeliveryapplicationbackend.exception.*;
import com.nsds.group15.fooddeliveryapplicationbackend.utils.MessagesUtilities;
import com.nsds.group15.fooddeliveryapplicationbackend.utils.ProducerConsumerFactory;
import com.nsds.group15.fooddeliveryapplicationbackend.utils.Topics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Future;

@Service
@ConditionalOnProperty(name="order.enabled", havingValue = "true", matchIfMissing = true)
public class OrderService {

    OrderServiceOrderConsumer orderServiceOrderConsumer;

    private static int id=0;

    private Map<Integer, Order> orders;
    private Map<String, Integer> productQuantity;
    private Map<String,Customer> customers;

    private  String serverAddr = "localhost:9092";

    private KafkaProducer<String,String> orderProducer;
    private KafkaProducer<String,String> productsProducer;
    private static final String producerTransactionalId = "OrderServiceTransactionalId";


    private KafkaConsumer<String,String> customerConsumer;
    private static final String offsetResetStrategy = "earliest";
    private static final String isolationLevelStrategy="read_committed";

    public OrderService(){

        System.out.print("Insert broker address: ");
        Scanner sc = new Scanner(System.in);
        serverAddr = sc.nextLine();
        sc.close();

        productQuantity=Collections.synchronizedMap(new HashMap<>());
        orders=new HashMap<>();
        customers= new HashMap<>();  //updateListOfOrders();

        orderProducer = ProducerConsumerFactory.initializeTransactionalProducer(serverAddr, producerTransactionalId);
        orderServiceOrderConsumer=new OrderServiceOrderConsumer();
        orderServiceOrderConsumer.start();

        productsProducer= ProducerConsumerFactory.initializeProducer(serverAddr);
        customerConsumer = ProducerConsumerFactory.initializeRecoverConsumer(serverAddr, "orderRegistrationGroup", isolationLevelStrategy);
        customerConsumer.subscribe(Collections.singletonList(Topics.CUSTOMER_TOPIC));

        recoverCustomers();
        recoverProducts();
        recoverOrders();

    }

    public void addProduct(String productName, int quantity) throws ProductAlreadyExistsException, NegativeQuantityException {

        if(productQuantity.containsKey(productName)){
            throw new ProductAlreadyExistsException();
        }
        else if(quantity<0) throw new NegativeQuantityException();
        else{
            productQuantity.put(productName,quantity);
            String product=productName+"#"+quantity;
            ProducerRecord<String,String> record = new ProducerRecord<String,String>(Topics.PRODUCT_TOPIC,productName,product);
            productsProducer.send(record);
            MessagesUtilities.printRecord(record, "Order Service");
        }

    }

    public void updateQuantity(String productName, int quantity) throws ProductDoNotExistsException, NegativeQuantityException {

        if(!productQuantity.containsKey(productName)){
            throw new ProductDoNotExistsException();
        }
        else if(quantity<0) throw new NegativeQuantityException();
        else{
            int newQuantity=productQuantity.get(productName);
            newQuantity=newQuantity+quantity;
            productQuantity.put(productName,newQuantity);
            String product=productName+"#"+quantity;
            System.out.println(product);
            ProducerRecord<String,String> record = new ProducerRecord<>(Topics.PRODUCT_TOPIC, productName, product);
            productsProducer.send(record);
        }

    }

    public Map<String, Integer> getProducts(){

        return productQuantity;

    }

    /* public void insertOrder(Order o) throws QuantityNotAvailableException, NegativeQuantityException, NoSuchUserException {
        updateListOfCustomers();
        orderProducer.beginTransaction();
        if(!productQuantity.containsKey(o.getProductName())){orderProducer.abortTransaction(); return;}
        if(!customers.containsKey(o.getCustomerEmail())) { orderProducer.abortTransaction(); throw new NoSuchUserException();}
        int quantity=productQuantity.get(o.getProductName());
        int newQuantity=quantity-o.getQuantity();
        if(o.getQuantity()<0) { orderProducer.abortTransaction(); throw new NegativeQuantityException();}
        if(newQuantity<0) {orderProducer.abortTransaction(); throw new QuantityNotAvailableException();}
        synchronized (this){
            o.setCode(id);
            id++;
        }
        String orderMessage=o.getCode()+"#"+o.getCustomerEmail();
        ProducerRecord<String, String> record = new ProducerRecord<>(insertOrderTopic,""+o.getCode(), orderMessage);
        ProducerRecord<String,String> recordProduct = new ProducerRecord<>(productsTopic, o.getProductName(), o.getProductName()+"#"+newQuantity);
        final Future<RecordMetadata> futureProduct= productsProducer.send(recordProduct);
        final Future<RecordMetadata> future = orderProducer.send(record);
        try {
            RecordMetadata ack = future.get();
            RecordMetadata ackOrder = futureProduct.get();
            productQuantity.put(o.getProductName(),newQuantity);
            orders.put(o.getCode(), o);
            System.out.println("Success!");
        } catch (Exception e) {
            orderProducer.abortTransaction();
            e.printStackTrace();
        }
        orderProducer.commitTransaction();
    }*/

    public void insertOrder(Order order) throws QuantityNotAvailableException, NegativeQuantityException, NoSuchUserException, ProductDoNotExistsException {

        updateListOfCustomers();

        if(!productQuantity.containsKey(order.getProductName())) { throw new ProductDoNotExistsException();}
        if(!customers.containsKey(order.getCustomerEmail())) throw new NoSuchUserException();
        int quantity=productQuantity.get(order.getProductName());
        int newQuantity=quantity-order.getQuantity();
        if(order.getQuantity()<0) throw new NegativeQuantityException();
        if(newQuantity<0) throw new QuantityNotAvailableException();
        synchronized (this){
            order.setCode(id);
            id++;
        }
        String orderMessage=order.getCode()+"#"+order.getCustomerEmail()+"#"+order.getProductName()+"#"+order.getQuantity()+"#"+order.getShippingAddress();
        orderProducer.beginTransaction();
        ProducerRecord<String, String> record = new ProducerRecord<>(Topics.ORDER_TOPIC,""+order.getCode(), orderMessage+"#"+OrderStatus.TO_BE_SHIPPED);
        ProducerRecord<String,String> recordProduct = new ProducerRecord<>(Topics.PRODUCT_TOPIC, order.getProductName(), order.getProductName()+"#"+newQuantity);
        final Future<RecordMetadata> futureProduct= productsProducer.send(recordProduct);
        final Future<RecordMetadata> future = orderProducer.send(record);
        try {
            RecordMetadata ack = future.get(); //This and the following instruction are blocking
            RecordMetadata ackOrder = futureProduct.get();
            order.setStatus(OrderStatus.TO_BE_SHIPPED);
            System.out.println(order.toString()+" has been validated and sent for shipping");
            productQuantity.put(order.getProductName(),newQuantity);
            orders.put(order.getCode(), order);
        } catch (Exception e) {
            orderProducer.abortTransaction();
            e.printStackTrace();
        }
        orderProducer.commitTransaction();

    }

    public List<Order> getOrdersByEmail(String email) throws NoSuchUserException{

        updateListOfCustomers();
        if(!customers.containsKey(email)) throw new NoSuchUserException();
        List<Order> result=new ArrayList<>();
        for(int orderCode : orders.keySet()){
            if(orders.get(orderCode).getCustomerEmail().equals(email)){
                result.add(orders.get(orderCode));
            }
        }
        return result;

    }

    private void updateListOfCustomers(){

        final ConsumerRecords<String, String> records = customerConsumer.poll(Duration.of(1, ChronoUnit.SECONDS));
        for (final ConsumerRecord<String, String> record : records) {
            Customer customer = new Customer(record.value());
            customers.put(customer.getEmail(), customer);
            MessagesUtilities.printRecord(record, "OrderService");
            System.out.println(customer);
        }

    }

    private void recoverCustomers(){

        KafkaConsumer<String,String> customerConsumer= ProducerConsumerFactory.initializeRecoverConsumer(serverAddr, "OrderServiceRecoverCustomers", isolationLevelStrategy);
        customerConsumer.subscribe(Collections.singletonList(Topics.CUSTOMER_TOPIC));
        int counter=0;
        if(customers.isEmpty()) {
            customerConsumer.poll(0);
            customerConsumer.seekToBeginning(customerConsumer.assignment());
            ConsumerRecords<String, String> records = customerConsumer.poll(Duration.of(1, ChronoUnit.SECONDS));
            for (ConsumerRecord<String, String> record : records) {
                record.value();
                Customer c = new Customer(record.value());
                customers.put(c.getEmail(), c);
                counter++;
            }
            System.out.println(counter + "  messages for topic " + Topics.CUSTOMER_TOPIC + " succesfully retrieved by Order Service");
            customers.keySet().forEach((value) -> System.out.print(customers.get(value)));
            System.out.println(" retrieved");
        }
        customerConsumer.close();

    }

    private void recoverProducts(){

        KafkaConsumer recoverProductsConsumer = ProducerConsumerFactory.initializeRecoverConsumer(serverAddr,"OrderServiceRecoverProducts",isolationLevelStrategy);
        recoverProductsConsumer.subscribe(Collections.singletonList(Topics.PRODUCT_TOPIC));
        int counter=0;
        if(productQuantity.isEmpty()) {
            recoverProductsConsumer.poll(0);
            recoverProductsConsumer.seekToBeginning(recoverProductsConsumer.assignment());;
            ConsumerRecords<String, String> records = recoverProductsConsumer.poll(Duration.of(1, ChronoUnit.SECONDS));
            for (ConsumerRecord<String, String> record : records) {
                String[] keyValue=record.value().split("#");
                productQuantity.put(keyValue[0], Integer.parseInt(keyValue[1]));
                counter++;
            }
            System.out.println(counter + " messages for topic " + Topics.PRODUCT_TOPIC + " succesfully retrieved");
            System.out.println(productQuantity.keySet());
            productQuantity.keySet().forEach((value) -> System.out.println(value+" with quantity "+productQuantity.get(value)));
        }
        recoverProductsConsumer.close();

    }

    private void recoverOrders(){

        KafkaConsumer<String, String> orderConsumer = ProducerConsumerFactory.initializeRecoverConsumer(serverAddr, "orderServiceRecoverOrders", isolationLevelStrategy);
        orderConsumer.subscribe(Collections.singletonList(Topics.ORDER_TOPIC));
        int counter=0;
        if(orders.isEmpty()) {
            orderConsumer.poll(0);
            orderConsumer.seekToBeginning(orderConsumer.assignment());
            ConsumerRecords<String, String> records = orderConsumer.poll(Duration.of(1, ChronoUnit.SECONDS));
            int maxId=0;
            for (ConsumerRecord<String, String> record : records) {
                System.out.println(record.value());
                Order o = new Order(record.value());
                orders.put(o.getCode(), o);
                if(o.getCode()>maxId) maxId=o.getCode();
                counter++;
            }
            id=maxId+1;
            System.out.println(counter + "  messages for topic " + Topics.ORDER_TOPIC + " succesfully retrieved by OrderService");
            orders.keySet().forEach((value) -> System.out.print(orders.get(value)));
            System.out.println("retrieved");
        }
        orderConsumer.close();

    }

    class OrderServiceOrderConsumer extends Thread{

        KafkaConsumer<String,String> orderConsumer;
        public void run(){

            orderConsumer=ProducerConsumerFactory.initializeRecoverConsumer(serverAddr, "orderConsumerGroup", isolationLevelStrategy);
            orderConsumer.subscribe(Collections.singletonList(Topics.ORDER_TOPIC));
            while (true){
                final ConsumerRecords<String, String> records = orderConsumer.poll(Duration.of(30, ChronoUnit.SECONDS));
                for (final ConsumerRecord<String, String> record : records) {
                    Order temp =new Order(record.value());
                    System.out.println(temp.getStatus());
                    if(temp.getStatus()==OrderStatus.ABORTED){
                        try{
                            updateQuantity(temp.getProductName(),temp.getQuantity());
                            MessagesUtilities.printRecord(record, "OrderService");
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }

        }

    }

    public static void main(String[] args) throws NegativeQuantityException, NoSuchUserException, QuantityNotAvailableException, ProductAlreadyExistsException {
    }
}


