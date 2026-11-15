# US3 — checkpoint T042 تا T049

تاریخ: 2026-11-15. مبنا: `a543407`. محدوده فقط فاز ۶؛ توقف در این سند و [به‌روزرسانی US5](us5.md). T050 به بعد و T056 اجرا نشده‌اند. پذیرش انسانی SC-006 انجام یا جعل نشده است.

## قرارداد تأییدشده پیش از ادامه

انتخاب صریح کاربر: Spec، semantics.md و openapi.yaml بدون تغییر بمانند. Approve تکراری همان عامل موفق با نقش جاری، مطابق FR-020/AS-21 و semantics، رسید خطای 409 INVALID_TRANSITION بدون DTO دارد؛ GET همچنان 404 است. این استثنا برای RequestChanges تعریف نشده است. Reviewer قبلی و Reviewer دیگر پس از ChangesRequested فاقد visibility و مشمول 404 NOT_FOUND هستند؛ مالک دو‌نقشه به علت self-review، 403 SELF_REVIEW_FORBIDDEN می‌گیرد. شکست race پس از خواندن معتبر، 409 CONCURRENT_MODIFICATION است.

عبارت شرطی T042 «تکرار مجاز برای مشاهده‌کننده 409» visibility یا مجوز تازه ایجاد نمی‌کند. در ماتریس فعلی هیچ Reviewer مستقلِ قابل‌مشاهده پس از ChangesRequested وجود ندارد؛ مالکِ قابل‌مشاهده نیز پیش از کنترل وضعیت، self-review رد می‌شود. خودِ گذار تکراری در domain نامعتبر است و TransitionRulesTest موجود آن را پوشش می‌دهد. تفاوت رسید Approve با RequestChanges با FR-020/AS-21 و A-001 سازگار است؛ توضیح توقف قبلی Agent که آن را تناقض قرارداد نامیده بود، تصحیح شد. هیچ استثنای تازه، دسترسی تازه یا تغییر محصول اعمال نشد.

## ترتیب و Test-First

پیش‌نیازها و جدول وابستگی tasks.md مطالعه شدند. Constitution، Spec، Plan، Tasks، semantics، Data Model، OpenAPI، Assumptions و ADR-001/002/003 مرجع بودند؛ research/quickstart مرور شدند. prerequisite script با require-tasks/include-tasks موفق و checklist برابر 16 checked / 0 unchecked بود؛ checklist تغییر نکرد. فایل extensions.yml وجود ندارد؛ hook پیش/پس ثبت نشده است. فایل‌های .agents/ و .specify/ دست‌نخورده‌اند.

ترتیب: T042/T043 و HistoryVisibilityIT/HistoryIT قبل از Reader؛ سپس T044، بررسی T045، T046، T047 و T048، سپس T049. شواهد واقعی فرمان‌ها و assertionها در [us3-tdd.txt](us3-tdd.txt) است. همهٔ اجراها روی PostgreSQL 17.12 واقعی، HTTP/Security/CSRF واقعی هستند؛ شکست compile شاهد قرمز نبوده است.

| مرحله | exit | نتیجهٔ مشاهده‌شده |
| --- | ---: | --- |
| RequestChangesIT و ResubmissionIT اولیه | 1 | 4 assertion failure، صفر error؛ 422/403/200 مورد انتظار ولی endpoint غایب 404 |
| T042/T043 همراه HistoryIT و HistoryVisibilityIT پیش از کد | 1 | 9 failure، صفر error؛ مسیرهای تاریخچه با انتظار 200/400، پاسخ 404 |
| CorrectionSafetyIT پیش از endpoint | 1 | 2 failure واقعی 409/500 در برابر 404؛ یک guard latch timeout چون endpoint هنوز به guard نمی‌رسید؛ timeout به‌عنوان شاهد رفتاری محسوب نشده است |
| T044/T045، پیش از Reader | 1 | 13 invocation؛ 6 failure فقط تاریخچه، صفر error؛ RequestChangesIT و CorrectionSafetyIT سبز و edit/Submit در ResubmissionIT سبز؛ بخش history عمداً تا T047 قرمز باقی ماند |
| T046 | 0 | 6 invocation، صفر failure/error؛ IdempotencyIT و دو رقابت هرکدام 20 بار سبز |
| نخستین اجرای Reader | 1 | 13 invocation؛ 12 سبز و یک ایراد fixture در تست جدید no-leak، صفر error |

