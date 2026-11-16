# ممیزی فنی T062 تا T064

نتیجهٔ اجرای واقعی: **کامل از نظر فنی، پذیرش انسانی SC-006 باز**. **T061/پذیرش انسانی SC-006 باز است**؛ نتیجهٔ آزمون خودکار معادل پذیرش ۵ کارمند نیست و «پروژه کامل» اعلام نمی‌شود.

## محدوده و معنای محیط تمیز

مبنای کد: commit `4d1330b`؛ در این نوبت فقط مستندات و شواهد تغییر کردند، نه کد محصول، تست‌ها یا رفتار. Spec، قراردادهای API و ADRها تغییر نکرده‌اند. طبق دستور صریح کاربر T061 پیش‌نیاز اجرای این ممیزی فنی تلقی نشد و باز می‌ماند.

فرمان واقعی:

```bash
MAVEN_USER_HOME=/tmp/changeflow-maven-user MAVEN_OPTS=-Dmaven.repo.local=/tmp/changeflow-m2 ./mvnw clean verify
```

«محیط تمیز» دقیقاً یعنی Maven clean، پوشهٔ target قبلی را حذف کرد؛ ۶۱ فایل main و ۴۷ فایل test دوباره با release 21 کامپایل، JAR دوباره ساخته و تست‌ها بدون فیلتر/skip اجرا شدند. Testcontainers برای آزمون‌ها PostgreSQL تازه با credential تصادفی فراهم کرد؛ reuse فعال نیست. منبع ورودی همان checkout موجود بود، نه checkout تازه؛ در زمان اجرا تغییرات مستنداتی T062 در working tree بود. ماشین/OS، Docker daemon، image کش‌شدهٔ postgres:17.12، Maven distribution و dependency cache موجود دوباره استفاده شدند. هیچ ادعای ماشین نو، cache خالی، build offline یا reproducibility بیتی مطرح نیست. محیط Linux 7.3.0-10-generic amd64، OpenJDK 21.0.13.0.3 و Maven 3.9.16 بود؛ نسخهٔ Boot در pom.xml برابر 4.0.9 است.

## نتیجهٔ clean verify

```text
[INFO] --- clean:3.5.0:clean (default-clean) @ change-flow ---
[INFO] Deleting /srv/projects/ai/sdd-train/change-flow/target
[INFO] Compiling 61 source files with javac [debug parameters release 21] to target/classes
[INFO] Compiling 47 source files with javac [debug parameters release 21] to target/test-classes
[INFO] Tests run: 66, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 97, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time: 08:37 min
[INFO] Finished at: 2026-11-16T15:34:53+03:30
```

**exit code=0**؛ مجموع ۱۶۳ invocation موفق، صفر failure/error/skip. جمع XMLهای تازهٔ target/surefire-reports و target/failsafe-reports همین ۶۶/۹۷ را تأیید کرد. هر ۴۲ method ارجاع‌شده در ماتریس FR/AS در testcaseهای XML همین اجرا پیدا شد؛ شمار پارامتری/تکراری داخل حلقه‌ها با تعداد testcase یکی نیست. خروج کامل و قالب اصلی در [final-clean-verify.txt](final-clean-verify.txt) نگه‌داری شد. شکست باقیمانده‌ای در suite شناسایی نشد.

## Quickstart واقعی

فرمان واقعی `./mvnw spring-boot:test-run -Dspring-boot.run.profiles=dev` با secret تصادفی environment روی 127.0.0.1:8080 و PostgreSQL موقت اجرا شد. هر سه کاربر requester/reviewer/dual از HTTP احراز هویت شدند. Create → Edit → Submit → RequestChanges → دیدن توضیح توسط مالک → Edit → Submit → Approve → History کامل، و Create/Delete یک Draft دیگر با موفقیت اجرا شد؛ هر ده operationId اجرا شدند. تاریخچه چهار گذار با resultingVersionهای 2/3/5/6 و زمان UTC داشت؛ GET Reviewer پس از Approved و GET Draft حذف‌شده 404 بودند؛ CSRF/cookie و no-store بررسی شدند.

harness خروج **0** داشت. فرآیند dev متعلق به همین smoke پس از پایان با SIGTERM متوقف شد؛ کد توقف فرآیند پیوسته 143 بود، نه خطای startup. هیچ listener/فرآیند قبلی کاربر متوقف یا استفاده نشد؛ پورت پیش از اجرا آزاد بررسی شد. شواهد بدون secret در [final-quickstart.txt](final-quickstart.txt) ثبت شده‌اند.

فرمان startup عین Quickstart است؛ HTTPهای معادل نمونه‌های curl با harness خودکار اجرا می‌شوند، نه با ورود تعاملی انسان در دو terminal. secret در environment فرآیند محلی تصادفی است؛ خروجی ذخیره‌شده شامل password، token، cookie یا Authorization نیست. این smoke تکنیکی جایگزین SC-006 نیست.

## رقابت، لغو نقش و مرزهای تراکنش

| سناریوی رقابت نسخه | شمار موفق ثبت‌شده در همین خروجی |
| --- | --- |
| ApproveApprove | 20 |
| ApproveRequestChanges | 20 |
| DeleteDelete | 20 |
| EditDelete | 20 |
| EditEdit | 20 |
| EditSubmit | 20 |
| EqualEditEqualEdit | 20 |
| RequestChangesRequestChanges | 20 |
| SubmitDelete | 20 |

۹ سناریو ×۲۰ = **۱۸۰ رقابت نسخه**؛ برای هر سناریو دقیقاً iterationهای 1 تا 20 (نه صرفاً ۲۰ خط تکراری) در خروجی پیدا شد.

| عملیات لغو نقش | revocation-first | guard-first |
| --- | --- | --- |
| Edit | 20 | 20 |
| Submit | 20 | 20 |
| Approve | 20 | 20 |
| Read | 20 | 20 |
| List | 20 | 20 |
| History | 20 | 20 |
| Delete | 20 | 20 |

