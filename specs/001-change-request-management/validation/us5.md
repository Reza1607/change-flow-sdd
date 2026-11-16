# US5 — checkpoint تکمیل پس از T056/T057

**US5 پس از موفقیت T046 و T056 و verify کامل تکمیل شد.** این اعلام مربوط به US5 است؛ completion کل پروژه یا پذیرش انسانی SC-006 نیست.

در فاز ۷، T056 شامل Edit/Delete، Submit/Delete و Delete/Delete، هرکدام ۲۰ بار با PostgreSQL واقعی و دو connection/EntityManager مستقل و barrier بدون sleep اجرا شد. هر بار یک پذیرش و یک 409 CONCURRENT_MODIFICATION، بدون retry/reload و بدون Audit یتیم. برندهٔ Delete ردیف را با Audit صفر حذف می‌کند؛ برندهٔ Edit داده/نسخه را بدون Audit و برندهٔ Submit نسخه/وضعیت و تنها Audit خود را حفظ می‌کند. actor دو عملیات همان مالک مجاز است؛ تعداد Audit به command برنده وابسته است و عملیات بازنده اثر تازه ندارد. adapter نسخه‌دار موجود نیاز به تغییر نداشت.

در verify نهایی همهٔ ۹ گروه race هرکدام ۲۰ بار (۱۸۰ رقابت) و ۱۴ گروه ترتیب guard/لغو هرکدام ۲۰ بار (۲۸۰ سناریو) سبزند. T035–T041 و T046 نیز همراه T056 دوباره در Full Suite اجرا شدند. List/History/Delete در دو ترتیب commit لغو نقش به تست‌های قبلی افزوده شدند؛ نقش دیگر کاربر dual حفظ می‌شود. موارد 200 با List خالی و 404 والد نامرئی مطابق A-001/A-006 هستند و مجوز از مالکیت یا cache گرفته نمی‌شود.

[checkpoint US4، فرمان‌ها، فایل‌ها و محدودیت‌ها](us4.md)، [خروجی واقعی کامل verify](us4-verify.txt) و [شواهد TDD](us4-tdd.txt). Exit code اجرای کامل **0**؛ خروجی واقعی منتخب:

```text
[INFO] Tests run: 64, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 89, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  07:57 min
[INFO] Finished at: 2026-11-15T18:10:55+03:30
```

گزارش‌های زیر تاریخی‌اند؛ عبارت‌های «تا تکمیل T056 Done نیست» وضعیت checkpointهای T049/T041 را توصیف می‌کنند و با نتیجهٔ نهایی بالا جایگزین شده‌اند. T058 به بعد اجرا نشده‌اند.

---

## گزارش تاریخی US5 تا checkpoint US3، T049

**US5 تا تکمیل T056 Done نیست**.

به‌روزرسانی 2026-11-15: T046 تکمیل شد. mixed Approve/RequestChanges و double RequestChanges هرکدام ۲۰ بار با PostgreSQL واقعی، دو connection/EntityManager مستقل و barrier بدون sleep اجرا شدند؛ هر بار یک موفقیت، یک 409 CONCURRENT_MODIFICATION، نسخهٔ صحیح و دقیقاً یک Audit با صفر Audit بازنده. تست HTTP تکرار RequestChanges با نسخهٔ قدیمی/جاری نیز تکمیل شد: Reviewer قبلی و Reviewer دیگرِ فاقد دید 404، مالک دو‌نقشه 403، بدون اثر و بدون Audit تازه. استثنای رسید تکرار Approve تعمیم داده نشد.

کنترل منفی CAS برای هر دو race قرمز واقعی با دو پذیرش به‌جای یکی ایجاد کرد؛ محافظ برگردانده شد. کنترل منفی nonblank RequestChanges نیز قرمز شد و برگردانده شد. نتیجهٔ کامل، exit code، فهرست فایل‌ها و تفاوت‌های اجرا در [checkpoint US3](us3.md)، [خروجی کامل verify](us3-verify.txt) و [شواهد TDD](us3-tdd.txt) ثبت شده‌اند. شواهد این checkpoint جایگزین ادعای ناقص T046 در زمان T041 می‌شوند؛ T056 و فازهای بعدی اجرا نشده‌اند.

