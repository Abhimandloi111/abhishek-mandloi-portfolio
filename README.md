# Abhishek Mandloi - Personal Portfolio & RAG Chatbot

A modern, responsive single-page portfolio website for **Abhishek Mandloi**, Senior Software Engineer with 4+ years of experience specializing in Java, Spring Boot, Big Data engineering, and Generative AI solutions.

![Tech Stack](https://img.shields.io/badge/Stack-Angular_18_|_Spring_Boot_3_|_Google_Gemini-10B981?style=for-the-badge)

---

## 🌐 100% FREE Hosting & Deployment Guide

You can host both the Angular frontend and Spring Boot RAG backend completely **FREE of charge** using free-tier cloud platforms:

| Component | Recommended Hosting Platform | Free Tier Limits | Cost |
| :--- | :--- | :--- | :--- |
| **Frontend** | **Vercel** or **Netlify** | Unlimited deployments, 100GB/mo bandwidth | **$0 / Month** |
| **Backend** | **Render.com** or **Railway** | 512MB RAM, 750 free execution hours/month | **$0 / Month** |
| **AI Model** | **Google Gemini API** | 15 Requests/min, 1 Million Tokens/min | **$0 / Month** |

---

### Step 1: Push Code to GitHub

1. Initialize Git in the project root:
   ```bash
   git init
   git add .
   git commit -m "Initial portfolio release"
   ```
2. Create a repository on GitHub (e.g. `abhishek-mandloi-portfolio`) and push your code:
   ```bash
   git remote add origin https://github.com/YOUR_USERNAME/abhishek-mandloi-portfolio.git
   git branch -M main
   git push -u origin main
   ```

---

### Step 2: Deploy Spring Boot Backend on Render.com (FREE)

1. Sign up for free at [Render.com](https://render.com/).
2. Click **New +** -> Select **Web Service**.
3. Connect your GitHub repository `abhishek-mandloi-portfolio`.
4. Configure build settings:
   - **Name**: `abhishek-mandloi-backend`
   - **Root Directory**: `backend`
   - **Environment**: `Java` (or Docker)
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/portfolio-backend-1.0.0.jar`
   - **Instance Type**: `Free`
5. **Add Environment Variable**:
   - `GEMINI_API_KEY`: Paste your free key from [Google AI Studio](https://aistudio.google.com/).
6. Click **Create Web Service**. Render will build and host your API at e.g.:
   `https://abhishek-mandloi-backend.onrender.com`

---

### Step 3: Deploy Angular Frontend on Vercel (FREE)

1. Open [`frontend/src/app/services/chat.service.ts`](file:///C:/Users/abhim/.gemini/antigravity/scratch/abhishek-mandloi-portfolio/frontend/src/app/services/chat.service.ts) and update the backend URL to your live Render backend URL:
   ```typescript
   private apiUrl = 'https://abhishek-mandloi-backend.onrender.com/api/chat';
   ```
2. Commit and push the file to GitHub:
   ```bash
   git add .
   git commit -m "Update backend API URL for production"
   git push origin main
   ```
3. Sign up for free at [Vercel.com](https://vercel.com/).
4. Click **Add New** -> **Project** -> Import your `abhishek-mandloi-portfolio` repository.
5. Project Settings:
   - **Framework Preset**: `Angular`
   - **Root Directory**: `frontend`
6. Click **Deploy**. In ~60 seconds, your site will be live at:
   `https://abhishek-mandloi-portfolio.vercel.app`

---

### Step 4: Get your Free Google Gemini API Key

1. Visit [Google AI Studio](https://aistudio.google.com/).
2. Click **Get API key** -> **Create API key**.
3. Paste the key in Render's environment variable settings under `GEMINI_API_KEY`.

---

## 🛠️ Local Development Setup

### 1. Running Spring Boot Backend Locally
```bash
cd backend
mvn spring-boot:run
```
- Local URL: `http://localhost:8080`

### 2. Running Angular Frontend Locally
```bash
cd frontend
npm start
```
- Local UI URL: `http://localhost:4200`