۷ عملیات ×۲ ترتیب ×۲۰ = **۲۸۰ اجرای تکرارشدهٔ لغو نقش/guard**؛ برای هر جفت نیز iterationهای 1 تا 20 ثبت شد. این ۲۸۰ مورد با ۱۸۰ رقابت نسخه جمع‌بندی متفاوتی دارند و معیار «یک برنده» برای هر دو نوع یکسان نیست.

سه کلاس ConcurrentMutationIT، MixedReviewRaceIT و DeleteRaceIT، برای هر iteration دو PID پایگاه‌داده و دو EntityManager مستقل و reads=2 را assert می‌کنند. ReadRace از CyclicBarrier و GuardGate از CountDownLatch استفاده می‌کند؛ در source تست‌ها Thread.sleep یا retry شفاف تصمیم وجود ندارد. رقابت نسخه، یک موفق و یک 409 CONCURRENT_MODIFICATION دارد؛ نتیجهٔ داده/نسخه/Audit و نبود Audit رقیب بازنده بررسی می‌شود. رقابت لغو نقش معیار متفاوت دارد: اگر لغو زودتر commit شود mutation رد می‌شود؛ اگر guard زودتر گرفته شود عملیات پیش از commit لغو تمام می‌شود و درخواست بعدی با نقش تازه ارزیابی می‌شود. برای read ممکن است 404 یا List مجازِ خالی با نقش باقی‌مانده باشد، نه الزاماً 403.

علاوه بر ۲۸۰ اجرای تکرارشدهٔ InFlightRevocationIT، CorrectionSafetyIT یک سناریوی اختصاصی RequestChanges با revocation-first دارد؛ این مورد ۲۰ بار یا در هر دو ترتیب تکرار نشده است و چنین ادعایی نمی‌شود. دو ترتیب تکرارشده برای Edit، Submit، Approve، GET Detail، List، History و Delete هستند؛ مسیر Create نیز تست اختصاصی دوترتیبی ندارد. برای RequestChanges/Create، استفاده از همان guard واقعی در application و تست‌های مجوز/API بازبینی شد. این محدودیت تفکیک پوشش، در حکم اثبات تمام interleavingهای ممکن نیست.

## Migration، privacy، معماری و بدون mock DB

- SchemaIT.migratesEmptyDatabaseAndEnforcesConstraints، Flyway را روی DB خالی واقعی اجرا می‌کند؛ Migration موجود فقط V1__initial_schema.sql است. در خروج این clean verify، ۲۸ پیام واقعی Successfully applied 1 migration تا version v1 ثبت شد؛ این تعداد پیام است، نه ۲۸ نسخهٔ Migration. سه جدول کسب‌وکار، FK، status/checkهای گذار و توضیح، version پیش‌فرض/NOT NULL، unique(request_id,resulting_version)، FK حذف محدود و indexها آزمایش می‌شوند. EntityMappingIT round-trip چهار وضعیت و متن nullable و تاریخچه را با Hibernate validate اجرا می‌کند. application.yaml مقدار ddl-auto=validate دارد؛ Hibernate سازندهٔ schema نیست.
- **بدون mock DB**: PostgresTestSupport با postgres:17.12 و ServiceConnection، DriverManager connection واقعی می‌سازد. HttpIntegrationSupport از SpringBootTest RANDOM_PORT و HTTP واقعی استفاده می‌کند. ConcurrencyTestConfiguration فقط adapter واقعی را با barrier/decorator مشاهده می‌کند؛ نقش، تصمیم یا SQL با mock جایگزین نشده‌اند. faultهای Audit/identity روی همین PostgreSQL با constraint/trigger/قطع اتصال ایجاد می‌شوند. جست‌وجوی source/pom برای MockBean/MockitoBean/Mockito.mock/mock()/spy()، H2/hsqldb، withReuse و Thread.sleep موردی پیدا نکرد؛ این ادعا نبود تمام ابزارهای mock در dependencyهای starter-test نیست. MockEnvironment/HTTP test doubles در unit testها DB mock نیستند.
- ArchitectureTest دو آزمون برای وابستگی به داخل، domain مستقل از Spring/JPA، ممنوعیت feature→web/identity.infrastructure، Constructor Injection، Entity غیرعمومی و عدم انتشار Entity و Controller بدون فراخوانی قواعد/ذخیره‌سازی دارد.
- LogPrivacyIT با debug=true، ورودی و query و correlation جعلی، credential و CSRF token را در لاگ جست‌وجو می‌کند و JSON outcome/correlationId/duration را می‌سنجد. GetRequestIT و HistoryVisibilityIT و DeleteDraftIT، ناموجود و نامرئی را در همان URL مقایسه می‌کنند. ProblemMappingTest، ApiContractIT و AuthorizationFailureIT شکل خطا و عدم بازتاب جزئیات را بررسی می‌کنند. این‌ها شواهد موارد آزموده‌شده‌اند، نه ادعای تست نفوذ جامع.
- SetupPackagingIT وجود entry point تولید و نبود bootstrap/test dev در JAR را بررسی می‌کند؛ DevBootstrapIT/DevConfigurationTest secret غایب/خالی، loopback و hash استاندارد را می‌سنجند.

## T062: بازبینی فرض‌ها و ADRها

همهٔ فرض‌ها همچنان «قابل بازبینی» هستند؛ تأیید فنی آزمون، وضعیت آن‌ها را به تصمیم محصولی قطعی تبدیل نمی‌کند.