بخش زیر **گزارش تاریخی checkpoint T041** است؛ عبارت‌های «هنوز اجرا نشده» در آن، وضعیت همان checkpoint قبلی را توصیف می‌کنند.

---

## گزارش تاریخی T041 — هستهٔ فاز ۵، T035 تا T041

تاریخ: **2026-11-15**. فقط T035–T041 طبق محدودهٔ تأییدشده اجرا و پس از تست سبز `[X]` شدند. در این checkpoint توقف شد؛ **T042 به بعد، T046 و T056 اجرا نشده‌اند**. endpoint، DTO یا use case جدید RequestChanges ساخته نشد.

**US5 تا تکمیل T046 و T056 Done نیست**.

## نتیجهٔ واقعی verify

پس از `./mvnw clean` موفق با exit 0، فرمان کامل از ریشه و بدون skip یا انتخاب تست اجرا شد:

```bash
MAVEN_USER_HOME=/tmp/changeflow-maven-user MAVEN_OPTS=-Dmaven.repo.local=/tmp/changeflow-m2 ./mvnw verify
```

**Exit code: 0 — BUILD SUCCESS**. مدت **04:48 min**؛ پایان **2026-11-15T15:35:52+03:30**.

| Runner | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Surefire | 64 | 0 | 0 | 0 |
| Failsafe | 56 | 0 | 0 | 0 |
| مجموع | 120 | 0 | 0 | 0 |

خروجی واقعی منتخب:

```text
[INFO] Tests run: 64, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 56, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  04:48 min
[INFO] Finished at: 2026-11-15T15:35:52+03:30
```

[خروجی کامل و بدون ویرایش](us5-verify.txt)، [شواهد baseline، قرمز و کنترل منفی](us5-tdd.md). XMLهای همین اجرا در `target/surefire-reports` و `target/failsafe-reports` هستند. ۱۹ invocation جدید integration در کنار ۱۰۱ تست قبلی سبز شد؛ ۲۴۰ تکرار داخلی سناریوهای رقابت/لغو، جدا از شمار invocationهای JUnit است و به‌عنوان ۲۴۰ تست JUnit گزارش نمی‌شود.

PostgreSQL **17.12 واقعی**، EntityManagerها و connectionهای مستقل، HTTP روی random port، Basic و CSRF واقعی استفاده شدند؛ DB یا repository یا پاسخ تصمیم mock نشده است. artifact در `target/change-flow-0.0.1-SNAPSHOT.jar` با تمام فایل‌های `target/classes` byte-for-byte تطابق دارد. fixtureهای ConcurrencyTestConfiguration/IdentityFaultConfiguration و تست‌ها و SQL قطع connection در JAR نیستند. marker خصوصی fault، Authorization خام و diagnostic موقت در خروجی نهایی یافت نشدند. پس از verify فقط اسناد/Taskها نهایی شدند؛ کد تغییر نکرد.

## پیش‌نیاز، ترتیب و Scope

- مبنا commit `890f8b9` و Git در شروع تمیز بود؛ US1 و US2 پیش‌نیازهای موجود بودند.
- Constitution، Spec، Plan، Tasks، semantics، Data Model، OpenAPI، Assumptions و ADRها به ترتیب مرجع مطالعه و research/quickstart مرور شدند.
- prerequisite script با `--json --require-tasks --include-tasks`: exit 0 و FEATURE_DIR همین feature؛ requirements checklist برابر **16 checked / 0 unchecked — PASS** و دست‌نخورده ماند.
- ignoreهای Java/Docker/secret کافی بودند. `.specify/extensions.yml` وجود ندارد؛ before/after implement hook ثبت نشده است. `.specify/` و `.agents/` تغییر نکردند.
- ابتدا T035/T036 نوشته و روی محافظ‌های موجود اجرا شدند؛ سپس بررسی T037/T038 و کنترل منفی/بازگرداندن؛ T039/T040 و اصلاح نگاشت root؛ verify کامل و T041. هیچ Task آینده تیک نخورد.

