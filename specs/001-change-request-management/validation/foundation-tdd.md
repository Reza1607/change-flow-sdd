# شواهد Test-First Foundation

تاریخ ادامه: 2026-11-15. خطوط زیر از خروجی واقعی فرمان‌ها استخراج شده‌اند؛ هیچ خطای compile شاهد red محسوب نشده است. خروجی کامل اجرای نهایی در [foundation-verify.txt](foundation-verify.txt) است. خروجی‌های red شامل اطلاعات تشخیصی مربوط به fixture مصنوعی‌اند؛ فقط خطوط شمارش و علت شکست لازم نگه‌داری می‌شوند.

## Schema — پیش از V1

فرمان: `./mvnw -Dit.test=SchemaIT verify`؛ exit code=1.

```text
[ERROR]   SchemaIT.migratesEmptyDatabaseAndEnforcesConstraints:17 migration must create all three tables ==> expected: <3> but was: <0>
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```

## Security — پیش از adapter

فرمان: `./mvnw -Dit.test=SecurityIT verify`؛ exit code=1.

```text
[ERROR]   SecurityIT.authenticatedMutationNeedsMatchingCookieAndHeader:33 expected: <403> but was: <401>
[ERROR]   SecurityIT.authenticatesDatabaseAccountAndProvidesCookieWithoutSession:13 expected: <200> but was: <401>
[ERROR]   SecurityIT.missingAndInvalidCredentialsHaveUniformProblemAndChallenge:25 » NoSuchElement No value present
[ERROR] Tests run: 3, Failures: 2, Errors: 1, Skipped: 0
[INFO] BUILD FAILURE
```

## Problem mapper — پیش از پیاده‌سازی

فرمان: `./mvnw -Dtest=ProblemMappingTest test`؛ exit code=1.

```text
[ERROR]   ProblemMappingTest.standardProblemsHaveOnlySafeContractFields:14->lambda$standardProblemsHaveOnlySafeContractFields$0:15 » UnsupportedOperation not implemented
[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0
[INFO] BUILD FAILURE
```

## CurrentAccess — پیش از guard/writer

فرمان: `./mvnw -Dit.test=CurrentAccessIT verify`؛ exit code=1.

```text
[ERROR]   CurrentAccessIT.disabledAndMissingAccountsFailClosedAtGuard:72 Expected org.springframework.security.access.AccessDeniedException to be thrown, but nothing was thrown.
[ERROR]   CurrentAccessIT.readsFreshRolesRegardlessOfOldAuthenticationAuthoritiesAndPersistsAccount:30 expected: <true> but was: <false>
[ERROR]   CurrentAccessIT.sharedGuardBlocksWriterUntilCommitAndAllowsConcurrentReaders:53->lambda$sharedGuardBlocksWriterUntilCommitAndAllowsConcurrentReaders$5:53 expected: <true> but was: <false>
[ERROR] Tests run: 3, Failures: 3, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```

## Entity mapping — پیش از نگاشت

فرمان: `./mvnw -Dit.test=SecurityIT,CurrentAccessIT,EntityMappingIT verify`؛ exit code=1.