| فرض | نتیجهٔ بازبینی با واقعیت |
| --- | --- |
| A-001 | VisibilityMatrixIT/GetRequestIT/HistoryVisibilityIT: Reviewer فقط PendingReview به اتکای نقش بررسی؛ خروج از آن وضعیت 404. مالک دو‌نقشه با Requester جاری همچنان دسترسی خود را دارد. |
| A-002 | JpaHistoryReader visibility والد را join می‌کند؛ HistoryVisibilityIT no-leak و HistoryIT نبود مسیر REST ویرایش/حذف را می‌سنجد. |
| A-003 | Submit/Approve توضیح غایب را خالی ثبت می‌کنند؛ AuditAtomicityIT/DraftFlowIT؛ Create/Edit/Delete گذار/Audit نمی‌سازند، از جمله DeleteDraftIT. |
| A-004 | ترتیب ثابت id صعودی در DB و شمارش قبل از pagination؛ PaginationIT مجموعهٔ ثابت ۲۵ مورد را بی‌تکرار/جاافتادگی با موارد نامرئی بررسی می‌کند. |
| A-005 | قرارداد trusted identity/roles بیرون قابلیت changerequest است؛ adapter فعلی DB در identity و پشت Portهاست، نه IdP خارجیِ مستقر. endpoint مدیریت نقش وجود ندارد؛ AccessProvisioner writer داخلی کنترل‌شده است. |
| A-006 | guard JDBC تازه FOR SHARE و writer FOR UPDATE با نسخه؛ commit لغو مرز اثر است. دو ترتیب واقعی و نقش باقی‌مانده در InFlightRevocationIT؛ مرز از لحظهٔ intent یا پس‌گرفتن commit وعده داده نمی‌شود. |
| A-007 | تفسیر صریح تأییدشدهٔ کاربر: query حاضر و خالی 400، غایب با رفتار قبلی؛ ApiContractIT/PaginationIT. expectedVersion غایب پیش‌فرض ندارد و required بودنش 400 می‌دهد. این حکم صریح قرارداد نامیده نمی‌شود؛ History فیلتر status تازه ندارد. |

ADR-001 با Java 21/Boot 4.0.9، BOM، Maven 3.9.16، PostgreSQL 17.12، Flyway و مدل مستقل تطبیق دارد. ADR-002 با Basic stateless، CSRF CookieCsrfTokenRepository، current-role JDBC و guard/writer تطبیق دارد. ADR-003 با expectedVersion، CAS/@Version، 409، نبود retry و transaction مشترک Audit و رسید محدود تکرار Approve تطبیق دارد. بنا بر محدودیت صریح کاربر، هیچ ADR تکمیل یا بازنویسی نشد؛ نتیجهٔ واقعی بازبینی همین‌جا ثبت می‌شود.

**کمبود مستنداتی گزارش‌شده:** ADR-002 فعال‌بودن CSRF را ثبت کرده اما دلیل مستقل CSRF را توضیح نمی‌دهد. Research بخش ۳، دلیل مرورگری HTTP Basic را از قبل دارد. تصمیم تازه‌ای دربارهٔ CSRF گرفته نشد؛ README و AGENTS وضعیت و همین تفاوت را منعکس می‌کنند. متن Plan و Spec از زمان طراحی به A-001 تا A-006 اشاره دارند؛ A-007 تفسیر بعدی تأییدشده در docs/assumptions.md است و اسناد محفوظ برای افزودن آن ویرایش نشدند.

**T042:** عبارت «409 برای مشاهده‌کننده» در ماتریس فعلی مصداق ندارد؛ توضیح از قبل در tasks.md/T042 و validation/us3.md بخش «انحرافات و تفسیرها» ثبت است. RequestChanges تکراری Reviewer قبلیِ بی‌دید 404، مالک دو‌نقشه 403 و شکست race برابر 409 CONCURRENT_MODIFICATION است؛ Approve تکراری همان عامل موفق با نقش جاری، طبق semantics خط ۴۲، 409 INVALID_TRANSITION بدون GET access دارد. توضیح قبلی کافی بود و این نوبت تغییر رفتار/توضیح تکراری جدیدی اضافه نشد.

## T064: ردیابی FR-001 تا FR-021

این جدول شاهد نیازمندی را به روش آزمون اجراشده وصل می‌کند؛ اندازه‌گیری درصد پوشش خط/شاخه نیست و ادعای همهٔ ورودی‌ها یا interleavingهای ممکن ندارد. «شاهد خودکار» به معنی پذیرش انسانی نیست. ارجاع‌ها در بررسی قابل‌تکرار با source و testcaseهای XML همین clean verify تطبیق داده می‌شوند.

