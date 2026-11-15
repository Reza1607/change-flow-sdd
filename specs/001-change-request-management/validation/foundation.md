# Foundation checkpoint — T005 تا T017

تاریخ تکمیل: **2026-11-15**. محدودهٔ مجاز کاربر فقط Foundation بود؛ تمام T005–T017 با شاهد مرتبط سبز و `[X]` شدند. **US1 آغاز نشده است؛ Task بعدی T018 است.**

## نتیجهٔ اجرای واقعی

دستور از ریشه، با cacheهای موقت Setup و بدون skip:

```bash
MAVEN_USER_HOME=/tmp/changeflow-maven-user MAVEN_OPTS=-Dmaven.repo.local=/tmp/changeflow-m2 ./mvnw verify
```

**Exit code: 0 — BUILD SUCCESS**. زمان: **01:17 min**؛ پایان: **2026-11-15T09:25:18+03:30**.

| Runner | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Surefire | 15 | 0 | 0 | 0 |
| Failsafe | 14 | 0 | 0 | 0 |
| مجموع | 29 | 0 | 0 | 0 |

[خروجی کامل واقعی verify](foundation-verify.txt). گزارش‌های XML همان اجرا در `target/surefire-reports` و `target/failsafe-reports` و JAR در `target/change-flow-0.0.1-SNAPSHOT.jar` هستند. پس از `./mvnw clean` موفق (exit code=0)، همین verify از target تمیز اجرا شد؛ تنظیمات source و resource بسته‌بندی‌شده مقایسه و byte-for-byte یکسان بودند. پس از این اجرا فقط Taskها و مستندات نهایی شدند؛ کد تغییر نکرد.

مانع دریافت image در [تلاش قبلی 2026-11-14](foundation-blocked-2026-11-14.md) ثبت شده است؛ با نصب image توسط کاربر برطرف شد. PostgreSQL 17.12 با دو backend مستقل واقعاً اجرا شد. هیچ DB mock یا H2 جایگزین نشد.

## دروازه‌ها، وابستگی و Scope

- prerequisite script با `--json --require-tasks --include-tasks`: exit code=0؛ FEATURE_DIR همین feature و AVAILABLE_DOCS شامل research، data-model، contracts، quickstart و tasks بود.
- requirements.md: 16 مورد، 16 checked، صفر unchecked، PASS؛ checklist و markerهای آن تغییر نکردند.
- منابع Constitution → Spec → Plan → Tasks → semantics → Data Model → OpenAPI → Assumptions → ADRها در این کار مطالعه شدند؛ طراحی و نسخه‌ها مطابق اسناد مصوب باقی ماندند.
- T005 پیش از تست‌های DB/API، T006 پیش از Migration T008، T007 پیش از Security، تست mapper پیش از پیاده‌سازی T011، تست guard پیش از adapter T013 و تست نگاشت پیش از mapper T014 اجرا شدند. T014 فقط شکل داده بود و T015 پس از آن تعریف شد؛ suite نهایی، checkpoint T017 است. T016 طبق جدول وابستگی مستقلاً پس از Setup مجاز بود.
- hooks.before_implement و hooks.after_implement وجود ندارند؛ `.specify/extensions.yml` موجود نیست. فایل‌های `.specify/` و `.agents/` تغییر نکردند.
- هیچ endpoint ایجاد/ویرایش/Submit/Approve/RequestChanges/list/history/delete، قاعدهٔ گردش کار، adapter کسب‌وکاری، کاربر dev یا API مدیریت هویت ساخته نشد. endpointهای `/test/*` فقط در test source set هستند و در JAR نیستند.

## ردیابی Task و شاهد

