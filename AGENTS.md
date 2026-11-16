# راهنمای Coding Agent

## هدف و Stack

سامانهٔ Change Request Management برای ثبت تغییر قابلیت، فرایند و API در شرکت نرم‌افزاری است.
درخواست با بررسی شخص دیگر تأیید می‌شود؛ سادگی و توسعهٔ تدریجی اولویت دارند.
Stack فعلی طبق pom.xml: Java 21 و Spring Boot 4.0.9؛ Maven Wrapper توزیع Maven 3.9.16 را اجرا می‌کند. PostgreSQL 17.12 طبق fixtureها و ADR-001 است؛ سایر dependencyها تحت BOM، به‌جز ArchUnit 1.5.1 صریح در pom.xml.

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

مرجع دستورها: [Quickstart](specs/001-change-request-management/quickstart.md). `pom.xml` و Maven Wrapper موجودند؛ JDK 21، Docker-compatible runtime فعال و دسترسی artifact/image لازم است. این دستورها مسیر فعلی Build/Test هستند:

```bash
./mvnw verify
./mvnw clean verify
```

`verify` و `clean verify` با ۱۶۳ تست سبز اجرا شده‌اند؛ خروج واقعی clean verify و تعریف محدود «محیط تمیز» باید از [گزارش نهایی](specs/001-change-request-management/validation/final.md) خوانده شود. پاک‌کردن target به معنی ماشین تازه یا cache خالی نیست.

اجرای واقعی dev در T058 با Boot 4.0.9 بررسی شده است:

```bash
read -rsp 'Local development password: ' CHANGEFLOW_DEV_PASSWORD
export CHANGEFLOW_DEV_PASSWORD
./mvnw spring-boot:test-run -Dspring-boot.run.profiles=dev
# پس از توقف برنامه:
unset CHANGEFLOW_DEV_PASSWORD
```

DevApplication، DevServicesConfiguration و application-dev.yaml فقط در test source set هستند؛ PostgreSQL موقت و سه کاربر requester/reviewer/dual از همین مسیر تأمین می‌شوند و bootstrap در JAR تولید نیست. secret غایب/خالی و bind غیر loopback خطای startup می‌دهند؛ Agent MUST NOT رمز ثابت بسازد. HTTP فقط توسعهٔ loopback و TLS برای محیط مشترک الزامی است.

CSRF مطابق ADR-002 فعال است: GET احرازشدهٔ `/api/v1/csrf` و cookie/header متناظر برای mutation؛ Basic stateless جایگزین CSRF نیست. دلیل مرورگری در Research ثبت شده، ولی ADR-002 دلیل CSRF را جداگانه توضیح نداده است؛ Agent MUST NOT برای آن دلیل تازه‌ای بسازد. A-007 تفسیر تأییدشدهٔ ورودی query خالی است، نه حکم صریح قرارداد.

## ساختار Package و وابستگی

ساختار واقعی زیر `com.changeflow` است؛ Foundation، US1 تا US5 و T058 تا T060 اجرا شده‌اند. T062 تا T064 انجام شده و شواهد نهایی در [final.md](specs/001-change-request-management/validation/final.md) ثبت شده‌اند؛ T061/SC-006 باز است. عبارت مجاز وضعیت: «کامل از نظر فنی، پذیرش انسانی SC-006 باز»؛ این عبارت فقط پس از تأیید شواهد فنی در final.md به کار می‌رود و به معنی تکمیل کامل پروژه نیست.

- `changerequest.api`: Records ورودی/خروجی مستقل و Controllerهای Create/Edit/Delete/Submit/Approve/RequestChanges/List/Detail/History؛ ورودی Query نسخهٔ حذف و DTO صفحه مستقل، بدون قواعد کسب‌وکار.
- `changerequest.application`: Portهای ذخیره/Audit/خواندن، DraftRequestService، ReviewRequestService، RequestMutationExecutor، RepeatedTransitionPolicy ، GetRequestService، HistoryQueryService، HistoryReader، ListRequestsService و DeleteDraftService؛ هویت جاری و مرز تراکنش/guard.
- `changerequest.domain`: مدل مستقل `ChangeRequest` با قواعد ایجاد/ویرایش و حذف فقط Draft مالک، TransitionPolicy چهار گذار، خطاهای دامنه، `StatusHistoryEntry` و enum چهار وضعیت.
- `changerequest.infrastructure`: Entity/نگاشت دستی، JpaChangeRequestStore با CAS و @Version برای update/delete، JpaAuditTrail، StatusHistoryRepository با query محدود boolean رسید Approve، JpaRequestReader و JpaHistoryReader با predicate مجوز و join تاریخچه به والد و RequestLoggingFilter.
- `identity.application / domain / infrastructure`: Portهای هویت و دسترسی، value typeها، حساب JPA، adapter هویت، guard تازه و writer داخلی نسخه‌دار نقش؛ بدون endpoint مدیریت کاربران.
- `web`: Security wiring، CSRF endpoint، JSON سخت‌گیرانه، اعتبارسنجی query خالی طبق A-007، mapper مشترک 503/500 و no-store؛ TransactionConfiguration با JpaTransactionManager برای حفظ علت اصلی هنگام شکست rollback؛ composition root و Clock UTC در ChangeFlowApplication.
- RequestChanges از executor اتمیک مشترک استفاده می‌کند؛ ویرایش ChangesRequested/ارسال مجدد، تاریخچهٔ append-only و mixed/double RequestChanges race اکنون تست شده‌اند. Reviewer قبلیِ فاقد visibility برای تکرار RequestChanges، 404 می‌گیرد و استثنای رسید Approve تعمیم داده نمی‌شود. List و حذف Draft و سه delete race تکمیل شده‌اند؛ query count/items یک predicate visibility/status دارند و guard تازه تا تهیهٔ نتیجه/commit حفظ می‌شود. US5 پس از T046/T056 و verify کامل تکمیل شده است.
- resources: Migration V1، Hibernate validate و لاگ JSON؛ API/Persistence tests از PostgreSQL واقعی 17.12 استفاده می‌کنند.

- وابستگی MUST به داخل باشد؛ domain MUST مستقل از Spring/JPA باشد؛ تزریق MUST با Constructor Injection انجام شود.
- featureها MUST NOT به `com.changeflow.web` وابسته باشند؛ root وب برای wiring و نگاشت خطا به لایه‌های داخلی وابسته می‌شود.
- ترتیب کار MUST طبق جدول وابستگی و checkpointها در [tasks.md](specs/001-change-request-management/tasks.md) باشد.

## Definition of Done و گزارش

- Spec MUST برآورده، تست‌ها سبز در Maven verify (`./mvnw verify`) و Assumptions و مستندات به‌روز باشند.
- قبل از پایان هر Task/فاز، Agent MUST خروجی واقعی verify، exit code و شکست‌های باقی‌مانده را گزارش کند.
- اگر verify هنوز قابل اجرا نیست، Agent MUST علت واقعی را اعلام کند و MUST NOT موفقیت تست را ادعا کند؛ برای Task مستنداتی بررسی قابل‌تکرار ثبت شود.
- پذیرش انسانی SC-006 MUST با افراد واقعی انجام شود؛ Agent MUST NOT شاهد پذیرش جعل یا آن را با تست خودکار جایگزین کند. T061 تا ثبت نتیجهٔ واقعی ۵ از ۵ باز می‌ماند؛ ممیزی T064 یا تست خودکار آن را تکمیل نمی‌کند.
