# US2 checkpoint — T027 تا T034

تاریخ تکمیل: **2026-11-15**. محدودهٔ نهاییِ صریح کاربر فقط T027–T034 بود؛ این Taskها با شاهد تست سبز `[X]` شدند. کار در همین checkpoint متوقف است. **T035/T036، T044 و T047 اجرا نشده‌اند**؛ هیچ Task فاز بعدی تکمیل اعلام نمی‌شود. RequestChanges فقط در domain تست و پیاده شد؛ endpoint، مشاهدهٔ توضیح و تاریخچهٔ عمومی متعلق به US3 هستند.

## اجرای واقعی Build

فرمان از ریشه، بدون skip یا انتخاب تست:

```bash
MAVEN_USER_HOME=/tmp/changeflow-maven-user MAVEN_OPTS=-Dmaven.repo.local=/tmp/changeflow-m2 ./mvnw verify
```

**Exit code: 0 — BUILD SUCCESS**. مدت **02:11 min**؛ پایان **2026-11-15T14:10:26+03:30**.

| Runner | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Surefire | 64 | 0 | 0 | 0 |
| Failsafe | 37 | 0 | 0 | 0 |
| مجموع | 101 | 0 | 0 | 0 |

خروجی واقعی منتخب:

```text
[INFO] Tests run: 64, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  02:11 min
[INFO] Finished at: 2026-11-15T14:10:26+03:30
```

[خروجی کامل و بدون ویرایش verify](us2-verify.txt)، [شواهد واقعی قرمز/سبز](us2-tdd.md). XMLهای همین اجرا در `target/surefire-reports` و `target/failsafe-reports` و JAR در `target/change-flow-0.0.1-SNAPSHOT.jar` هستند. ۳۳ تست جدید US2 و ۶۸ تست قبلی همگی سبزند. PostgreSQL 17.12 واقعی با Testcontainers، HTTP واقعی، Basic و CSRF واقعی استفاده شدند؛ هیچ DB mock یا H2 جایگزین نشد.

YAML بسته‌بندی‌شده byte-for-byte با source یکسان است. کلاس‌های AuditAtomicityIT، FaultConfiguration، AuditProbe و سایر fixtureهای US2 در JAR نیستند. جست‌وجوی متن خصوصی تزریق‌شده به Audit، marker privacy و Authorization خام در لاگ نهایی صفر نتیجه داشت. پس از verify فقط اسناد و checkboxها نهایی شدند؛ کد تغییر نکرد.

## پیش‌نیازها و ترتیب

- اسناد مرجع Constitution، Spec، Plan، Tasks، semantics، Data Model، OpenAPI، Assumptions و ADRها مطالعه شدند؛ research و quickstart نیز مرور شدند.
- prerequisite script با `--json --require-tasks --include-tasks` exit 0؛ FEATURE_DIR همین feature و AVAILABLE_DOCS شامل research/data-model/contracts/quickstart/tasks بود.
- requirements checklist: **16 total / 16 checked / 0 unchecked — PASS**؛ markerهای checklist تغییر نکردند.
- ignoreهای Java/Docker/secret موجود و کافی بودند. Git در شروع تمیز و مبنای کار commit `bb18c66` بود.
- ترتیب وابستگی رعایت شد: T027/T028 پیش از قواعد و endpoint؛ T029 سپس T030؛ شکست باقی‌ماندهٔ receipt قبل از T031؛ T032 پس از تأیید پایه؛ بررسی مرز تراکنش T033؛ گزارش T034.
- `.specify/extensions.yml` وجود ندارد؛ هیچ before/after implement hook ثبت نشده است. `.specify/` و `.agents/` تغییر نکردند.

## ردیابی Task و شاهد