| Task | نتیجه و شاهد مرتبط |
| --- | --- |
| T005 | PostgresConnectionIT: image دقیق 17.12، JDBC واقعی و دو pg_backend_pid متفاوت؛ credential تصادفی، ServiceConnection؛ FR-014/015/016 |
| T006/T008 | SchemaIT: Migration از DB خالی؛ سه جدول، FK، nullable text و عنوان تکراری، status check، version اولیه/NOT NULL، unique Audit، زوج گذار/توضیح اجباری، حذف RESTRICT، indexها و timestamptz؛ FR-002/005/016 |
| T007/T010/T012 | SecurityIT، 6 تست HTTP واقعی با Security و DB واقعی: حساب معتبر/نامعتبر/غیرفعال، identity فقط از Context با ورودی جعلی، matching cookie/header، header یا cookie مفقود/غلط، منع token از query، عدم session و replay با cookie، 401 challenge، 403 و CORS بسته با Problem، نبود logout endpoint؛ CsrfCookieTest نیز Secure در TLS، HttpOnly و SameSite را بررسی می‌کند؛ FR-002/014/019 |
| T009/T013 | CurrentAccessIT، 3 تست: نقش تازه مستقل از authority قدیمی و Entity کش‌شده، account پایدار پس از حذف نقش‌ها، accessVersion/CAS، الزام transaction برای guard، missing/disabled fail closed و shared guard هم‌زمان با reader دیگر؛ writer واقعاً در pg_stat_activity منتظر Lock می‌ماند و پس از commit جلو می‌رود، با barrier و بدون sleep؛ FR-014/AS-22 |
| T011 | ProblemMappingTest و بخش خطاهای SecurityIT: RFC 9457، type/title/status/code صریح برای 400/401/403/404/409/422/500/503 و عدم بازتاب exception/SQL/stacktrace؛ نگاشت خطاهای feature طبق Plan در T024/T030 افزوده می‌شود؛ FR-019 |
| T014 | EntityMappingIT: Hibernate validate، round-trip همهٔ چهار status، متن nullable/Unicode و همهٔ فیلدهای تاریخچه و Instant، نگاشت دستی، @Version و Entityهای داخلی؛ FR-002/015/016 |
| T015 | Portهای typed ذخیره/Audit پس از شکل داده؛ build و ArchitectureTest سبز، بازبینی امضای expectedVersion اجباری update/delete، CAS و افزایش نسخه حتی edit هم‌مقدار، transaction مشترک و rollback در شکست Audit؛ adapter/تست رفتاری CAS و Audit طبق Plan در داستان‌ها ساخته می‌شود؛ هیچ ادعای اجرای adapter هنوزساخته‌نشده وجود ندارد؛ FR-015/016 |
| T016 | ArchitectureTest، 2 تست: domain بدون Spring/JPA و لایهٔ بیرونی، وابستگی به داخل، منع feature → web و identity → changerequest، ارتباط featureها از interfaceهای application/value typeها، Entity داخلی و غیرقابل‌انتشار در API، منع field injection و فراخوانی domain/persistence از Controller. Controller فعلی CSRF نیز با بازبینی فقط token را به DTO می‌برد؛ Constitution بخش ۵ |
| T017 | LogPrivacyIT با debug=true: JSON و correlationId تولیدشدهٔ سرور، operation امن (method/route template)، outcome و duration؛ بدون query/body/header/token/password/Authorization یا correlationId جعلی. سطوح صریح framework جلوی افشای query توسط DEBUG را می‌گیرند؛ کل suite سبز؛ FR-019 |

[شواهد red و green و حدود آن‌ها](foundation-tdd.md). شکست compile شاهد red محسوب نشد. هیچ stub، mapper با null یا adapter موقت بدون رفتار در کد نهایی باقی نمانده است.

## نسخه‌ها و منابع رسمی

هیچ تغییر نسخه یا dependency انجام نشد: Java 21، Boot 4.0.9/BOM، Security 7.0.8 و PostgreSQL 17.12 مطابق Plan. مستندات رسمی زیر برای API و پیکربندی بررسی شدند:

- [Boot starters/BOM](https://docs.spring.io/spring-boot/4.0/reference/using/build-systems.html)، [Testcontainers/ServiceConnection](https://docs.spring.io/spring-boot/4.0/reference/testing/testcontainers.html)، [StructuredLogEncoder](https://docs.spring.io/spring-boot/4.0/reference/features/logging.html)
- [Basic](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passwords/basic.html)، [DaoAuthenticationProvider](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passwords/dao-authentication-provider.html)، [CSRF](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html)
- [CsrfConfigurer و محل AccessDeniedHandler](https://docs.spring.io/spring-security/reference/7.0/api/java/org/springframework/security/config/annotation/web/configurers/CsrfConfigurer.html)، [CookieCsrfTokenRepository](https://docs.spring.io/spring-security/reference/7.0/api/java/org/springframework/security/web/csrf/CookieCsrfTokenRepository.html)، [CsrfTokenRequestAttributeHandler](https://docs.spring.io/spring-security/reference/7.0/api/java/org/springframework/security/web/csrf/CsrfTokenRequestAttributeHandler.html)
- [ProblemDetail](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)، [DefaultCorsProcessor](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/cors/DefaultCorsProcessor.html)، [Transaction](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)، [PostgreSQL row locks](https://www.postgresql.org/docs/17/explicit-locking.html)

## انحراف، اصلاح‌ها و محدودیت

**انحراف از Plan: ندارد.** تأخیر محیطی تلاش قبلی رفع شد. خطای پیاده‌سازی در محل handler CSRF، ثبت دوبارهٔ controller تستی و default type در mapper حین کار اصلاح شدند؛ این‌ها تعارض Plan با نسخه/API نبودند. نشت DEBUG در آزمون privacy پیدا و در محدودهٔ T017 رفع شد. Basic پیش از CSRF قرار گرفت تا ترتیب 401/403 قرارداد برقرار بماند؛ CSRF فقط cookie همراه header را می‌پذیرد. هیچ تصمیم محصولی تازه‌ای گرفته نشد؛ A-001 تا A-006 همچنان «قابل بازبینی» و بدون تغییرند.

تکمیل Foundation به معنی تکمیل محصول نیست: قواعد و endpointهای داستان‌ها، رقابت mutation/Audit، CI، dev bootstrap در T058 و پذیرش انسانی SC-006 هنوز بازند. نتیجهٔ پذیرش انسانی ساخته نشده و با تست خودکار جایگزین نشده است.

README و AGENTS با build واقعی، packageهای موجود و آماده‌نبودن dev تا T058 تطبیق یافتند. دستور `spring-boot:test-run` هنوز مسیر آینده است و اجرای آن در Foundation ادعا نشده است.

## بررسی نهایی قابل‌تکرار

`git diff --check` و بررسی Python برای XML گزارش‌ها، جمع 15/14 با صفر failure/error/skip، تیک T005–T017 و بازماندن T018–T064، وجود تمام فایل‌های فهرست‌شده، نبود support/test endpoint در JAR، و ثابت‌ماندن Spec/Plan/Assumptions/ADRها اجرا شدند؛ exit code=0. هیچ تستی skip نشده است.

## فایل‌های ویرایش‌شده

- `README.md`
- `AGENTS.md`
- `specs/001-change-request-management/tasks.md` (فقط checkboxهای T005–T017)
- `src/main/resources/application.yaml` (سطح امن لاگ‌های framework)

## فایل‌های ساخته‌شده

فهرست نسبت به HEAD پیش از Foundation است و fixture و گزارش‌های تلاش قبلی را نیز شامل می‌شود:

- `specs/001-change-request-management/validation/foundation-architecture.txt`
- `specs/001-change-request-management/validation/foundation-blocked-2026-11-14.md`
- `specs/001-change-request-management/validation/foundation-blocked-verify.txt`
- `specs/001-change-request-management/validation/foundation-postgres-pull.txt`
- `specs/001-change-request-management/validation/foundation-tdd.md`
- `specs/001-change-request-management/validation/foundation-verify.txt`
- `specs/001-change-request-management/validation/foundation.md`
- `src/main/java/com/changeflow/changerequest/application/AuditTrail.java`
- `src/main/java/com/changeflow/changerequest/application/ChangeRequestStore.java`
- `src/main/java/com/changeflow/changerequest/domain/ChangeRequest.java`
- `src/main/java/com/changeflow/changerequest/domain/ChangeRequestStatus.java`
- `src/main/java/com/changeflow/changerequest/domain/StatusHistoryEntry.java`
- `src/main/java/com/changeflow/changerequest/infrastructure/ChangeRequestEntity.java`
- `src/main/java/com/changeflow/changerequest/infrastructure/RequestLoggingFilter.java`
- `src/main/java/com/changeflow/changerequest/infrastructure/StatusHistoryEntity.java`
- `src/main/java/com/changeflow/identity/application/CurrentAccess.java`
- `src/main/java/com/changeflow/identity/application/CurrentActor.java`
- `src/main/java/com/changeflow/identity/domain/AccessSnapshot.java`
- `src/main/java/com/changeflow/identity/domain/ActorId.java`
- `src/main/java/com/changeflow/identity/domain/Role.java`
- `src/main/java/com/changeflow/identity/infrastructure/AccessProvisioner.java`
- `src/main/java/com/changeflow/identity/infrastructure/DatabaseCurrentAccess.java`
- `src/main/java/com/changeflow/identity/infrastructure/DatabaseIdentityAdapter.java`
- `src/main/java/com/changeflow/identity/infrastructure/IdentityAccountEntity.java`
- `src/main/java/com/changeflow/identity/infrastructure/IdentityAccountRepository.java`
- `src/main/java/com/changeflow/web/ApiExceptionHandler.java`
- `src/main/java/com/changeflow/web/CsrfController.java`
- `src/main/java/com/changeflow/web/ProblemResponses.java`
- `src/main/java/com/changeflow/web/SecurityConfiguration.java`
- `src/main/resources/db/migration/V1__initial_schema.sql`
- `src/main/resources/logback-spring.xml`
- `src/test/java/com/changeflow/architecture/ArchitectureTest.java`
- `src/test/java/com/changeflow/changerequest/api/LogPrivacyIT.java`
- `src/test/java/com/changeflow/changerequest/infrastructure/EntityMappingIT.java`
- `src/test/java/com/changeflow/changerequest/infrastructure/SchemaIT.java`
- `src/test/java/com/changeflow/identity/CurrentAccessIT.java`
- `src/test/java/com/changeflow/identity/SecurityIT.java`
- `src/test/java/com/changeflow/support/FoundationTestEndpoints.java`
- `src/test/java/com/changeflow/support/HttpIntegrationSupport.java`
- `src/test/java/com/changeflow/support/PostgresConnectionIT.java`
- `src/test/java/com/changeflow/support/PostgresTestSupport.java`
- `src/test/java/com/changeflow/web/CsrfCookieTest.java`
- `src/test/java/com/changeflow/web/ProblemMappingTest.java`