```text
[ERROR]   EntityMappingIT.allStatusesNullableTextsAndAuditRoundTripAgainstValidatedSchema:23->lambda$allStatusesNullableTextsAndAuditRoundTripAgainstValidatedSchema$0:25 request mapping must preserve all fields ==> expected: <ChangeRequest[id=3df25f82-bfb4-44f3-b8e9-5d63dc396e3a, title=null, description=شرح فارسی, reason=null, requesterId=f548f4ce-7963-4e37-805b-f2e18b773fda, status=Draft, version=0]> but was: <null>
[ERROR]   SecurityIT.authenticatedMutationNeedsMatchingCookieAndHeader » IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@2ac50ab0 testClass = com.changeflow.identity.SecurityIT, locations = [], classes = [com.changeflow.ChangeFlowApplication], contextInitializerClasses = [], activeProfiles = [], propertySourceDescriptors = [], propertySourceProperties = ["server.ssl.enabled=false", "server.ssl.bundle=",  …
[ERROR]   SecurityIT.authenticatesDatabaseAccountAndProvidesCookieWithoutSession » IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@2ac50ab0 testClass = com.changeflow.identity.SecurityIT, locations = [], classes = [com.changeflow.ChangeFlowApplication], contextInitializerClasses = [], activeProfiles = [], propertySourceDescriptors = [], propertySourceProperties = ["server.ssl.enabled=false", "server.ssl. …
[ERROR]   SecurityIT.corsIsClosedAndDisabledAccountCannotAuthenticate » IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@2ac50ab0 testClass = com.changeflow.identity.SecurityIT, locations = [], classes = [com.changeflow.ChangeFlowApplication], contextInitializerClasses = [], activeProfiles = [], propertySourceDescriptors = [], propertySourceProperties = ["server.ssl.enabled=false", "server.ssl.bundle=", " …
[ERROR]   SecurityIT.forbiddenAndErrorsNeverReflectExceptionDetails » IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@2ac50ab0 testClass = com.changeflow.identity.SecurityIT, locations = [], classes = [com.changeflow.ChangeFlowApplication], contextInitializerClasses = [], activeProfiles = [], propertySourceDescriptors = [], propertySourceProperties = ["server.ssl.enabled=false", "server.ssl.bundle=", "se …
[ERROR]   SecurityIT.matchingCsrfAllowsTestMutationAndIdentityOnlyComesFromContext » IllegalState Failed to load ApplicationContext for [WebMergedContextConfiguration@2ac50ab0 testClass = com.changeflow.identity.SecurityIT, locations = [], classes = [com.changeflow.ChangeFlowApplication], contextInitializerClasses = [], activeProfiles = [], propertySourceDescriptors = [], propertySourceProperties = ["server.ssl.enabled=false", "server.ssl.bundle=", "server.address=127.0.0.1", "spring.security.us …
[ERROR]   SecurityIT.missingAndInvalidCredentialsHaveUniformProblemAndChallenge » IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@2ac50ab0 testClass = com.changeflow.identity.SecurityIT, locations = [], classes = [com.changeflow.ChangeFlowApplication], contextInitializerClasses = [], activeProfiles = [], propertySourceDescriptors = [], propertySourceProperties = ["server.ssl.enabled=false", "server.ssl.b …
[ERROR] Tests run: 10, Failures: 1, Errors: 6, Skipped: 0
[INFO] BUILD FAILURE
```

## Logging/CORS — پیش از تکمیل

فرمان: `./mvnw -Dit.test=LogPrivacyIT,SecurityIT#corsIsClosedAndDisabledAccountCannotAuthenticate verify`؛ exit code=1.

```text
[ERROR]   LogPrivacyIT.logsStructuredOutcomeAndNeverLogsSensitiveInputOrCredentials:37 structured HTTP events must be present ==> expected: <true> but was: <false>
[ERROR]   SecurityIT.corsIsClosedAndDisabledAccountCannotAuthenticate:75 expected: <true> but was: <false>
[ERROR] Tests run: 2, Failures: 2, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```

## حدود شاهد و اصلاح‌های میانی

- Security red شامل دو assertion رفتاری (200/403 مورد انتظار در برابر 401 واقعی) و یک خطای نبود header است؛ همان assertionها شاهد red هستند.
- mapper ابتدا skeleton با UnsupportedOperationException داشت و اجرا شد؛ سپس نبود type صریح about:blank نیز با assertion آشکار شد. هر دو رفع شدند.
- در اجرای mapping red، assertion نگاشت درخواست (مدل مورد انتظار در برابر null) پیش از mapper شکست خورد. شش خطای context مربوط به ثبت دوبارهٔ controller تستی، شاهد red نگاشت نیستند؛ ثبت تستی اصلاح شد.
- اجرای اول suite پس از logging، نشت query از DispatcherServlet در DEBUG محیط را با assertion «sensitive content must not be logged» کشف کرد. سطح صریح لاگ‌های framework تنظیم شد و LogPrivacyIT نهایی با debug=true سبز است.
- خطای compile میانیِ محل تنظیم AccessDeniedHandler شاهد red نیست؛ طبق Javadoc رسمی CsrfConfigurer، handler از ExceptionHandlingConfigurer گرفته می‌شود و کد همان‌جا اصلاح شد. این اصلاحِ پیاده‌سازی بود؛ Plan نام API ناسازگاری را الزام نکرده است.
- Schema، Security، CurrentAccess، Entity mapping و privacy همگی در suite نهایی واقعی سبز شدند؛ هیچ stub باقی نمانده است. قواعد گردش کارِ US1/US2 در Foundation پیاده نشده‌اند.