چهار تست اولیه در RequestChangesIT و ResubmissionIT تا پایان بدون تغییر ماندند. SHA-256 فایل‌ها:

```text
cfb0d352176e9de46b6385b7e8534c1d33441ce172ca37d8ef56e01caccb545d  RequestChangesIT.java
76439fa8abebb951a30fefe31e2acfa79a73b973c61b23dbaaf1f08e678a6893  ResubmissionIT.java
```

در بازبینی پایانی T048، فقط fixture تست جدید HistoryVisibilityIT تصحیح شد: ابتدا پاسخ دو UUID متفاوت کامل مقایسه می‌شد؛ Spring فیلد مجاز instance را با URL ورودی می‌سازد و آن دو URL متفاوت بودند. اکنون برای یک UUID/URL ثابت، پاسخ پیش از ایجاد والد با پاسخ پس از ایجاد والد نامرئی مقایسه می‌شود. assertion برابری کامل JSON، 404، no-store و نبود دادهٔ خصوصی حفظ شدند؛ انتظار ضعیف، skip یا تغییر mapper تولیدی برای عبور تست اضافه نشد. این اصلاح تست صریح به کاربر اعلام شد.

## رفتار پیاده‌شده و شاهد

RequestChanges فقط از ReviewRequestService به RequestMutationExecutor مشترک متصل شد؛ Controller بدون business logic و DTO ورودی/خروجی موجود مستقل از Entity است. CurrentActor از Security Context می‌آید. مجوز اولیه/visibility، منع self، وضعیت، نسخه و سپس nonblank دامنه به ترتیب قرارداد سنجیده می‌شوند؛ guard نهایی مجوز جاری را در تراکنش بازخوانی می‌کند، CAS و Audit همان تراکنش‌اند و commit قبل از رسید موفق رخ می‌دهد. توضیح مفقود، خالی یا Unicode whitespace 422 و بدون اثر/Audit؛ هویت جعلی/ورودی نوع نامعتبر 400؛ نسخهٔ قدیمی در PendingReview برابر 409 است. تست fault واقعی قید insert Audit، rollback کامل وضعیت/نسخه را می‌سنجد. تست barrier، لغو commit‌شده قبل از guard RequestChanges را با 403 و صفر اثر ثابت می‌کند و نقش Requester باقی‌مانده را نیز می‌سنجد.

ویرایش nullable و Submit مجدد ChangesRequested از قبل در DraftRequestService/دامنه موجود بود. T045 با تست جدید تأیید شد؛ بازنویسی غیرضروری انجام نشد. مالکیت جای نقش جاری را نمی‌گیرد. تاریخچه پاک یا reset نمی‌شود، edit Audit گذار ندارد و Submit موفق دقیقاً یک Audit می‌افزاید. هیچ امکان حذف ChangesRequested ساخته نشد. تست ویرایش Approved و تمام تست‌های قبلی حفظ شده‌اند.

HistoryQueryService، Port داخلی HistoryReader، JpaHistoryReader و DTO/Controller جدا ساخته شدند. guard تازه خواندن تا تهیهٔ نتیجه نگه داشته می‌شود. query صفحه و count هر دو History را به والد با predicate `(requesterRole AND requesterId=actorId) OR (reviewerRole AND status=PendingReview)` join می‌کنند؛ والد ناموجود و نامرئی همان 404 دارند، از جمله برای تاریخچهٔ خالی. actorId، occurredAt UTC، fromStatus/toStatus، explanation و resultingVersion همراه id/requestId نمایش داده می‌شوند؛ username/hash منتشر نمی‌شود. ترتیب resultingVersion صعودی با فاصلهٔ نسخه ناشی از edit؛ صفحه از صفر، size پیش‌فرض 20 و سقف 100، صفحهٔ بعد انتها خالی و پارامتر نامعتبر 400 است. تست‌ها تاریخچهٔ Draft خالی، visibility بعد از خروج PendingReview، لغو Requester کاربر دو‌نقشه، بدون نقش/بدون هویت و no-leak را می‌سنجند. endpoint update/delete تاریخچه وجود ندارد؛ PUT/DELETE/POST همان مسیر 405 و DB ثابت است. append-only از adapter Audit موجود حفظ شده است.

