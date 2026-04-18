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

    // ── Email wrapper ─────────────────────────────────────────────────────────
    private String wrap(String content) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>HK PG Akurdi</title>
            </head>
            <body style="margin:0;padding:0;background:#f4f4f8;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f8;padding:30px 0;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%;">

                    <!-- Header -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#d63384,#c026d3);border-radius:16px 16px 0 0;padding:32px 40px;text-align:center;">
                        <div style="display:inline-block;background:rgba(255,255,255,0.15);border-radius:50%%;padding:12px;margin-bottom:12px;">
                          <span style="font-size:36px;">🏠</span>
                        </div>
                        <h1 style="margin:0;color:#fff;font-size:26px;font-weight:800;letter-spacing:-0.5px;">HK PG Akurdi</h1>
                        <p style="margin:6px 0 0;color:rgba(255,255,255,0.85);font-size:13px;font-weight:500;">Boys Accommodation · Near Akurdi Railway Station, Pune</p>
                      </td>
                    </tr>

                    <!-- Content -->
                    <tr>
                      <td style="background:#ffffff;padding:36px 40px;">
                        %s
                      </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                      <td style="background:#1a1a2e;border-radius:0 0 16px 16px;padding:24px 40px;text-align:center;">
                        <p style="margin:0 0 8px;color:rgba(255,255,255,0.9);font-size:13px;font-weight:600;">HK PG Akurdi — Boys Accommodation</p>
                        <p style="margin:0 0 8px;color:rgba(255,255,255,0.6);font-size:12px;">📍 Near Gurudwara, Akurdi Railway Station, Pune – 411035</p>
                        <p style="margin:0 0 12px;color:rgba(255,255,255,0.6);font-size:12px;">
                          📞 <a href="tel:9579828996" style="color:#f472b6;text-decoration:none;">9579828996</a> &nbsp;|&nbsp;
                          📞 <a href="tel:9096398032" style="color:#f472b6;text-decoration:none;">9096398032</a>
                        </p>
                        <a href="%s" style="display:inline-block;background:linear-gradient(135deg,#d63384,#c026d3);color:#fff;text-decoration:none;padding:10px 24px;border-radius:8px;font-size:12px;font-weight:700;">Visit Website</a>
                        <p style="margin:16px 0 0;color:rgba(255,255,255,0.4);font-size:11px;">© 2026 HK PG Akurdi. All rights reserved.</p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(content, siteUrl);
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

    // ── 3. Email to ADMIN when student submits rent payment ───────────────────
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

        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'/></head>"
            + "<body style='font-family:Arial,sans-serif;background:#f4f4f8;padding:30px;'>"
            + "<div style='max-width:560px;margin:0 auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);'>"
            + "<div style='background:linear-gradient(135deg,#d63384,#c026d3);padding:28px;text-align:center;'>"
            + "<img src='https://hk-pg-akurdi.vercel.app/hkpg-logo.png' width='60' height='60' style='border-radius:50%;border:3px solid rgba(255,255,255,0.4);display:block;margin:0 auto 10px;'/>"
            + "<h1 style='margin:0;color:#fff;font-size:20px;font-weight:800;'>HK PG Akurdi</h1>"
            + "</div>"
            + "<div style='padding:28px;'>"
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
            + "</div>"
            + "<div style='background:#1a1a2e;padding:20px;text-align:center;'>"
            + "<p style='margin:0 0 8px;color:rgba(255,255,255,0.6);font-size:12px;'>&#128205; Near Gurudwara, Akurdi Railway Station, Pune</p>"
            + "<a href='https://www.instagram.com/hkpg.akurdi' style='display:inline-block;background:linear-gradient(135deg,#f09433,#dc2743,#bc1888);color:#fff;text-decoration:none;padding:7px 14px;border-radius:8px;font-size:11px;font-weight:700;margin:3px;'>&#128248; Instagram</a>"
            + "<a href='https://wa.me/919579828996' style='display:inline-block;background:#25d366;color:#fff;text-decoration:none;padding:7px 14px;border-radius:8px;font-size:11px;font-weight:700;margin:3px;'>&#128172; WhatsApp</a>"
            + "<a href='" + siteUrl + "' style='display:inline-block;background:linear-gradient(135deg,#d63384,#c026d3);color:#fff;text-decoration:none;padding:7px 14px;border-radius:8px;font-size:11px;font-weight:700;margin:3px;'>&#127760; Website</a>"
            + "<p style='margin:10px 0 0;color:rgba(255,255,255,0.35);font-size:11px;'>NOTE: Auto-generated mail. Do not reply.</p>"
            + "</div></div></body></html>";

        send(adminTo, "&#128176; Rent Payment — " + studentName + " | Bed " + bedNumber + " | " + month, html);
    }

    // ── 4. Email to STUDENT when admin confirms rent payment ──────────────────
    @Async
    public void sendRentConfirmationToStudent(
            String studentEmail, String studentName,
            String bedNumber, String roomType, String amount, String month) {

        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'/></head>"
            + "<body style='font-family:Arial,sans-serif;background:#f4f4f8;padding:30px;'>"
            + "<div style='max-width:560px;margin:0 auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);'>"
            + "<div style='background:linear-gradient(135deg,#d63384,#c026d3);padding:28px;text-align:center;'>"
            + "<img src='https://hk-pg-akurdi.vercel.app/hkpg-logo.png' width='60' height='60' style='border-radius:50%;border:3px solid rgba(255,255,255,0.4);display:block;margin:0 auto 10px;'/>"
            + "<h1 style='margin:0;color:#fff;font-size:20px;font-weight:800;'>HK PG Akurdi</h1>"
            + "</div>"
            + "<div style='padding:28px;'>"
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
            + "<p style='margin:0;color:#92400e;font-size:13px;font-weight:600;'>&#128204; Always pay rent through the HK PG application only.</p>"
            + "</div>"
            + "<p style='margin:0;color:#374151;font-size:13px;'>Thanks &amp; Regards,<br/><strong style='color:#c026d3;'>HK PG MANAGEMENT</strong></p>"
            + "</div>"
            + "<div style='background:#1a1a2e;padding:20px;text-align:center;'>"
            + "<p style='margin:0 0 8px;color:rgba(255,255,255,0.6);font-size:12px;'>&#128205; Near Gurudwara, Akurdi Railway Station, Pune</p>"
            + "<a href='https://www.instagram.com/hkpg.akurdi' style='display:inline-block;background:linear-gradient(135deg,#f09433,#dc2743,#bc1888);color:#fff;text-decoration:none;padding:7px 14px;border-radius:8px;font-size:11px;font-weight:700;margin:3px;'>&#128248; Instagram</a>"
            + "<a href='https://wa.me/919579828996' style='display:inline-block;background:#25d366;color:#fff;text-decoration:none;padding:7px 14px;border-radius:8px;font-size:11px;font-weight:700;margin:3px;'>&#128172; WhatsApp</a>"
            + "<a href='https://hk-pg-akurdi.vercel.app' style='display:inline-block;background:linear-gradient(135deg,#d63384,#c026d3);color:#fff;text-decoration:none;padding:7px 14px;border-radius:8px;font-size:11px;font-weight:700;margin:3px;'>&#127760; Website</a>"
            + "<p style='margin:10px 0 0;color:rgba(255,255,255,0.35);font-size:11px;'>NOTE: Auto-generated mail. Do not reply.</p>"
            + "</div></div></body></html>";

        send(studentEmail, "&#9989; Rent Payment Confirmed — HK PG Akurdi | " + month, html);
    }
}
