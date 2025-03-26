package com.nsds.group15.fooddeliveryapplicationbackend.services;
import com.nsds.group15.fooddeliveryapplicationbackend.entity.Order;
import com.nsds.group15.fooddeliveryapplicationbackend.entity.OrderStatus;
import com.nsds.group15.fooddeliveryapplicationbackend.exception.AbortedOrderException;
import com.nsds.group15.fooddeliveryapplicationbackend.exception.OrderDoNotExists;
import com.nsds.group15.fooddeliveryapplicationbackend.utils.MessagesUtilities;
import com.nsds.group15.fooddeliveryapplicationbackend.utils.ProducerConsumerFactory;
import com.nsds.group15.fooddeliveryapplicationbackend.utils.Topics;
import org.apache.kafka.clients.consumer.*;
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
@ConditionalOnProperty(name="shipping.enabled", havingValue = "true", matchIfMissing = true)
public class ShippingService {

    private Map<Integer, Order> orders;
    private static  String serverAddr = "localhost:9092";
    private static final String isolationLevelStrategy = "read_committed";
    private KafkaProducer<String,String> shippedOrderproducer;
    private static final String producerTransactionalId = "ShippedProducerTransactionalId";

    public ShippingService(){

        System.out.print("Insert broker address: ");
        Scanner sc = new Scanner(System.in);
        serverAddr = sc.nextLine();
        sc.close();

        this.orders=new HashMap<>();
        ShippingServiceOrderProducerConsumer shippingServiceOrderProducerConsumer=new ShippingServiceOrderProducerConsumer();
        shippingServiceOrderProducerConsumer.start();
        shippedOrderproducer=ProducerConsumerFactory.initializeTransactionalProducer(serverAddr,producerTransactionalId);

        recoverOrders();

    }

    private Boolean validateAddress(String serverAddr){

        Random r = new Random();
        return 1+r.nextInt(100) >= 20;  //%20 probability of not being valid

    }

    /*private void updateListOfOrders(){

        final ConsumerRecords<String, String> records = orderConsumer.poll(Duration.of(1, ChronoUnit.SECONDS));

    }*/

    /*
    private void updateListOfShippings(){
        try{
            shippingProducer.initTransactions();
            final ConsumerRecords<String,String> records = orderConsumer.poll(Duration.of(10,ChronoUnit.SECONDS));
            for(final ConsumerRecord<String,String> record: records){
                Shipping shipping = new Shipping(record.value());
                Customer customer=customers.get(shipping.getCustomerEmail());
                shipping.setAddress(customer.getAddress());
                MessagesUtilities.printRecord(record, "ShippingService");
                ProducerRecord<String,String> prodRecord=new ProducerRecord<>(shipping.getOrderCode()+"", shipping.toToken());
                shippingProducer.send(prodRecord);
            }
            shippingProducer.commitTransaction();
        }
        catch(Exception e){
            e.printStackTrace();
            shippingProducer.abortTransaction();
        }

    }*/


    /*private void updateListOfCustomers(){
        final ConsumerRecords<String, String> records = registrationConsumer.poll(Duration.of(10, ChronoUnit.SECONDS));
        for (final ConsumerRecord<String, String> record : records) {
            StringTokenizer stringTokenizer = new StringTokenizer(recorlp l,l-  xx556d.value(), "#");
            Customer customer = new Customer("", "", "", "");
            customer.setEmail(stringTokenizer.nextToken());
            customer.setName(stringTokenizer.nextToken());
            customer.setSurname(stringTokenizer.nextToken());
            customer.setAddress(stringTokenizer.nextToken());
            customers.put(customer.getEmail(), customer);
            MessagesUtilities.printRecord(record, "ShippingService");

        }
    }*/

    public List<Order> getShippingsByEmail(String email){

        List<Order> result=new ArrayList<>();
        for(Integer i : orders.keySet()){
            if(orders.get(i).getCustomerEmail().equals(email)){
                result.add(orders.get(i));
            }
        }
        return result;

    }

    public List<Order> getShippings(){

        List<Order> result=new ArrayList<>();
        for(Integer i: orders.keySet()){
            result.add(orders.get(i));
        }
        return result;

    }

    public void deliverOrder(int code) throws OrderDoNotExists, AbortedOrderException {

        if(orders.containsKey(code) ){
            Order order=orders.get(code);
            if(order.getStatus().equals(OrderStatus.ABORTED)) throw new AbortedOrderException();
            String orderMessage=order.getCode()+"#"+order.getCustomerEmail()+"#"+order.getProductName()+"#"+order.getQuantity()+"#"+order.getShippingAddress();
            shippedOrderproducer.beginTransaction();
            ProducerRecord<String, String> record = new ProducerRecord<>(Topics.ORDER_TOPIC,""+order.getCode(), orderMessage+"#"+OrderStatus.SHIPPED);
            final Future<RecordMetadata> future = shippedOrderproducer.send(record);
            try {
                RecordMetadata ack = future.get(); //This and the following instruction are blocking
                order.setStatus(OrderStatus.SHIPPED);
                System.out.println(order.toString()+" has been shipped to "+order.getCustomerEmail()+" at address "+order.getShippingAddress());
                orders.put(order.getCode(), order);
            } catch (Exception e) {
                shippedOrderproducer.abortTransaction();
                e.printStackTrace();
            }
            shippedOrderproducer.commitTransaction();
        }
        else throw new OrderDoNotExists();

    }

