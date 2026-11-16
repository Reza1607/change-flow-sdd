# US4 — checkpoint فاز ۷، T050 تا T057

تاریخ: 2026-11-15. مبنا `3ef3a27`. فقط Phase 7 اجرا شد؛ توقف در T057. T058 به بعد، bootstrap محیط dev و پذیرش انسانی SC-006 اجرا نشده‌اند. اشارهٔ Spring Boot 3 در پرامپت با تأیید صریح کاربر به نسخهٔ موجود **Spring Boot 4.0.9** تصحیح شد؛ تغییر نسخه، dependency، schema، Spec یا ADR انجام نشد.

## پیش‌نیاز و ترتیب

Constitution، Spec (US4 و AS-14 تا AS-18 و FR-008/012–019)، Plan، Tasks، semantics، Data Model، OpenAPI، Assumptions و ADRها بررسی شدند. A-001 و A-006 بدون تغییر مرجع visibility و مرز commit لغو نقش‌اند. prerequisite script با require-tasks/include-tasks موفق بود؛ requirements checklist: 16 checked / 0 unchecked — PASS. checklist، .specify/ و .agents/ دست‌نخورده‌اند. extensions.yml وجود ندارد و hook پیش/پس ثبت نشده است. ignoreهای موجود برای Java، artifact و secret کافی بودند.

پیش از کد، مدل‌ها/Entityها، Port و repository، serviceها و DTO/Controller، Basic/CSRF/CurrentActor/CurrentAccess، Problem mapper و TransactionConfiguration و زیرساخت HTTP/PostgreSQL/Concurrency بررسی شدند. امکانات قبلی create/edit/Submit/Approve/RequestChanges/resubmission، Detail، History، guard مجوز، writer داخلی نقش، @Version و delete نسخه‌دار adapter حفظ شدند. List و use case/endpoint حذف هنوز وجود نداشتند.

ترتیب واقعی: T050 و T051 تست قرمز؛ T052/T053 و تست سبز List/Detail/History؛ T054 و تست سبز حذف و regression؛ T055 و تست دو ترتیب revocation؛ T056 و raceهای حذف همراه raceهای قبلی؛ سپس clean و verify کامل و T057. checkbox هر Task فقط پس از شاهد سبز مربوط ثبت شد؛ T050 تا سبزشدن حذف در T054 باز بود، T057 پس از verify کامل و این گزارش بسته شد.

## Test-First و فرمان‌های واقعی

همهٔ فرمان‌ها از ریشه با این تنظیم مسیر cache اجرا شدند؛ معنای lifecycle و مجموعهٔ تست‌ها تغییر نمی‌کند:

```bash
MAVEN_USER_HOME=/tmp/changeflow-maven-user MAVEN_OPTS=-Dmaven.repo.local=/tmp/changeflow-m2 ./mvnw verify -Dit.test=DeleteDraftIT,VisibilityMatrixIT,PaginationIT
MAVEN_USER_HOME=/tmp/changeflow-maven-user MAVEN_OPTS=-Dmaven.repo.local=/tmp/changeflow-m2 ./mvnw verify -Dit.test=VisibilityMatrixIT,PaginationIT,GetRequestIT,HistoryVisibilityIT
MAVEN_USER_HOME=/tmp/changeflow-maven-user MAVEN_OPTS=-Dmaven.repo.local=/tmp/changeflow-m2 ./mvnw verify -Dit.test=DeleteDraftIT,RequestStoreIT,ApprovalIT,DraftFlowIT
MAVEN_USER_HOME=/tmp/changeflow-maven-user MAVEN_OPTS=-Dmaven.repo.local=/tmp/changeflow-m2 ./mvnw verify -Dit.test=InFlightRevocationIT,VisibilityMatrixIT
MAVEN_USER_HOME=/tmp/changeflow-maven-user MAVEN_OPTS=-Dmaven.repo.local=/tmp/changeflow-m2 ./mvnw verify -Dit.test=DeleteRaceIT,DeleteDraftIT,ConcurrentMutationIT,MixedReviewRaceIT
```