| FR | رفتار تحت پوشش | شاهد خودکار |
| --- | --- | --- |
| FR-001 | Draft ناقص با نقش جاری | [DraftFlowIT.createsIncompleteDraftThenReplacesTextAndSubmitsWithOneAudit](../../../src/test/java/com/changeflow/changerequest/api/DraftFlowIT.java) |
| FR-002 | شناسه/مالک از هویت؛ ورودی جعل مالک رد | [DraftFlowIT.rejectsUnknownFieldsBadBodiesAndMissingOrNegativeVersionsWithoutWrites](../../../src/test/java/com/changeflow/changerequest/api/DraftFlowIT.java)<br>[SecurityIT.matchingCsrfAllowsTestMutationAndIdentityOnlyComesFromContext](../../../src/test/java/com/changeflow/identity/SecurityIT.java) |
| FR-003 | ویرایش فقط مالک دارای نقش و وضعیت مجاز | [DraftFlowIT.enforcesOwnershipCurrentRoleStateAndVersionInContractOrder](../../../src/test/java/com/changeflow/changerequest/api/DraftFlowIT.java)<br>[CorrectionSafetyIT.changesRequestedOwnerNeedsCurrentRequesterForEditAndSubmitAndNoDeleteIsAdded](../../../src/test/java/com/changeflow/changerequest/api/CorrectionSafetyIT.java) |
| FR-004 | Submit کامل/ناقص و صفر اثر شکست | [DraftFlowIT.everyInvalidSubmitLeavesContentStateVersionAndAuditUnchanged](../../../src/test/java/com/changeflow/changerequest/api/DraftFlowIT.java)<br>[DraftFlowIT.createsIncompleteDraftThenReplacesTextAndSubmitsWithOneAudit](../../../src/test/java/com/changeflow/changerequest/api/DraftFlowIT.java) |
| FR-005 | دو عنوان یکسان و دو Submit مستقل | [DraftFlowIT.duplicateTitlesAreIndependentAndSuccessfulEqualEditIncrementsVersion](../../../src/test/java/com/changeflow/changerequest/api/DraftFlowIT.java) |
| FR-006 | Approve دیگری و یک Audit | [ApprovalIT.successfulApprovalIsAReceiptWithCompleteAuditAndDoesNotGrantGetAccess](../../../src/test/java/com/changeflow/changerequest/api/ApprovalIT.java) |
| FR-007 | Self-Approval کاربر دو‌نقشه رد | [ApprovalIT.dualRoleOwnerCannotApproveAndApprovedCannotBeEdited](../../../src/test/java/com/changeflow/changerequest/api/ApprovalIT.java) |
| FR-008 | حذف فقط Draft مالک؛ غیرDraft ثابت | [DeleteDraftIT.ownerDeletesDraftWithoutAuditAndRepeatedDeleteAndReadAreNotFound](../../../src/test/java/com/changeflow/changerequest/api/DeleteDraftIT.java)<br>[DeleteDraftIT.everyNonDraftStateIncludingApprovedStaysUnchangedWithItsAudit](../../../src/test/java/com/changeflow/changerequest/api/DeleteDraftIT.java) |
| FR-009 | بازگشت با توضیح و نمایش به مالک | [RequestChangesIT.explanationIsRequiredIncludingUnicodeWhitespaceWithoutAnyEffects](../../../src/test/java/com/changeflow/changerequest/api/RequestChangesIT.java)<br>[RequestChangesIT.validReturnIsAtomicAndDualOwnerCannotReviewSelf](../../../src/test/java/com/changeflow/changerequest/api/RequestChangesIT.java)<br>[ResubmissionIT.historySurvivesRealCorrectionCycle](../../../src/test/java/com/changeflow/changerequest/api/ResubmissionIT.java) |
| FR-010 | اصلاح/ارسال مجدد با حفظ تاریخچه | [ResubmissionIT.editsAndSubmitKeepEarlierAuditAndValidateContentAndCurrentOwnerRole](../../../src/test/java/com/changeflow/changerequest/api/ResubmissionIT.java) |
| FR-011 | جدول کامل چهار وضعیت × سه عمل؛ Approved ثابت | [TransitionRulesTest.onlyFourTransitionsAreAllowedAndOriginalRemainsUnchanged](../../../src/test/java/com/changeflow/changerequest/domain/TransitionRulesTest.java)<br>[ApprovalIT.dualRoleOwnerCannotApproveAndApprovedCannotBeEdited](../../../src/test/java/com/changeflow/changerequest/api/ApprovalIT.java) |
| FR-012 | Requester فقط مالکیت جاری | [VisibilityMatrixIT.fullRoleOwnerStateMatrixAndFreshDualRoleRevocationAndGrant](../../../src/test/java/com/changeflow/changerequest/api/VisibilityMatrixIT.java) |
| FR-013 | Reviewer فقط PendingReview؛ تجمیع دو نقش | [VisibilityMatrixIT.fullRoleOwnerStateMatrixAndFreshDualRoleRevocationAndGrant](../../../src/test/java/com/changeflow/changerequest/api/VisibilityMatrixIT.java)<br>[GetRequestIT.reviewerSeesOnlyPendingAndDualRolesAggregateWithFreshRevocation](../../../src/test/java/com/changeflow/changerequest/api/GetRequestIT.java) |
| FR-014 | نقش fresh و دو ترتیب guard/revocation | [InFlightRevocationIT.committedRevocationBeforeFinalGuardRejectsInFlightAndLaterOperation](../../../src/test/java/com/changeflow/identity/InFlightRevocationIT.java)<br>[InFlightRevocationIT.earlierSharedGuardMakesRevocationWaitUntilTheAcceptedOperationCommits](../../../src/test/java/com/changeflow/identity/InFlightRevocationIT.java)<br>[InFlightRevocationIT.us4CommittedRevocationRejectsInFlightAccessBeforeGuard](../../../src/test/java/com/changeflow/identity/InFlightRevocationIT.java)<br>[InFlightRevocationIT.us4EarlierGuardOrdersReadsAndDeleteBeforeRoleCommit](../../../src/test/java/com/changeflow/identity/InFlightRevocationIT.java)<br>[CorrectionSafetyIT.committedRevocationBeforeRequestChangesGuardRejectsAlreadyStartedOperation](../../../src/test/java/com/changeflow/changerequest/api/CorrectionSafetyIT.java) |
| FR-015 | نسخه و ۹ رقابت واقعی ×۲۰ | [ConcurrentMutationIT.competingMutationsPreserveExactlyOneWinner](../../../src/test/java/com/changeflow/changerequest/infrastructure/ConcurrentMutationIT.java)<br>[MixedReviewRaceIT.twoIndependentReviewersAcceptExactlyOneDecision](../../../src/test/java/com/changeflow/changerequest/infrastructure/MixedReviewRaceIT.java)<br>[DeleteRaceIT.exactlyOneVersionCheckedMutationWinsAndNoOrphanAuditExists](../../../src/test/java/com/changeflow/changerequest/infrastructure/DeleteRaceIT.java) |
| FR-016 | یک Audit کامل و rollback مشترک تا commit | [AuditAtomicityIT.auditFailureDeferredUntilCommitCannotReturnASuccessReceipt](../../../src/test/java/com/changeflow/changerequest/infrastructure/AuditAtomicityIT.java)<br>[AuditAtomicityIT.rejectedAuditInsertRollsBackAlreadyFlushedStatusAndVersion](../../../src/test/java/com/changeflow/changerequest/infrastructure/AuditAtomicityIT.java)<br>[DraftFlowIT.nullableReplacementCanClearDraftAndAuditFailureRollsBackSubmit](../../../src/test/java/com/changeflow/changerequest/api/DraftFlowIT.java)<br>[CorrectionSafetyIT.rejectedAuditRollsBackRequestChangesStatusAndVersion](../../../src/test/java/com/changeflow/changerequest/api/CorrectionSafetyIT.java) |
| FR-017 | تاریخچهٔ والد مجاز، کامل و بدون REST mutation | [HistoryIT.historyIsPagedByResultingVersionWithCompleteUtcDtoAndNoMutationRoutes](../../../src/test/java/com/changeflow/changerequest/api/HistoryIT.java)<br>[HistoryVisibilityIT.invisibleParentAndAbsentParentHaveIdenticalNoLeakErrors](../../../src/test/java/com/changeflow/changerequest/api/HistoryVisibilityIT.java) |
| FR-018 | count/صفحات فقط مجاز، ترتیب پایدار | [PaginationIT.paginationCountsOnlyVisibleRowsAndUsesDatabaseUuidOrderAcrossPages](../../../src/test/java/com/changeflow/changerequest/api/PaginationIT.java)<br>[VisibilityMatrixIT.fullRoleOwnerStateMatrixAndFreshDualRoleRevocationAndGrant](../../../src/test/java/com/changeflow/changerequest/api/VisibilityMatrixIT.java) |
| FR-019 | Problem، CSRF، no-store، privacy و no-leak | [ApiContractIT.authenticationCsrfAndIdentityFaultsMatchEveryOperationWithoutEffects](../../../src/test/java/com/changeflow/changerequest/api/ApiContractIT.java)<br>[LogPrivacyIT.logsStructuredOutcomeAndNeverLogsSensitiveInputOrCredentials](../../../src/test/java/com/changeflow/changerequest/api/LogPrivacyIT.java)<br>[GetRequestIT.missingAndInvisibleHaveIdenticalProblemWithoutLeakingParent](../../../src/test/java/com/changeflow/changerequest/api/GetRequestIT.java) |
| FR-020 | تکرار Submit/Approve بدون اثر؛ رسید محدود | [IdempotencyIT.submitRepeatChecksCurrentRoleOwnerAndStateBeforeVersionWithoutNewEffects](../../../src/test/java/com/changeflow/changerequest/api/IdempotencyIT.java)<br>[IdempotencyIT.approveRepeatRecognizesOnlySuccessfulActorAndDoesNotSurviveRoleRevocation](../../../src/test/java/com/changeflow/changerequest/api/IdempotencyIT.java) |
| FR-021 | رد هر دو self-review بر مبنای هویت | [TransitionRulesTest.selfReviewIsIdentityBasedEvenWhenActorHasBothRoles](../../../src/test/java/com/changeflow/changerequest/domain/TransitionRulesTest.java)<br>[RequestChangesIT.validReturnIsAtomicAndDualOwnerCannotReviewSelf](../../../src/test/java/com/changeflow/changerequest/api/RequestChangesIT.java)<br>[ApprovalIT.dualRoleOwnerCannotApproveAndApprovedCannotBeEdited](../../../src/test/java/com/changeflow/changerequest/api/ApprovalIT.java) |

