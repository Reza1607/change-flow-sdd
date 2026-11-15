# Foundation — اجرای ناقص و مانع محیطی

تاریخ: 2026-11-14. محدودهٔ مجاز کاربر فقط Foundation، T005 تا T017؛ US1 آغاز نشد. checkpoint پایان Foundation محقق نشده است.

## دروازه‌ها و ترتیب

- prerequisite script با `--json --require-tasks --include-tasks` موفق، exit code=0؛ FEATURE_DIR همین پوشه است.
- requirements.md: تعداد 16، checked=16، unchecked=0، PASS؛ checklist تغییر نکرد.
- منابع Constitution، Spec، Plan، Tasks، semantics، Data Model، OpenAPI، Assumptions و سه ADR خوانده شدند؛ research و quickstart نیز بررسی شدند.
- Setup T001–T004 قبلاً [X] است. T005 پیش‌نیاز T006/T007 است؛ پس از آن schema T008، هویت T009/T010، mapper T011 و Security T012، guard T013، شکل داده/نگاشت T014، Portها T015، تست مستقل T016 و logging/suite T017 هستند. T016 طبق جدول وابستگی، مستقلاً پس از Setup مجاز است.
- هیچ hook before_implement یا after_implement وجود ندارد؛ `.specify/extensions.yml` موجود نیست.

## منابع رسمی بررسی‌شده

- [Boot 4.0.9 starters/BOM](https://docs.spring.io/spring-boot/4.0/reference/using/build-systems.html)
- [Boot 4.0.9 Testcontainers و ServiceConnection](https://docs.spring.io/spring-boot/4.0/reference/testing/testcontainers.html)
- [Security 7.0.8 CSRF](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html)
- [Security 7.0.8 Basic](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passwords/basic.html)
- [DaoAuthenticationProvider](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passwords/dao-authentication-provider.html)
- [Boot structured logging](https://docs.spring.io/spring-boot/4.0/reference/features/logging.html)
- [Framework ProblemDetail](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)

نسخه یا API مغایر Plan مشاهده نشد. هیچ dependency یا نسخه‌ای عوض نشد؛ Security/mapper/logging هنوز پیاده نشده‌اند و بررسی مستندات شاهد اجرای آن‌ها نیست.

## کار انجام‌شده و حدود آن

- پیش‌نویس T005: fixture PostgreSQL 17.12 با `@Container` و `@ServiceConnection`، username/password تصادفی در زمان تست، helper اتصال JDBC مستقل. PostgresConnectionIT نوع/نسخهٔ DB و دو backend مستقل را بررسی می‌کند. compilation موفق بود، ولی container بالا نیامد؛ بنابراین T005 باز است.
- بخش مستقل T016: ArchitectureTest قواعد وابستگی به داخل، domain بدون Spring/JPA، منع feature → web، منع identity → changerequest، منع API → infrastructure، منع اتصال feature به identity.infrastructure و منع field injection را بررسی می‌کند. تست جدا سبز است، اما تست کامل Controller بدون Business Logic و قرارداد عمومی featureها هنوز تکمیل نشده؛ T016 نیز باز است. نبود Entity/Controller فعلی شاهد صحت پیاده‌سازی آینده نیست.
- T006–T015 و T017 شروع نشدند؛ بدون PostgreSQL واقعی، تست قرمز رفتاری قابل تأیید نبود. هیچ شکست محیطی یا compile به‌عنوان red رفتار ثبت نشده است. چرخهٔ red/green دامنه اجرا نشد؛ رفتار کسب‌وکار اضافه نشد.
- README و AGENTS با وجود واقعی Wrapper/POM/packageهای Setup و آماده‌نبودن dev bootstrap تا T058 تطبیق یافتند. هیچ endpoint کسب‌وکار، Migration یا adapter هویت ساخته نشده است.

## اجرای واقعی و مانع

دستور با cacheهای Setup، بدون skip تست:

```bash
MAVEN_USER_HOME=/tmp/changeflow-maven-user MAVEN_OPTS=-Dmaven.repo.local=/tmp/changeflow-m2 ./mvnw verify
```

- اجرای sandbox: exit code=1؛ socket Docker مجاز نبود.
- اجرای بیرون sandbox: Docker 29.0.1 قابل دسترسی بود؛ Maven compilation و 11 تست Surefire قبلی موفق شدند. Failsafe در PostgresConnectionIT به دریافت image رسید، اما TLS handshake timeout، connection reset و چند بار «Docker image pull has not made progress in 30s» رخ داد.
- تلاش مستقل `timeout 300s docker pull postgres:17.12` با exit code=124 تمام شد؛ image آماده نشد.
- JVM تست معلق همین verify با SIGTERM متوقف شد؛ Maven با **exit code=1 / BUILD FAILURE** خاتمه یافت (خطای پایان fork). این نتیجه، شکست رفتاری نرم‌افزار نیست؛ نتیجهٔ verify سبز نیز نیست. هیچ ادعای تعداد تست integration موفق وجود ندارد.
- [خروجی واقعی verify](foundation-blocked-verify.txt) و [خروجی تلاش دریافت image](foundation-postgres-pull.txt).
- اجرای مستقل `./mvnw -Dtest=ArchitectureTest test` با همان cacheها: **exit code=0 / BUILD SUCCESS**؛ 1 تست، failures=0، errors=0، skipped=0؛ [خروجی واقعی](foundation-architecture.txt).

به علت شکست پیش‌نیاز ترتیبی T005، کارهای وابسته متوقف شدند؛ هیچ checkbox تازه‌ای Done نشد. مهارت [speckit-implement](../../../.agents/skills/speckit-implement/SKILL.md) می‌گوید: “Halt execution if any non-parallel task fails.” اجرای بعدی باید از T005 و تأیید اتصال واقعی ادامه یابد؛ checkpoint T017 هنوز باز است.

## انحراف و باقی‌مانده

هیچ تغییر طراحی، نسخه، تصمیم محصولی یا جایگزینی DB انجام نشد. انحراف اجرایی: توقف پیش از checkpoint به علت شبکه/دریافت image؛ T005 و T016 ناقص‌اند و T006–T015/T017 اجرا نشده‌اند. تست‌های Schema/API/Security/guard/mapping/privacy و suite کامل Foundation هنوز باقی‌اند. راه ادامه، فراهم‌شدن image `postgres:17.12` در Docker همین محیط و اجرای مجدد verify است.

## فایل‌های ساخته‌شده

- `src/test/java/com/changeflow/support/PostgresTestSupport.java`
- `src/test/java/com/changeflow/support/PostgresConnectionIT.java`
- `src/test/java/com/changeflow/architecture/ArchitectureTest.java`
- `specs/001-change-request-management/validation/foundation.md`
- `specs/001-change-request-management/validation/foundation-verify.txt`
- `specs/001-change-request-management/validation/foundation-architecture.txt`
- `specs/001-change-request-management/validation/foundation-postgres-pull.txt`

فایل‌های ویرایش‌شده: `README.md` و `AGENTS.md`. tasks.md و منابع حقیقت تغییر نکردند.

## بررسی مستنداتی قابل‌تکرار

`git diff --check` و اسکریپت Python محلی برای وجود هفت فایل فوق، وجود Wrapper/POM، بازماندن تمام T005–T017 و T018، و نبود ادعای قدیمی «pom.xml و Wrapper وجود ندارند» در README/AGENTS اجرا شد؛ exit code=0. این بررسی جای تست Foundation نیست.