| اجرا | exit | Surefire | Failsafe | نتیجه |
| --- | ---: | ---: | ---: | --- |
| T050/T051 پیش از پیاده‌سازی | 1 | 64 سبز | 8 | 8 assertion failure، صفر error؛ پاسخ واقعی 405 به‌جای 204/200/400/403/404/409 مورد انتظار، نه خطای compile |
| T052/T053 | 0 | 64 | 9 | صفر failure/error/skipped؛ ماتریس و pagination و regression Detail/History |
| T050/T054 | 0 | 64 | 19 | حذف، Persistence و regression Draft/Approve سبز |
| T055 | 0 | 64 | 15 | 14 invocation لغو نقش + ماتریس سبز؛ 280 تکرار داخلی دو ترتیب guard/لغو |
| T056 | 0 | 64 | 15 | سه race حذف و raceهای قبلی همراه حذف HTTP سبز؛ 180 رقابت داخلی |

[شواهد واقعی منتخب TDD و اجراها](us4-tdd.txt). همهٔ integrationها HTTP/Security/CSRF و PostgreSQL 17.12 واقعی دارند؛ DB، repository یا مجوز mock نشده است. تستی skip یا assertionی برای سبزکردن ضعیف نشد. محافظ موجود برای ساختن قرمز مصنوعی حذف نشد: T055 روی guard موجود و T056 روی adapter موجود از ابتدا سبز بودند؛ این‌ها تأیید رفتار موجودند و ادعای اصلاح باگ تازه در آن محافظ‌ها نمی‌شود.

## وضعیت Taskها و ردیابی

| Task | وضعیت و شاهد | ردیابی |
| --- | --- | --- |
| T050 | DeleteDraftIT: 5 تست HTTP، حذف مالک، نقش/مالکیت/visibility، نسخهٔ مفقود/نامعتبر/قدیمی، حذف مجدد، همهٔ غیرDraftها و Approved بدون اثر، no-leak و CSRF | FR-008/015/019، AS-08/16/17 |
| T051 | VisibilityMatrixIT و PaginationIT؛ دو مالک، چهار وضعیت، Requester/Reviewer/dual/بدون نقش، لغو/اعطای نقش، List/Detail/History و metadata | FR-012/013/014/017/018، AS-14/15/17/18 |
| T052 | predicate مشترک DB در JpaRequestReader، همان where برای count و items، فیلتر status محدودکننده، LIMIT/OFFSET و ORDER BY id | FR-012/013/018 |
| T053 | ListRequestsService و GET Collection با DTO مستقل، اعتبارسنجی query، قرارداد صفحه و عدم افشای Entity؛ Detail/History قبلی بدون تغییر رفتار | FR-012/013/017/018/019 |
| T054 | DeleteDraftService، قاعدهٔ requireDeletableBy در domain و Controller نازک؛ guard و DELETE نسخه‌دار adapter قبلی؛ بدون Audit | FR-008/014/015 |
| T055 | InFlightRevocationIT گسترش‌یافته؛ guard جاری List و محافظ‌های موجود Get/History/Delete، رفتار دو ترتیب commit، نقش دیگر و grant نقش | FR-012/013/014/017/019، A-001/A-006، AS-22 |
| T056 | DeleteRaceIT: Edit/Delete، Submit/Delete، Delete/Delete، هرکدام 20 بار؛ دو connection/EntityManager، conflict 409، هیچ Audit یتیم | FR-008/015/016، AS-20، SC-003 |
| T057 | این checkpoint و به‌روزرسانی US5 پس از verify کامل؛ فایل‌ها و محدودیت‌های واقعی ثبت شدند | SC-002/005، AS-14 تا AS-18 |

## Authorization و visibility نهایی

هویت فقط CurrentActor از Security Context است؛ Body/Query مالک یا نقش را تعیین نمی‌کند. مجوز از DB جاری است؛ snapshot authentication یا حساب managed قدیمی JPA مبنا نیست. predicate دقیق مشاهده:

```text
(requesterRole AND requesterId = actorId) OR (reviewerRole AND status = PendingReview)
```

Requester فقط درخواست‌های خودش در هر چهار وضعیت را می‌بیند؛ Reviewer فقط PendingReviewها، از جمله درخواست خودش را می‌بیند. dual اجتماع دو دسترسی است. لغو Requester مالک را از مشاهدهٔ Draft/ChangesRequested/Approved خود خارج می‌کند؛ مالکیت جانشین نقش نیست. لغو Reviewer در dual، دسترسی Requester به درخواست خود را حفظ می‌کند و دسترسی به PendingReview دیگران را حذف می‌کند. grant نقش در عملیات بعدی با همان predicate اثر می‌کند. بدون هیچ نقش جاری، عملیات 403 است. برای نقش‌دار، والد نامرئی/ناموجود در Detail/History همان 404 عمومی با no-store دارد؛ تست GET جزئیات no-leak قبلی و تست History حفظ و در regression اجرا شدند.