## ردیابی AS-01 تا AS-23

| AS | سناریوی تحت پوشش | شاهد خودکار |
| --- | --- | --- |
| AS-01 | ایجاد ناقص با هویت مالک و شناسه | [DraftFlowIT.createsIncompleteDraftThenReplacesTextAndSubmitsWithOneAudit](../../../src/test/java/com/changeflow/changerequest/api/DraftFlowIT.java) |
| AS-02 | ویرایش خود و رد دیگری/وضعیت قفل | [DraftFlowIT.enforcesOwnershipCurrentRoleStateAndVersionInContractOrder](../../../src/test/java/com/changeflow/changerequest/api/DraftFlowIT.java) |
| AS-03 | Submit کامل و دقیقاً یک Audit | [DraftFlowIT.createsIncompleteDraftThenReplacesTextAndSubmitsWithOneAudit](../../../src/test/java/com/changeflow/changerequest/api/DraftFlowIT.java) |
| AS-04 | هر فیلد مفقود/خالی/فاصله با صفر اثر | [DraftFlowIT.everyInvalidSubmitLeavesContentStateVersionAndAuditUnchanged](../../../src/test/java/com/changeflow/changerequest/api/DraftFlowIT.java) |
| AS-05 | دو عنوان یکسان و Submit هر دو | [DraftFlowIT.duplicateTitlesAreIndependentAndSuccessfulEqualEditIncrementsVersion](../../../src/test/java/com/changeflow/changerequest/api/DraftFlowIT.java) |
| AS-06 | Approve مجاز شخص دیگر | [ApprovalIT.successfulApprovalIsAReceiptWithCompleteAuditAndDoesNotGrantGetAccess](../../../src/test/java/com/changeflow/changerequest/api/ApprovalIT.java) |
| AS-07 | Self-Approval دو‌نقشه | [ApprovalIT.dualRoleOwnerCannotApproveAndApprovedCannotBeEdited](../../../src/test/java/com/changeflow/changerequest/api/ApprovalIT.java) |
| AS-08 | Approved ویرایش/حذف/گذار ندارد | [ApprovalIT.dualRoleOwnerCannotApproveAndApprovedCannotBeEdited](../../../src/test/java/com/changeflow/changerequest/api/ApprovalIT.java)<br>[DeleteDraftIT.everyNonDraftStateIncludingApprovedStaysUnchangedWithItsAudit](../../../src/test/java/com/changeflow/changerequest/api/DeleteDraftIT.java)<br>[TransitionRulesTest.onlyFourTransitionsAreAllowedAndOriginalRemainsUnchanged](../../../src/test/java/com/changeflow/changerequest/domain/TransitionRulesTest.java) |
| AS-09 | ۱۲ ترکیب چهار وضعیت و سه command در domain | [TransitionRulesTest.onlyFourTransitionsAreAllowedAndOriginalRemainsUnchanged](../../../src/test/java/com/changeflow/changerequest/domain/TransitionRulesTest.java) |
| AS-10 | شکست Audit و تاریخچهٔ کامل UTC | [AuditAtomicityIT.rejectedAuditInsertRollsBackAlreadyFlushedStatusAndVersion](../../../src/test/java/com/changeflow/changerequest/infrastructure/AuditAtomicityIT.java)<br>[AuditAtomicityIT.successfulApprovalCommitsExactlyOneCompleteAuditWithServerUtcAndEmptyDefaultExplanation](../../../src/test/java/com/changeflow/changerequest/infrastructure/AuditAtomicityIT.java)<br>[HistoryIT.historyIsPagedByResultingVersionWithCompleteUtcDtoAndNoMutationRoutes](../../../src/test/java/com/changeflow/changerequest/api/HistoryIT.java) |
| AS-11 | RequestChanges و توضیح قابل مشاهده | [RequestChangesIT.validReturnIsAtomicAndDualOwnerCannotReviewSelf](../../../src/test/java/com/changeflow/changerequest/api/RequestChangesIT.java)<br>[ResubmissionIT.historySurvivesRealCorrectionCycle](../../../src/test/java/com/changeflow/changerequest/api/ResubmissionIT.java) |
| AS-12 | توضیح مفقود/خالی/Unicode whitespace | [RequestChangesIT.explanationIsRequiredIncludingUnicodeWhitespaceWithoutAnyEffects](../../../src/test/java/com/changeflow/changerequest/api/RequestChangesIT.java) |
| AS-13 | اصلاح و ارسال مجدد توسط مالک و حفظ Audit | [ResubmissionIT.editsAndSubmitKeepEarlierAuditAndValidateContentAndCurrentOwnerRole](../../../src/test/java/com/changeflow/changerequest/api/ResubmissionIT.java)<br>[CorrectionSafetyIT.changesRequestedOwnerNeedsCurrentRequesterForEditAndSubmitAndNoDeleteIsAdded](../../../src/test/java/com/changeflow/changerequest/api/CorrectionSafetyIT.java) |
| AS-14 | Requester فقط موارد خود در چهار وضعیت | [VisibilityMatrixIT.fullRoleOwnerStateMatrixAndFreshDualRoleRevocationAndGrant](../../../src/test/java/com/changeflow/changerequest/api/VisibilityMatrixIT.java) |
| AS-15 | PendingReviewهای مجاز و تجمیع نقش؛ منع بررسی خود | [VisibilityMatrixIT.fullRoleOwnerStateMatrixAndFreshDualRoleRevocationAndGrant](../../../src/test/java/com/changeflow/changerequest/api/VisibilityMatrixIT.java)<br>[TransitionRulesTest.selfReviewIsIdentityBasedEvenWhenActorHasBothRoles](../../../src/test/java/com/changeflow/changerequest/domain/TransitionRulesTest.java) |
| AS-16 | حذف Draft، حذف مجدد و وضعیت‌های ممنوع | [DeleteDraftIT.ownerDeletesDraftWithoutAuditAndRepeatedDeleteAndReadAreNotFound](../../../src/test/java/com/changeflow/changerequest/api/DeleteDraftIT.java)<br>[DeleteDraftIT.visibilityRoleAndOwnershipPrecedeStateAndVersion](../../../src/test/java/com/changeflow/changerequest/api/DeleteDraftIT.java)<br>[DeleteDraftIT.everyNonDraftStateIncludingApprovedStaysUnchangedWithItsAudit](../../../src/test/java/com/changeflow/changerequest/api/DeleteDraftIT.java) |
| AS-17 | بی‌هویت/بی‌نقش/نامرئی | [SecurityIT.missingAndInvalidCredentialsHaveUniformProblemAndChallenge](../../../src/test/java/com/changeflow/identity/SecurityIT.java)<br>[VisibilityMatrixIT.fullRoleOwnerStateMatrixAndFreshDualRoleRevocationAndGrant](../../../src/test/java/com/changeflow/changerequest/api/VisibilityMatrixIT.java)<br>[HistoryVisibilityIT.invisibleParentAndAbsentParentHaveIdenticalNoLeakErrors](../../../src/test/java/com/changeflow/changerequest/api/HistoryVisibilityIT.java) |
| AS-18 | مجموعهٔ ثابت، صفحات بی‌تکرار/بی‌جاافتادگی | [PaginationIT.paginationCountsOnlyVisibleRowsAndUsesDatabaseUuidOrderAcrossPages](../../../src/test/java/com/changeflow/changerequest/api/PaginationIT.java) |
| AS-19 | Approve/Approve و mixed decision، یک برنده | [ConcurrentMutationIT.competingMutationsPreserveExactlyOneWinner](../../../src/test/java/com/changeflow/changerequest/infrastructure/ConcurrentMutationIT.java)<br>[MixedReviewRaceIT.twoIndependentReviewersAcceptExactlyOneDecision](../../../src/test/java/com/changeflow/changerequest/infrastructure/MixedReviewRaceIT.java) |
| AS-20 | Edit/Edit، Edit/Submit، Edit/Delete، Submit/Delete، Delete/Delete | [ConcurrentMutationIT.competingMutationsPreserveExactlyOneWinner](../../../src/test/java/com/changeflow/changerequest/infrastructure/ConcurrentMutationIT.java)<br>[DeleteRaceIT.exactlyOneVersionCheckedMutationWinsAndNoOrphanAuditExists](../../../src/test/java/com/changeflow/changerequest/infrastructure/DeleteRaceIT.java) |
| AS-21 | Submit/Approve تکراری و صفر اثر | [IdempotencyIT.submitRepeatChecksCurrentRoleOwnerAndStateBeforeVersionWithoutNewEffects](../../../src/test/java/com/changeflow/changerequest/api/IdempotencyIT.java)<br>[IdempotencyIT.approveRepeatRecognizesOnlySuccessfulActorAndDoesNotSurviveRoleRevocation](../../../src/test/java/com/changeflow/changerequest/api/IdempotencyIT.java) |
| AS-22 | عملیات آغازشده و لغو نقش پیش از پذیرش | [InFlightRevocationIT.committedRevocationBeforeFinalGuardRejectsInFlightAndLaterOperation](../../../src/test/java/com/changeflow/identity/InFlightRevocationIT.java)<br>[InFlightRevocationIT.us4CommittedRevocationRejectsInFlightAccessBeforeGuard](../../../src/test/java/com/changeflow/identity/InFlightRevocationIT.java)<br>[CorrectionSafetyIT.committedRevocationBeforeRequestChangesGuardRejectsAlreadyStartedOperation](../../../src/test/java/com/changeflow/changerequest/api/CorrectionSafetyIT.java) |
| AS-23 | RequestChanges خود حتی دو‌نقشه، بدون اثر | [RequestChangesIT.validReturnIsAtomicAndDualOwnerCannotReviewSelf](../../../src/test/java/com/changeflow/changerequest/api/RequestChangesIT.java) |

