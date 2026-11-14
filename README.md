# سامانه مدیریت درخواست‌های تغییر (Change Request Management)

کارمندان شرکت نرم‌افزاری درخواست تغییر قابلیت، فرایند یا API ثبت می‌کنند. درخواست‌ها پیش از تأیید نهایی توسط شخص دیگری بررسی می‌شوند؛ مسیر اصلاح و تاریخچهٔ تصمیم‌ها نیز نگه‌داری می‌شود.

## وضعیت فعلی

پروژه در مرحلهٔ SDD است: Constitution، Spec، Clarify، Plan، قراردادها و ۶۴ Task تهیه شده‌اند. پیاده‌سازی شروع نشده؛ `src/`، `pom.xml` و Maven Wrapper هنوز وجود ندارند. دستورهای اجرای برنامه و تست در این صفحه فقط برای **پس از Implement** هستند.

## Stack برنامه‌ریزی‌شده

Java 21، Spring Boot 4.0.9 و Maven؛ Modular Monolith با Package-by-Feature. طبق Plan و ADRها: PostgreSQL 17.12، Flyway، Spring Data JPA، HTTP Basic روی HTTPS با CSRF protection و تست‌های PostgreSQL واقعی با Testcontainers. جزئیات فنی در Plan و ADRها نگه‌داری می‌شوند.

## ساختار مخزن

پوشه‌های مرتبطِ موجود:

```text
.agents/skills/
.specify/
├── memory/
├── scripts/
└── templates/
docs/
└── decisions/
specs/
└── 001-change-request-management/
    ├── checklists/
    └── contracts/
```

[AGENTS.md](AGENTS.md) راهنمای Coding Agent و [Assumptions](docs/assumptions.md) محل شش فرض قابل بازبینی است.

## Quickstart — پس از Implement

پیش‌نیاز: JDK 21، Docker-compatible runtime فعال، curl و jq، و دسترسی دریافت Maven artifacts و container image. Maven Wrapper را پروژه تأمین می‌کند؛ تست‌ها PostgreSQL واقعی را با Testcontainers اجرا می‌کنند و نصب DB محلی لازم نیست.

**پس از Implement**، از ریشهٔ پروژه Build و تمام تست‌ها را اجرا کنید:

```bash
./mvnw verify
```

**پس از Implement**، اجرای محلی dev طبق Quickstart:

```bash
read -rsp 'Local development password: ' CHANGEFLOW_DEV_PASSWORD
export CHANGEFLOW_DEV_PASSWORD
./mvnw spring-boot:test-run -Dspring-boot.run.profiles=dev
```

پروفایل dev روی `127.0.0.1:8080` اجرا می‌شود و secret خالی را نمی‌پذیرد. کاربران محلی `requester`، `reviewer` و `dual` به‌ترتیب نقش Requester، Reviewer و هر دو را دارند. این provision فقط در test source set است؛ در محیط مشترک HTTPS لازم است. پس از توقف اجرا:

```bash
unset CHANGEFLOW_DEV_PASSWORD
```

راهنمای [Quickstart کامل](specs/001-change-request-management/quickstart.md) شامل CSRF، نمونهٔ درخواست‌ها و سناریوهای اعتبارسنجی است. نتیجهٔ مورد انتظار verify شامل Unit، Architecture و API/Persistence Integration Testهاست؛ این تست‌ها هنوز اجرا نشده‌اند.

## نقش‌ها و گردش وضعیت

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