## T035/T037 — رقابت درخواست و CAS

| سناریو | تکرار موفق در اجرای نهایی | نتیجهٔ هر تکرار |
| --- | ---: | --- |
| edit/edit با محتوای متفاوت | 20 | یک 200، یک 409 CONCURRENT_MODIFICATION؛ payload برنده و version=1، Audit=0 |
| edit/Submit | 20 | یک 200، یک 409 CONCURRENT_MODIFICATION؛ محتوای edit یا گذار Submit مطابق برنده، Audit فقط اگر Submit برنده باشد |
| Approve/Approve دو Reviewer | 20 | یک 200، یک 409 CONCURRENT_MODIFICATION؛ Approved/version=1، دقیقاً یک Audit عامل برنده و صفر Audit عامل بازنده |
| edit هم‌مقدار/edit هم‌مقدار | 20 | یک 200، یک 409 CONCURRENT_MODIFICATION؛ محتوا ثابت، version=1، Audit=0 |

در هر تکرار، decorator تست پس از `find` واقعی هر دو درخواست را روی CyclicBarrier متوقف می‌کند؛ هر دو snapshot پیش از write خوانده شده‌اند. `pg_backend_pid`های متفاوت و دو delegate واقعی EntityManager بررسی می‌شوند؛ خواندن پورت دقیقاً دو بار است، بدون reload/retry. هیچ outer test transaction یا sleep رقابت را پنهان نمی‌کند.

snapshot نهایی DB با DTO برنده، محتوا، requesterId، status و version تطبیق داده می‌شود. تعداد و عامل/مبدأ/مقصد/resultingVersion تاریخچه نیز کنترل می‌شوند. در edit/Submit با عامل مشترک، تعداد Audit به نتیجهٔ command برنده مقید است؛ edit گذار/Audit ندارد. تست مستقل نسخهٔ قدیمی HTTP برای edit و Approve، پاسخ 409 و برابری تمام ستون‌های DB و Audit صفر را ثابت می‌کند.

@Version، CAS و OPTIMISTIC_FORCE_INCREMENT و نگاشت optimistic failure قبلاً درست بودند؛ T037 به بازنویسی یا retry تازه نیاز نداشت. فایل JpaChangeRequestStore نهایی دقیقاً با commit مبنا یکسان است. endpoint و DTO موجود تغییر نکردند.

## T036/T038 — دو ترتیب لغو نقش

برای هر یک از **edit، Submit، Approve و GET**، هر ترتیب زیر **۲۰ بار** اجرا شد: ۴ × ۲ × ۲۰ = **۱۶۰** سناریو.

| ترتیب | شاهد و نتیجه |
| --- | --- |
| لغو commit شد، سپس ادامه تا guard | عملیات پس از شروع معتبر در latch پیش از guard متوقف می‌شود؛ حساب قدیمی در first-level cache JPA عمداً managed است. writer واقعی با connection/EntityManager مستقل نقش را با accessVersion=0 لغو و commit می‌کند. mutationها 403، تمام ستون‌های درخواست ثابت و Audit صفر؛ GET روی Draft پس از لغو Requester و باقی‌ماندن Reviewer طبق visibility، 404 است. |
| guard زودتر گرفته شد | عملیات پس از guard اشتراکی متوقف می‌شود؛ writer انحصاری واقعی شروع و با pg_stat_activity و pg_blocking_pids ثابت می‌شود دقیقاً پشت backend همان عملیات منتظر قفل است. پس از آزادکردن barrier، عملیات پذیرفته‌شده commit می‌شود و writer پیش از commit لغو، وضعیت/نسخهٔ commitشدهٔ آن را می‌بیند. سپس لغو مؤثر و عملیات بعدی رد می‌شود. |

