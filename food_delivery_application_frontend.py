"""
Food Delivery Application - Client CLI

This script allows users to interact with a microservices-based food delivery system.
It provides functionalities for customers, admins, and delivery personnel.

Features:
- User registration
- Order placement
- Product management (Admins only)
- Shipping management
- Kafka-based microservices interaction

Author: Your Name
"""

import requests
import json

# Service URLs
CUSTOMER_SERVICE_URL = 'http://127.0.0.1:9090'
ORDER_SERVICE_URL = 'http://127.0.0.1:9090/order'
SHIPPING_SERVICE_URL = 'http://127.0.0.1:9090/shippings'

# User Roles
REGULAR_USER = "REGULAR_USER"
ADMIN = "ADMIN"
DELIVERY_MAN = "DELIVERY_MAN"

def make_request(method, url, params=None, data=None, headers=None):
    """Helper function to make HTTP requests and handle errors."""
    try:
        response = requests.request(method, url, params=params, json=data, headers=headers)
        response.raise_for_status()
        return response.json() if response.content else "Success"
    except requests.exceptions.RequestException as err:
        print(f"Error: {err}")
        return None

def registration():
    """Registers a new user."""
    data = {
        'name': input("Insert name: "),
        'surname': input("Insert surname: "),
        'email': input("Insert email: "),
        'address': input("Insert address: ")
    }
    if make_request("POST", f"{CUSTOMER_SERVICE_URL}/register", params=data):
        print("You have successfully registered.")

def add_product():
    """Adds a new product (Admin only)."""
    data = {
        'productName': input("Insert product name: "),
        'quantity': input("Insert quantity: ")
    }
    if make_request("PUT", f"{ORDER_SERVICE_URL}/addProduct", params=data):
        print("The product has been successfully added.")

def update_product():
    """Updates an existing product (Admin only)."""
    data = {
        'productName': input("Insert product name: "),
        'quantity': input("Insert quantity: ")
    }
    if make_request("POST", f"{ORDER_SERVICE_URL}/updateProduct", params=data):
        print("The product has been successfully updated.")

def get_products():
    """Retrieves all available products."""
    print(make_request("GET", f"{ORDER_SERVICE_URL}/getProducts"))

def insert_order():
    """Inserts a new order."""
    data = {
        'customerEmail': input("Insert email: "),
        'productName': input("Insert product name: "),
        'quantity': input("Insert quantity: "),
        'shippingAddress': input("Insert shipping address: ")
    }
    if make_request("POST", f"{ORDER_SERVICE_URL}/insertOrder", data=data):
        print("The order has been successfully inserted.")

def deliver_shipping():
    """Marks an order as delivered (Delivery personnel only)."""
    if make_request("PUT", f"{SHIPPING_SERVICE_URL}/deliverOrder", params={'code': input("Insert code: ")}):
        print("Order delivered successfully.")

def get_shippings_by_email():
    """Retrieves all shipping records for a given email."""
    print(make_request("GET", f"{SHIPPING_SERVICE_URL}/getShippingsByEmail", params={'email': input("Insert email: ")}))

def get_shippings():
    """Retrieves all shipping records."""
    print(make_request("GET", f"{SHIPPING_SERVICE_URL}/getShippings"))

def main_menu():
    """Displays the main menu to select user type."""
    user_type = None
    while True:
        print("Please select user type:")
        print("1. Regular user")
        print("2. Admin")
        print("3. Delivery man")
        choice = input("Enter choice: ")

        if choice == "1":
            user_type = REGULAR_USER
        elif choice == "2":
            user_type = ADMIN
        elif choice == "3":
            user_type = DELIVERY_MAN
        else:
            print("Invalid selection. Try again.")
            continue

        user_menu(user_type)

def user_menu(user_type):
    """Displays the menu based on user type."""
    while True:
        if user_type == REGULAR_USER:
            print("\nHello User, choose an action:")
            print("1. Register")
            print("2. Insert an order")
            print("3. Get all orders by email")
            print("4. Get all products")
            print("5. Switch user type")
            choice = input("Enter choice: ")

            if choice == "1": registration()
            elif choice == "2": insert_order()
            elif choice == "3": get_shippings_by_email()
            elif choice == "4": get_products()
            elif choice == "5": break
            else: print("Invalid option. Try again.")

if __name__ == "__main__":
    main_menu()
