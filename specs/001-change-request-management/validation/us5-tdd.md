# US5 — شواهد اجرا، قرمز و کنترل منفی

محدوده فقط T035–T041 است. تمام تست‌ها با PostgreSQL 17.12 واقعی، HTTP/Security/CSRF واقعی و adapterهای واقعی اجرا شدند؛ wrapperهای test source set فقط barrier، مشاهدهٔ transaction و fault داخلی می‌سازند. DB یا پاسخ تصمیم mock نشده است. هیچ sleep در تست‌های جدید نیست.

## baseline پیش از تغییر محافظ‌ها

فرمان `./mvnw -Dtest=ArchitectureTest -Dit.test=ConcurrentMutationIT,InFlightRevocationIT verify`، **exit 0**. این واقعیت با انتظار «قرمز اولیه» در درخواست متفاوت بود: محافظ CAS/@Version و shared guard/fresh read قبلاً در US1/US2/Foundation موجود بودند. برای ادعای قرمز اولیه، کد درست حذف نشد. اجرای baseline شامل چهار race هرکدام ۲۰ بار (۸۰) و دو ترتیب لغو برای چهار operation هرکدام ۲۰ بار (۱۶۰) بود.

```text
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.251 s -- in com.changeflow.architecture.ArchitectureTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 36.57 s -- in com.changeflow.changerequest.infrastructure.ConcurrentMutationIT
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 114.2 s -- in com.changeflow.identity.InFlightRevocationIT
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  02:41 min
[INFO] Finished at: 2026-11-15T15:18:23+03:30
```

## کنترل منفی ۱ — رزرو نسخهٔ edit هم‌مقدار

پس از baseline سبز، فقط فراخوانی `OPTIMISTIC_FORCE_INCREMENT` در شاخهٔ edit هم‌مقدار JpaChangeRequestStore موقتاً حذف شد؛ بقیهٔ CAS، وضعیت و DB واقعی باقی ماندند. فرمان `./mvnw -Dtest=ArchitectureTest -Dit.test=ConcurrentMutationIT verify`، **exit 1**. نتیجهٔ واقعی رفتاری، بدون خطای compile/runtime:

```text
[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 30.72 s <<< FAILURE! -- in com.changeflow.changerequest.infrastructure.ConcurrentMutationIT
[ERROR]   ConcurrentMutationIT.competingMutationsPreserveExactlyOneWinner:35 exactly one accepted mutation ==> expected: <1> but was: <2>
[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```

تست دو پذیرش واقعی HTTP را به‌جای یک پذیرش تشخیص داد. فایل بلافاصله byte-for-byte برگردانده شد.

## کنترل منفی ۲ — اعتماد به نقش قدیمی در guard

پس از برگرداندن محافظ نسخه، در DatabaseCurrentAccess فقط flags خروجی guard موقتاً همیشه مجاز شدند؛ query/قفل واقعی همچنان اجرا می‌شد. فرمان `./mvnw -Dtest=ArchitectureTest -Dit.test=InFlightRevocationIT#committedRevocationBeforeFinalGuardRejectsInFlightAndLaterOperation verify`، **exit 1**. پاسخ 200 واقعی پس از commit لغو، موجب شکست assertion شد؛ timeout یا خطای compile شاهد قرمز نیست:

```text
[ERROR] Tests run: 4, Failures: 4, Errors: 0, Skipped: 0, Time elapsed: 17.36 s <<< FAILURE! -- in com.changeflow.identity.InFlightRevocationIT
[ERROR]   InFlightRevocationIT.committedRevocationBeforeFinalGuardRejectsInFlightAndLaterOperation:45->DraftApiSupport.problem:29 expected: <403> but was: <200>
[ERROR]   InFlightRevocationIT.committedRevocationBeforeFinalGuardRejectsInFlightAndLaterOperation:45->DraftApiSupport.problem:29 expected: <403> but was: <200>
[ERROR]   InFlightRevocationIT.committedRevocationBeforeFinalGuardRejectsInFlightAndLaterOperation:45->DraftApiSupport.problem:29 expected: <403> but was: <200>
[ERROR]   InFlightRevocationIT.committedRevocationBeforeFinalGuardRejectsInFlightAndLaterOperation:45->DraftApiSupport.problem:29 expected: <404> but was: <200>
[ERROR] Tests run: 4, Failures: 4, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```