این دو connection/EntityManager مستقل از یک transaction مشترک تست ساخته نشده‌اند؛ تراکنش‌های واقعی سرویس و writer هستند. writer مشاهده‌گر تست، همان SQL production را با `super.change` و propagation/isolation واقعی اجرا می‌کند. barrierها فقط در test source set قرار دارند؛ endpoint مدیریت نقش یا hook تولیدی اضافه نشد.

در همهٔ تکرارها، کاربر دو‌نقشه است: لغو Requester، Reviewer را نگه می‌دارد و Approve درخواست شخص دیگر همچنان موفق است؛ لغو Reviewer، Requester را نگه می‌دارد و ایجاد Draft خود همچنان موفق است. accessVersion لغو دقیقاً 1 است. عملیات بعدیِ نیازمند نقش لغوشده با 403 یا 404 مطابق visibility رد و snapshot/Audit ثابت می‌ماند؛ مالکیت جانشین نقش نیست. در عملیات پذیرفته‌شدهٔ guard-first، محتوا/نسخه/مالک و تعداد/عامل تاریخچه بررسی می‌شوند. GET mutation/Audit ندارد.

GET=404 در حالت بالا تغییر قرارداد نیست: پس از لغو Requester و باقی‌ماندن Reviewer، Draft خود نیز از predicate مشاهده خارج می‌شود. انتظار 403 مربوط به mutation جاری است که مجوز اولیه را گذرانده ولی guard نهایی‌اش نقش را رد می‌کند. پاسخ شبکهٔ عملیات پیش‌تر پذیرفته‌شده پس گرفته نمی‌شود؛ مرز commit همان A-006/ADR-002 است.

guard اشتراکی، fresh JDBC read بدون cache، read guard و writer انحصاری نسخه‌دار از قبل برقرار بودند؛ T038 به تغییر SQL یا ترتیب قفل نیاز نداشت. DatabaseCurrentAccess و AccessProvisioner نهایی دقیقاً با مبنا یکسان‌اند.

## کنترل‌های منفیِ درخواستی و تفاوت Test-First

**تفاوت با ترتیب قرمز اولیهٔ درخواستی:** T035/T036 روی baseline از ابتدا سبز بودند؛ محافظ‌های لازم در فازهای قبلی پیاده شده بودند. این اجرا به‌عنوان قرمز معرفی نشد و محافظ درست برای ادعای وجود باگ حذف نشد. پس از baseline سبز، هر محافظ یک بار به‌طور صریح و موقت شکسته شد، تست قرمز رفتاری مشاهده و فایل اصلی بلافاصله برگردانده شد:

| کنترل منفی | exit | شاهد واقعی |
| --- | ---: | --- |
| حذف رزرو نسخهٔ equal edit | 1 | `exactly one accepted mutation ==> expected: <1> but was: <2>`؛ دو پاسخ موفق واقعی، صفر compile/runtime error |
| guard با flags نقش قدیمی/همیشه مجاز | 1 | سه mutation با `expected: <403> but was: <200>` و GET با `expected: <404> but was: <200>`؛ چهار assertion failure، صفر error |

این کنترل‌ها تست حساسیت‌اند، نه ادعای شکست نسخهٔ سالم. تست‌های mutant در نخستین تکرارِ خراب شکست می‌خورند؛ شمار ۲۰ متعلق به سناریوهای سالم baseline و verify نهایی است. SHA-256 و خروجی دقیق در [گزارش TDD](us5-tdd.md) ثبت شده‌اند. بازگرداندن هر دو فایل با مقایسهٔ byte-for-byte با مبنا و verify نهایی سبز اثبات شد. هیچ mutant، diagnostic، sleep یا retry تصمیم در کد نهایی نیست.

