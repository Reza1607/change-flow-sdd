# Implementation Plan: مدیریت درخواست‌های تغییر

**Branch**: `master` (شاخهٔ واقعی Git؛ پوشهٔ feature مستقل است) | **Date**: 2026-11-14 | **Spec**: [spec.md](spec.md)

**Input**: `specs/001-change-request-management/spec.md`، شامل پاسخ Clarify و فرض‌های A-001 تا A-006.

## Summary

یک REST service به شکل Modular Monolith برای ایجاد Draft، اصلاح، حذف مجاز، بررسی مستقل و تاریخچه ساخته می‌شود. چهار گذار Spec تنها گذارهای مجازند. Java 21 و Spring Boot 4 با Maven، PostgreSQL و Flyway مبنای اجرا هستند. دامنه مستقل از وب و Persistence، DTOها جدا و نقش‌های جاری در application service بررسی می‌شوند. همهٔ تغییرات موجودیت درخواست با نسخهٔ مورد انتظار، و گذار و Audit در یک تراکنش انجام می‌شوند. خروجی این مرحله فقط طراحی است؛ کد و Tasks در این مرحله تولید نمی‌شوند.

## Technical Context

**Language/Version**: Java 21؛ Spring Boot 4.0.9 به‌عنوان نسخهٔ مشخص و مستند این Plan؛ Maven Wrapper سری 3.9 با patch ثابت در زمان ایجاد Wrapper. وابستگی‌ها تحت BOM همان Boot؛ به‌روزرسانی minor/patch با بررسی سازگاری، تغییر major با Amendment.

**Primary Dependencies**: starters رسمی webmvc، validation، security، data-jpa و flyway؛ PostgreSQL JDBC و `flyway-database-postgresql`؛ بدون Lombok، framework نگاشت DTO یا موتور گردش کار. Jackson از BOM، DTO با Records و نگاشت دستی.

**Storage**: PostgreSQL 17.12 برای توسعه و تست؛ Flyway SQL نسخه‌دار تنها منبع Schema، Hibernate فقط validate؛ یک پایگاه‌داده و تراکنش محلی.

**Testing**: JUnit Jupiter و Spring Boot test support تحت BOM، Spring Security test، PostgreSQL واقعی با Testcontainers و ServiceConnection؛ Surefire برای `*Test` و Failsafe برای `*IT` در `./mvnw verify`؛ تست ساختار وابستگی با ArchUnit در test scope.

**Target Platform**: JVM روی Linux و TLS در ورودی سرویس؛ Docker-compatible runtime برای تست. HTTP فقط در loopback توسعه مجاز است.

**Project Type**: یک Maven module و executable JAR؛ REST API، بدون UI مستقل یا سرویس خارجی.

**Performance Goals**: هدف عملکردی جدیدی به محصول اضافه نمی‌شود؛ معیار پذیرش SC-001 تا SC-006 است. اندازه‌گیری زمان عملیات در تست و لاگ انجام می‌شود، اما SLA یا load target بدون نیاز مصوب تعیین نمی‌شود.

**Constraints**: HTTP Basic با CSRF protection و بدون session احراز هویت؛ شناسهٔ معتبر فقط از Security Context. نقش از منبع معتبر فعلی خوانده می‌شود. optimistic concurrency همهٔ ویرایش‌ها، حذف‌ها و گذارها را پوشش می‌دهد. هیچ retry خودکاری برای تصمیم یا تعارض انجام نمی‌شود.

**Scale/Scope**: یک شرکت، دو نقش و چهار وضعیت؛ فهرست با page/size و سقف ۱۰۰ مورد؛ قابلیت‌های خارج از Scope وارد طراحی نمی‌شوند. A-001 تا A-005 همچنان قابل بازبینی‌اند؛ A-006 تعریف ترتیب پذیرش/لغو مجوز را ثبت می‌کند.

## Constitution Check

| Gate | پیش از Research | پس از Design / شاهد |
| --- | --- | --- |
| Spec، Plan و Tasks پیش از کد؛ ردیابی FR | PASS؛ این فرمان فقط طراحی است | PASS؛ ماتریس پایین؛ Implement تا تولید و تأیید Tasks مجاز نیست |
| Java 21، Boot 4، Maven، relational و Migration | PASS | PASS؛ Technical Context و ADR-001 |
| فقط چهار گذار، مالکیت، منع بررسی خود، Approved ثابت | PASS | PASS؛ data-model و قرارداد، AS-07/08/09/23 |
| Optimistic Concurrency تمام تغییرات؛ Audit اتمیک | PASS | PASS؛ ADR-003، نسخه در درخواست و Audit |
| Security Context، مجوز جاری و لغو عملیات جاری | PASS؛ نیازمند طراحی ترتیب اتمیک | PASS تحت A-006؛ ADR-002، تست دو ترتیب race؛ مجوز کش‌شده پذیرفته نیست |
| Modular Monolith، package-by-feature، وابستگی به داخل | PASS | PASS؛ ساختار پایین؛ دامنه بدون Spring/JPA |
| DTO جدا، Constructor Injection، حداقل Abstraction | PASS | PASS؛ فقط Portهای مورد نیاز برای داده، هویت، مجوز و Audit |
| REST v1، Validation، Pagination، Problem Details | PASS | PASS؛ contracts/openapi.yaml و semantics.md |
| Test-First و تست واقعی Persistence/API | PASS | PASS؛ راهبرد تست و Quickstart؛ تست‌ها هنوز اجرا نشده‌اند |
| Build یک‌دست، README، structured log، ADR | PASS | PASS؛ Quickstart و ADRها؛ README در Implement نوشته می‌شود |
| Scope و Definition of Done | PASS | PASS؛ Scope مطابق Spec؛ تست و مستندات جزو Done |