### موارد بدون پوشش و حدود شواهد

- در سطح ردیابی ۲۱ FR و ۲۳ AS، موردی بدون شاهد آزمون خودکار شناسایی نشد؛ این نتیجه محدود به ماتریس و assertionهای ارجاع‌شده است. محدودیت تست اختصاصی دو ترتیب Create/RequestChanges در بخش رقابت صریح ثبت شد.
- **SC-006/T061 بدون شاهد پذیرش انسانی است:** در این نوبت هیچ آزمون ۵ کارمند/Reviewer واقعی اجرا نشد؛ acceptance.md و نتیجهٔ ۵ از ۵ وجود ندارد. چرخهٔ خودکار ResubmissionIT/ApiContractIT یا smoke هیچ‌کدام این خلا را پر نمی‌کند.
- CI فقط بررسی ساختاری شده است؛ اجرای GitHub Actions، محیط انتشار، TLS handshake واقعیِ محیط مشترک، امنیت‌سنجی جامع و cache/ماشین نو آزمایش نشده‌اند. الزام TLS در config/SetupConfigurationTest و Secure cookie در CsrfCookieTest بررسی می‌شوند؛ این‌ها ادعای استقرار نیستند.

## SC و Definition of Done

| معیار | نتیجه و شاهد |
| --- | --- |
| SC-001 | چهار گذار مجاز/هشت ترکیب نامجاز در ماتریس ۱۲تایی domain؛ چرخه‌های HTTP موفق و آزمون‌های رد بدون اثر. |
| SC-002 | هر دو self-review با هویت دو‌نقشه رد؛ Approved در Edit/Delete/command ثابت؛ شواهد AS-07/08/23. |
| SC-003 | شمار دقیق ۹ سناریو ×۲۰ از خروجی همین اجرا؛ نسخه/داده/برنده/Audit بررسی شدند. |
| SC-004 | موفقیت دقیقاً یک Audit؛ failure درج/commit و تکرار بدون اثر؛ AuditAtomicityIT/AuditStoreIT/IdempotencyIT. |
| SC-005 | ماتریس دو مالک، چهار وضعیت، نقش تک/دو/بدون نقش و grant/revocation؛ List/Detail/History و ۲۸۰ ترتیب guard/revocation. |
| SC-006 | باز؛ شاهد انسانی ۵ از ۵ ثبت نشده است. |
| Stack، migration، معماری و DTO | pom/wrapper/fixtureها، clean compile، SchemaIT/EntityMappingIT و ArchitectureTest. |
| هویت جاری، منع self-review، Approved ثابت، optimistic concurrency/Audit | شواهد FR-006 تا FR-021 و شمارش‌های واقعی بالا. |
| API/validation/CSRF/no-store/privacy | ApiContractIT، SecurityIT، ProblemMappingTest، LogPrivacyIT، A-007؛ پوشش محدود به موارد ثبت‌شده. |
| Test-First | شواهد تاریخی قرمز/سبز در foundation-tdd.md، us1-tdd.md، us2-tdd.md، us5-tdd.md، us3-tdd.txt، us4-tdd.txt و polish-t058-t060.md؛ این نوبت مستنداتی است و شاهد قرمز تازه جعل/بازاجرا نشده است. برای محافظ‌های از قبل سالم و تفسیر A-007 توضیح استثنا/کنترل منفی در گزارش فازها آمده است. |
| مستندات و فرض‌ها | README/AGENTS/Quickstart با runtime و هفت فرض تطبیق داده شدند؛ ADRها فقط بازبینی، کمبود دلیل CSRF گزارش شد. |
| Scope | یک Maven module؛ کد/وابستگی/endpoint/تصمیم تازه‌ای در این نوبت ایجاد نشد. |
| پذیرش و DoD کلی | بخش فنی با شواهد ثبت‌شده تکمیل است؛ DoD کل پروژه به‌دلیل T061/SC-006 هنوز بسته نیست. |