## T039 — تکرار و بخش عقب‌افتاده

IdempotencyIT سه تست end-to-end دارد:

- Submit مالک در مقصد با نسخهٔ قدیمی/جاری → 409 INVALID_TRANSITION بدون DTO یا Audit تازه؛ عامل دیگر مطابق visibility/نقش 404/403 و عامل با نقش لغوشده 403.
- Approve همان عامل موفق با نقش جاری و نسخهٔ قدیمی/جاری → 409 عمومی، GET مستقل 404؛ Reviewer دیگر 404؛ پس از لغو نقش عامل قبلی → 403 بدون رسید ویژه یا اثر تازه.
- fixture موجود ChangesRequested با یک رکورد RequestChanges مستقیم در DB، سپس Submit موجود و تکرار آن؛ رکورد قبلی بدون تغییر حفظ و فقط یک Audit Submit افزوده می‌شود؛ تکرارها هیچ اثری ندارند.

**عقب‌افتاده تا T046:** HTTP تکرار RequestChanges و mixed/double RequestChanges race. fixture به معنی endpoint یا DTO جدید نیست؛ هیچ مسیر `/request-changes` در این فاز ساخته نشد. T046 و T056 باز باقی‌اند.

## T040 — منبع هویت/مجوز و root وب

از semantics: قطعی منبع هویت/ذخیره‌سازی **503 DEPENDENCY_UNAVAILABLE**؛ خطای غیرمنتظره **500 INTERNAL_ERROR**. این انتخاب حدسی نیست. AuthorizationFailureIT ابتدا دو شکست رفتاری واقعی نشان داد: قطع connection مجوز هنگام rollback به‌اشتباه 500 و fault غیرمنتظرهٔ هویت به‌اشتباه 503 بود.

تست‌ها connection واقعی PostgreSQL را با pg_terminate_backend پیش از guard یا پس از lookup واقعی authentication قطع می‌کنند؛ mutation/commit یا Audit رخ نمی‌دهد و درخواست بعد از بازیابی connection دوباره خواندنی است. fault غیرمنتظره پس از lookup واقعی نیز 500 عمومی می‌گیرد. هر سه تست نهایی سبزند؛ no-store، Problem Details، code و عدم افشای محتوا/secret/SQL/stacktrace بررسی می‌شوند. Security و API هر دو از mapper root مشترک استفاده می‌کنند.

اصلاح فنی تولیدی در root وب است: ProblemResponses علت‌های شناخته‌شدهٔ resource و SQLSTATE اتصال را بدون خواندن متن خطا طبقه‌بندی می‌کند؛ ApiExceptionHandler و SecurityConfiguration همان تصمیم را مصرف می‌کنند. در شکست rollback روی connection قطع‌شده، Hibernate علت اولیه را با SQLException بدون SQLSTATE جایگزین می‌کرد. TransactionConfiguration همان JpaTransactionManager را طوری تنظیم می‌کند که خطای rollback در TransactionSystemException استاندارد منتقل شود؛ interceptor خطای application اولیه را حفظ و mapper 503/500 درست را انتخاب می‌کند. Boot TransactionManagerCustomizers نیز اعمال می‌شوند. هیچ خطایی swallowed یا موفقیت مصنوعی نمی‌شود.

این تنظیم فنی پیش از اعمال به کاربر اعلام شد و در فایل‌های اصلی پیش‌بینی‌شدهٔ T040 به‌صورت صریح وجود نداشت؛ علت افزودن آن، نگه‌داشتن شاهد واقعی قطعی تا root بود. مرز تراکنش، datasource/EntityManagerFactory، isolation/propagation، الگوریتم CAS، ترتیب قفل‌ها و commit در ADR-002/003 تغییر نکردند. adapterهای production هویت دست‌نخورده و بدون import از API feature/web هستند. ArchitectureTest سبز است.