## رقابت و کنترل‌های منفی

MixedReviewRaceIT شامل Approve/RequestChanges و RequestChanges/RequestChanges است، هرکدام 20 تکرار مستقل: مجموع 40 رقابت. دو Reviewer، دو pg_backend_pid متفاوت، دو delegate مستقل EntityManager و CyclicBarrier پس از خواندن واقعی استفاده می‌شوند؛ دقیقاً دو read، بدون reload/retry/sleep و بدون outer test transaction. هر بار دقیقاً یک HTTP 200 و یک 409 CONCURRENT_MODIFICATION، نسخهٔ نهایی 1، payload/owner ثابت، وضعیت مطابق برنده، دقیقاً یک Audit با عامل/توضیح/from/to/version صحیح و صفر Audit برای بازنده بررسی می‌شود. این 40 تکرار، دو invocation پارامتری JUnit هستند.

پس از baseline سبز، هر محافظ یک بار موقتاً شکسته و بلافاصله بازگردانده شد:

| محافظ موقتاً شکسته | exit | شکست واقعی |
| --- | ---: | --- |
| CAS adapter با UPDATE بدون expectedVersion جایگزین شد | 1 | هر دو race: `exactly one accepted decision ==> expected: <1> but was: <2>`؛ 2 failure، صفر error |
| nonblank RequestChanges در TransitionPolicy غیرفعال شد | 1 | HTTP: `expected: <422> but was: <500>`؛ 1 failure، صفر error؛ قید DB آخرین دفاع بود و جای validation دامنه را نمی‌گیرد |

JpaChangeRequestStore و TransitionPolicy پس از کنترل‌ها byte-for-byte با HEAD یکسان‌اند. clean و verify کامل پس از restore اجرا شدند؛ هیچ mutant، diagnostic، sleep یا retry در نتیجهٔ نهایی نیست. IdempotencyIT تکرار RequestChanges را با نسخهٔ قدیمی و جاری برای عامل قبلی/دیگری 404، مالک دو‌نقشه 403 و نقش لغوشده 403 می‌سنجد؛ snapshot و تمام Auditها برابر می‌مانند.

## نسخه، API رسمی و انحراف‌ها

Java 21، Boot 4.0.9، PostgreSQL 17.12، BOM، Starterها و Migration تغییر نکردند. منابع رسمی بررسی‌شده:

- [Boot 4.0.9 SpringBootTest](https://docs.spring.io/spring-boot/4.0/api/java/org/springframework/boot/test/context/SpringBootTest.html) و [Testcontainers/ServiceConnection](https://docs.spring.io/spring-boot/4.0/reference/testing/testcontainers.html).
- [Spring MVC RequestParam](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestparam.html) و [method validation، Min/Max و HandlerMethodValidationException](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html).
- [Jakarta Persistence 3.2، range join و pagination](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html#joins) و [Query API](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/Query.html).

انحراف رفتاری از Spec/Plan/ADR-002/003 وجود ندارد. جزئیات اجرایی قابل بازبینی: T045 کد قبلی را تأیید کرد؛ HistoryReader و HistoryDtos و HistoryApiSupport/CorrectionSafetyIT فایل‌های پشتیبان لازم علاوه بر نام‌های نمونهٔ Task هستند؛ fixture اشتباه تست تازه در انتهای T048 اصلاح شد و چهار تست قبلی دست‌نخورده‌اند. T046 روی محافظ موجود از ابتدا سبز بود و سپس کنترل منفی حساسیت آن را اثبات کرد؛ قرمز اولیهٔ جعلی برای محافظ سالم ادعا نمی‌شود. T042 به تفسیر شرطی بالا و انتخاب صریح کاربر اجرا شد؛ Taskها جز checkbox تغییر محتوایی ندارند. Spec، semantics، OpenAPI، Plan، Assumptions و ADRها دست‌نخورده‌اند.

## انحرافات و تفسیرها

بند «تکرار مجاز برای مشاهده‌کننده 409» در T042 در ماتریس فعلی (A-001 و FR-020) عاملِ مصداق ندارد: تکرار RequestChanges برای Reviewer قبلیِ فاقد دید **404 NOT_FOUND**، برای مالک دو‌نقشه **403 SELF_REVIEW_FORBIDDEN** و شکست رقابت همزمان **409 CONCURRENT_MODIFICATION** است. تکرار Approve توسط همان Reviewer موفق قبلی با نقش جاری، طبق [semantics.md، خط ۴۲](../contracts/semantics.md)، **409 INVALID_TRANSITION** است؛ این استثنای رسید تکرار به RequestChanges تعمیم داده نمی‌شود.

پس از ثبت checkpoint، فقط عبارت بی‌مصداق T042 با تأیید صریح کاربر روشن شد؛ محدوده، شماره و checkbox Task تغییر نکرد. این توضیح مستنداتی، رفتار یا تست تازه‌ای اضافه نمی‌کند و Spec، semantics و OpenAPI را تغییر نمی‌دهد.

## نتیجهٔ verify کامل

پس از clean موفق (exit 0)، دستور بدون انتخاب تست و بدون skip اجرا شد:

```bash
MAVEN_USER_HOME=/tmp/changeflow-maven-user MAVEN_OPTS=-Dmaven.repo.local=/tmp/changeflow-m2 ./mvnw verify
```

**Exit code: 0**. Surefire: 64، Failsafe: 72؛ مجموع **136** invocation، صفر failure/error/skipped.

```text
[INFO] Tests run: 64, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 72, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  06:05 min
[INFO] Finished at: 2026-11-15T17:24:36+03:30
```

[خروجی واقعی کامل و بدون ویرایش](us3-verify.txt). XMLها در target/surefire-reports و target/failsafe-reports هستند. ۴۰ تکرار جدید race در همین اجرای نهایی ثبت شده‌اند؛ ۸۰ race قبلی و ۱۶۰ ترتیب لغو نقش نیز در verify کامل باقی مانده‌اند. ArchitectureTest سبز است. JAR با تمام target/classes byte-for-byte تطبیق داده شد و fixtureهای test و mutant CAS در artifact نیستند. پس از verify فقط اسناد و checkboxها نهایی شدند؛ کد تولیدی/تست تغییر نکرد.


## فایل‌های ساخته‌شده و ویرایش‌شده

### ساخته‌شده

- `specs/001-change-request-management/validation/us3-tdd.txt`
- `specs/001-change-request-management/validation/us3-verify.txt`
- `specs/001-change-request-management/validation/us3.md`
- `src/main/java/com/changeflow/changerequest/api/HistoryController.java`
- `src/main/java/com/changeflow/changerequest/api/HistoryDtos.java`
- `src/main/java/com/changeflow/changerequest/application/HistoryQueryService.java`
- `src/main/java/com/changeflow/changerequest/application/HistoryReader.java`
- `src/main/java/com/changeflow/changerequest/infrastructure/JpaHistoryReader.java`
- `src/test/java/com/changeflow/changerequest/api/CorrectionSafetyIT.java`
- `src/test/java/com/changeflow/changerequest/api/HistoryIT.java`
- `src/test/java/com/changeflow/changerequest/api/HistoryVisibilityIT.java`
- `src/test/java/com/changeflow/changerequest/api/RequestChangesIT.java`
- `src/test/java/com/changeflow/changerequest/api/ResubmissionIT.java`
- `src/test/java/com/changeflow/changerequest/infrastructure/MixedReviewRaceIT.java`
- `src/test/java/com/changeflow/support/HistoryApiSupport.java`

### ویرایش‌شده

- `AGENTS.md`
- `README.md`
- `specs/001-change-request-management/tasks.md`
- `specs/001-change-request-management/validation/us5.md`
- `src/main/java/com/changeflow/changerequest/api/ReviewRequestController.java`
- `src/main/java/com/changeflow/changerequest/application/RequestMutationExecutor.java`
- `src/main/java/com/changeflow/changerequest/application/ReviewRequestService.java`
- `src/test/java/com/changeflow/changerequest/api/IdempotencyIT.java`

**US5 تا تکمیل T056 Done نیست**. T046 اکنون شاهد دارد؛ delete race، US4 و فازهای بعدی خارج این اجرا هستند. هیچ commit یا push انجام نشد.
