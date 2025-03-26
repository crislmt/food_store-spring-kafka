package com.nsds.group15.fooddeliveryapplicationbackend.controllers;

import com.nsds.group15.fooddeliveryapplicationbackend.entity.Order;
import com.nsds.group15.fooddeliveryapplicationbackend.exception.AbortedOrderException;
import com.nsds.group15.fooddeliveryapplicationbackend.exception.OrderDoNotExists;
import com.nsds.group15.fooddeliveryapplicationbackend.services.ShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shippings")
@ConditionalOnProperty(name="shipping.enabled", havingValue = "true", matchIfMissing = true)
public class ShippingController {

    @Autowired
    ShippingService shippingService;

    @GetMapping("/getShippingsByEmail")
    public ResponseEntity getShippingsByEmail(@RequestParam String email){

        try{
            List<Order> shippingList = shippingService.getShippingsByEmail(email);
            return new ResponseEntity(shippingList, HttpStatus.OK);
        }
        catch (Exception e){
            return new ResponseEntity<>("Not ok", HttpStatus.BAD_REQUEST);
        }

    }

    @GetMapping("/getShippings")
    public ResponseEntity getShippings(){

        try{
            return new ResponseEntity(shippingService.getShippings(), HttpStatus.OK);
        }
        catch(Exception e){
            return new ResponseEntity("Not ok", HttpStatus.BAD_REQUEST);
        }

    }

    @PutMapping("/deliverOrder")
    public ResponseEntity<String> deliverOrder(@RequestParam int code){

        try{
            shippingService.deliverOrder(code);
            return new ResponseEntity<>("The order "+code+" has been shipped",HttpStatus.OK);
        }
        catch(OrderDoNotExists odne){
            return new ResponseEntity<>(odne.toString(), HttpStatus.BAD_REQUEST);
        }
        catch (AbortedOrderException aoe){
            return new ResponseEntity<>(aoe.toString(), HttpStatus.BAD_REQUEST);
        }

    }


}
