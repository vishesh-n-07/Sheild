# 🛡️ SHEILD
### Smart Human Emergency Instant Location Device

A real-time emergency response system that connects victims in danger with nearby rescuers instantly.

> "Every second counts in an emergency. SHEILD makes sure help is never far away."

---

## 🎯 What Problem Does It Solve?

In emergency situations like harassment, kidnapping, or accidents:
- Calling 112 takes time
- Typing a message is impossible under stress
- Help from nearby people is faster than waiting for authorities

SHEILD solves this by letting victims send an instant SOS with their live location and voice evidence to nearby rescuers in seconds.

---

## 🚀 How It Works

Victim presses SOS → Location + Voice captured → Alert sent to database → Nearby rescuers notified instantly

---

## ✅ Features

### 🆘 Victim Side
- One press SOS trigger
- Automatic location capture
- 60 second voice recording as evidence
- Alert instantly sent to Supabase backend

### 🚑 Rescuer Side
- Real-time alert feed
- 2km radius distance filtering
- Time-based filtering (last 24 hours only)
- Voice evidence playback
- One click Google Maps navigation to victim
- Responding status system

---

## 🖥️ Screenshots

### Home Screen
![Home](screenshots/home.png)

### Victim Mode
![Victim](screenshots/victim.png)

### Rescuer Dashboard
![Rescuer](screenshots/rescuer.png)

---

## ⚙️ Tech Stack

- **Frontend:** React.js
- **Backend:** Supabase (PostgreSQL + Realtime)
- **Storage:** Supabase Storage (voice recordings)
- **Location:** Browser Geolocation API
- **Audio:** MediaRecorder API
- **Maps:** Google Maps

---

## 🔮 Roadmap

- [ ] Push notifications (FCM)
- [ ] Background SOS trigger
- [ ] Family contact alerts
- [ ] AI-powered danger detection
- [ ] Face recognition
- [ ] Native mobile app (Android/iOS)

---

## 🚀 Getting Started

### 1. Clone the repo
```bash
git clone https://github.com/vishesh-n-07/SHEILD.git
cd SHEILD
```

### 2. Install dependencies
```bash
npm install
```

### 3. Add Supabase credentials
Create `src/supabase.js`:
```javascript
import { createClient } from '@supabase/supabase-js'
const supabaseUrl = "your_supabase_url"
const supabaseKey = "your_anon_key"
export const supabase = createClient(supabaseUrl, supabaseKey)
```

### 4. Run the app
```bash
npm start
```

---

## 💡 About

SHEILD started as a personal startup idea to make emergency response faster and smarter in India. Built as an MVP to validate the concept.

**Built by Vishesh** 🛡️

[![GitHub](https://img.shields.io/badge/GitHub-vishesh--n--07-black?logo=github)](https://github.com/vishesh-n-07)