## بررسی قابل‌تکرار مستندات

از ریشهٔ پروژه پس از clean verify اجرا شد؛ فایل‌های هدف لینک‌های محلی (نه دسترس‌پذیری URLهای خارجی/fragmentها)، نسخه‌ها، تمام ارجاع‌های آزمون و شمار تکرارها را بررسی می‌کند:

```bash
python3 - <<'PY'
from pathlib import Path
from collections import defaultdict
from urllib.parse import unquote
import re,xml.etree.ElementTree as E
base=Path('specs/001-change-request-management')
final=(base/'validation/final.md').read_text()
assert set(re.findall(r'^\| (FR-\d{3}) \|',final,re.M))=={f'FR-{i:03}' for i in range(1,22)}
assert set(re.findall(r'^\| (AS-\d{2}) \|',final,re.M))=={f'AS-{i:02}' for i in range(1,24)}
cases=[]
for kind,n in [('surefire',66),('failsafe',97)]:
 roots=[E.parse(p).getroot() for p in Path('target/'+kind+'-reports').glob('TEST-*.xml')]
 assert [sum(int(r.get(k,'0')) for r in roots) for k in ['tests','failures','errors','skipped']]==[n,0,0,0]
 for r in roots:cases.extend(r.iter('testcase'))
refs=set(re.findall(r'\[([A-Za-z0-9]+)\.([A-Za-z0-9_]+)\]\(',final))
assert len(refs)==42
for cls,method in refs:
 assert any(c.get('classname','').endswith('.'+cls) and method in c.get('name','') for c in cases),(cls,method)
raw=(base/'validation/final-clean-verify.txt').read_text()
for pattern,n in [(r'US[345] race=(\w+) iteration=(\d+)',9),(r'US[45] ((?:revocation|guard)-first operation=\w+) iteration=(\d+)',14)]:
 groups=defaultdict(list)
 for name,iteration in re.findall(pattern,raw):groups[name].append(int(iteration))
 assert len(groups)==n
 for name,iterations in groups.items():assert sorted(iterations)==list(range(1,21)),name
assert 'BUILD SUCCESS' in raw and 'Deleting /srv/projects/ai/sdd-train/change-flow/target' in raw
pom=E.parse('pom.xml').getroot();ns={'m':'http://maven.apache.org/POM/4.0.0'}
assert pom.findtext('m:parent/m:version',namespaces=ns)=='4.0.9'
assert pom.findtext('m:properties/m:java.version',namespaces=ns)=='21'
assert 'apache-maven-3.9.16-bin.zip' in Path('.mvn/wrapper/maven-wrapper.properties').read_text()
docs=[Path('README.md'),Path('AGENTS.md'),base/'quickstart.md',base/'tasks.md',base/'validation/final.md']
for path in docs:
 text=path.read_text()
 if path.name in ['README.md','AGENTS.md','quickstart.md']:
  assert '4.0.9' in text and '3.9.16' in text
  assert './mvnw clean verify' in text
  assert './mvnw spring-boot:test-run -Dspring-boot.run.profiles=dev' in text
  assert 'هنوز آماده نیست' not in text and 'پس از Implement' not in text
 plain=re.sub(r'```.*?```','',text,flags=re.S)
 for href in re.findall(r'\[[^\]]+\]\(([^)]+)\)',plain):
  if href.startswith(('http:','https:','#')):continue
  target=unquote(href.split('#',1)[0]);assert (path.parent/target).exists(),(path,href)
assumptions=Path('docs/assumptions.md').read_text()
assert set(re.findall(r'\*\*(A-\d{3}) — قابل بازبینی',assumptions))=={f'A-{i:03}' for i in range(1,8)}
assert 'مصداق' in (base/'validation/us3.md').read_text()
assert 'مصداق' in (base/'tasks.md').read_text()
tasks=(base/'tasks.md').read_text();assert '- [ ] T061 ' in tasks
assert len(re.findall(r'^- \[[X ]\] T\d{3}\b',tasks,re.M))==64
assert 'NOT HUMAN SC-006' in (base/'validation/final-quickstart.txt').read_text()
print('PASS: versions, local links (file targets), 21 FR / 23 AS, 42 executed method references, 180+280 iterations, 163 tests, A-001–A-007 and open T061')
PY
```

