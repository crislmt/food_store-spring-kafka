package com.nsds.group15.fooddeliveryapplicationbackend.controllers;

import com.nsds.group15.fooddeliveryapplicationbackend.entity.Order;
import com.nsds.group15.fooddeliveryapplicationbackend.exception.*;
import com.nsds.group15.fooddeliveryapplicationbackend.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
@ConditionalOnProperty(name = "order.enabled", havingValue = "true", matchIfMissing = true)
public class OrderController {

    @Autowired
    OrderService orderService;

    @PutMapping("/addProduct")
    public ResponseEntity addProduct(@RequestParam String productName, @RequestParam int quantity) {

        try {
            orderService.addProduct(productName, quantity);
            return new ResponseEntity<>("Ok", HttpStatus.OK);
        } catch (ProductAlreadyExistsException paee) {
            return new ResponseEntity<>(paee.toString(), HttpStatus.BAD_REQUEST);
        } catch (NegativeQuantityException nqe) {
            return new ResponseEntity<>(nqe.toString(), HttpStatus.BAD_REQUEST);
        }

    }

    ;

    @PostMapping("/updateProduct")
    public ResponseEntity updateProduct(@RequestParam String productName, @RequestParam int quantity) {

        try {
            orderService.updateQuantity(productName, quantity);
            return new ResponseEntity<>("Ok", HttpStatus.OK);
        } catch (ProductDoNotExistsException paee) {
            return new ResponseEntity<>(paee.toString(), HttpStatus.BAD_REQUEST);
        } catch (NegativeQuantityException nqe) {
            return new ResponseEntity<>(nqe.toString(), HttpStatus.BAD_REQUEST);
        }

    }

    ;

    @PostMapping("/insertOrder")
    public ResponseEntity insertOrder(@RequestBody Order order) {

        try {
            orderService.insertOrder(order);
            return new ResponseEntity<>("Ok", HttpStatus.OK);
        } catch (QuantityNotAvailableException paee) {
            return new ResponseEntity<>(paee.toString(), HttpStatus.BAD_REQUEST);
        } catch (NegativeQuantityException nqe) {
            return new ResponseEntity<>(nqe.toString(), HttpStatus.BAD_REQUEST);
        } catch (NoSuchUserException nsue) {
            return new ResponseEntity<>(nsue.toString(), HttpStatus.BAD_REQUEST);
        } catch (ProductDoNotExistsException pdnee) {
            return new ResponseEntity<>(pdnee.toString(), HttpStatus.BAD_REQUEST);
        }

    }

    ;

    @GetMapping("getOrdersByEmail")
    public ResponseEntity getOrdersByEmail(@RequestParam String email) {

        try {
            List<Order> ret = orderService.getOrdersByEmail(email);
            return new ResponseEntity<>(ret, HttpStatus.OK);
        } catch (NoSuchUserException nsue) {
            return new ResponseEntity<>(nsue.toString(), HttpStatus.BAD_REQUEST);
        }

    }

    @GetMapping("getProducts")
    public ResponseEntity getProducts() {

        try {
            Map<String, Integer> map = orderService.getProducts();
            return new ResponseEntity<>(map, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.toString(), HttpStatus.BAD_REQUEST);
        }

    }


}
