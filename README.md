# Java Recruiter Outreach Utility

A Java 17 console application using Spring Boot Mail and Gmail SMTP. Enter a recruiter's name, email address, and company name; the app creates a personalized Java Backend Engineer application email and attaches your configured resume PDF.

## Features

- Interactive console input for recruiter name, email, and company
- Personalized subject and message
- Resume PDF attachment
- Gmail SMTP using environment variables (no credentials in source code)
- Preview-only mode by default
- CSV log with timestamp, recruiter, company, recipient, and status (`DRY_RUN`, `SENT`, or `FAILED`)
- Sends one email per recruiter, never a bulk To/Cc list

## Requirements

- JDK 17+
- Maven 3.9+
- Gmail account with 2-Step Verification enabled
- Google App Password (regular Gmail password will not work)
- Resume saved as a PDF

## 1. Configure Gmail

1. Enable 2-Step Verification on your Google account.
2. Create an App Password from your Google Account security settings, if your account is eligible.
3. Use the generated app password as `SMTP_APP_PASSWORD`. Do not use your regular Gmail password.
4. Some Google Workspace accounts or organization policies may disable App Passwords. If so, use an approved SMTP/OAuth setup from your administrator.

## 2. Set environment variables

### macOS / Linux (current terminal)

```bash
export SMTP_USERNAME="your.gmail.address@gmail.com"
export SMTP_APP_PASSWORD="your_16_character_app_password"
export SENDER_NAME="Faisal Ali"
export RESUME_PATH="/Users/your-user/Documents/Faisal_Ali_Resume.pdf"
export DRY_RUN="true"
export OUTREACH_LOG_PATH="outreach-log.csv"
```

### Windows PowerShell (current terminal)

```powershell
$env:SMTP_USERNAME = "your.gmail.address@gmail.com"
$env:SMTP_APP_PASSWORD = "your_16_character_app_password"
$env:SENDER_NAME = "Faisal Ali"
$env:RESUME_PATH = "C:\Users\your-user\Documents\Faisal_Ali_Resume.pdf"
$env:DRY_RUN = "true"
$env:OUTREACH_LOG_PATH = "outreach-log.csv"
```

Set `RESUME_PATH` to the actual location of your PDF. Do not put your real app password in source code, `pom.xml`, or a committed file.

## 3. Build and run

From the project root:

```bash
mvn clean package
mvn spring-boot:run
```

Or run the packaged jar:

```bash
java -jar target/java-recruiter-outreach-1.0.0.jar
```

The first run uses `DRY_RUN=true`, so it previews the email and logs `DRY_RUN` without sending. Check the recipient, generated message, and resume path.

## 4. Send real emails

After testing, set:

```bash
export DRY_RUN="false"
```

(Windows PowerShell: `$env:DRY_RUN = "false"`)

Restart the app. Each email will be sent individually. Confirm the recruiter address and company spelling before sending. Do not send unsolicited repetitive messages; personalize outreach and respect opt-outs.

## Console input

For each recruiter, enter:

- Recruiter name
- Recruiter email
- Company name

Enter `y` when asked whether to process another recruiter, or `n` to exit.

## Log format

The default `outreach-log.csv` is created in the current working directory.

```csv
timestamp,recruiter_name,company_name,recipient,status,details
```

Possible statuses:
- `DRY_RUN` — previewed but not sent
- `SENT` — SMTP accepted the message for sending (not a guarantee of inbox delivery)
- `FAILED` — validation, attachment, or sending failed

The app appends to the log. Keep it private because it contains recruiter contact details.

## Email template

**Subject:** Application for Java Backend Engineer – <Company>

Dear <Recruiter>,

I hope you are doing well.

I am writing to express my interest in Java Backend Engineer opportunities at <Company>. I have 3+ years of experience in backend development, working with Core Java, Spring Boot, REST APIs, microservices, SQL, Hibernate/JPA, and messaging technologies across fintech and travel domains.

I would appreciate it if you could consider my profile for a suitable opening. Please find my resume attached for your review. I would be happy to discuss how my experience aligns with your team's requirements.

Thank you for your time and consideration.

Best regards,
Faisal Ali
Java Backend Engineer

## Troubleshooting

- **Authentication failed:** verify `SMTP_USERNAME` and the Google App Password; ensure 2-Step Verification is enabled.
- **Resume not found:** check `RESUME_PATH`, including spaces and file extension.
- **Connection issues:** check network restrictions and Gmail SMTP access on port 587.
- **No real email sent:** `DRY_RUN` is intentionally `true` by default.