ماتریس برای دو مالک × چهار وضعیت، با Requester، Reviewer، dual و بدون نقش و سپس وضعیت‌های لغو/اعطای نقش اجرا می‌شود. همهٔ Detail/Historyها و پنج حالت status فهرست (بدون فیلتر و چهار وضعیت) بررسی می‌شوند؛ ID مجموعه و totalElements/totalPages باید دقیقاً از دادهٔ مجاز باشند. فیلتر status هرگز دسترسی ایجاد نمی‌کند. استثنای رسید تکرار Approve و 404 تکرار RequestChanges عوض نشدند.

## Pagination

page از صفر؛ size پیش‌فرض 20 و حداکثر 100؛ sort ثابت id صعودی در PostgreSQL. count و items یک عبارت where مشترک visibility/status دارند؛ count/limit در DB و پیش از نگاشت DTO است. هیچ فیلتر دسترسی یا بارگذاری همهٔ رکوردها در حافظه وجود ندارد. صفحهٔ بعد انتها 200 و items خالی با count واقعی است؛ page/size نامعتبر، overflow نوع و status نامعتبر 400 هستند. offset با long محاسبه می‌شود؛ صفحهٔ بسیار دور پس از count به‌صورت خالی برمی‌گردد.

PaginationIT با 25 رکورد مالک و 8 رکورد نامرئی، پیمایش سه صفحه و صفحهٔ خالی، نبود duplicate/جاافتادگی، ترتیب UUID در DB، defaults، size=100، status narrowing و page=2147483647 را کنترل می‌کند. مقایسهٔ UUIDها با رشتهٔ canonical مطابق ترتیب unsigned PostgreSQL است؛ UUID.compareTo جاوا برای این کار مبنا نیست. صفحهٔ مالک بدون داده نیز 200 با count/pages صفر است. تاریخچهٔ موجود همچنان resultingVersion صعودی و visibility join والد خود را حفظ می‌کند.

## حذف و نسخه

DELETE `/api/v1/change-requests/{id}?expectedVersion=...` فقط برای مالک دارای Requester جاری و وضعیت Draft پذیرفته می‌شود. application ابتدا مجوز/visibility، سپس مالکیت/وضعیت و نسخه را بررسی می‌کند. پیش از DELETE، guard shared نقش دوباره از DB می‌خواند و تا commit نگه داشته می‌شود؛ state snapshot با همان expectedVersion در write محافظت می‌شود و reload/retry وجود ندارد. JpaChangeRequestStore موجود از entity دارای @Version و em.remove/flush استفاده می‌کند؛ SQL DELETE نسخهٔ قبلی را شرط می‌کند و شکست optimistic به 409 CONCURRENT_MODIFICATION نگاشت می‌شود. این adapter byte-for-byte با HEAD یکسان ماند.

موفق 204 و بدنهٔ خالی/no-store، بدون Audit؛ تکرار حذف و GET پس از حذف 404. Draft پنهان/ناموجود 404؛ عمل نامجاز روی منبع visible برابر 403؛ غیرDraft visible برای مالک Requester برابر 409 INVALID_TRANSITION (پیش از بررسی نسخه)؛ نسخهٔ stale در Draft برابر 409 CONCURRENT_MODIFICATION؛ نسخهٔ مفقود/نامعتبر 400. Approved و ChangesRequested حذف‌پذیر نشدند و Audit قبلی غیرDraft بدون تغییر باقی می‌ماند. هیچ cascade یا endpoint حذف/ویرایش تاریخچه ساخته نشد.

## لغو نقش حین اجرا، A-006

برای List، History و Delete هر دو ترتیب **20 بار** به تست‌های قبلی اضافه شدند: 120 اجرای تازه در کنار 160 اجرای قبلی Edit/Submit/Approve/GET. هر بار حساب قدیمی عمداً در JPA managed است؛ دو PID و delegate مستقل EntityManager بررسی می‌شوند. هیچ sleep استفاده نمی‌شود.

