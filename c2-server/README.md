## Lab-RATS Tactical C2 Backend (`/c2-server`)

<img width="1417" height="444" alt="Screenshot 2026-10-25 at 2 06 52 AM" src="https://github.com/user-attachments/assets/0f89bc20-52e0-43f6-b6f2-23ecaec60523" />

### This directory contains the Node.js / Express backend server designed for deploying a relay C2 instance on **Render**, **Railway**, or **Fly.io** *(Option 2 in the main README)*.

## **Quick Deployment on** <a href="https://render.com" target="_blank">Render.com</a> 

1. Fork the **Lab-RATS-v1.6** repo.
2. Create a new **Web Service** on **Render** and **connect your Lab-RATS repository**.
3. Set the **Root Directory** to `c2-server`.
4. Set the **Build Command** to `npm install`.
5. Set the **Start Command** to `npm start`.
6. **Deploy!** Once live, copy your **Render URL** (e.g., `https://labrats-github1.onrender.com`).
7. Enter this URL into the **APK Builder** when prompted for the **C2 Webhook URL**.
