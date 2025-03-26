package com.nsds.group15.fooddeliveryapplicationbackend.services;

import com.nsds.group15.fooddeliveryapplicationbackend.entity.Customer;
import com.nsds.group15.fooddeliveryapplicationbackend.exception.CustomerAlreadyExistsException;
import com.nsds.group15.fooddeliveryapplicationbackend.exception.FailInRegistrationExceptions;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
@Service
@ConditionalOnProperty(name="registration.enabled", havingValue = "true", matchIfMissing = true)
public class CustomerService {

    private List<Customer> customers=new ArrayList<>();

    private  String serverAddr = "localhost:9092";

    private static final String producerTransactionalId = "customerServiceTransactionalId";
    private KafkaProducer<String,String> customerProducer;
    private KafkaConsumer<String,String> customerConsumer;
    private static final String isolationLevelStrategy="read_committed";

    public CustomerService(){

        System.out.print("Insert broker address: ");
        Scanner sc = new Scanner(System.in);
        serverAddr = sc.nextLine();
        sc.close();

        customerProducer=ProducerConsumerFactory.initializeTransactionalProducer(serverAddr,Topics.CUSTOMER_TOPIC);
        customerConsumer=ProducerConsumerFactory.initializeRecoverConsumer(serverAddr, "CustomerServiceCustomerConsumer", isolationLevelStrategy);
        customerConsumer.subscribe(Collections.singletonList(Topics.CUSTOMER_TOPIC));
        recoverCustomers();

    }

    public void registration(Customer c) throws CustomerAlreadyExistsException, FailInRegistrationExceptions {

        if(!customers.contains(c)){
            customerProducer.beginTransaction();
            String value=c.getEmail()+"#"+c.getName()+"#"+c.getSurname()+"#"+c.getAddress();
            String key=c.getEmail();
            ProducerRecord<String, String> record = new ProducerRecord<>(Topics.CUSTOMER_TOPIC, c.getEmail(), value);
            final Future<RecordMetadata> future = customerProducer.send(record);
            try {
                RecordMetadata ack = future.get();
                customers.add(c);
                for(Customer c1:customers){
                    System.out.println(c1.getEmail());
                }
                customerProducer.commitTransaction();
                MessagesUtilities.printRecord(record, "CustomerService");
            } catch (InterruptedException | ExecutionException e1) {
                customerProducer.abortTransaction();
                throw new FailInRegistrationExceptions();
            }
        }
        else {
            throw new CustomerAlreadyExistsException();
        }

    }

    private void recoverCustomers(){

        int counter=0;

        if(customers.isEmpty()){
            customerConsumer.poll(0);
            customerConsumer.seekToBeginning(customerConsumer.assignment());
            ConsumerRecords<String,String> records= customerConsumer.poll(Duration.of(1, ChronoUnit.SECONDS));
            for(ConsumerRecord<String,String> record : records){
                customers.add(new Customer(record.value()));
                counter++;
            }
            System.out.println(counter+ " customers succesfully retrieved by CustomerService");
            customers.forEach((value) -> System.out.println(value.getEmail()));
        }

    }


    public static void main(String[] args) {

        CustomerService cs=new CustomerService();

        for(int i=9;i<11;i++) {
            String email = "e" + i;
            String nome = "n" + i;
            String surname = "s" + i;
            String address = "a" + i;
            Customer c = new Customer(email, nome, surname, address);
            try {
                cs.registration(c);
            } catch (CustomerAlreadyExistsException | FailInRegistrationExceptions e) {
                e.printStackTrace();
            }
        }

    }


}
