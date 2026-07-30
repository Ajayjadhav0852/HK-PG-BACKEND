# 🔧 Backend Environment Variables Setup Guide

## Render Deployment Configuration

### Required Environment Variables

Copy these to Render Dashboard → Environment section:

```bash
# ============================================================================
# DATABASE (Supabase PostgreSQL)
# ============================================================================
DB_URL=jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require&prepareThreshold=0&socketTimeout=30&connectTimeout=10
DB_USERNAME=postgres.xygllkdhllaltysdbrhf
DB_PASSWORD=Admin@ajay0852

# ============================================================================
# JWT AUTHENTICATION
# ============================================================================
JWT_SECRET=hkpg_super_secret_jwt_key_2024_change_in_production_min_256_bits
JWT_EXPIRATION_MS=86400000

# ============================================================================
# CORS (Frontend URLs)
# ============================================================================
ALLOWED_ORIGINS=https://hk-pg-akurdi.vercel.app,http://localhost:5173

# ============================================================================
# CLOUDINARY (Image Storage)
# ============================================================================
CLOUDINARY_CLOUD_NAME=dzr0crkvr
CLOUDINARY_API_KEY=366447514547635
CLOUDINARY_API_SECRET=IUdHiXU8H3Uu3Z-l2JyJLa9hueY

# ============================================================================
# EMAIL CONFIGURATION (Gmail SMTP)
# ⚠️ CRITICAL: Must set MAIL_PASSWORD or email will fail!
# ============================================================================
MAIL_USERNAME=hk.pg.akurdi@gmail.com
MAIL_PASSWORD=<REPLACE_WITH_GMAIL_APP_PASSWORD>

# ============================================================================
# ADMIN & SITE CONFIGURATION
# ============================================================================
ADMIN_EMAIL=hk.pg.akurdi@gmail.com
SITE_URL=https://hk-pg-akurdi.vercel.app
BACKEND_URL=https://hk-pg-backend.onrender.com
```

---

## ⚠️ CRITICAL: Gmail App Password Setup

**You MUST generate a Gmail App Password for MAIL_PASSWORD**

### Steps to Generate:

1. **Go to Google Account**: https://myaccount.google.com/
2. **Enable 2-Step Verification** (if not already enabled):
   - Security → 2-Step Verification → Turn on
3. **Generate App Password**:
   - Security → App passwords
   - Select "Mail" and "Other (Custom name)"
   - Name it "HK PG Backend"
   - Click "Generate"
4. **Copy the 16-character password** (e.g., `abcd efgh ijkl mnop`)
5. **Set in Render**: `MAIL_PASSWORD=abcdefghijklmnop` (no spaces)

### Without This Password:
- ❌ Booking emails won't send
- ❌ Admin won't receive notifications
- ❌ Students won't receive confirmations
- ❌ Server logs will flood with errors

---

## 🚀 Deployment Steps

1. **Set all environment variables in Render Dashboard**
2. **Verify MAIL_PASSWORD is set correctly**
3. **Deploy/Redeploy backend**
4. **Check logs for email success**: `✅ Email sent successfully`
5. **Test booking flow to verify emails work**

---

## 📝 Local Development (.env file)

Create `HK_Backend/.env` for local testing (DO NOT COMMIT):

```bash
# Copy all variables from above
# Replace MAIL_PASSWORD with your actual app password
# Use localhost URLs for SITE_URL and BACKEND_URL if testing locally
```

---

## ✅ Verification Checklist

- [ ] All environment variables set in Render
- [ ] MAIL_PASSWORD generated from Gmail
- [ ] Backend deployed successfully
- [ ] Health check passes: `curl https://hk-pg-backend.onrender.com/api/health`
- [ ] Test booking sends email successfully
- [ ] Check Render logs show: `✅ Email sent successfully`

---

**Last Updated**: January 30, 2026  
**Status**: Production Ready
