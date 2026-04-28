package com.example.hk.HK_Backend.service;

import com.example.hk.HK_Backend.dto.ApplicationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender; 

    @Value("${spring.mail.username:hkpgakurdi@gmail.com}")
    private String fromEmail;

    @Value("${app.admin.email:admin@hkpg.com}")
    private String adminEmail;

    @Value("${app.site.url:https://hk-pg-akurdi.vercel.app}")
    private String siteUrl;

    // ── Send email helper ─────────────────────────────────────────────────────
    private void send(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail, "HK PG Akurdi");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.warn("Email failed to {}: {}", to, e.getMessage());
            // Never fail the main flow due to email issues
        }
    }

    // ── Direct email — full HTML already built, bypasses wrap() ──────────────
    // Use for password reset where content has no % format specifiers
    @Async
    public void sendPasswordResetEmailDirect(String to, String fullHtml) {
        send(to, "\uD83D\uDD11 Password Reset \u2014 HK PG Akurdi", fullHtml);
    }

    // ── Shared header — pure inline HTML/SVG, no external images (Gmail-safe) ─
    private static final String EMAIL_HEADER =
        "<!DOCTYPE html><html><head><meta charset='UTF-8'/><meta name='viewport' content='width=device-width,initial-scale=1.0'/></head>"
        + "<body style='margin:0;padding:0;background:#f4f4f8;font-family:Segoe UI,Arial,sans-serif;'>"
        + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f4f8;padding:30px 0;'>"
        + "<tr><td align='center'>"
        + "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;'>"
        // Header — dark background with styled text logo (no external image)
        + "<tr><td style='background:linear-gradient(135deg,#0f0c29,#1a1a2e,#16213e);border-radius:16px 16px 0 0;padding:32px 24px 24px;text-align:center;'>"
        + "<div style='display:inline-block;border:2px solid #b8860b;border-radius:12px;padding:10px 28px;margin-bottom:10px;background:rgba(0,0,0,0.3);'>"
        + "<span style='font-size:42px;font-weight:900;letter-spacing:2px;'>"
        + "<span style='color:#c8a84b;text-shadow:0 0 20px rgba(200,168,75,0.5);'>HK</span>"
        + "<span style='color:#a8a8b8;text-shadow:0 0 20px rgba(168,168,184,0.5);'>PG</span>"
        + "</span>"
        + "</div>"
        + "<div style='background:#b8860b;border-radius:6px;padding:5px 20px;display:inline-block;'>"
        + "<span style='color:#fff;font-size:12px;font-weight:800;letter-spacing:3px;text-transform:uppercase;'>Boys PG Accommodation</span>"
        + "</div>"
        + "<p style='margin:10px 0 0;color:rgba(255,255,255,0.6);font-size:12px;'>Near Akurdi Railway Station, Pune</p>"
        + "</td></tr>"
        // Content area open
        + "<tr><td style='background:#ffffff;padding:32px 36px;'>";

    private String emailFooter() {
        return "</td></tr>"
            + "<tr><td style='background:#1a1a2e;border-radius:0 0 16px 16px;padding:24px 36px;text-align:center;'>"
            + "<p style='margin:0 0 4px;color:rgba(255,255,255,0.9);font-size:13px;font-weight:700;'>HK PG Akurdi</p>"
            + "<p style='margin:0 0 16px;color:rgba(255,255,255,0.5);font-size:11px;'>&#128205; Near Gurudwara, Akurdi Railway Station, Pune &#8211; 411035</p>"
            // 3 icon links using emoji in colored table cells — renders in ALL email clients including Gmail
            + "<table cellpadding='0' cellspacing='0' style='margin:0 auto 14px;'><tr>"
            // WhatsApp
            + "<td style='padding:0 6px;'>"
            + "<a href='https://wa.me/919579828996' style='display:block;width:44px;height:44px;background:#25d366;border-radius:10px;text-align:center;line-height:44px;text-decoration:none;font-size:22px;'>&#128172;</a>"
            + "</td>"
            // Instagram
            + "<td style='padding:0 6px;'>"
            + "<a href='https://www.instagram.com/hkpg.akurdi' style='display:block;width:44px;height:44px;background:linear-gradient(45deg,#f09433,#e6683c,#dc2743,#cc2366,#bc1888);border-radius:10px;text-align:center;line-height:44px;text-decoration:none;font-size:22px;'>&#128247;</a>"
            + "</td>"
            // Website
            + "<td style='padding:0 6px;'>"
            + "<a href='" + siteUrl + "' style='display:block;width:44px;height:44px;background:linear-gradient(135deg,#d63384,#c026d3);border-radius:10px;text-align:center;line-height:44px;text-decoration:none;font-size:22px;'>&#127760;</a>"
            + "</td>"
            + "</tr></table>"
            + "<p style='margin:0;color:rgba(255,255,255,0.35);font-size:11px;'>&#169; 2026 HK PG Akurdi. All rights reserved.</p>"
            + "</td></tr>"
            + "</table></td></tr></table>"
            + "</body></html>";
    }
    // ── Email wrapper (used by emails 1 & 2 which use text blocks) ───────────
    private String wrap(String content) {
        return EMAIL_HEADER + content + emailFooter();
    }

    // ── 1. Email to ADMIN when student submits application ────────────────────
    @Async
    public void sendNewBookingToAdmin(ApplicationResponse app) {
        String content = """
            <h2 style="margin:0 0 6px;color:#1a1a2e;font-size:22px;font-weight:800;">🔔 New Booking Request</h2>
            <p style="margin:0 0 24px;color:#6b7280;font-size:14px;">A student has submitted a booking application and is awaiting your approval.</p>

            <!-- Student Info Card -->
            <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:24px;margin-bottom:20px;">
              <h3 style="margin:0 0 16px;color:#374151;font-size:14px;font-weight:700;text-transform:uppercase;letter-spacing:0.5px;">👤 Student Details</h3>
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  <td style="padding:6px 0;color:#6b7280;font-size:13px;width:40%%;">Full Name</td>
                  <td style="padding:6px 0;color:#1a1a2e;font-size:13px;font-weight:600;">%s</td>
                </tr>
                <tr>
                  <td style="padding:6px 0;color:#6b7280;font-size:13px;">Mobile</td>
                  <td style="padding:6px 0;color:#1a1a2e;font-size:13px;font-weight:600;">%s</td>
                </tr>
                <tr>
                  <td style="padding:6px 0;color:#6b7280;font-size:13px;">Occupation</td>
                  <td style="padding:6px 0;color:#1a1a2e;font-size:13px;font-weight:600;">%s</td>
                </tr>
                <tr>
                  <td style="padding:6px 0;color:#6b7280;font-size:13px;">Institution</td>
                  <td style="padding:6px 0;color:#1a1a2e;font-size:13px;font-weight:600;">%s</td>
                </tr>
              </table>
            </div>

            <!-- Room Info Card -->
            <div style="background:linear-gradient(135deg,#fff0f6,#fdf3e7);border:1px solid #fce7f3;border-radius:12px;padding:24px;margin-bottom:20px;">
              <h3 style="margin:0 0 16px;color:#374151;font-size:14px;font-weight:700;text-transform:uppercase;letter-spacing:0.5px;">🛏️ Room & Booking Details</h3>
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  <td style="padding:6px 0;color:#6b7280;font-size:13px;width:40%%;">Room Type</td>
                  <td style="padding:6px 0;color:#c026d3;font-size:13px;font-weight:700;">%s</td>
                </tr>
                <tr>
                  <td style="padding:6px 0;color:#6b7280;font-size:13px;">Room Number</td>
                  <td style="padding:6px 0;color:#1a1a2e;font-size:13px;font-weight:600;">%s</td>
                </tr>
                <tr>
                  <td style="padding:6px 0;color:#6b7280;font-size:13px;">Bed Number</td>
                  <td style="padding:6px 0;color:#1a1a2e;font-size:13px;font-weight:600;">Bed %s</td>
                </tr>
                <tr>
                  <td style="padding:6px 0;color:#6b7280;font-size:13px;">Joining Date</td>
                  <td style="padding:6px 0;color:#1a1a2e;font-size:13px;font-weight:600;">%s</td>
                </tr>
                <tr>
                  <td style="padding:6px 0;color:#6b7280;font-size:13px;">Duration</td>
                  <td style="padding:6px 0;color:#1a1a2e;font-size:13px;font-weight:600;">%s months</td>
                </tr>
                <tr>
                  <td style="padding:6px 0;color:#6b7280;font-size:13px;">Deposit Amount</td>
                  <td style="padding:6px 0;color:#16a34a;font-size:14px;font-weight:700;">₹%s</td>
                </tr>
              </table>
            </div>

            <!-- Action Button -->
            <div style="text-align:center;margin-top:28px;">
              <a href="%s/admin" style="display:inline-block;background:linear-gradient(135deg,#d63384,#c026d3);color:#fff;text-decoration:none;padding:14px 36px;border-radius:10px;font-size:15px;font-weight:700;letter-spacing:0.3px;">
                ✅ Review & Confirm Application
              </a>
              <p style="margin:12px 0 0;color:#9ca3af;font-size:12px;">Login to Admin Dashboard to approve or reject this booking</p>
            </div>
            """.formatted(
                app.getFullName(),
                app.getMobile(),
                app.getOccupation() != null ? app.getOccupation() : "—",
                app.getInstitutionName() != null ? app.getInstitutionName() : "—",
                app.getRoomTypeTitle() != null ? app.getRoomTypeTitle() : "—",
                app.getRoomNumber() != null ? app.getRoomNumber() : "—",
                app.getBedNumber() != null ? app.getBedNumber() : "—",
                app.getJoiningDate() != null ? app.getJoiningDate().toString() : "—",
                app.getDurationMonths() != null ? app.getDurationMonths() : "—",
                app.getDepositAmount() != null ? app.getDepositAmount().toPlainString() : "—",
                siteUrl
            );

        send(adminEmail,
             "🔔 New Booking Request — " + app.getFullName() + " | " + app.getRoomTypeTitle(),
             wrap(content));
    }

    // ── 2. Email to STUDENT when admin confirms ───────────────────────────────
    @Async
    public void sendConfirmationToStudent(ApplicationResponse app) {
        String studentEmail = app.getEmail();
        if (studentEmail == null || studentEmail.isBlank()) {
            log.warn("No email for student {}, skipping confirmation email", app.getFullName());
            return;
        }

        String content = """
            <h2 style="margin:0 0 6px;color:#16a34a;font-size:22px;font-weight:800;">🎉 Booking Confirmed!</h2>
            <p style="margin:0 0 24px;color:#6b7280;font-size:14px;">Congratulations! Your bed has been confirmed at HK PG Akurdi.</p>

            <!-- Confirmation Banner -->
            <div style="background:linear-gradient(135deg,#f0fdf4,#dcfce7);border:2px solid #86efac;border-radius:12px;padding:20px;margin-bottom:20px;text-align:center;">
              <div style="font-size:48px;margin-bottom:8px;">✅</div>
              <h3 style="margin:0 0 4px;color:#15803d;font-size:18px;font-weight:800;">Your Booking is Confirmed</h3>
              <p style="margin:0;color:#16a34a;font-size:13px;">Welcome to the HK PG family!</p>
            </div>

            <!-- Booking Details -->
            <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:24px;margin-bottom:20px;">
              <h3 style="margin:0 0 16px;color:#374151;font-size:14px;font-weight:700;text-transform:uppercase;letter-spacing:0.5px;">🛏️ Your Room Details</h3>
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  <td style="padding:8px 0;color:#6b7280;font-size:13px;width:45%%;border-bottom:1px solid #f1f5f9;">Room Type</td>
                  <td style="padding:8px 0;color:#c026d3;font-size:13px;font-weight:700;border-bottom:1px solid #f1f5f9;">%s</td>
                </tr>
                <tr>
                  <td style="padding:8px 0;color:#6b7280;font-size:13px;border-bottom:1px solid #f1f5f9;">Room Number</td>
                  <td style="padding:8px 0;color:#1a1a2e;font-size:13px;font-weight:600;border-bottom:1px solid #f1f5f9;">%s</td>
                </tr>
                <tr>
                  <td style="padding:8px 0;color:#6b7280;font-size:13px;border-bottom:1px solid #f1f5f9;">Bed Number</td>
                  <td style="padding:8px 0;color:#1a1a2e;font-size:13px;font-weight:600;border-bottom:1px solid #f1f5f9;">Bed %s</td>
                </tr>
                <tr>
                  <td style="padding:8px 0;color:#6b7280;font-size:13px;border-bottom:1px solid #f1f5f9;">Joining Date</td>
                  <td style="padding:8px 0;color:#1a1a2e;font-size:13px;font-weight:600;border-bottom:1px solid #f1f5f9;">%s</td>
                </tr>
                <tr>
                  <td style="padding:8px 0;color:#6b7280;font-size:13px;">Duration</td>
                  <td style="padding:8px 0;color:#1a1a2e;font-size:13px;font-weight:600;">%s months</td>
                </tr>
              </table>
            </div>

            <!-- What to bring -->
            <div style="background:linear-gradient(135deg,#fff0f6,#fdf3e7);border:1px solid #fce7f3;border-radius:12px;padding:24px;margin-bottom:24px;">
              <h3 style="margin:0 0 14px;color:#374151;font-size:14px;font-weight:700;text-transform:uppercase;letter-spacing:0.5px;">📋 What to Bring on Joining Day</h3>
              <ul style="margin:0;padding-left:20px;color:#4b5563;font-size:13px;line-height:2;">
                <li>Original ID proof (Aadhaar / PAN / Passport)</li>
                <li>2 passport size photographs</li>
                <li>Remaining rent amount (first month)</li>
                <li>Guardian contact details</li>
              </ul>
            </div>

            <!-- Motivational Message -->
            <div style="background:#1a1a2e;border-radius:12px;padding:24px;margin-bottom:24px;text-align:center;">
              <p style="margin:0 0 8px;color:#f472b6;font-size:16px;font-weight:700;">"Live Comfortably. Achieve Your Goals."</p>
              <p style="margin:0;color:rgba(255,255,255,0.7);font-size:13px;line-height:1.6;">
                Welcome to HK PG — your home away from home. We're committed to providing you a safe, comfortable, and supportive environment so you can focus on what matters most.
              </p>
            </div>

            <!-- Contact -->
            <div style="text-align:center;">
              <p style="margin:0 0 12px;color:#374151;font-size:13px;font-weight:600;">Need help? Contact us anytime:</p>
              <a href="tel:9579828996" style="display:inline-block;background:linear-gradient(135deg,#d63384,#c026d3);color:#fff;text-decoration:none;padding:10px 20px;border-radius:8px;font-size:13px;font-weight:700;margin:4px;">📞 9579828996</a>
              <a href="https://wa.me/919579828996" style="display:inline-block;background:#25d366;color:#fff;text-decoration:none;padding:10px 20px;border-radius:8px;font-size:13px;font-weight:700;margin:4px;">💬 WhatsApp</a>
            </div>

            <p style="margin:24px 0 0;text-align:center;color:#9ca3af;font-size:12px;font-style:italic;">
              Thank you for choosing HK PG Akurdi. Have a wonderful stay! 🏠
            </p>
            """.formatted(
                app.getRoomTypeTitle() != null ? app.getRoomTypeTitle() : "—",
                app.getRoomNumber() != null ? app.getRoomNumber() : "—",
                app.getBedNumber() != null ? app.getBedNumber() : "—",
                app.getJoiningDate() != null ? app.getJoiningDate().toString() : "—",
                app.getDurationMonths() != null ? app.getDurationMonths() : "—"
            );

        send(studentEmail,
             "🎉 Booking Confirmed — HK PG Akurdi | " + app.getRoomTypeTitle() + " · Bed " + app.getBedNumber(),
             wrap(content));
    }

    // ── 3. Email to STUDENT on booking status change (REJECTED / PENDING) ────
    @Async
    public void sendBookingStatusEmail(String to, String name, String status, String roomType, String bedNumber) {
        if (to == null || to.isBlank()) return;
        boolean isRejected = "REJECTED".equals(status);
        String html = EMAIL_HEADER
            + "<div style='text-align:center;margin-bottom:20px;'>"
            + "<div style='font-size:52px;'>" + (isRejected ? "&#10060;" : "&#128260;") + "</div>"
            + "<h2 style='margin:8px 0 4px;color:" + (isRejected ? "#dc2626" : "#d97706") + ";font-size:22px;font-weight:800;'>Booking " + (isRejected ? "Rejected" : "Status Updated") + "</h2>"
            + "</div>"
            + "<p style='color:#374151;font-size:14px;'>Dear <strong>" + name + "</strong>,</p>"
            + "<p style='color:#6b7280;font-size:13px;line-height:1.6;'>"
            + (isRejected
                ? "We regret to inform you that your booking application has been <strong style='color:#dc2626;'>rejected</strong> by the admin. If you have any questions, please contact us directly."
                : "Your booking application status has been updated to <strong style='color:#d97706;'>Pending</strong>. Our team will review it shortly.")
            + "</p>"
            + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:16px;margin:16px 0;'>"
            + "<p style='margin:0 0 6px;color:#6b7280;font-size:12px;'>Room Type: <strong>" + roomType + "</strong></p>"
            + "<p style='margin:0;color:#6b7280;font-size:12px;'>Bed Number: <strong>Bed " + bedNumber + "</strong></p>"
            + "</div>"
            + "<p style='color:#374151;font-size:13px;'>For any queries, contact us at <a href='tel:9579828996' style='color:#c026d3;'>9579828996</a></p>"
            + "<hr style='border:none;border-top:1px solid #f1f5f9;margin:20px 0;'/>"
            + "<p style='margin:0;color:#374151;font-size:13px;'>Thanks &amp; Regards,<br/><strong style='color:#c026d3;'>HK PG MANAGEMENT</strong></p>"
            + emailFooter();
        send(to, (isRejected ? "❌" : "🔄") + " Booking Status Update — HK PG Akurdi", html);
    }

    // ── 4. Email to STUDENT on payment status change (deposit/rent) ───────────
    @Async
    public void sendPaymentStatusEmail(String to, String name, String paymentType, String status, String roomType, String bedNumber) {
        if (to == null || to.isBlank()) return;
        boolean isReceived = "RECEIVED".equals(status);
        boolean isOverdue  = "OVERDUE".equals(status);
        String emoji = isReceived ? "&#9989;" : isOverdue ? "&#128308;" : "&#9203;";
        String color = isReceived ? "#16a34a" : isOverdue ? "#dc2626" : "#d97706";
        String message = isReceived
            ? "Your <strong>" + paymentType + "</strong> payment has been <strong style='color:#16a34a;'>received and confirmed</strong> by the admin. Thank you for paying on time!"
            : isOverdue
            ? "Your <strong>" + paymentType + "</strong> payment is <strong style='color:#dc2626;'>overdue</strong>. Please pay immediately to avoid any inconvenience."
            : "Your <strong>" + paymentType + "</strong> payment status is <strong style='color:#d97706;'>pending</strong>. Please complete your payment at the earliest.";
        String html = EMAIL_HEADER
            + "<div style='text-align:center;margin-bottom:20px;'>"
            + "<div style='font-size:52px;'>" + emoji + "</div>"
            + "<h2 style='margin:8px 0 4px;color:" + color + ";font-size:22px;font-weight:800;'>" + paymentType + " Payment " + (isReceived ? "Confirmed" : isOverdue ? "Overdue" : "Pending") + "</h2>"
            + "</div>"
            + "<p style='color:#374151;font-size:14px;'>Dear <strong>" + name + "</strong>,</p>"
            + "<p style='color:#6b7280;font-size:13px;line-height:1.6;'>" + message + "</p>"
            + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:16px;margin:16px 0;'>"
            + "<p style='margin:0 0 6px;color:#6b7280;font-size:12px;'>Payment Type: <strong>" + paymentType + "</strong></p>"
            + "<p style='margin:0 0 6px;color:#6b7280;font-size:12px;'>Room Type: <strong>" + roomType + "</strong></p>"
            + "<p style='margin:0;color:#6b7280;font-size:12px;'>Bed Number: <strong>Bed " + bedNumber + "</strong></p>"
            + "</div>"
            + (isReceived ? "<div style='background:#f0fdf4;border:1px solid #86efac;border-radius:10px;padding:14px;margin-bottom:16px;'>"
                + "<p style='margin:0;color:#15803d;font-size:13px;font-weight:600;'>&#128204; Always pay rent through the HK PG application only.</p>"
                + "</div>" : "")
            + "<p style='color:#374151;font-size:13px;'>For any queries, contact us at <a href='tel:9579828996' style='color:#c026d3;'>9579828996</a></p>"
            + "<hr style='border:none;border-top:1px solid #f1f5f9;margin:20px 0;'/>"
            + "<p style='margin:0;color:#374151;font-size:13px;'>Thanks &amp; Regards,<br/><strong style='color:#c026d3;'>HK PG MANAGEMENT</strong></p>"
            + emailFooter();
        send(to, (isReceived ? "✅" : isOverdue ? "🔴" : "⏳") + " " + paymentType + " Payment " + (isReceived ? "Confirmed" : isOverdue ? "Overdue" : "Pending") + " — HK PG Akurdi", html);
    }

    // ── 5. Email to ADMIN when student submits rent payment ───────────────────
    @Async
    public void sendRentPaymentToAdmin(
            String adminTo, String studentName, String studentEmail,
            String bedNumber, String roomNumber, String roomType,
            String amount, String month, String screenshotUrl, String siteUrl) {

        // Build confirm URL — admin clicks this to mark payment received
        String confirmUrl = siteUrl + "/api/rent/confirm"
            + "?studentEmail=" + java.net.URLEncoder.encode(studentEmail, java.nio.charset.StandardCharsets.UTF_8)
            + "&studentName="  + java.net.URLEncoder.encode(studentName,  java.nio.charset.StandardCharsets.UTF_8)
            + "&bedNumber="    + java.net.URLEncoder.encode(bedNumber,    java.nio.charset.StandardCharsets.UTF_8)
            + "&roomType="     + java.net.URLEncoder.encode(roomType,     java.nio.charset.StandardCharsets.UTF_8)
            + "&amount="       + java.net.URLEncoder.encode(amount,       java.nio.charset.StandardCharsets.UTF_8)
            + "&month="        + java.net.URLEncoder.encode(month,        java.nio.charset.StandardCharsets.UTF_8);

        String screenshotHtml = (screenshotUrl != null && !screenshotUrl.isBlank())
            ? "<div style='margin-bottom:20px;text-align:center;'>"
              + "<p style='margin:0 0 8px;color:#374151;font-size:13px;font-weight:600;'>📸 Payment Screenshot:</p>"
              + "<a href='" + screenshotUrl + "' target='_blank'>"
              + "<img src='" + screenshotUrl + "' alt='Payment Screenshot' style='max-width:100%;border-radius:12px;border:2px solid #e2e8f0;'/>"
              + "</a></div>"
            : "<p style='color:#9ca3af;font-size:13px;'>No screenshot uploaded.</p>";

        String html = EMAIL_HEADER
            + "<h2 style='margin:0 0 6px;color:#1a1a2e;font-size:20px;font-weight:800;'>&#128176; Rent Payment Received</h2>"
            + "<p style='margin:0 0 20px;color:#6b7280;font-size:14px;'>A student has submitted their monthly rent payment.</p>"
            + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:20px;margin-bottom:20px;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='padding:6px 0;color:#6b7280;font-size:13px;width:40%;'>Student Name</td><td style='padding:6px 0;color:#1a1a2e;font-size:13px;font-weight:600;'>" + studentName + "</td></tr>"
            + "<tr><td style='padding:6px 0;color:#6b7280;font-size:13px;'>Email</td><td style='padding:6px 0;color:#1a1a2e;font-size:13px;font-weight:600;'>" + studentEmail + "</td></tr>"
            + "<tr><td style='padding:6px 0;color:#6b7280;font-size:13px;'>Room Type</td><td style='padding:6px 0;color:#c026d3;font-size:13px;font-weight:700;'>" + roomType + "</td></tr>"
            + "<tr><td style='padding:6px 0;color:#6b7280;font-size:13px;'>Room Number</td><td style='padding:6px 0;color:#1a1a2e;font-size:13px;font-weight:600;'>" + roomNumber + "</td></tr>"
            + "<tr><td style='padding:6px 0;color:#6b7280;font-size:13px;'>Bed Number</td><td style='padding:6px 0;color:#1a1a2e;font-size:13px;font-weight:600;'>Bed " + bedNumber + "</td></tr>"
            + "<tr><td style='padding:6px 0;color:#6b7280;font-size:13px;'>Month</td><td style='padding:6px 0;color:#1a1a2e;font-size:13px;font-weight:600;'>" + month + "</td></tr>"
            + "<tr><td style='padding:6px 0;color:#6b7280;font-size:13px;'>Amount</td><td style='padding:6px 0;color:#16a34a;font-size:14px;font-weight:700;'>&#8377;" + amount + "</td></tr>"
            + "</table></div>"
            + screenshotHtml
            + "<div style='text-align:center;margin-top:24px;'>"
            + "<a href='" + confirmUrl + "' style='display:inline-block;background:linear-gradient(135deg,#16a34a,#15803d);color:#fff;text-decoration:none;padding:14px 36px;border-radius:10px;font-size:15px;font-weight:700;'>&#9989; Mark Payment as Received</a>"
            + "<p style='margin:10px 0 0;color:#9ca3af;font-size:12px;'>Click above to send confirmation email to student</p>"
            + "</div>"
            + "<hr style='border:none;border-top:1px solid #f1f5f9;margin:24px 0;'/>"
            + "<p style='margin:0;color:#374151;font-size:13px;'>Thanks &amp; Regards,<br/><strong style='color:#c026d3;'>HK PG MANAGEMENT</strong></p>"
            + emailFooter();

        send(adminTo, "&#128176; Rent Payment — " + studentName + " | Bed " + bedNumber + " | " + month, html);
    }

    // ── 4. Email to STUDENT when admin confirms rent payment ──────────────────
    @Async
    public void sendRentConfirmationToStudent(
            String studentEmail, String studentName,
            String bedNumber, String roomType, String amount, String month) {

        String html = EMAIL_HEADER
            + "<div style='text-align:center;margin-bottom:20px;'>"
            + "<div style='font-size:52px;'>&#9989;</div>"
            + "<h2 style='margin:8px 0 4px;color:#15803d;font-size:22px;font-weight:800;'>Payment Received!</h2>"
            + "<p style='margin:0;color:#16a34a;font-size:14px;'>Thank you for paying your rent on time.</p>"
            + "</div>"
            + "<div style='background:#f0fdf4;border:1px solid #86efac;border-radius:12px;padding:20px;margin-bottom:20px;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='padding:6px 0;color:#6b7280;font-size:13px;width:40%;'>Student</td><td style='padding:6px 0;color:#1a1a2e;font-size:13px;font-weight:600;'>" + studentName + "</td></tr>"
            + "<tr><td style='padding:6px 0;color:#6b7280;font-size:13px;'>Room Type</td><td style='padding:6px 0;color:#c026d3;font-size:13px;font-weight:700;'>" + roomType + "</td></tr>"
            + "<tr><td style='padding:6px 0;color:#6b7280;font-size:13px;'>Bed Number</td><td style='padding:6px 0;color:#1a1a2e;font-size:13px;font-weight:600;'>Bed " + bedNumber + "</td></tr>"
            + "<tr><td style='padding:6px 0;color:#6b7280;font-size:13px;'>Month</td><td style='padding:6px 0;color:#1a1a2e;font-size:13px;font-weight:600;'>" + month + "</td></tr>"
            + "<tr><td style='padding:6px 0;color:#6b7280;font-size:13px;'>Amount</td><td style='padding:6px 0;color:#16a34a;font-size:14px;font-weight:700;'>&#8377;" + amount + "</td></tr>"
            + "</table></div>"
            + "<div style='background:#1a1a2e;border-radius:12px;padding:20px;margin-bottom:20px;text-align:center;'>"
            + "<p style='margin:0 0 6px;color:#f472b6;font-size:15px;font-weight:700;'>\"Live Comfortably. Achieve Your Goals.\"</p>"
            + "<p style='margin:0;color:rgba(255,255,255,0.7);font-size:13px;'>Thank you for staying with us. We appreciate your timely payment.</p>"
            + "</div>"
            + "<div style='background:#fef3c7;border:1px solid #fcd34d;border-radius:10px;padding:14px;margin-bottom:20px;'>"
            + "<p style='margin:0;color:#92400e;font-size:13px;font-weight:600;'>&#128204; Always pay rent through the HK PG website only.</p>"
            + "</div>"
            + "<p style='margin:0;color:#374151;font-size:13px;'>Thanks &amp; Regards,<br/><strong style='color:#c026d3;'>HK PG MANAGEMENT</strong></p>"
            + emailFooter();

        send(studentEmail, "&#9989; Rent Payment Confirmed — HK PG Akurdi | " + month, html);
    }

    // ── 5. Google Review Reminder — sent 7 days after joining ─────────────────
    @Async
    public void sendGoogleReviewReminder(String to, String name, String roomType) {
        if (to == null || to.isBlank()) return;
        String html = EMAIL_HEADER
            + "<div style='text-align:center;margin-bottom:24px;'>"
            + "<div style='font-size:52px;margin-bottom:8px;'>&#11088;</div>"
            + "<h2 style='margin:0 0 6px;color:#1a1a2e;font-size:22px;font-weight:800;'>How's Your Stay?</h2>"
            + "<p style='margin:0;color:#6b7280;font-size:14px;'>We hope you're settling in well at HK PG Akurdi!</p>"
            + "</div>"
            + "<p style='color:#374151;font-size:14px;'>Dear <strong>" + name + "</strong>,</p>"
            + "<p style='color:#6b7280;font-size:13px;line-height:1.7;'>It's been a week since you joined us in <strong>" + roomType + "</strong>. We hope you're comfortable and enjoying your stay.</p>"
            + "<p style='color:#6b7280;font-size:13px;line-height:1.7;'>If you're happy with your experience, we'd truly appreciate a quick Google review — it helps other students find a safe and comfortable home like yours.</p>"
            + "<div style='text-align:center;margin:28px 0;'>"
            + "<a href='https://g.page/r/hkpg-akurdi/review' style='display:inline-block;background:linear-gradient(135deg,#4285f4,#34a853);color:#fff;text-decoration:none;padding:14px 32px;border-radius:12px;font-size:15px;font-weight:700;'>&#11088; Write a Google Review</a>"
            + "<p style='margin:10px 0 0;color:#9ca3af;font-size:12px;'>Takes less than 1 minute — means a lot to us!</p>"
            + "</div>"
            + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:16px;margin-bottom:20px;'>"
            + "<p style='margin:0;color:#374151;font-size:13px;font-weight:600;'>&#128204; Any issues or suggestions?</p>"
            + "<p style='margin:6px 0 0;color:#6b7280;font-size:13px;'>Contact us anytime at <a href='tel:9579828996' style='color:#c026d3;'>9579828996</a> — we're here to help.</p>"
            + "</div>"
            + "<p style='margin:0;color:#374151;font-size:13px;'>Thanks &amp; Regards,<br/><strong style='color:#c026d3;'>HK PG MANAGEMENT</strong></p>"
            + emailFooter();
        send(to, "&#11088; How's Your Stay? — HK PG Akurdi", html);
    }
}
