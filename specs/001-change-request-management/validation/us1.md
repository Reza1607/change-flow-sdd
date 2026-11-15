# US1 checkpoint — T018 تا T026

تاریخ: **2026-11-15**. فقط فاز ۳ اجرا شد؛ T018–T026 تکمیل و `[X]` شدند. کار در همین checkpoint متوقف است؛ **US2 و T027 به بعد آغاز نشده‌اند**.

## نتیجهٔ واقعی Build

فرمان از ریشه، بدون انتخاب تست و بدون skip:

```bash
MAVEN_USER_HOME=/tmp/changeflow-maven-user MAVEN_OPTS=-Dmaven.repo.local=/tmp/changeflow-m2 ./mvnw verify
```

**Exit code: 0 — BUILD SUCCESS**. مدت **01:42 min**؛ پایان **2026-11-15T10:51:37+03:30**.

| Runner | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Surefire | 40 | 0 | 0 | 0 |
| Failsafe | 28 | 0 | 0 | 0 |
| مجموع | 68 | 0 | 0 | 0 |

خروجی واقعی منتخب:

```text
[INFO] Tests run: 40, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  01:42 min
[INFO] Finished at: 2026-11-15T10:51:37+03:30
```

[خروجی کامل و بدون ویرایش verify](us1-verify.txt)، [شواهد واقعی Test-First](us1-tdd.md). گزارش XML در `target/surefire-reports` و `target/failsafe-reports` و JAR در `target/change-flow-0.0.1-SNAPSHOT.jar` است. PostgreSQL 17.12 واقعی با Testcontainers و HTTP/Security/CSRF واقعی استفاده شد؛ DB mock جایگزین نشده است. تنظیمات YAML بسته‌بندی‌شده با source byte-for-byte یکسان و fixtureهای تست خارج JAR هستند. پس از verify فقط Taskها و مستندات نهایی شدند؛ کد تغییر نکرد.

## ترتیب و پوشش Taskها

پیش‌نیازهای workflow موفق، requirements checklist با ۱۶ مورد checked و صفر unchecked، و اسناد به ترتیب AGENTS مطالعه شدند. ترتیب وابستگی T018/T019 ← T020 ← T021/T022 ← T023 ← T024 ← T025 ← T026 رعایت شد؛ HTTP پیش از service/controller قرمز بود. `.specify/extensions.yml` وجود ندارد؛ hook پس از implement ثبت نشده است.

| Task | شاهد اجرا/نتیجه |
| --- | --- |
| T018، T020 | DraftRulesTest: ۲۵ invocation سبز؛ ایجاد ناقص، هویت/مالکیت ثابت، nullable edit، منع PendingReview/Approved، Submit غیرخالی و تمام code pointهای Java whitespace/space separator؛ FR-001 تا FR-005، AS-01 تا AS-05 |
| T019، T023، T024 | DraftFlowIT: ۶ تست HTTP سبز؛ ایجاد 201/version=0/Location، PUT جایگزین سه فیلد، Submit 200/PendingReview و یک Audit، 400 قرارداد JSON، 403 نقش/مالکیت، 404 پنهان، 409 state/version و 422 فیلد نامعتبر؛ FR-001 تا FR-005 و FR-014 تا FR-019 |
| T021 | RequestStoreIT: ۳ تست PostgreSQL؛ create/load، @Version، افزایش نسخه در edit هم‌مقدار، CAS نسخهٔ قدیمی و جلوگیری از overwrite با managed entity قدیمی در دو transaction/connection مستقل؛ FR-015 |
| T022 | AuditStoreIT: ۲ تست PostgreSQL؛ actor/UTC/explanation/resultingVersion، قید یکتایی نسخه، شرکت در transaction caller و rollback؛ FR-016 |
| T025 | GetRequestIT: ۳ تست HTTP؛ predicate مجوز در query، guard تازه، مالک/Reviewer/dual، لغو نقش، هویت جعلی query بی‌اثر، DTO مستقل، hidden و missing با پاسخ یکسان؛ FR-012 تا FR-014 و FR-019، AS-14/AS-15 |
| T026 | این گزارش، خروجی verify و شواهد قرمز/سبز؛ README و AGENTS مطابق وضعیت واقعی |