    private void recoverOrders(){

        KafkaConsumer<String, String> orderConsumer=ProducerConsumerFactory.initializeRecoverConsumer(serverAddr, "ShippingServiceRecoverOrders", isolationLevelStrategy);
        orderConsumer.subscribe(Collections.singletonList(Topics.ORDER_TOPIC));
        int counter=0;
        if(orders.isEmpty()) {
            orderConsumer.poll(0);
            orderConsumer.seekToBeginning(orderConsumer.assignment());
            ConsumerRecords<String, String> records = orderConsumer.poll(Duration.of(1, ChronoUnit.SECONDS));
            for (ConsumerRecord<String, String> record : records) {
                Order o = new Order(record.value());
                orders.put(o.getCode(), o);
                counter++;
            }
            System.out.println(counter + "  messages for topic " + Topics.ORDER_TOPIC + " successfully retrieved by ShippingService");
            orders.keySet().forEach((value) -> System.out.print(orders.get(value)));
            System.out.println("retrieved");
        }
        orderConsumer.close();

    }

    /*private void recoverCustomers(){
        KafkaConsumer recoverConsumer = ProducerConsumerFactory.initializeConsumer(serverAddr,customersGroup,isolationLevelStrategy);
        recoverConsumer.subscribe(Collections.singletonList(customersTopic));
        int counter=0;
        if(customers.isEmpty()) {
            ConsumerRecords<String, String> records = recoverConsumer.poll(Duration.of(0, ChronoUnit.SECONDS));
            recoverConsumer.seekToBeginning(records.partitions());
            for (ConsumerRecord<String, String> record : records) {
                Customer c = new Customer(record.value());
                customers.put(c.getEmail(), c);
                counter++;
            }
            System.out.println(counter + "  messages for topic " + customersTopic + " succesfully retrieved");
            customers.keySet().forEach((value) -> System.out.print(customers.get(value)));
            System.out.println(" retrieved");
        }
        recoverConsumer.unsubscribe();
    }*/

    class ShippingServiceOrderProducerConsumer extends Thread {

        KafkaProducer<String, String> orderProducer;
        private static final String producerTransactionalId = "ShippingServiceTransactionalId";

        KafkaConsumer<String, String> orderConsumer;
        private static final String offsetResetStrategy = "latest";
        private static final String isolationLevelStrategy = "read_committed";

        public ShippingServiceOrderProducerConsumer() {

            orderProducer = ProducerConsumerFactory.initializeTransactionalProducer(serverAddr, producerTransactionalId);
            orderConsumer = ProducerConsumerFactory.initializeRecoverConsumer(serverAddr, "OrderServiceOrderConsumer", isolationLevelStrategy);
            orderConsumer.subscribe(Collections.singletonList(Topics.ORDER_TOPIC));

        }

        public void run() {

            /*If the order status changes to ABORTED a KafkaProducer sends a new message to OrderTopic*/

            while (true) {
                final ConsumerRecords<String, String> records = orderConsumer.poll(Duration.of(30, ChronoUnit.SECONDS));
                for (final ConsumerRecord<String, String> record : records) {
                    Order order = new Order(record.value());
                    if(order.getStatus().equals(OrderStatus.TO_BE_SHIPPED)){
                        if (validateAddress(order.getShippingAddress())) {
                            orders.put(order.getCode(), order);
                            continue;
                        }
                        String orderMessage = order.getCode() + "#" + order.getCustomerEmail() + "#" + order.getProductName() + "#" + order.getQuantity() + "#" + order.getShippingAddress();
                        orderProducer.beginTransaction();
                        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(Topics.ORDER_TOPIC, "" + order.getCode(), orderMessage + "#" + OrderStatus.ABORTED);
                        final Future<RecordMetadata> future = orderProducer.send(producerRecord);
                        try {
                            RecordMetadata ack = future.get();
                            order.setStatus(OrderStatus.ABORTED);
                            orders.put(order.getCode(), order);
                            System.out.println("Order n° " + order.getCode() + "from " + order.getCustomerEmail() + " has been aborted because the specified shipping address do not exists");
                            MessagesUtilities.printRecord(record, "ShippingService");
                        } catch (Exception e) {
                            orderProducer.abortTransaction();
                            e.printStackTrace();
                        }
                        orderProducer.commitTransaction();
                    }
                }
            }
        }

    }

    public static void main(String[] args){

    }

}
