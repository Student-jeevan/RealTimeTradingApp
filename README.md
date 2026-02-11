# RealTimeTradingApp — Developer Documentation

## Overview
RealTimeTradingApp is a full-stack crypto trading platform that allows users to perform cryptocurrency transactions, manage wallet balance, and view real-time market data.

The system uses Redis caching to reduce external API calls and prevent rate-limit failures when fetching cryptocurrency prices from CoinGecko.

The application is containerized using Docker and deployed on AWS EC2.

---

## Features

- Secure authentication using JWT and Spring Security
- Two-factor authentication using Gmail OTP verification
- Buy and sell cryptocurrency coins
- Wallet deposit and withdrawal using Razorpay
- Peer-to-peer money transfer
- Real-time cryptocurrency price updates using CoinGecko API
- Redis caching to reduce third-party API calls and handle rate limits
- Integrated real-time chatbot that answers queries related to user accounts and cryptocurrency market data

---

## Tech Stack

Frontend
- React

Backend
- Java
- Spring Boot
- Spring Security
- JWT Authentication
- Two-Factor Authentication (Email OTP)

Database
- MySQL

Caching
- Redis

Payments
- Razorpay

External API
- CoinGecko API

Deployment
- Docker
- Docker Compose
- AWS EC2

---

## Project Structure

```
RealTimeTradingApp/
│
├── frontend/
├── backend/
├── docker-compose.yml
└── README.md
```

---

## System Components

### Frontend
Provides UI for:
- Authentication
- Trading dashboard
- Wallet management
- Chat system

Runs on:
```
localhost:3001
```

---

### Backend
Handles:
- Authentication (JWT + 2FA)
- Trading operations
- Wallet transactions
- Payment processing
- API integrations
- Redis caching

Runs on:
```
localhost:8080
```

---

### MySQL
Stores:
- Users
- Transactions
- Wallet data
- OTP records

Runs on:
```
localhost:3308
```

---

### Redis
Used for:
- Market data caching
- Reducing API calls
- Improving performance

Runs on:
```
localhost:6379
```

---

## Running the Project (Docker)

Clone repository:
```
git clone https://github.com/Student-jeevan/RealTimeTradingApp.git
cd RealTimeTradingApp
```

Build and run:
```
docker compose up --build -d
```

Stop containers:
```
docker compose down
```

---

## Deployment (AWS EC2)

Steps:
1. Launch EC2 instance
2. Install Docker
3. Clone repository
4. Run docker compose
5. Configure security group ports

Required ports:
```
3001
8080
3308
6379
22
```

---

## Author
Jeevan