در suite نهایی، ۳۹ تست جدید US1 و ۲۹ تست قبلی Setup/Foundation همگی سبزند. ArchitectureTest نیز استقلال domain از Spring/JPA، جهت وابستگی‌ها و نبود فراخوانی domain/persistence از Controller را بررسی می‌کند.

## شاهد بی‌اثر بودن Submit نامعتبر و اتمیک‌بودن Audit

`everyInvalidSubmitLeavesContentStateVersionAndAuditUnchanged` هر سه فیلد را جداگانه با null، رشتهٔ خالی، whitespace متداول و spaceهای Unicode امتحان می‌کند؛ Draft کاملاً ناقص نیز بررسی می‌شود. پس از 422، **تمام ستون‌های رکورد DB** با snapshot قبل برابرند؛ content، status و version ثابت و شمار Audit صفر است. خطا فقط نام فیلد و کد را می‌دهد.

`nullableReplacementCanClearDraftAndAuditFailureRollsBackSubmit` با یک CHECK constraint موقت واقعی، درج Audit را پس از mutation ناموفق می‌کند؛ پاسخ sanitized 500، رکورد درخواست کاملاً برابر snapshot قبل و Audit صفر است. constraint در finally حذف می‌شود. متن توضیح حساس آزمایشی در لاگ نیست. نشت Hibernate در اجرای قرمز پیدا و با بستن logger جزئیات خطای JDBC رفع شد.

Submit موفق دقیقاً یک Audit با actor از Security Context، زمان UTC از Clock تزریق‌شده، مبدأ Draft، مقصد PendingReview، توضیح پیش‌فرض خالی و resultingVersion جدید می‌سازد. زمان با Clock ثابت در HTTP تست شده است. Create و edit Audit ندارند. عنوان یکسان در دو درخواست مستقل مانع ایجاد یا Submit نیست؛ edit هم‌مقدار نیز نسخه را افزایش می‌دهد.

مجوز اولیه، مالکیت/وضعیت، نسخه و سپس اعتبارسنجی semantic به ترتیب اجرا می‌شوند؛ پیش از write، guard تازهٔ نقش تا commit نگه داشته می‌شود. mutation و Audit یک transaction هستند. Controller فقط DTO/service را متصل می‌کند؛ Entity منتشر نمی‌شود. هیچ actor یا role از body/query پذیرفته نمی‌شود.

این شاهد، سهم US1 در SC-001 و SC-004 است؛ چهار مسیر کل محصول، raceهای کامل US5 و پذیرش انسانی SC-006 هنوز متعلق به فازهای آینده‌اند و تکمیل‌شده ادعا نمی‌شوند.

## تأیید رسمی نسخه/API

نسخه و Starter یا dependency جدیدی تغییر نکرد؛ Java 21، Spring Boot 4.0.9 و PostgreSQL 17.12 طبق Plan باقی‌اند. برای API مورد استفاده اسناد رسمی بررسی شدند:

- [Boot 4 JSON/Jackson 3](https://docs.spring.io/spring-boot/4.0/reference/features/json.html) و [JsonMapperBuilderCustomizer](https://docs.spring.io/spring-boot/4.0/api/java/org/springframework/boot/jackson/autoconfigure/JsonMapperBuilderCustomizer.html): wiring تنظیم JSON سخت‌گیرانه در Boot 4.
- [Boot MVC how-to](https://docs.spring.io/spring-boot/4.0/how-to/spring-mvc.html) و [Spring MVC validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html): Records، RequestBody و Bean Validation.
- [Spring Data JPA 4 entity persistence](https://docs.spring.io/spring-data/jpa/reference/4.0/jpa/entity-persistence.html): ایجاد با UUID تخصیص‌یافته و version primitive از طریق EntityManager.persist تا new-state حدس زده نشود.
- [JPA LockModeType](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/LockModeType.html) و [EntityManager](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/EntityManager.html): OPTIMISTIC_FORCE_INCREMENT برای edit هم‌مقدار؛ @Version برای update/CAS. امکان تأخیر افزایش نسخه تا commit لحاظ و نتیجهٔ DB در تست تأیید شده است.

## انحراف و محدودیت

**انحراف از Plan: ندارد.** تصمیم محصولی جدید گرفته نشد؛ Spec، Plan، قراردادها، ADRها و Assumptions تغییر نکردند. اصلاح تشخیص missing/null در Jackson و بستن logger افشاکننده، اصلاح پیاده‌سازی در محدودهٔ قرارداد JSON/FR-019 هستند. خطای compile و NPE اولیه به‌عنوان شاهد رفتاری شمرده نشده‌اند؛ جزئیات در گزارش Test-First آمده است.

متد فنی delete در adapter صرفاً قرارداد CAS پورت موجود T015 را کامل می‌کند؛ use case، قواعد یا endpoint حذف US4 ساخته نشده‌اند. Approve/RequestChanges، فهرست/تاریخچه، race suiteهای آینده و bootstrap محیط dev در T058 بازند. README و AGENTS دستور verify و packageهای واقعی را شرح می‌دهند و dev را آماده معرفی نمی‌کنند. فایل‌های `.agents/` و `.specify/` دست‌نخورده‌اند. هیچ commit یا deploy انجام نشد.

## فایل‌های ساخته‌شده و ویرایش‌شده

### ساخته‌شده

- `specs/001-change-request-management/validation/us1-tdd.md`
- `specs/001-change-request-management/validation/us1-verify.txt`
- `specs/001-change-request-management/validation/us1.md`
- `src/main/java/com/changeflow/changerequest/api/ChangeRequestDtos.java`
- `src/main/java/com/changeflow/changerequest/api/DraftRequestController.java`
- `src/main/java/com/changeflow/changerequest/api/RequestReadController.java`
- `src/main/java/com/changeflow/changerequest/application/DraftRequestService.java`
- `src/main/java/com/changeflow/changerequest/application/GetRequestService.java`
- `src/main/java/com/changeflow/changerequest/application/RequestAccessFailure.java`
- `src/main/java/com/changeflow/changerequest/application/RequestMutationExecutor.java`
- `src/main/java/com/changeflow/changerequest/application/RequestReader.java`
- `src/main/java/com/changeflow/changerequest/domain/DraftRuleViolation.java`
- `src/main/java/com/changeflow/changerequest/infrastructure/ChangeRequestRepository.java`
- `src/main/java/com/changeflow/changerequest/infrastructure/JpaAuditTrail.java`
- `src/main/java/com/changeflow/changerequest/infrastructure/JpaChangeRequestStore.java`
- `src/main/java/com/changeflow/changerequest/infrastructure/JpaRequestReader.java`
- `src/main/java/com/changeflow/web/JsonConfiguration.java`
- `src/test/java/com/changeflow/changerequest/api/DraftFlowIT.java`
- `src/test/java/com/changeflow/changerequest/api/GetRequestIT.java`
- `src/test/java/com/changeflow/changerequest/domain/DraftRulesTest.java`
- `src/test/java/com/changeflow/changerequest/infrastructure/AuditStoreIT.java`
- `src/test/java/com/changeflow/changerequest/infrastructure/RequestStoreIT.java`
- `src/test/java/com/changeflow/support/DraftApiSupport.java`

### ویرایش‌شده

- `AGENTS.md`
- `README.md`
- `specs/001-change-request-management/tasks.md`
- `src/main/java/com/changeflow/changerequest/domain/ChangeRequest.java`
- `src/main/java/com/changeflow/changerequest/infrastructure/ChangeRequestEntity.java`
- `src/main/java/com/changeflow/web/ApiExceptionHandler.java`
- `src/main/resources/application.yaml`