برای mutationها 403 مورد انتظار بود؛ GET پس از لغو Requester و باقی‌ماندن Reviewer روی Draft طبق visibility قرارداد باید 404 بدهد. هر چهار حالت با mutant پاسخ 200 دادند و تست آن را گرفت. فایل بلافاصله برگردانده شد.

### شاهد بازگرداندن محافظ‌ها

نسخهٔ نهایی هر دو فایل با مبنای commit `890f8b9` دقیقاً یکسان است؛ SHA-256:

- `src/main/java/com/changeflow/changerequest/infrastructure/JpaChangeRequestStore.java`: `44f50623ee42e055501e7a0882aab9a8f44e84c6ad2861147b41d623d7dd4162`
- `src/main/java/com/changeflow/identity/infrastructure/DatabaseCurrentAccess.java`: `b149ba3341789bdbdb056053ff3d8894da1a143eb64bd5a4532f29e6a4a6582b`

## T040 — قرمز واقعی برای نگاشت خطای منبع

فرمان `./mvnw -Dtest=ArchitectureTest -Dit.test=AuthorizationFailureIT,IdempotencyIT verify`، **exit 1**. دو شکست واقعی پیش از اصلاح root وب:

```text
[ERROR] Tests run: 3, Failures: 2, Errors: 0, Skipped: 0, Time elapsed: 6.900 s <<< FAILURE! -- in com.changeflow.identity.AuthorizationFailureIT
[ERROR]   AuthorizationFailureIT.lostRoleConnectionDuringFinalGuardFailsClosedWithoutCommit:27->DraftApiSupport.problem:29 expected: <503> but was: <500>
[ERROR]   AuthorizationFailureIT.unexpectedIdentityFailureIs500AndNeverLeakingItsCause:42->DraftApiSupport.problem:29 expected: <500> but was: <503>
[ERROR] Tests run: 6, Failures: 2, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```

قطع connection مجوز در همان PostgreSQL با `pg_terminate_backend` در barrier پیش از guard انجام شد. در مسیر authentication نیز پس از lookup واقعی، connection واقعی قطع شد؛ fault غیرمنتظره بعد از lookup واقعی تزریق شد. قطعی در API باید 503 و خطای غیرمنتظره در Security باید 500 می‌داد؛ هر دو ابتدا نادرست بودند.

نگاشت سادهٔ cause ابتدا کافی نبود: شکست rollback به JpaSystemException/TransactionException با SQLException بدون SQLSTATE تبدیل و خطای اولیه را گم می‌کرد. TransactionConfiguration در root، همان JpaTransactionManager را با wrapper استاندارد TransactionSystemException برای شکست rollback تنظیم می‌کند تا interceptor علت اصلی را حفظ کند. Boot customizerها همچنان اعمال می‌شوند؛ commit، قفل‌ها، propagation و مرزهای ADR عوض نشدند. classifier مشترک وب فقط نوع خطا/SQLSTATE را بررسی می‌کند، نه متن exception. adapter هویت هیچ import از API/web نگرفت.

اجرای `./mvnw -Dtest=ArchitectureTest -Dit.test=AuthorizationFailureIT verify`، **exit 0**:

```text
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.918 s -- in com.changeflow.architecture.ArchitectureTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 17.36 s -- in com.changeflow.identity.AuthorizationFailureIT
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

diagnostic موقت فقط نام کلاس و SQLSTATE را چاپ کرد و پیش از نسخهٔ نهایی حذف شد. پیام خصوصی fault در لاگ یا پاسخ نهایی منع و بررسی شد. هیچ خطای compile، timeout یا خطای fixture به‌عنوان قرمز رفتاری معرفی نمی‌شود.

## بازگشت نهایی به سبز

پس از بازگرداندن هر دو mutant، تکمیل مشاهدات EntityManager مستقل و اصلاح T040، `./mvnw clean` با exit 0 و سپس **`./mvnw verify` کامل، بدون انتخاب تست یا skip** اجرا شد؛ خروجی کامل در [us5-verify.txt](us5-verify.txt) و نتیجه/شمار تکرارها در [checkpoint](us5.md) ثبت شده است. متغیرهای Maven فقط مسیر cacheهای `/tmp/changeflow-maven-user` و `/tmp/changeflow-m2` را تعیین کردند.
