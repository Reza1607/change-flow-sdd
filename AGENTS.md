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
- پیش از شروع هر فاز، Task ناتمام بعدی را از tasks.md بخوان و فقط همان فاز را انجام بده؛ از checkpoint فراتر نرو.
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
`pom.xml` و Maven Wrapper از Setup موجودند. Build و تست با JDK 21 و Docker-compatible runtime فعال اجرا می‌شوند؛ نتیجهٔ checkpoint T049 باید از [گزارش واقعی validation](specs/001-change-request-management/validation/us3.md) خوانده شود:

```bash
./mvnw verify
```

اجرای `./mvnw spring-boot:test-run -Dspring-boot.run.profiles=dev` هنوز آماده نیست؛ bootstrap و provision محیط dev متعلق به T058 است. اجرای dev MUST پیش‌نیازها و secret محلی Quickstart را داشته باشد؛ Agent MUST NOT رمز ثابت بسازد.

## ساختار Package و وابستگی

پس از Foundation، US1، US2، هستهٔ US5 و US3، ساختار واقعی زیر `com.changeflow` است؛ checkpoint T049 در [گزارش US3](specs/001-change-request-management/validation/us3.md) و [وضعیت US5](specs/001-change-request-management/validation/us5.md) ثبت شده و Task بعدی T050 است؛ فاز بعدی آغاز نشده است. **US5 تا تکمیل T056 Done نیست**:

- `changerequest.api`: Records ورودی/خروجی مستقل و Controllerهای POST/PUT/Submit/Approve/RequestChanges/GET تکی و History صفحه‌بندی‌شده، بدون قواعد کسب‌وکار.
- `changerequest.application`: Portهای ذخیره/Audit/خواندن، DraftRequestService، ReviewRequestService، RequestMutationExecutor، RepeatedTransitionPolicy ، GetRequestService، HistoryQueryService و HistoryReader؛ هویت جاری و مرز تراکنش/guard.
- `changerequest.domain`: مدل مستقل `ChangeRequest` با قواعد ایجاد/ویرایش، TransitionPolicy چهار گذار، خطاهای دامنه، `StatusHistoryEntry` و enum چهار وضعیت.
- `changerequest.infrastructure`: Entity/نگاشت دستی، JpaChangeRequestStore با CAS و @Version، JpaAuditTrail، StatusHistoryRepository با query محدود boolean رسید Approve، JpaRequestReader و JpaHistoryReader با predicate مجوز و join تاریخچه به والد و RequestLoggingFilter.
- `identity.application / domain / infrastructure`: Portهای هویت و دسترسی، value typeها، حساب JPA، adapter هویت، guard تازه و writer داخلی نسخه‌دار نقش؛ بدون endpoint مدیریت کاربران.
- `web`: Security wiring، CSRF endpoint، JSON سخت‌گیرانه، mapper مشترک 503/500 و no-store؛ TransactionConfiguration با JpaTransactionManager برای حفظ علت اصلی هنگام شکست rollback؛ composition root و Clock UTC در ChangeFlowApplication.
- RequestChanges از executor اتمیک مشترک استفاده می‌کند؛ ویرایش ChangesRequested/ارسال مجدد، تاریخچهٔ append-only و mixed/double RequestChanges race اکنون تست شده‌اند. Reviewer قبلیِ فاقد visibility برای تکرار RequestChanges، 404 می‌گیرد و استثنای رسید Approve تعمیم داده نمی‌شود. delete race تا T056 باز است؛ endpoint فهرست/حذف هنوز ساخته نشده است.
- resources: Migration V1، Hibernate validate و لاگ JSON؛ API/Persistence tests از PostgreSQL واقعی 17.12 استفاده می‌کنند.

- وابستگی MUST به داخل باشد؛ domain MUST مستقل از Spring/JPA باشد؛ تزریق MUST با Constructor Injection انجام شود.
- featureها MUST NOT به `com.changeflow.web` وابسته باشند؛ root وب برای wiring و نگاشت خطا به لایه‌های داخلی وابسته می‌شود.
- ترتیب کار MUST طبق جدول وابستگی و checkpointها در [tasks.md](specs/001-change-request-management/tasks.md) باشد.

## Definition of Done و گزارش

- Spec MUST برآورده، تست‌ها سبز در Maven verify (`./mvnw verify`) و Assumptions و مستندات به‌روز باشند.
- قبل از پایان هر Task/فاز، Agent MUST خروجی واقعی verify، exit code و شکست‌های باقی‌مانده را گزارش کند.
- اگر verify هنوز قابل اجرا نیست، Agent MUST علت واقعی را اعلام کند و MUST NOT موفقیت تست را ادعا کند؛ برای Task مستنداتی بررسی قابل‌تکرار ثبت شود.
- پذیرش انسانی SC-006 MUST با افراد واقعی انجام شود؛ Agent MUST NOT شاهد پذیرش جعل یا آن را با تست خودکار جایگزین کند.