## منابع رسمی و نسخه

Java 21، Boot **4.0.9** و PostgreSQL **17.12**، dependencyها و Migration V1 تغییر نکردند. API/رفتار استفاده‌شده با منابع رسمی بررسی شد:

- [Boot 4 TestConfiguration](https://docs.spring.io/spring-boot/4.0/api/java/org/springframework/boot/test/context/TestConfiguration.html)، [Boot TransactionManagerCustomizers](https://docs.spring.io/spring-boot/4.0/api/java/org/springframework/boot/transaction/autoconfigure/TransactionManagerCustomizers.html).
- [Spring transaction annotations](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)، [JpaTransactionManager](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/orm/jpa/JpaTransactionManager.html) و [TransactionSystemException/getOriginalException](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/TransactionSystemException.html).
- [JPA optimistic force increment](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/LockModeType.html).
- [PostgreSQL 17 row locks](https://www.postgresql.org/docs/17/explicit-locking.html)، [backend termination](https://www.postgresql.org/docs/17/functions-admin.html) و [SQLSTATE codes](https://www.postgresql.org/docs/17/errcodes-appendix.html).

## انحراف و باقی‌مانده

- تصمیم محصولی، Spec، Plan، Assumptions و ADRها تغییر نکردند؛ هیچ تصمیم یا endpoint جدید برای RequestChanges ساخته نشد.
- تفاوت اجرای Test-First و baseline سبز T035/T036 و سپس mutantهای قرمز بالا صریح ثبت شده است؛ شکست اولیهٔ نسخهٔ سالم جعل نمی‌شود. T037/T038 محافظ موجود را با شواهد تازه تأیید کردند و بازنویسی غیرضروری نداشتند.
- تنظیم فنی اضافهٔ TransactionConfiguration برای حفظ علت rollback و نگاشت دقیق قرارداد T040 در بالا توضیح داده شد؛ انحراف از ترتیب پذیرش/لغو ADR-002 یا اتمیک‌بودن/CAS ADR-003 وجود ندارد.
- README/AGENTS با checkpoint، ساختار root و وضعیت باز US5 تطبیق یافتند؛ dev bootstrap T058 هنوز آماده نیست. T042 به بعد همگی بدون تغییر بازند. هیچ commit/deploy انجام نشد.

**US5 تا تکمیل T046 و T056 Done نیست**. SC-003 فقط برای چهار race موجود شاهد دارد؛ mixed review و delete race آینده هنوز اثبات نشده‌اند. پذیرش انسانی SC-006 یا تکمیل کل محصول ادعا نمی‌شود.

## فایل‌های ساخته‌شده و ویرایش‌شده

### ساخته‌شده

- `specs/001-change-request-management/validation/us5-tdd.md`
- `specs/001-change-request-management/validation/us5-verify.txt`
- `specs/001-change-request-management/validation/us5.md`
- `src/main/java/com/changeflow/web/TransactionConfiguration.java`
- `src/test/java/com/changeflow/changerequest/api/IdempotencyIT.java`
- `src/test/java/com/changeflow/changerequest/infrastructure/ConcurrentMutationIT.java`
- `src/test/java/com/changeflow/identity/AuthorizationFailureIT.java`
- `src/test/java/com/changeflow/identity/InFlightRevocationIT.java`
- `src/test/java/com/changeflow/identity/infrastructure/IdentityFaultConfiguration.java`
- `src/test/java/com/changeflow/support/ConcurrencyTestConfiguration.java`

### ویرایش‌شده

- `AGENTS.md`
- `README.md`
- `specs/001-change-request-management/tasks.md`
- `src/main/java/com/changeflow/web/ApiExceptionHandler.java`
- `src/main/java/com/changeflow/web/ProblemResponses.java`
- `src/main/java/com/changeflow/web/SecurityConfiguration.java`