این کد از ریشه اجرا شد؛ **exit code=0**:

```text
PASS: versions, local links (file targets), 21 FR / 23 AS, 42 executed method references, 180+280 iterations, 163 tests, A-001–A-007 and open T061
```

متن Spec، contracts، ADRها و src/pom نیز با Git نسبت به مبنای این نوبت مقایسه شد و تغییری نداشت. این بررسی جایگزین اجرای CI، بررسی تمام URLهای خارجی یا پذیرش انسانی نیست.

## وضعیت Taskها و فایل‌ها

| Task | وضعیت واقعی | شاهد |
| --- | --- | --- |
| T061 | [ ] باز | پذیرش انسانی SC-006 اجرا/ثبت نشده است |
| T062 | [X] | تطبیق README/AGENTS/Quickstart، هفت فرض، بازبینی ADRها بدون تغییر و بررسی قابل‌تکرار لینک/نسخه |
| T063 | [X] | clean verify با exit=0، ۱۶۳ تست، smoke واقعی و شمارش ۱۸۰/۲۸۰ |
| T064 | [X] | ۲۱ FR، ۲۳ AS و DoD با ۴۲ method اجراشدهٔ ارجاعی و محدودیت‌های صریح |

۶۳ Task از ۶۴ Task فنی/پذیرش در tasks.md تیک خورده‌اند؛ تنها T061 باز است. **کامل از نظر فنی، پذیرش انسانی SC-006 باز**. این نتیجه مجوز اعلام «پروژه کامل» یا پذیرش انسانی نیست.

فایل‌های این نوبت: README.md، AGENTS.md، specs/001-change-request-management/quickstart.md، tasks.md، validation/final.md، validation/final-clean-verify.txt و validation/final-quickstart.txt. هیچ Spec/قرارداد/ADR یا فایل src/pom تغییر نکرد. raw خروجی Maven قالب اصلی و فاصله‌های چاپ‌شدهٔ ابزار را حفظ می‌کند.