لغو commit قبل از guard: DELETE آغازشده با 403 و صفر اثر/Audit رد می‌شود؛ History والد Draft پس از لغو Requester و باقی‌ماندن Reviewer، 404؛ List با فیلتر Draft، 200 با items/count/pages صفر است، چون نقش Reviewer همچنان معتبر است. بعد از لغو، عملیات بعدی نیز همین visibility جاری را دارد و Reviewer باقی‌مانده می‌تواند درخواست شخص دیگر را Approve کند.

guard زودتر: writer داخلی واقعی با SELECT FOR UPDATE پشت guard مشترک منتظر می‌ماند؛ انتظار با pg_stat_activity/pg_blocking_pids به backend همان عملیات اثبات می‌شود. عملیات قبلاً پذیرفته‌شده ابتدا commit می‌شود؛ سپس writer تغییر نقش را commit می‌کند و عملیات بعدی رد/محدود می‌شود. در Delete، مشاهده‌گر تست نبود ردیف پس از commit را ثبت می‌کند؛ در readها همان ردیف بدون تغییر دیده می‌شود. مرز «بلافاصله» همان commit موفق لغو است؛ لغو از intent یا پس‌گرفتن پاسخ شبکه تعهدی ندارد. endpoint مدیریت نقش و authorization cache ساخته نشدند.

## Raceهای حذف و US5

هر سه race با دو HTTP همزمان، دو PostgreSQL connection و EntityManager مستقل، و CyclicBarrier بعد از خواندن واقعی همان Draft/version اجرا شدند؛ در هر بار دقیقاً دو read و بدون reload/retry/outer test transaction. هر سناریو 20 تکرار مستقل دارد. یک پاسخ 200 برای Edit/Submit یا 204 برای Delete، و دقیقاً یک 409 CONCURRENT_MODIFICATION پذیرفته می‌شود.

اگر Delete برنده شود، ردیف و Audit آن وجود ندارند و GET Detail/History تازه 404 هستند. اگر Edit برنده شود، payload و owner با version=1 و بدون Audit حفظ می‌شوند. اگر Submit برنده شود، PendingReview با version=1 و دقیقاً یک Audit Draft→PendingReview حفظ می‌شود؛ Delete بازنده نمی‌تواند آن را پاک کند. query سراسری LEFT JOIN در هر تکرار، صفر Audit یتیم را می‌سنجد. در raceها actor هر دو عملیات همان مالک مجاز است؛ نبود Audit عملیات بازنده از تعداد دقیق Audit وابسته به command برنده سنجیده می‌شود، نه از actorId مشترک.

در اجرای هدفمند، Edit/Delete و Submit/Delete هرکدام یک بار برندهٔ غیرDelete و 19 بار برندهٔ Delete داشتند؛ Delete/Delete در 20 بار دقیقاً یک حذف پذیرفت. این شاهد اجرای واقعی است، نه تضمین توزیع یا اجبار scheduler. شواهد همان 60 تکرار در verify کامل نیز جدا ثبت شده‌اند. T046 و T056 و تمام هستهٔ T035–T041 اکنون با مجموعهٔ کامل دوباره اجرا می‌شوند؛ وضعیت نهایی US5 در [us5.md](us5.md) ثبت است.

## API رسمی، معماری و انحرافات

