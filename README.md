# سامانه مدیریت درخواست‌های تغییر (Change Request Management)

کارمندان شرکت نرم‌افزاری درخواست تغییر قابلیت، فرایند یا API ثبت می‌کنند. درخواست‌ها پیش از تأیید نهایی توسط شخص دیگری بررسی می‌شوند؛ مسیر اصلاح و تاریخچهٔ تصمیم‌ها نیز نگه‌داری می‌شود.

## وضعیت فعلی

Setup، Foundation و US1 تا US5 تا checkpoint T057 اعتبارسنجی شده‌اند؛ [گزارش US4 و خروجی واقعی verify](specs/001-change-request-management/validation/us4.md) و [تکمیل US5](specs/001-change-request-management/validation/us5.md) شامل شواهد دسترسی، pagination، حذف، رقابت و لغو نقش‌اند. List/Detail/History مطابق نقش جاری و مالکیت، حذف Draft با نسخهٔ مورد انتظار، و چرخهٔ کامل ثبت/بررسی/اصلاح موجودند. کار در T057 متوقف شده و Task بعدی T058 است؛ فاز Polish و پذیرش انسانی هنوز انجام نشده‌اند.

## Stack

Java 21، Spring Boot 4.0.9 و Maven؛ Modular Monolith با Package-by-Feature. طبق Plan و ADRها: PostgreSQL 17.12، Flyway، Spring Data JPA، HTTP Basic روی HTTPS با CSRF protection و تست‌های PostgreSQL واقعی با Testcontainers. جزئیات فنی در Plan و ADRها نگه‌داری می‌شوند. نسخه‌های دقیق با [Plan](specs/001-change-request-management/plan.md)، [ADR-001](docs/decisions/001-runtime-and-storage.md) و مستندات رسمی [Spring Boot 4.0.9](https://docs.spring.io/spring-boot/4.0/system-requirements.html) و [PostgreSQL 17.12](https://www.postgresql.org/docs/release/17.12/) تطبیق داده شده‌اند.

فعال‌بودن CSRF در [ADR-002](docs/decisions/002-identity-and-live-authorization.md) و Plan صریح است؛ دلیل ثبت‌شده در [Research، بخش هویت و نقش جاری](specs/001-change-request-management/research.md#۳-هویت-و-نقش-جاری)، نیاز کلاینت مرورگریِ HTTP Basic به حفاظت CSRF است. ADR-002 این دلیل را جداگانه توضیح نداده است.

## ساختار مخزن

پوشه‌ها و فایل‌های مرتبطِ موجود:

```text
pom.xml
mvnw
.mvn/wrapper/
src/main/java/com/changeflow/
├── ChangeFlowApplication.java
├── web/                      # Security، CSRF، Problem و no-store
├── identity/                 # ports، value types و adapterهای DB/guard
└── changerequest/
    ├── api/                  # DTOهای مستقل و Create/Edit/Delete/Submit/Approve/RequestChanges/List/Detail/History
    ├── application/          # use caseهای Draft/Submit/Approve/RequestChanges/Get/List/History/Delete، executor و سیاست تکرار
    ├── domain/               # مدل مستقل، قواعد Draft و policy چهار گذار
    └── infrastructure/       # Entity/نگاشت، CAS، Audit/رسید محدود، reader و لاگ
src/main/resources/           # application.yaml، logback و Migration V1
src/test/java/com/changeflow/
.agents/skills/
.specify/
├── memory/
├── scripts/
└── templates/
docs/
└── decisions/
specs/
└── 001-change-request-management/
    ├── checklists/requirements.md
    └── contracts/
```

[AGENTS.md](AGENTS.md) راهنمای Coding Agent و [Assumptions](docs/assumptions.md) محل شش فرض قابل بازبینی است.

## Build و Quickstart

پیش‌نیاز: JDK 21، Docker-compatible runtime فعال، curl و jq، و دسترسی دریافت Maven artifacts و container image. Maven Wrapper را پروژه تأمین می‌کند؛ تست‌ها PostgreSQL واقعی را با Testcontainers اجرا می‌کنند و نصب DB محلی لازم نیست.

از ریشهٔ پروژه Build و تست‌های موجود را اجرا کنید:

```bash
./mvnw verify
```

اجرای محلی dev زیر هنوز آماده نیست: bootstrap و کاربران محلی در T058 (Polish) ساخته می‌شوند. این دستور قرارداد اجرای آینده طبق Quickstart است:

```bash
read -rsp 'Local development password: ' CHANGEFLOW_DEV_PASSWORD
export CHANGEFLOW_DEV_PASSWORD
./mvnw spring-boot:test-run -Dspring-boot.run.profiles=dev
```

پس از T058، پروفایل dev روی `127.0.0.1:8080` اجرا خواهد شد و secret خالی را نخواهد پذیرفت. کاربران محلی `requester`، `reviewer` و `dual` به‌ترتیب نقش Requester، Reviewer و هر دو را دارند. این provision طبق Plan فقط در test source set خواهد بود؛ در محیط مشترک HTTPS لازم است. پس از توقف اجرا:

```bash
unset CHANGEFLOW_DEV_PASSWORD
```

راهنمای [Quickstart کامل](specs/001-change-request-management/quickstart.md) شامل CSRF، نمونهٔ درخواست‌ها و سناریوهای اعتبارسنجی است. `verify` تست‌های موجود را با Surefire و Failsafe اجرا می‌کند؛ تست‌های داستان‌های آینده هنوز پیاده نشده‌اند. وضعیت واقعی اجرا در گزارش validation آمده است. تست‌ها، نه race درخواست (از جمله سه race حذف) را هرکدام ۲۰ بار و دو ترتیب لغو/guard را برای edit، Submit، Approve، GET، List، History و Delete هرکدام ۲۰ بار با PostgreSQL واقعی و barrier اجرا می‌کنند؛ sleep و retry تصمیم استفاده نمی‌شود. خطای قطع منبع، حتی با شکست rollback، 503 عمومی و خطای غیرمنتظره 500 است.

## نقش‌ها و گردش وضعیت

اکنون `GET /api/v1/change-requests`، `DELETE /api/v1/change-requests/{id}?expectedVersion=...`، `POST /api/v1/change-requests`، `PUT /api/v1/change-requests/{id}`، `POST /api/v1/change-requests/{id}/submit`، `POST /api/v1/change-requests/{id}/approve` ، `POST /api/v1/change-requests/{id}/request-changes`، `GET /api/v1/change-requests/{id}` و `GET /api/v1/change-requests/{id}/history` پیاده شده‌اند. پاسخ موفق Approve رسید همان عملیات است و برای Reviewer دسترسی GET پس از Approved ایجاد نمی‌کند؛ تکرار همان عامل موفق با نقش جاری، 409 عمومی بدون DTO و دیگر Reviewerهای فاقد visibility، 404 می‌گیرند. تکرار RequestChanges توسط Reviewer قبلیِ فاقد دید 404 و توسط مالک دو‌نقشه 403 است؛ رسید ویژهٔ Approve به RequestChanges تعمیم داده نمی‌شود. تاریخچه فقط با visibility والد و شامل توضیح بازگشت است؛ endpoint ویرایش/حذف تاریخچه وجود ندارد. فهرست قبل از count و pagination در DB محدود می‌شود؛ page از صفر، size پیش‌فرض 20 و سقف 100 و ترتیب id صعودی است. حذف فقط Draft مالک دارای Requester جاری، بدون Audit و با پاسخ 204 است؛ نسخهٔ قدیمی 409 و حذف مجدد 404 می‌دهد.

- **Requester:** ایجاد Draft، ویرایش درخواست خود فقط در Draft و ChangesRequested، Submit و حذف فقط Draft خود.
- **Reviewer:** مشاهدهٔ PendingReviewها و Approve یا RequestChanges روی درخواست شخص دیگر؛ توضیح RequestChanges اجباری است.
- یک کاربر می‌تواند هر دو نقش را داشته باشد؛ این موضوع ممنوعیت بررسی درخواست خود را برنمی‌دارد.

```text
Draft --Submit--> PendingReview --Approve--> Approved
                        |
                  RequestChanges
                        v
                 ChangesRequested --Submit--> PendingReview
```

هر گذار دیگر نامعتبر است. Self-Approval و RequestChanges روی درخواست خود ممنوع‌اند؛ Approved تغییرناپذیر است. عنوان، شرح و دلیل در Draft می‌توانند ناقص باشند ولی هنگام Submit باید غیرخالی باشند. تعارض همزمانی خطای صریح دارد و هر گذار موفق با Audit اتمیک ثبت می‌شود.

Requester فقط درخواست‌های خود را می‌بیند؛ Reviewer به اتکای نقش بررسی فقط PendingReviewها را می‌بیند و Draft دیگران نامرئی است. دسترسی‌های کاربر دو‌نقشه تجمیع می‌شوند؛ جزئیات در Spec و API semantics آمده است.

## اسناد و ترتیب Spec Kit

[Constitution](.specify/memory/constitution.md) → [Spec](specs/001-change-request-management/spec.md) → [Plan](specs/001-change-request-management/plan.md) → [Tasks](specs/001-change-request-management/tasks.md)

[API semantics](specs/001-change-request-management/contracts/semantics.md)، [OpenAPI](specs/001-change-request-management/contracts/openapi.yaml)، [Data Model](specs/001-change-request-management/data-model.md) و [ADRها](docs/decisions/) جزئیات طراحی را ثبت می‌کنند.

ترتیب workflow برای توسعهٔ قابلیت:

```text
/speckit.constitution → /speckit.specify → /speckit.clarify
→ /speckit.plan → /speckit.tasks → /speckit.analyze → /speckit.implement
```

پیش از کدنویسی، Spec، Plan و Tasks باید تأیید شده باشند. قابلیت‌های AI، RAG، MCP، Multi-Agent، Microservices، Broker و Event Sourcing خارج Scope فعلی‌اند؛ اعلان‌ها، پیوست، کامنت چندنفره، تأیید چندمرحله‌ای و گزارش‌گیری پیشرفته نیز در این نسخه پیاده‌سازی نمی‌شوند.