دروازه‌ها بررسی طراحی هستند و به معنی اجرای موفق نرم‌افزار یا تأیید خودکار اسناد نیستند. درخواست کاربر برای Plan مجوز تهیهٔ طراحی است؛ Spec از نظر محتوایی تغییر نکرده و وضعیت تأیید نهایی آن مستقل باقی می‌ماند.

## Project Structure

### Documentation (this feature)

```text
specs/001-change-request-management/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── openapi.yaml
│   └── semantics.md
└── checklists/requirements.md

docs/
├── assumptions.md
└── decisions/
    ├── 001-runtime-and-storage.md
    ├── 002-identity-and-live-authorization.md
    └── 003-concurrency-and-audit.md
```

`tasks.md` خروجی فرمان بعدی است و در این مرحله ساخته نمی‌شود.

### Source Code (repository root)

ساختار پیشنهادی برای Implement؛ هنوز ایجاد نشده است:

```text
pom.xml
mvnw
.mvn/wrapper/
src/main/java/com/changeflow/
├── ChangeFlowApplication.java
├── web/                      # مرز وب مشترک و Composition Root؛ وابستگی فقط به داخل
│   ├── ProblemResponses.java
│   ├── ApiExceptionHandler.java
│   ├── SecurityConfiguration.java
│   └── CsrfController.java
├── changerequest/
│   ├── api/                  # Controller و Records؛ بدون مالکیت mapper مشترک
│   ├── application/          # use cases، transaction boundary، ports
│   ├── domain/               # مدل مستقل، status، policies، errors
│   └── infrastructure/       # JPA entities، adapters، mapping، audit persistence
└── identity/
    ├── application/          # CurrentActor و CurrentAccess ports
    ├── domain/               # ActorId، Role، AccessSnapshot
    └── infrastructure/       # DB identity adapter؛ بدون وابستگی به API یا web
src/main/resources/
├── application.yaml
└── db/migration/
src/test/java/com/changeflow/
├── changerequest/domain/
├── changerequest/api/
├── changerequest/infrastructure/
├── identity/
├── web/
└── architecture/
```

**Structure Decision**: package-by-feature در یک ماژول. api → application → domain؛ infrastructure قراردادهای application را پیاده می‌کند و به داخل وابسته است. اتصال adapterها در composition root انجام می‌شود. `changerequest.application` فقط قرارداد عمومی `identity.application` و value objectهای آن را مصرف می‌کند؛ هیچ feature به infrastructure دیگری وابسته نیست. Entity JPA مستقل از مدل دامنه است. Audit یک جزء درخواست تغییر است، نه feature یا سرویس جدا. Clock استاندارد برای زمان قابل‌کنترل در تست تزریق می‌شود. مرز بیرونی `com.changeflow.web` مالک ساخت Problem Details، نگاشت استثناهای HTTP، Security configuration و wiring handlerهاست؛ root به Portها/adapterهای داخلی وابسته است و identity.infrastructure هیچ وابستگی به web یا changerequest.api ندارد. Controllerهای feature نیز به mapper root وابسته نمی‌شوند؛ exceptionها در root به HTTP تبدیل می‌شوند. T011 فقط mapper عمومیِ بدون ارجاع به نوع‌های هنوزتعریف‌نشده می‌سازد؛ نگاشت exceptionهای feature پس از تعریف آن‌ها در T024/T030 تکمیل می‌شود.

## Phase 0 — Research

تمام انتخاب‌ها و جایگزین‌ها در [research.md](research.md) و سه ADR ثبت شده‌اند. منابع رسمی Boot 4، Spring Security، Jakarta Persistence و PostgreSQL بررسی شدند. نکتهٔ کلیدی: بررسی مجوز ابتدا و سپس نوشتن بدون هماهنگی با لغو نقش، کافی نیست؛ ADR-002 ترتیب اتمیک را تعریف می‌کند.

## Phase 1 — Design

