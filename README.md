# ZipDrop - Peer-to-Peer File Sharing Application

## Overview
ZipDrop is a lightweight, peer-to-peer file sharing application that allows users to upload and download files seamlessly over a simple web interface. Built with Java for the backend and a modern frontend, it ensures quick, secure, and efficient file transfers.

---

## Features
- 📂 Upload multiple files (supports images, documents, videos, etc.)
- 📥 Download files easily through unique links
- ⚡ Fast and simple peer-to-peer sharing
- 🌐 Backend server with REST API
- 🎨 Frontend UI (React / Next.js suggested)
- 🚀 Easy to run locally

---

## Tech Stack
- **Backend:** Java (Core Java + HTTP server)
- **Frontend:** React / Next.js (UI for upload & download)
- **Build Tool:** Maven
- **Dependencies:**
  - [Apache Commons IO](https://commons.apache.org/proper/commons-io/)

---

## Project Structure
```
zipdrop/
│
├── src/main/java/server/
│   ├── App.java                # Main entry point
│   └── controller/
│       └── FileController.java # Handles upload & download
│
├── frontend/                   # React/Next.js UI
│
├── pom.xml                     # Maven build file
└── README.md                   # Project documentation
```

---

## Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/zipdrop.git
cd zipdrop
```

### 2. Build the Project
```bash
mvn clean install
```

### 3. Run the Server
```bash
java -cp target/classes server.App
```
The backend will start on **http://localhost:8080**.

### 4. Start the Frontend
```bash
cd frontend
npm install
npm run dev
```
The UI will be available at **http://localhost:3000**.

---

## Usage
- Open **http
