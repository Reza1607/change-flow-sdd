# راهنمای Coding Agent

## هدف و Stack

سامانهٔ Change Request Management برای ثبت تغییر قابلیت، فرایند و API در شرکت نرم‌افزاری است.
درخواست با بررسی شخص دیگر تأیید می‌شود؛ سادگی و توسعهٔ تدریجی اولویت دارند.
Stack الزامی: Java 21، Spring Boot 4، Maven؛ جزئیات نسخه و ابزارها طبق Plan و ADR.

## منبع حقیقت و ترتیب مطالعه

Agent MUST به‌ترتیب بخواند؛ Constitution مرجع الزام‌ها و Spec مرجع رفتار است:
1. [Constitution](.specify/memory/constitution.md)
2. [Spec](specs/001-change-request-management/spec.md)
3. [Plan](specs/001-change-request-management/plan.md)
4. [Tasks](specs/001-change-request-management/tasks.md)
5. [API semantics](specs/001-change-request-management/contracts/semantics.md)
6. [Data Model](specs/001-change-request-management/data-model.md)
7. [OpenAPI](specs/001-change-request-management/contracts/openapi.yaml)
8. [Assumptions](docs/assumptions.md)
9. [ADR-001](docs/decisions/001-runtime-and-storage.md)، [ADR-002](docs/decisions/002-identity-and-live-authorization.md)، [ADR-003](docs/decisions/003-concurrency-and-audit.md)

## قواعد کار

- Agent MUST NOT بدون Task در tasks.md کد بنویسد؛ Spec، Plan و Tasks MUST پیش از کدنویسی تأیید شده باشند.
- هر Task واقعاً تمام‌شده MUST با `[X]` در tasks.md علامت بخورد؛ کار ناقص یا تست‌نشده MUST NOT Done شود.
- Agent MUST NOT تصمیم محصولی بی‌صدا بگیرد؛ ابهام MUST پرسیده یا با شناسه، دلیل و وضعیت «قابل بازبینی» در docs/assumptions.md ثبت شود.
- تغییر رفتار MUST ابتدا در Spec و سپس در کد اعمال شود؛ Task MUST به FR یا Acceptance Criterion قابل‌ردیابی باشد.
- قواعد دامنه MUST به روش Test-First اجرا شوند: قرمز ← پیاده‌سازی ← سبز؛ خطای compile شاهد کافی شکست رفتار نیست.
- Agent MUST NOT API یا Starter مربوط به Spring Boot 4 را حدس بزند؛ مستندات رسمی MUST بررسی شوند.
- AI، RAG، MCP، Multi-Agent، Microservices، Broker و Event Sourcing خارج Scope محصول‌اند و MUST NOT پیاده‌سازی شوند.
- Entity پایگاه‌داده MUST NOT در API منتشر شود؛ DTO جدا MUST استفاده شود؛ Controller MUST NOT Business Logic داشته باشد.
- هویت MUST فقط از Security Context بیاید، نه Body/Query؛ نقش جاری MUST در زمان اجرا بررسی شود، مطابق A-006 و ADR-002.
- Approve و RequestChanges روی درخواست خود MUST رد شوند، حتی برای کاربر دو‌نقشه؛ Approved MUST تغییرناپذیر بماند.
- تغییرات MUST Optimistic Concurrency داشته باشند؛ تعارض MUST خطای صریح 409 بدهد؛ گذار و Audit MUST اتمیک باشند.
- Integration Test برای API و Persistence MUST از PostgreSQL واقعی استفاده کند؛ Mock DB MUST NOT جایگزین شود.
- Secret و دادهٔ حساس MUST NOT در مخزن، لاگ یا پاسخ API قرار گیرند.
- فایل‌های `.specify/` و `.agents/` MUST NOT دستی تغییر کنند؛ تغییر لازم MUST از workflow مربوط Spec Kit انجام شود.

## Build و Test

مرجع دستورها: [Quickstart](specs/001-change-request-management/quickstart.md).
اکنون pom.xml و Maven Wrapper وجود ندارند؛ مسیر Build/Test «پس از Foundation تکمیل می‌شود».
دستورهای زیر فعلاً قابل اجرا نیستند و برای **پس از Implement** هستند:

```bash
./mvnw verify
./mvnw spring-boot:test-run -Dspring-boot.run.profiles=dev
```

اجرای dev MUST پیش‌نیازها و secret محلی Quickstart را داشته باشد؛ Agent MUST NOT رمز ثابت بسازد.

## ساختار Package و وابستگی

ساختار برنامه‌ریزی‌شده زیر `com.changeflow` است؛ هنوز کد ایجاد نشده است:
- `changerequest.api / application / domain / infrastructure`: DTO/Controller، use case/Port، قواعد، adapterهای داده/Audit.
- `identity.application / domain / infrastructure`: قرارداد هویت و مجوز، نوع‌های هویت، adapterهای DB.
- `web`: Composition Root، Problem Details، Security wiring و CSRF endpoint.
- وابستگی MUST به داخل باشد؛ domain MUST مستقل از Spring/JPA باشد؛ تزریق MUST با Constructor Injection انجام شود.
- featureها MUST NOT به `com.changeflow.web` وابسته باشند؛ root وب برای wiring و نگاشت خطا به لایه‌های داخلی وابسته می‌شود.
- نوع‌های حداقلی Foundation در T014/T015 و قواعد Test-First در T020 تکمیل می‌شوند؛ checkpointها MUST طبق جدول Tasks رعایت شوند.

## Definition of Done و گزارش

- Spec MUST برآورده، تست‌ها سبز در Maven verify (`./mvnw verify`) و Assumptions و مستندات به‌روز باشند.
- قبل از پایان هر Task/فاز، Agent MUST خروجی واقعی verify، exit code و شکست‌های باقی‌مانده را گزارش کند.
- اگر verify هنوز قابل اجرا نیست، Agent MUST علت واقعی را اعلام کند و MUST NOT موفقیت تست را ادعا کند؛ برای Task مستنداتی بررسی قابل‌تکرار ثبت شود.
- پذیرش انسانی SC-006 MUST با افراد واقعی انجام شود؛ Agent MUST NOT شاهد پذیرش جعل یا آن را با تست خودکار جایگزین کند.
