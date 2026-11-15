# US2 — شواهد Test-First

محدودهٔ تأییدشده: فقط T027–T034. T035/T036، T044 و T047 اجرا نشدند. پیش از پیاده‌سازی قواعد، فقط امضاهای قابل compile و stub بدون رفتار برای TransitionPolicy ایجاد شد؛ خطای compile شاهد قرمز نیست.

## T027 → T029: domain

فرمان: `./mvnw -Dtest=TransitionRulesTest test`؛ exit code **1**. خروجی واقعی منتخب:

```text
[ERROR] Tests run: 24, Failures: 24, Errors: 0, Skipped: 0, Time elapsed: 0.674 s <<< FAILURE! -- in com.changeflow.changerequest.domain.TransitionRulesTest
[ERROR]   TransitionRulesTest.approvalExplanationIsOptionalAndSubmitStillValidatesAllThreeFields:55 expected: <Approved> but was: <PendingReview>
[ERROR]   TransitionRulesTest.currentRequiredRoleAndIdentityAreMandatory:38 Expected com.changeflow.changerequest.domain.DomainFailure to be thrown, but nothing was thrown.
[ERROR]   TransitionRulesTest.onlyFourTransitionsAreAllowedAndOriginalRemainsUnchanged:24 expected: <PendingReview> but was: <ChangesRequested>
```

## T028 → T030: HTTP واقعی

فرمان: `./mvnw -Dtest=ArchitectureTest -Dit.test=ApprovalIT,RepeatedSubmitIT verify`؛ exit code **1**. پیش از endpoint، هر پنج تست ApprovalIT با assertion وضعیت HTTP شکست خوردند.

```text
[ERROR] Tests run: 5, Failures: 5, Errors: 0, Skipped: 0, Time elapsed: 9.429 s <<< FAILURE! -- in com.changeflow.changerequest.api.ApprovalIT
[ERROR]   ApprovalIT.authorizationVisibilityStateAndVersionAreCheckedWithoutWrites:31->DraftApiSupport.problem:29 expected: <403> but was: <404>
[ERROR]   ApprovalIT.dualRoleOwnerCannotApproveAndApprovedCannotBeEdited:22->DraftApiSupport.problem:29 expected: <403> but was: <404>
[ERROR]   ApprovalIT.rejectsBadJsonAndMissingCsrfWithoutWrites:64->DraftApiSupport.problem:29 expected: <400> but was: <404>
[ERROR]   ApprovalIT.repeatedApprovalGivesGenericConflictOnlyToTheSuccessfulActor:50 expected: <200> but was: <404>
[ERROR]   ApprovalIT.successfulApprovalIsAReceiptWithCompleteAuditAndDoesNotGrantGetAccess:12 expected: <200> but was: <404>
[ERROR] Tests run: 6, Failures: 5, Errors: 0, Skipped: 0
```

## T030 → T031: تکرار Approve

همان فرمان HTTP پس از تأیید پایه؛ exit code **1**. چهار تست پایه سبز و تنها مسیر رسید تکرار قرمز ماند:

```text
[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 12.74 s <<< FAILURE! -- in com.changeflow.changerequest.api.ApprovalIT
[ERROR]   ApprovalIT.repeatedApprovalGivesGenericConflictOnlyToTheSuccessfulActor:54->DraftApiSupport.problem:29 expected: <409> but was: <404>
[ERROR] Tests run: 6, Failures: 1, Errors: 0, Skipped: 0
```

## نتیجهٔ سبز

قواعد پس از قرمز پیاده شدند. اجرای `./mvnw -Dtest=TransitionRulesTest,DraftRulesTest test` با exit **0** و ۴۹ تست سبز پایان یافت. سپس اجرا با `./mvnw -Dtest=ArchitectureTest,TransitionRulesTest -Dit.test=ApprovalIT,RepeatedSubmitIT,AuditAtomicityIT verify` با exit **0** پایان یافت:

```text
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.447 s -- in com.changeflow.architecture.ArchitectureTest
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.238 s -- in com.changeflow.changerequest.domain.TransitionRulesTest
[INFO] Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 21.59 s -- in com.changeflow.changerequest.api.RepeatedSubmitIT
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 15.04 s -- in com.changeflow.changerequest.api.ApprovalIT
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 9.029 s -- in com.changeflow.changerequest.infrastructure.AuditAtomicityIT
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## شفافیت دربارهٔ baseline و اصلاح تست

- `RepeatedSubmitIT` پیش از هر تغییر رفتار، **از اولین اجرا سبز بود**: US1 قبلاً تکرار Submit مالک در PendingReview را پیش از بررسی نسخه رد می‌کرد. برای ساختن شاهد قرمز مصنوعی، رفتار درست حذف نشد. suite مربوط به T028 با پنج شکست ApprovalIT قرمز بود؛ تفاوت پیش‌بینی Task دربارهٔ قرمزشدن این subtest با واقعیت baseline در checkpoint گزارش شده است. در T031 مسیر تکرار Submit در RepeatedTransitionPolicy صریح شد و regression سبز باقی ماند.
- پس از T031 یک assertion بیش‌ازحد سخت‌گیرانهٔ تست، UUID را حتی در `instance` مشکل HTTP ممنوع کرده بود؛ Framework URL ورودی را در instance قرار می‌دهد و semantics این فیلد پاک‌سازی‌شده را مجاز می‌داند. assertion به نبود فیلد DTO `id` و نبود محتوای خصوصی، requesterId و وضعیت Approved اصلاح شد. هیچ رفتار محصول یا قرارداد برای عبور تست تغییر نکرد.
- T032 برای رفتار تراکنشی موجود نوشته شد: هر سه تست از اولین اجرا سبز شدند؛ هیچ «قرمز ساختگی» یا اصلاح غیرضروری برای T033 انجام نشد. تست با DB واقعی، وضعیت و نسخهٔ flushشده را در همان transaction **پیش از insert Audit** می‌خواند، سپس CHECK constraint واقعی درج را رد می‌کند. تست دیگر با constraint trigger تعویق‌پذیر PostgreSQL فقط در commit شکست می‌دهد. هر دو 500 و rollback کامل، بدون رسید موفق دارند.
- timeout اولیهٔ بازبینی مجوز دستور ترکیبی باعث اجرا نشدن آن دستور شد؛ نوشتن فایل‌ها و اجرای Maven جدا شدند و اجرای تست‌ها موفق شد. مانع باقی‌مانده‌ای وجود ندارد.

پارامترهای محیطی Maven فقط cache را به `/tmp/changeflow-maven-user` و `/tmp/changeflow-m2` هدایت کردند. PostgreSQL 17.12 واقعی و Security/CSRF/HTTP واقعی استفاده شد. [خروجی کامل verify نهایی](us2-verify.txt) بدون انتخاب تست یا skip اجرا شده است.
