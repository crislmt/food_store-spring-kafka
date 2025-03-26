package com.nsds.group15.fooddeliveryapplicationbackend.entity;

import java.util.StringTokenizer;

public class Order {

    private int code;
    private String customerEmail;
    private String productName;
    private int quantity;
    private OrderStatus status;
    private String shippingAddress;

    public Order(String customerEmail, String productName, int quantity, String shippingAddress){
        this.customerEmail=customerEmail;
        this.productName=productName;
        this.quantity=quantity;
        this.shippingAddress =shippingAddress;
    }

    public Order(String record){
        StringTokenizer stk = new StringTokenizer(record, "#");
        this.code=Integer.parseInt(stk.nextToken());
        this.customerEmail = stk.nextToken();
        this.productName= stk.nextToken();
        this.quantity = Integer.parseInt(stk.nextToken());
        this.shippingAddress =stk.nextToken();
        this.status= OrderStatus.valueOf(stk.nextToken());
    }

    @Override
    public String toString(){
        return "Order n. "+code+" by "+customerEmail+" for "+productName+" with quantity "+quantity+" and status "+status;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setStatus(OrderStatus status){
        this.status=status;
    }

    public OrderStatus getStatus(){return status;}

    public String getShippingAddress(){return shippingAddress;}

}
