# ArthVeda – Banking Application

## About the Project

ArthVeda is a full-stack banking application developed using Spring Boot and React. It provides secure banking operations, customer management, transaction management, admin dashboard, and AI-powered financial assistance using Google Gemini.

## Technologies Used

### Backend
- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- MySQL
- Maven
- Google Gemini AI

### Frontend
- React.js
- JavaScript
- HTML
- CSS
- Vite

## Features

### Customer Features
- Customer Registration and Login
- Secure Authentication
- Account Management
- Deposit and Withdrawal
- Money Transfer
- Transaction History
- Transaction Filtering and Pagination
- Monthly Spending Analysis
- CSV Bank Statement Download
- AI Financial Assistant

### Admin Features
- Admin Dashboard
- Customer Management
- Account Management
- Transaction Monitoring
- Account Funding
- Audit Logs
- Suspicious Activity Review
- Admin Reports

## Security Features
- Spring Security Authentication and Authorization
- Role-Based Access Control (Admin and Customer)
- Protected REST APIs
- Account Ownership Verification
- Secure Banking Transactions
- Masked Account Numbers
- Secure API Key Management

## AI Integration

The application uses Google Gemini AI for:
- AI Financial Assistant
- Monthly Spending Insights
- General Financial Education

The Gemini API key is configured through an environment variable.

## Backend Setup

1. Clone the repository.
2. Open the backend project in your IDE.
3. Configure MySQL database settings.
4. Set your Gemini API key in PowerShell:

powershell
$env:GEMINI_API_KEY = "your-api-key"


5. Start the backend:

bash
.\mvnw.cmd spring-boot:run


Backend URL:

`http://localhost:8080`

## Frontend Setup

Open another terminal:

bash
cd frontend
npm install
npm run dev


Frontend URL:

`http://localhost:5173`

## Environment Configuration

Configure the following properties before running the application:

- MySQL database credentials
- Gemini API key
- Admin email and password
- Frontend origin (if required)



## Project Structure


ArthVeda/
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── mvnw
│
├── frontend/
│   ├── src/
│   ├── public/
│   └── package.json
│
└── README.md


## Future Enhancements
- Email Notifications
- Beneficiary Management
- Payment Reminders
- Advanced Fraud Detection

