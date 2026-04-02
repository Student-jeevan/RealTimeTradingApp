# How to Run & Verify Test Cases

## ✅ Method 1 — Run Tests in IntelliJ IDEA (Easiest)

### Option A: Run a single test class
1. Open any test file (e.g., `OtpServiceTest.java`)
2. Click the **▶ green arrow** beside the class name
3. OR right-click anywhere in the file → **"Run OtpServiceTest"**

### Option B: Run all unit tests at once
1. Right-click the folder: `src/test/java/com/jeevan/TradingApp/unit/`
2. Click **"Run 'Tests in unit'"**

### Option C: Run via the Test Runner panel
1. Go to **Run → Run...** or press `Alt+Shift+F10`
2. Select the test class you want to run

### What success looks like in IntelliJ:
```
✅ OtpServiceTest > generateAndStoreOtp > should generate OTP   PASSED
✅ OtpServiceTest > verifyOtp > should return true when correct PASSED
✅ OtpServiceTest > verifyOtp > should throw when expired       PASSED
...
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
```

---

## ✅ Method 2 — Run via Maven in PowerShell

### Step 1: Open terminal in project root
```powershell
cd C:\Users\hp\OneDrive\Desktop\TradingAppApplication\backend
```

### Step 2A: Run ONE specific test class (fastest check)
```powershell
mvn test -Dtest="OtpServiceTest"
```

### Step 2B: Run all unit tests (no Docker needed)
```powershell
mvn test -Dtest="**/unit/**/*Test"
```

### Step 2C: Run ALL tests (Docker must be running for integration)
```powershell
mvn test
```

### Step 2D: Run with detailed output (see actual test names)
```powershell
mvn test -Dtest="OtpServiceTest" -Dsurefire.failIfNoSpecifiedTests=false
```

---

## ✅ Method 3 — Run ONE test method (pinpoint check)
```powershell
# Syntax: ClassName#methodName
mvn test -Dtest="OtpServiceTest#shouldGenerateAndStoreOtp_whenNoCooldownExists"
```

---

## 📊 Reading Maven Test Output

### ✅ PASSING output
```
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### ❌ FAILING output
```
[ERROR] Tests run: 11, Failures: 2, Errors: 1, Skipped: 0
[ERROR] OtpServiceTest.shouldThrowOtpExpired_whenOtpKeyMissing  <<< FAILURE!
  Expected: CustomException
  But was:  NullPointerException
[INFO] BUILD FAILURE
```

### 📄 Full HTML report location after any test run:
```
backend/target/surefire-reports/index.html
```
Open this in any browser for:
- Pass/fail count per class
- Test execution time
- Full stack traces on failures

---

## ✅ Method 4 — Check Test Report Files (No IDE needed)

After running `mvn test`, check these files:

```
backend/target/surefire-reports/
├── TEST-com.jeevan.TradingApp.unit.service.OtpServiceTest.xml
├── TEST-com.jeevan.TradingApp.unit.service.OrderServiceImplTest.xml
├── TEST-com.jeevan.TradingApp.unit.kafka.TradeEventConsumerTest.xml
└── ... (one XML per test class)
```

Each XML contains:
```xml
<testsuite tests="11" failures="0" errors="0" skipped="0">
  <testcase name="shouldGenerateAndStoreOtp_whenNoCooldownExists" time="0.045"/>
  <testcase name="shouldThrowOtpCooldown_whenCooldownKeyExists" time="0.012"/>
  ...
</testsuite>
```

### Quick check via PowerShell:
```powershell
# Count passed/failed across all test reports
Get-Content backend\target\surefire-reports\*.xml |
  Select-String "testsuite" |
  Select-String "tests=" |
  ForEach-Object { $_ }
```

---

## 🐋 Integration Tests — Docker Required

Before running integration tests, ensure Docker Desktop is running:

```powershell
# Check Docker is running
docker ps

# Then run integration tests
mvn test -Dtest="**/integration/**/*Test"
```

Testcontainers will automatically:
1. Pull `mysql:8.0.33`, `redis:7-alpine`, `confluentinc/cp-kafka:7.5.0`
2. Start containers
3. Run tests against real services
4. Stop and clean containers

**First run:** ~2–3 minutes (image download)
**Subsequent runs:** ~30–60 seconds (images cached)

---

## ⚡ Recommended First Check (Quickest Proof)

Run this exact command — it runs **OtpServiceTest** (no Docker, no Kafka, pure Mockito):

```powershell
cd C:\Users\hp\OneDrive\Desktop\TradingAppApplication\backend
mvn test -Dtest="OtpServiceTest" --no-transfer-progress
```

**Expected output in ~20 seconds:**
```
[INFO] Running com.jeevan.TradingApp.unit.service.OtpServiceTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 🔧 Common Issues & Fixes

| Error | Cause | Fix |
|---|---|---|
| `ClassNotFoundException` | Test class not compiled | Run `mvn compile test-compile` first |
| `NoSuchBeanDefinitionException` | Missing `@Service` or wrong package | Check `@SpringBootApplication` scan path |
| `Cannot connect to Docker` | Docker not running (integration tests) | Start Docker Desktop |
| `Port 6370 already in use` | Another Redis on same port | Kill it or change port in `application-test.yml`|
| `Kafka topic not found` | Topic auto-create disabled | KafkaTopicConfig beans create them on startup |
| `ProcessedEvent already exists` | Forgot `@AfterEach` cleanup | Add `processedEventRepository.deleteAll()` |