نسخه Java 21 / Boot 4.0.9 / PostgreSQL 17.12 و Maven/BOM تغییر نکردند. APIهای مورد استفاده از مستندات رسمی بررسی شدند: [Boot 4.0.9 SpringBootTest](https://docs.spring.io/spring-boot/4.0/api/java/org/springframework/boot/test/context/SpringBootTest.html)، [Spring MVC validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)، [JPA 3.2 @Version برای update/delete](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/Version.html). هیچ Starter یا API حدسی اضافه نشد.

Controllerها فقط bind/validate و delegation/DTO دارند؛ قواعد مالکیت/وضعیت در domain/application هستند. Entity منتشر نمی‌شود؛ Port خواندن و DTO صفحه مستقل‌اند. Shared guard در transaction عادی READ COMMITTED و CAS درخواست optimistic است. root وب، mapper خطا و transaction manager پیشین دست‌نخورده‌اند. ArchitectureTest و تمام تست‌های قبلی در verify کامل اجرا شدند.

انحراف رفتاری/معماری از Spec، Plan یا ADRها وجود ندارد. جزئیات اجرایی: قاعدهٔ domain requireDeletableBy برای T054 در ChangeRequest اضافه شد؛ GetRequestService/HistoryQueryService و JpaChangeRequestStore به بازنویسی نیاز نداشتند و با تست جدید تأیید شدند؛ فقط مشاهده‌گر test-only writer برای ثبت «ردیف حذف شده» از queryForMap به queryForList تغییر کرد تا نبود ردیف را بدون خطای fixture بسنجد. T055/T056 baseline سبز محافظ موجود را به‌عنوان قرمز اولیه معرفی نمی‌کنند. README/AGENTS با وضعیت نهایی و packageها تطبیق داده شدند. Spec، semantics، OpenAPI، Assumptions، ADRها و فایل‌های .agents/.specify تغییر نکردند.

## verify کامل، خروجی و محدودیت‌ها

**US4 تکمیل شد؛ T050 تا T057 با شواهد واقعی [X] هستند.** پس از clean موفق با exit 0، این فرمان بدون فیلتر یا skip اجرا شد:

```bash
MAVEN_USER_HOME=/tmp/changeflow-maven-user MAVEN_OPTS=-Dmaven.repo.local=/tmp/changeflow-m2 ./mvnw verify
```

**Exit code: 0**. Surefire: 64، Failsafe: 89، مجموع **153** invocation؛ failures/errors/skipped همگی صفر. تعداد تکرارهای داخلی race/لغو، جدا از تعداد invocationهای JUnit است.

```text
[INFO] Tests run: 64, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 89, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  07:57 min
[INFO] Finished at: 2026-11-15T18:10:55+03:30
```

[خروجی واقعی کامل و بدون ویرایش](us4-verify.txt)، XMLها در target/surefire-reports و target/failsafe-reports. ۹ گروه race × ۲۰ و ۱۴ گروه guard/لغو × ۲۰ در همین اجرای نهایی کنترل شدند: مجموع ۱۸۰ رقابت و ۲۸۰ سناریوی لغو. build/compile/package و ArchitectureTest سبزند و تست‌های تمام USهای قبلی، Identity/Authorization و Persistence اجرا شدند. پس از این اجرا فقط اسناد و checkboxها نهایی شدند؛ کد تولیدی/تست عوض نشد.


محدودیت باقی‌مانده در محدودهٔ US4/US5: هیچ شکست تست یا مانع شناخته‌شده‌ای باقی نماند. این checkpoint completion کل پروژه نیست: T058 به بعد، تست قرارداد جامع T059، CI/Quickstart تمیز و پذیرش انسانی SC-006 هنوز بازند؛ dev test-run تا T058 آماده نیست. Task جدیدی بیرون فاز اجرا نشد. هیچ commit یا push انجام نشد.

## فایل‌های ایجادشده و تغییرکرده

### ایجادشده

- `specs/001-change-request-management/validation/us4-tdd.txt`
- `specs/001-change-request-management/validation/us4-verify.txt`
- `specs/001-change-request-management/validation/us4.md`
- `src/main/java/com/changeflow/changerequest/application/DeleteDraftService.java`
- `src/main/java/com/changeflow/changerequest/application/ListRequestsService.java`
- `src/test/java/com/changeflow/changerequest/api/DeleteDraftIT.java`
- `src/test/java/com/changeflow/changerequest/api/PaginationIT.java`
- `src/test/java/com/changeflow/changerequest/api/VisibilityMatrixIT.java`
- `src/test/java/com/changeflow/changerequest/infrastructure/DeleteRaceIT.java`

### تغییرکرده

- `AGENTS.md`
- `README.md`
- `specs/001-change-request-management/tasks.md`
- `specs/001-change-request-management/validation/us5.md`
- `src/main/java/com/changeflow/changerequest/api/ChangeRequestDtos.java`
- `src/main/java/com/changeflow/changerequest/api/DraftRequestController.java`
- `src/main/java/com/changeflow/changerequest/api/RequestReadController.java`
- `src/main/java/com/changeflow/changerequest/application/RequestReader.java`
- `src/main/java/com/changeflow/changerequest/domain/ChangeRequest.java`
- `src/main/java/com/changeflow/changerequest/infrastructure/JpaRequestReader.java`
- `src/test/java/com/changeflow/identity/InFlightRevocationIT.java`
- `src/test/java/com/changeflow/support/ConcurrencyTestConfiguration.java`