| Task | شاهد و نتیجه |
| --- | --- |
| T027 | TransitionRulesTest: ۲۴ invocation، شامل ماتریس ۴ وضعیت × ۳ command، فقط چهار گذار مجاز، same-state نامجاز، نقش لازم، هویت معتبر، مالک Submit و منع self برای هر دو تصمیم؛ ابتدا ۲۴ assertion failure و صفر error، سپس سبز |
| T028 | ApprovalIT: پنج تست HTTP برای تأیید شخص دیگر، self دو‌نقشه، edit Approved، نقش/visibility/version، قرارداد JSON/CSRF، Audit و repeat؛ پیش از endpoint پنج شکست رفتاری. RepeatedSubmitIT: یک regression مستقل؛ رفتار آن از US1 موجود بود و از ابتدا سبز ماند |
| T029 | TransitionPolicy خالص و DomainFailure؛ مجوز جاری به شکل مقدار ورودی از application، مقایسهٔ actor/requester، state مجاز، validation متن Submit و توضیح Unicode nonblank در RequestChanges؛ ۲۴ تست policy و ۲۵ تست Draft قبلی سبز |
| T030 | ReviewRequestService با CurrentActor، executor مشترک و transaction READ_COMMITTED؛ controller فقط DTO/service؛ بررسی fresh role اولیه و guard نهایی، CAS و Audit؛ چهار تست پایهٔ ApprovalIT سبز و repeat تا T031 قرمز |
| T031 | RepeatedTransitionPolicy و ApprovalHistory port؛ StatusHistoryRepository فقط query boolean برای actor/request/resultingVersion و زوج PendingReview→Approved؛ تکرار عامل موفق 409 عمومی، دیگر Reviewerها 404، GET عامل موفق نیز 404؛ Submit مالک در مقصد پیش از stale version رد؛ تمام تست‌های T028 سبز |
| T032 | AuditAtomicityIT: سه تست DB/HTTP واقعی با Clock ثابت، موفقیت دقیق، شکست insert پس از flush وضعیت و شکست deferred هنگام commit؛ rollback تمام ستون‌های درخواست و Audit؛ همه سبز |
| T033 | مرز transaction مشترک، propagation MANDATORY ذخیره/Audit و پاسخ پس از commit در T030 برقرار بود؛ تست‌های T032 ثابت کردند اصلاح دیگری لازم نیست؛ شکست persistence به موفقیت تبدیل نشد |
| T034 | این checkpoint، لاگ واقعی، شواهد Test-First، فهرست فایل‌ها و مستندات سازگار |

ArchitectureTest نیز استقلال domain از Spring/JPA، جهت وابستگی‌ها، عدم انتشار Entity و عدم فراخوانی domain/persistence توسط Controller را سبز بررسی کرد.

## رفتار قابل مشاهدهٔ این فاز

`POST /api/v1/change-requests/{id}/approve` با expectedVersion و explanation اختیاری، فقط برای Reviewer جاریِ غیرثبت‌کننده روی PendingReview مجاز است. نسخهٔ قدیمی در وضعیت مجاز 409 CONCURRENT_MODIFICATION می‌دهد. هویت از CurrentActor/Security Context است؛ actorId/status اضافی در JSON برابر 400 است. نقش کش‌شده مبنا نیست: CurrentAccess ابتدا با JDBC تازه خوانده می‌شود و پیش از write نیز guard تازهٔ لازم تا commit نگه داشته می‌شود.

عامل و مجوز، سپس وضعیت، نسخه و اعتبار محتوا بررسی می‌شوند. policy برای Approve و RequestChanges هویت ثبت‌کننده را مستقل از تعداد نقش‌های عامل مقایسه می‌کند؛ self برابر 403 SELF_REVIEW_FORBIDDEN است. تست HTTP کاربر دو‌نقشه در T028 و تست pure policy برای هر دو تصمیم در T027 قرار دارند. Approved با تست ویرایش US1 و تست صریح T028 ثابت باقی می‌ماند؛ حذف Approved تا T050/T054 ادعا نمی‌شود.

پاسخ موفق Approve DTO مستقل با نسخهٔ جدید، صرفاً رسید همان عملیات است. پس از Approved، Reviewer حق GET مستقل ندارد. تکرار همان Reviewer موفق با نقش جاری، برای نسخهٔ قدیمی و جدید 409 INVALID_TRANSITION عمومی و بدون DTO/محتوا/جزئیات تاریخچه می‌گیرد؛ برای Reviewer دیگر 404 باقی است. درخواست و تمام رکوردهای Audit پس از تکرار دقیقاً برابر snapshot قبل هستند. تشخیص receipt هیچ endpoint یا مجوز خواندن تاریخچه ایجاد نمی‌کند.

تست لغو نقش حین عملیات در T036 است و بنا بر محدودهٔ تأییدشده اضافه نشد؛ T030 اجرای guard تازه را برقرار می‌کند، اما شاهد رقابت لغو یا دو Reviewer هم‌زمان در این فاز ادعا نمی‌شود. آزمون‌های T035/T036 بازند.

## اتمیک‌بودن و زمان پاسخ

در AuditAtomicityIT یک decorator فقط در test source set، رکورد درخواست را در همان transaction و درست پیش از درج Audit می‌خواند؛ Approved و نسخهٔ افزوده‌شده را مشاهده می‌کند. CHECK constraint واقعی درج Audit را رد می‌کند؛ پاسخ 500 عمومی، تمام ستون‌های درخواست برابر snapshot قبل و Audit صفر است.

تست دوم با constraint trigger `DEFERRABLE INITIALLY DEFERRED` در PostgreSQL، شکست را تا commit عقب می‌اندازد؛ با اینکه update و insert قبلاً flush شده‌اند، پاسخ موفق صادر نمی‌شود و هر دو rollback می‌شوند. function/trigger/constraint آزمایشی در finally حذف می‌شوند. هیچ fault endpoint عمومی اضافه نشد.