- [data-model.md](data-model.md): مدل، قیود، مجوز و الگوریتم تراکنش.
- [contracts/openapi.yaml](contracts/openapi.yaml): قرارداد قابل‌پردازش.
- [contracts/semantics.md](contracts/semantics.md): تقدم خطاها، تکرار، دسترسی و تراکنش.
- [quickstart.md](quickstart.md): اجرای آتی و اعتبارسنجی سناریوها، بدون پیاده‌سازی در این مرحله.

### مرز Foundation و اجرای Test-First

T014 فقط شکل داده/امضای ChangeRequest، ChangeRequestStatus و StatusHistoryEntry بدون رفتار کسب‌وکاری را پیش از Entity mapping تعریف می‌کند؛ T015 Portها را با همین نوع‌ها تعریف می‌کند. قواعد ایجاد، ویرایش و Submit در T020 پس از تست قرمز T018 تکمیل می‌شوند و adapter Audit در T022 از نوع موجود استفاده می‌کند. دلیل: کمترین تغییر در ترتیب موجود، بدون وابستگی Foundation به پیاده‌سازی آیندهٔ US1.

تست HTTP پیش از endpoint نوشته و قرمز می‌شود؛ T023 سبزشدن کل T019 را نمی‌خواهد، بلکه T024 endpoint را تکمیل و T026 checkpoint می‌شود. کل T028 پس از T031 و checkpoint US2 پس از T033 در T034 سبز است. بخش history از T043 پس از T047 و checkpoint US3 پس از T048 در T049 تکمیل می‌شود.

### راهبرد تست و ردیابی

| نیازمندی‌ها | آزمون و شاهد |
| --- | --- |
| FR-001 تا FR-005 | unit اعتبارسنجی/مالکیت؛ API واقعی ایجاد ناقص، ویرایش، Submit ناقص/کامل، عنوان تکراری؛ AS-01 تا AS-05 |
| FR-006، FR-007، FR-009 تا FR-011، FR-021 | جدول کامل ۴ وضعیت × ۳ عمل؛ ماتریس هویت/نقش؛ منع هر دو عمل روی خود؛ AS-06/07/09/11/12/13/23 |
| FR-003، FR-008 | edit/delete هر وضعیت توسط مالک/غیرمالک؛ Approved ثابت؛ AS-02/08/16 |
| FR-012، FR-013، FR-017، FR-018 | فهرست و مشاهده/تاریخچهٔ مستقیم برای دو نقش و دو‌نقشه، ۴ وضعیت، page boundary و empty page؛ AS-14/15/17/18 |
| FR-014 | دو تراکنش با barrier کنترل‌شده: لغو commit قبل از guard نهایی؛ و guard قبل از لغو؛ بدون sleep؛ AS-22، A-006 |
| FR-015 | دو EntityManager/connection مستقل؛ update/update، update/delete، update/Submit، Approve/Approve، Approve/RequestChanges؛ هر رقابت ۲۰ بار؛ AS-19/20 |
| FR-016 | خطای اجباری هنگام درج Audit و rollback کامل؛ دقیقاً یک رکورد در موفقیت؛ AS-10 |
| FR-019، FR-020 | همهٔ خطاهای قرارداد، CSRF، عدم افشا، تکرار با نسخهٔ قدیمی و جاری؛ AS-04/12/17/21 |
| SC-006 | پذیرش دستی با ۵ کارمند و Reviewer دیگر؛ نتیجه ثبت شود، با تست خودکار جایگزین نمی‌شود |

قواعد دامنه Test-First با مشاهدهٔ تست قرمز و سپس پیاده‌سازی؛ application integration با PostgreSQL واقعی. API integration با HTTP واقعی روی random port، زنجیرهٔ Security واقعی و همان Migrationها؛ repository یا DB mock نمی‌شود. خطای Audit با fault injection در مرز adapter و بررسی rollback DB آزموده می‌شود. هیچ تست رقابت نباید با outer test transaction یا اجرای ترتیبی، رقابت را پنهان کند. تست معماری ورود وابستگی Spring/JPA به domain و انتشار Entity در API را منع می‌کند.

لاگ JSON شامل correlationId، operation، outcome و شناسهٔ فنی درخواست؛ بدون متن عنوان/شرح/دلیل/توضیح، credential، Authorization header یا secret. خطای Security نیز از همان Problem mapper استفاده می‌کند. اطلاعات داخلی stack trace فقط در پاسخ منع می‌شود و دادهٔ حساس در لاگ نیز مجاز نیست.

## Complexity Tracking

نقضی از Constitution ثبت نشده است. guard کوتاه روی رکورد مجوز برای الزام موجود FR-014 ضروری است؛ کنترل همزمانی خود درخواست همچنان optimistic است. هیچ Broker، سرویس جدا، موتور Workflow یا Abstraction برای نیاز نامشخص اضافه نمی‌شود.
