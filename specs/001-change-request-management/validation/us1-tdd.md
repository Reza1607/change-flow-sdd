# US1 — شواهد Test-First

ترتیب اجرا: تست‌های domain و HTTP پیش از منطق کسب‌وکار؛ سپس domain، store، Audit، service/controller و GET. برای اتصال تست‌ها فقط امضا/stub قابل compile وجود داشت. شکست compile به‌عنوان قرمز پذیرفته نشد. تمام اجراهای قرمز زیر exit code برابر 1 داشتند؛ اجراهای سبز متناظر exit code برابر 0 داشتند.

در اولین اجرای domain، علاوه بر ۲۴ assertion رفتاری، یک NPE ناشی از خروجی nullِ stub ایجاد دیده شد؛ آن NPE شاهد رفتاری محسوب نشده است. assertion صریح not-null بعداً اضافه شد. پیش از اجرای قرمز store نیز ambiguity مربوط به overload assertion اصلاح شد؛ آن خطای compile شاهد قرمز نیست.

## domain

فرمان قرمز: `./mvnw -Dtest=DraftRulesTest test`؛ خروجی واقعی منتخب:

```text
[ERROR] Tests run: 25, Failures: 24, Errors: 1, Skipped: 0, Time elapsed: 0.462 s <<< FAILURE! -- in com.changeflow.changerequest.domain.DraftRulesTest
[ERROR]   DraftRulesTest.editsOwnEditableStateAndKeepsIdentityAndVersion:27 expected: <null> but was: <عنوان>
[ERROR]   DraftRulesTest.invalidSubmissionNamesFieldAndLeavesOriginalUnchanged:53 Expected com.changeflow.changerequest.domain.DraftRuleViolation to be thrown, but nothing was thrown.
[INFO] BUILD FAILURE
```

سبز متناظر (exit 0):

```text
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.369 s -- in com.changeflow.changerequest.domain.DraftRulesTest
[INFO] BUILD SUCCESS
```

## http

فرمان قرمز: `./mvnw -Dtest=ArchitectureTest -Dit.test=DraftFlowIT verify`؛ خروجی واقعی منتخب:

```text
[ERROR] Tests run: 6, Failures: 6, Errors: 0, Skipped: 0, Time elapsed: 20.45 s <<< FAILURE! -- in com.changeflow.changerequest.api.DraftFlowIT
[ERROR]   DraftFlowIT.createsIncompleteDraftThenReplacesTextAndSubmitsWithOneAudit:11 expected: <201> but was: <404>
[ERROR]   DraftFlowIT.duplicateTitlesAreIndependentAndSuccessfulEqualEditIncrementsVersion:44->DraftApiSupport.create:20 expected: <201> but was: <404>
[INFO] BUILD FAILURE
```

سبز متناظر (exit 0):

```text
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 32.97 s -- in com.changeflow.changerequest.api.DraftFlowIT
[INFO] BUILD SUCCESS
```

## store

فرمان قرمز: `./mvnw -Dit.test=RequestStoreIT verify`؛ خروجی واقعی منتخب:

```text
[ERROR] Tests run: 3, Failures: 3, Errors: 0, Skipped: 0, Time elapsed: 17.45 s <<< FAILURE! -- in com.changeflow.changerequest.infrastructure.RequestStoreIT
[ERROR]   RequestStoreIT.persistsNewRequestAndAdvancesVersionIncludingEqualEdits:24 expected: <1> but was: <0>
[ERROR]   RequestStoreIT.preservesNewerCommitAgainstAnAlreadyLoadedManagedEntity:36 Unexpected exception type thrown, expected: <org.springframework.dao.OptimisticLockingFailureException> but was: <java.util.NoSuchElementException>
[INFO] BUILD FAILURE
```

سبز متناظر (exit 0):

```text
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 16.49 s -- in com.changeflow.changerequest.infrastructure.RequestStoreIT
[INFO] BUILD SUCCESS
```

## audit

فرمان قرمز: `./mvnw -Dit.test=AuditStoreIT verify`؛ خروجی واقعی منتخب:

```text
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 15.74 s <<< FAILURE! -- in com.changeflow.changerequest.infrastructure.AuditStoreIT
[ERROR]   AuditStoreIT.appendsCompleteAuditAndRejectsDuplicateResultVersion:25 expected: <1> but was: <0>
[INFO] BUILD FAILURE
```

سبز متناظر (exit 0):

```text
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 15.47 s -- in com.changeflow.changerequest.infrastructure.AuditStoreIT
[INFO] BUILD SUCCESS
```

## get

فرمان قرمز: `./mvnw -Dit.test=GetRequestIT verify`؛ خروجی واقعی منتخب:

```text
[ERROR] Tests run: 3, Failures: 3, Errors: 0, Skipped: 0, Time elapsed: 18.82 s <<< FAILURE! -- in com.changeflow.changerequest.api.GetRequestIT
[ERROR]   GetRequestIT.missingAndInvisibleHaveIdenticalProblemWithoutLeakingParent:37->DraftApiSupport.problem:29 expected: <404> but was: <405>
[ERROR]   GetRequestIT.requesterOwnsVisibilityAcrossAllStatesAndDtoHasOnlyContractFields:13 expected: <200> but was: <405>
[INFO] BUILD FAILURE
```

سبز متناظر (exit 0):

```text
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 21.72 s -- in com.changeflow.changerequest.api.GetRequestIT
[INFO] BUILD SUCCESS
```

## اصلاحات حاصل از تست واقعی

- HTTP اولیه پس از wiring، توضیح اختیاری غایب را به‌اشتباه 400 می‌داد. Jackson 3 برای creator property، null و missing را در روش اول یکسان می‌کرد. DTO اکنون با JsonNode غیاب را از null صریح جدا می‌کند؛ missing مجاز و null صریح طبق OpenAPI نامجاز است. قرارداد محصول تغییر نکرد.
- تست شکست واقعی درج Audit نشان داد Hibernate با logger `org.hibernate.orm.jdbc.error`، سطر ردشدهٔ PostgreSQL و متن توضیح را چاپ می‌کند. ابتدا assertion عدم افشا شکست خورد؛ پس از خاموش‌کردن این logger تست سبز شد. دادهٔ حساسِ اجرای شکست در این سند کپی نشده است.

```text
[ERROR] Tests run: 6, Failures: 1, Errors: 0, Skipped: 0
[ERROR]   DraftFlowIT.nullableReplacementCanClearDraftAndAuditFailureRollsBackSubmit:101 failed Audit must not leak explanation to logs ==> expected: <false> but was: <true>
```

فرمان اصلاح: `./mvnw -Dit.test=DraftFlowIT verify`؛ exit 0، شش تست HTTP سبز. شکست Audit همچنان پاسخ sanitized 500 می‌دهد، کل رکورد درخواست rollback می‌شود و Audit صفر می‌ماند.

پارامترهای محیطی تمام اجراها فقط cache Maven را به `/tmp/changeflow-maven-user` و `/tmp/changeflow-m2` هدایت کرده‌اند. Integrationها با PostgreSQL 17.12 واقعی در Testcontainers و HTTP/Security/CSRF واقعی اجرا شدند. اجرای نهایی بدون انتخاب تست و بدون skip در [خروجی verify](us1-verify.txt) ثبت شده است.