در موفقیت، دقیقاً یک رکورد با id/requestId/actorId، UTC ثابت سرور، PendingReview→Approved، توضیح و resultingVersion صحیح commit می‌شود. نبود توضیح، مقدار خالی دارد. این شواهد سهم US2 در SC-001/SC-002/SC-004 هستند؛ تکمیل محصول یا پذیرش انسانی SC-006 ادعا نمی‌شود.

## منابع رسمی API و نسخه

Java 21، Spring Boot **4.0.9** و PostgreSQL **17.12** مطابق Plan باقی ماندند؛ dependency، Starter یا Migration جدیدی لازم نشد. مستندات رسمی پیش از استفاده بررسی شدند:

- [Boot 4.0.9 Servlet/MVC](https://docs.spring.io/spring-boot/4.0/reference/web/servlet.html): RestController و mapping/JSON در stack موجود.
- [Boot 4 JSON](https://docs.spring.io/spring-boot/4.0/reference/features/json.html): Jackson 3 پیش‌فرض؛ TransitionInput همان DTO سخت‌گیرانهٔ قبلی Submit است که برای Submit/Approve مشترک شد.
- [Boot 4.0.9 TestConfiguration](https://docs.spring.io/spring-boot/4.0/api/java/org/springframework/boot/test/context/TestConfiguration.html): Clock و decorator فقط در تست، با proxyBeanMethods=false.
- [Spring transaction annotations](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html) و [propagation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html): proxy سرویس و transaction مشترک، بدون self-invocation برای مرز transaction یا REQUIRES_NEW برای Audit.

## انحراف، تفاوت با baseline و محدودیت

**انحراف محصول، نسخه یا API از Plan: ندارد.** هیچ تصمیم محصولی تازه‌ای گرفته نشد؛ Spec، Plan، contracts، ADRها و Assumptions تغییر نکردند. port کوچک ApprovalHistory برای حفظ جهت وابستگی application→port به query محدود T031 اضافه شد؛ data reader عمومی یا CRUD تاریخچه ساخته نشد.

**تفاوت شواهد با پیش‌بینی T028:** RepeatedSubmitIT از ابتدا سبز بود، چون US1 رفتار آن را پیاده کرده بود. رفتار درست برای قرمزکردن مصنوعی حذف نشد. suite T028 پیش از پیاده‌سازی با پنج شکست ApprovalIT قرمز بود؛ شاهد compile یا NPE استفاده نشد. تست‌های اتمیک‌بودن T032 نیز از اولین اجرا سبز بودند؛ T033 اصلاح غیرضروری نداشت. یک assertion تست دربارهٔ instance URL مطابق semantics اصلاح شد؛ جزئیات در [گزارش TDD](us2-tdd.md) آمده است.

README و AGENTS با packageها و endpoint واقعی و توقف US2 تطبیق داده شدند. dev bootstrap در T058 آماده نیست و آماده معرفی نشده است. T035 تا پایان Tasks بدون تغییر باز مانده‌اند؛ US3/US5 یا محصول کامل Done نیست. هیچ commit/deploy انجام نشد.

## فایل‌های ساخته‌شده و ویرایش‌شده

### ساخته‌شده

- `specs/001-change-request-management/validation/us2-tdd.md`
- `specs/001-change-request-management/validation/us2-verify.txt`
- `specs/001-change-request-management/validation/us2.md`
- `src/main/java/com/changeflow/changerequest/api/ReviewRequestController.java`
- `src/main/java/com/changeflow/changerequest/application/ApprovalHistory.java`
- `src/main/java/com/changeflow/changerequest/application/RepeatedTransitionPolicy.java`
- `src/main/java/com/changeflow/changerequest/application/ReviewRequestService.java`
- `src/main/java/com/changeflow/changerequest/domain/DomainFailure.java`
- `src/main/java/com/changeflow/changerequest/domain/TransitionPolicy.java`
- `src/main/java/com/changeflow/changerequest/infrastructure/StatusHistoryRepository.java`
- `src/test/java/com/changeflow/changerequest/api/ApprovalIT.java`
- `src/test/java/com/changeflow/changerequest/api/RepeatedSubmitIT.java`
- `src/test/java/com/changeflow/changerequest/domain/TransitionRulesTest.java`
- `src/test/java/com/changeflow/changerequest/infrastructure/AuditAtomicityIT.java`

### ویرایش‌شده

- `AGENTS.md`
- `README.md`
- `specs/001-change-request-management/tasks.md`
- `src/main/java/com/changeflow/changerequest/api/ChangeRequestDtos.java`
- `src/main/java/com/changeflow/changerequest/api/DraftRequestController.java`
- `src/main/java/com/changeflow/changerequest/application/RequestMutationExecutor.java`
- `src/main/java/com/changeflow/changerequest/domain/ChangeRequest.java`
- `src/main/java/com/changeflow/web/ApiExceptionHandler.java`

