# Research — مدیریت درخواست تغییر

تاریخ: 2026-11-14. تصمیم‌ها برای Plan نسخهٔ اولیه‌اند و Constitution را تغییر نمی‌دهند.

## ۱. Runtime و ساخت

**Decision:** Java 21، Spring Boot 4.0.9 و Maven Wrapper سری 3.9؛ نسخهٔ patch Wrapper در Implement ثابت می‌شود. Boot parent/BOM وابستگی‌های Spring، Hibernate، Jackson و ابزار تست را مدیریت می‌کند. starters رسمی webmvc، validation، data-jpa، security و flyway انتخاب می‌شوند؛ artifactهای database-specific به‌صورت صریح افزوده می‌شوند.

**Rationale:** قیدهای Constitution را با کمترین تنظیم مستقل برآورده می‌کند. مستندات شاخهٔ 4.0 نسخهٔ 4.0.9 و سازگاری Java 21 را نشان می‌دهند؛ انتخاب این خط به معنی نیاز به آخرین minor نیست.

**Alternatives considered:** تغییر major خلاف Constitution؛ نسخه‌گذاری مستقل تمام dependencyها ریسک ناسازگاری می‌افزاید؛ reactive stack برای این گردش تراکنشی لازم نیست.

منابع: [System Requirements](https://docs.spring.io/spring-boot/4.0/system-requirements.html)، [Build Systems و starters](https://docs.spring.io/spring-boot/4.0/reference/using/build-systems.html).

## ۲. Persistence، Migration و تست

**Decision:** PostgreSQL 17.12، Flyway SQL و Spring Data JPA با Entity جدا از مدل دامنه. Schema فقط با Migration؛ Hibernate validate. Testcontainers همان نسخهٔ PostgreSQL را در Integration Test اجرا می‌کند؛ ServiceConnection تنظیم اتصال تست را می‌سازد.

**Rationale:** یک تراکنش برای درخواست و Audit کافی است؛ آزمون روی همان موتور، رفتار نسخه و قفل را واقعی می‌سنجد.

**Alternatives considered:** H2 یا mock جایگزین DB واقعی نیست؛ Liquibase ممکن است اما changelog چندفرمتی نیاز این Scope نیست؛ JDBC مستقیم ممکن است اما JPA نگهداری نسخه را استاندارد می‌کند.

منابع: [Testcontainers در Boot 4](https://docs.spring.io/spring-boot/4.0/reference/testing/testcontainers.html)، [PostgreSQL 17 locks](https://www.postgresql.org/docs/17/explicit-locking.html)، [Flyway starter](https://docs.spring.io/spring-boot/4.0/reference/using/build-systems.html).

## ۳. هویت و نقش جاری

**Decision:** HTTP Basic روی HTTPS، password hash با PasswordEncoder استاندارد و کاربران provisionشده در DB؛ هیچ endpoint مدیریت کاربر/نقش در محصول نیست. CurrentActor از Security Context فقط شناسه می‌گیرد. CurrentAccess نقش را از DB می‌خواند؛ authorities خوانده‌شده هنگام authentication به‌تنهایی مجوز عملیات نیستند. Basic برای هر درخواست بررسی و session احراز هویت ایجاد نمی‌شود.

CSRF فعال می‌ماند؛ CookieCsrfTokenRepository و GET احرازشدهٔ `/api/v1/csrf` token/headerName را می‌دهند؛ کلاینت cookie و header را برای mutation ارسال می‌کند. CORS پیش‌فرض بسته، پاسخ حساس no-store و Cookie در محیط TLS با Secure و SameSite مناسب. غیرفعال‌کردن CSRF به دلیل stateless بودن Basic مجاز نیست. handlerهای Security نیز Problem Details می‌دهند.

**Rationale:** ساده و قابل‌تعویض پشت Port هویت است و به token بلندعمر حاوی نقش نیاز ندارد. CSRF برای کلاینت مرورگریِ Basic نیز باید در نظر گرفته شود.

**Alternatives considered:** JWT خودبسنده نقش لغوشده را تا انقضا نگه می‌دارد؛ session قابل استفاده است اما برای این API مدیریت ابطال اضافه می‌خواهد؛ IdP سازمانی تا تعیین نیاز واقعی وارد Scope نمی‌شود.

منابع: [Basic](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/basic.html)، [Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)، [Security Context](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html)، [CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

## ۴. ترتیب مجوز و پذیرش

**Decision:** رکورد پایدار access برای هر کاربر نگه داشته می‌شود. عملیات پیش از پذیرش نهایی، در همان transaction یک guard اشتراکی روی آن می‌گیرد، نقش را دوباره می‌خواند و تا commit نگه می‌دارد. لغو نقش با guard انحصاری همان رکورد و نسخهٔ مورد انتظار انجام می‌شود. تمام مسیرهای تغییر نقش باید این قرارداد را رعایت کنند؛ writer بیرونی ناسازگار پشتیبانی نمی‌شود.

**Rationale:** این طراحی استنتاج ما از الزامات است، نه قابلیت خودکار Spring. خواندنِ دوباره بدون guard بازهٔ race را نمی‌بندد. «لغوشدن» به معنی commit موفق تغییر نقش است (A-006): اگر لغو قبل از guard نهایی commit شود، عملیات شروع‌شده نیز رد می‌شود؛ اگر عملیات guard را زودتر گرفته باشد، commit آن پیش از مؤثرشدن لغو خواهد بود. آغاز درخواست لغو به معنی ابطال فوری تراکنش‌های پیش‌تر پذیرفته‌شده نیست.

**Alternatives considered:** نقش cacheشده، نقش داخل JWT و صرف بررسی در Controller رد می‌شوند؛ سراسری‌کردن قفل درخواست‌ها لازم نیست. اگر منظور محصول لغو از لحظهٔ اعلام intent، حتی پیش از commit لغو باشد، A-006 باید بازبینی و Spec اصلاح شود؛ Plan چنین تضمینی ادعا نمی‌کند.

منابع: [PostgreSQL row locks](https://www.postgresql.org/docs/17/explicit-locking.html)، [Spring Data JPA locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)، [Spring transaction](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html). جزئیات در ADR-002.

## ۵. نسخه و Audit

**Decision:** version عددی با JPA `@Version`؛ `expectedVersion` برای edit و commandها و query متناظر برای delete الزامی است. current state نامعتبر در تکرار command → 409 INVALID_TRANSITION؛ state معتبر ولی نسخه قدیمی → 409 CONCURRENT_MODIFICATION. رقابت پس از خواندن با optimistic lock به 409 تبدیل می‌شود. هیچ retry خودکار انجام نمی‌شود. Audit و mutation در یک تراکنش؛ unique(request_id, resulting_version).

**Rationale:** نسخهٔ JPA به‌تنهایی stale edit میان دو HTTP request را نمی‌گیرد؛ نسخهٔ مورد انتظار کلاینت نیز لازم است. قرارداد عددی امکان پاسخ 409 مطابق Constitution را می‌دهد، بدون استفادهٔ نادرست از If-Match و 412.

**Alternatives considered:** last-write-wins رد؛ pessimistic lock برای خود درخواست لازم نیست؛ Event Sourcing خارج از Scope؛ ETag/If-Match صحیح به 412 نیاز دارد و انتخاب این نسخه نیست.

منبع: [Jakarta Persistence Version](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/Version.html).

## ۶. قرارداد و مرزها

**Decision:** OpenAPI 3.1 در Git منبع قرارداد است؛ REST `/api/v1`، Records مستقل از Entity، نگاشت دستی، RFC 9457 با ProblemDetail و ControllerAdvice؛ security failureها با entry point/access denied handler همان قالب را دارند. Pagination با ترتیب id ثابت و فیلتر مجوز در query، نه پس از صفحه‌بندی.

**Rationale:** قرارداد خطا، رفتار تکرار و visibility را قابل‌آزمون می‌کند؛ پنهان‌شدن درخواست در query مانع افشای تعداد/صفحات نامجاز می‌شود.

**Alternatives considered:** انتشار خودکار repository/Entity و DTO mapper framework نیاز نیست؛ Postman collection هنوز مخاطب اجرایی مستقلی ندارد، بنابراین قرارداد موازی تولید نمی‌شود و workspace منتشر نمی‌شود.

منبع: [Spring RFC 9457 support](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html).

## نتیجه

ابهام فنی باز باقی نمانده است. A-001 تا A-006 تصمیم قطعی محصول نیستند و قابل بازبینی‌اند. این پژوهش صحت طراحی را مستند می‌کند؛ هیچ dependency resolve، build یا آزمون runtime در این مرحله اجرا نشده است.

بررسی ابزار: CLI نصب‌شدهٔ Postman، فرمان `postman status` را با «unknown command» رد کرد. هیچ workspace ساخته یا منتشر نشد؛ OpenAPI با parser محلی YAML و بررسی تمام ارجاع‌های داخلی اعتبارسنجی شد. این بررسی جای اجرای API یا lint کامل استاندارد OpenAPI را نمی‌گیرد.
