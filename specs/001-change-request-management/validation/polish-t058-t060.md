# شواهد T058 تا T060 و تفسیر تأییدشدهٔ A-007

محدوده: T058 تا T060 و اصلاح ورودی A-007 با اجازهٔ صریح کاربر؛ T061 تا T064 اجرا نشده‌اند. Spec، semantics.md، OpenAPI و ADRها بدون تغییرند. این گزارش جایگزین پذیرش انسانی یا final.md نیست.

## وضعیت

- T058: [X]؛ dev bootstrap و اجرای واقعی test-run اعتبارسنجی شدند.
- T059: [X]؛ پس از verify نهایی سبز، با ۱۶۳ تست موفق ثبت شد.
- T060: [X]؛ ساختار workflow و فرمان README بررسی شدند. **CI اینجا اجرا نشده و ادعای CI سبز وجود ندارد.**

## T058 — dev فقط در test source set

DevApplication از `SpringApplication.from(...).with(...)` استفاده می‌کند. DevServicesConfiguration با TestConfiguration و پروفایل dev صریحاً import می‌شود؛ PostgreSQL 17.12 یک bean مدیریت‌شده با ServiceConnection است. سه کاربر requester/reviewer/dual با نقش‌های مربوط، شناسه‌های تصادفی و hash استاندارد PasswordEncoder از CHANGEFLOW_DEV_PASSWORD ساخته می‌شوند؛ secret غایب/خالی startup را رد می‌کند. bind غیر loopback و bind خالی رد می‌شوند. bootstrapها و application-dev.yaml در executable JAR نیستند؛ Start-Class تولید حفظ شده است. هیچ password ثابت یا Migration seed افزوده نشد.

شاهد قرمز اولیه (پس از skeleton بدون رفتار، compile موفق):

```bash
./mvnw -Dit.test=DevBootstrapIT -Dtest=NoTests -Dsurefire.failIfNoSpecifiedTests=false verify
```

```text
missingOrEmptySecretFailsStartup: Expected Exception to be thrown, but nothing was thrown.
provisionsOnlyThreeAccountsWithHashedExternalSecret: requester must be provisioned ==> expected: <1> but was: <0>
refusesNonLoopbackOverrideBeforeServing: Expected Exception to be thrown, but nothing was thrown.
Tests run: 3, Failures: 3, Errors: 0, Skipped: 0
BUILD FAILURE
```

exit=1. پس از پیاده‌سازی:

```bash
./mvnw -Dit.test=DevBootstrapIT,SetupPackagingIT -Dtest=NoTests -Dsurefire.failIfNoSpecifiedTests=false verify
```

۴ تست، صفر شکست/خطا/skip، BUILD SUCCESS، 26.630 s، exit=0. fixture اضافهٔ کانتینر ارث‌بری‌شده سپس حذف شد تا خود bean dev تنها دیتابیس این تست باشد؛ اجرای کامل آن را نیز بررسی می‌کند.

تست تکمیلی DevConfigurationTest برای bind خالی ابتدا رفتاری قرمز شد (۲ تست، ۱ شکست، صفر error، exit=1)، سپس guard مقدار blank را هم رد کرد و همان `./mvnw -Dtest=DevConfigurationTest test` با ۲ تست سبز و exit=0 در 3.176 s پایان یافت. این تست نبود کامل secret را هم پیش از ایجاد کانتینر بررسی می‌کند.

فرمان واقعی Boot 4.0.9 اجرا شد:

```bash
./mvnw spring-boot:test-run -Dspring-boot.run.profiles=dev
```

با secret تصادفی فقط در environment، startup و GET واقعی csrf و فهرست با هر سه کاربر 200 بود؛ no-store و اندازهٔ پیش‌فرض 20 بررسی شدند. harness خروج 0 داشت؛ فرآیند پس از smoke عمداً با SIGTERM متوقف شد (کد توقف 143). همان فرمان با حذف کامل secret از environment خروج 1 و پیام CHANGEFLOW_DEV_PASSWORD is required داشت. هدف test-run و property مربوط به profiles با descriptor واقعی پلاگین 4.0.9 و مستندات رسمی تطبیق داده شدند؛ ناسازگاری هدف/پارامتر مشاهده نشد.

## T059 — قرارداد و سابقهٔ قرمز

ApiContractIT هر ۱۰ operationId را با فایل واقعی OpenAPI بررسی می‌کند: مسیر و متد HTTP، schema ورودی‌های موفق و پاسخ‌ها، required/additionalProperties/type/enum/const/bounds/minLength، UUID و زمان UTC. Location، Basic challenge، cookie/token CSRF، content-type و no-store بررسی می‌شوند؛ 204 بدون بدنه/content-type است. unknown fields، JSON خراب و نوع نامعتبر 400؛ خطاهای 401/403/404/409/422، faultهای identity با 500/503 و رسید تکرار Approve بدون GET access پوشش دارند. faultها پس از lookup واقعی PostgreSQL با fixture قبلی ایجاد می‌شوند؛ snapshot، نسخه، Audit و تعداد درخواست‌ها ثابت می‌مانند.

| operationId | شاهد موفقیت و رفتار ویژه |
| --- | --- |
| getCsrf | 200، cookie و headerName |
| createDraft | 201، Location و مالک از هویت |
| editRequest | 200 و نسخهٔ جدید |
| deleteDraft | 204 و بدنهٔ خالی |
| submitRequest | 200 |
| approveRequest | 200، تکرار عامل قبلی 409 و GET او 404 |
| requestChanges | 200؛ توضیح Unicode whitespace برابر 422 |
| listRequests | 200 و صفحهٔ مطابق schema |
| getRequest | 200 مالک، 404 Reviewer پس از Approve |
| listHistory | 200، چهار Audit چرخه و زمان UTC |

تمام عملیات با 401/500/503 و تمام mutationها بدون CSRF با 403 بررسی شدند. سایر وضعیت‌ها را تست‌های قبلی نیز پوشش می‌دهند؛ این جدول ادعای دسترس‌پذیری تمام statusهای اعلام‌شده برای تمام عملیات نیست.

سه تست اولیهٔ قرارداد از ابتدا سبز بودند، چون endpointها قبلاً وجود داشتند. برای اثبات حساسیت تست، no-store موقتاً به public تغییر کرد؛ تست واقعاً با `expected: <no-store> but was: <public>` قرمز شد (۱ تست، ۱ شکست، exit=1، 21.486 s). در finally کد اصلی بازگردانده شد. سپس سه تست تقویت‌شده سبز شدند (exit=0، 32.177 s). این جهش موقت، فقدان قابلیت محصول نبود و به‌عنوان چنین چیزی ارائه نمی‌شود.

تست چهارم query خالی، پنج تفاوت واقعی پیدا کرد: List با page=/size=/status= و History با page=/size= پاسخ 200 داشتند. اجرای کامل پیش از اصلاح، در 2026-11-16T11:55:17+03:30 با ۶۶ تست Surefire سبز و ۹۶ تست Failsafe شامل یک تست ناموفق با پنج assertion، صفر error/skip، BUILD FAILURE، exit=1 و زمان 08:29 min پایان یافت. تست قرمز حفظ شد و اصلاح تا روشن‌شدن مبنا متوقف ماند.

## A-007 — تفسیر تأییدشده، نه حکم صریح قرارداد

بازبینی نشان داد هیچ خطی در semantics.md یا OpenAPI صریحاً نمی‌گوید query حاضر ولی خالی باید 400 باشد. معرفی اولیهٔ این انتظار به‌عنوان الزام صریح قرارداد دقیق نبود. کاربر در 2026-11-16 آن را **به‌عنوان تفسیر صریح** تأیید کرد؛ A-007 با دلیل و وضعیت «قابل بازبینی» در docs/assumptions.md ثبت شد. دلیل تأیید: خط ۲۴ semantics.md، parse/type/query نامعتبر را 400 می‌داند؛ page/size در OpenAPI integer با حدود و status یک enum است و رشتهٔ خالی این نوع‌ها را برآورده نمی‌کند. هیچ حکم صریح تازه‌ای به Spec یا قرارداد نسبت داده نمی‌شود.

قاعدهٔ تأییدشده: query حاضر ولی خالی برای page، size، status و expectedVersion برابر 400 BAD_REQUEST؛ query غایب رفتار قبلی را دارد. page/size غایب برابر 0/20، status غایب بدون فیلتر، و expectedVersion غایب همچنان به‌علت required بودن 400 است؛ برای آن پیش‌فرض ساخته نشده است. README تعداد ثابت فرض‌ها را از شش به هفت همسو کرد؛ docs/README.md تعداد ثابتی ندارد و تغییر نکرد.

### علت و اصلاح محدود ورودی

RequestParam با defaultValue، مقدار خالی page/size را پیش از اعتبارسنجی عددی به پیش‌فرض تبدیل می‌کرد؛ enum اختیاری status نیز مقدار خالی را به null تبدیل می‌کرد. QueryParameterValidationConfiguration در root وب با WebMvcConfigurer و HandlerInterceptor، پیش از binding مقدار خام چهار نام یادشده را در API درخواست‌های تغییر بررسی می‌کند. فقط مقدار empty رد می‌شود؛ هیچ trim یا قاعدهٔ جدید برای whitespace افزوده نشد. ServletRequestBindingException از mapper موجود به همان Problem Details با code=BAD_REQUEST، status=400، application/problem+json و no-store می‌رسد. Security/CSRF همچنان پیش از MVC اجرا می‌شوند؛ Controller، domain/application، مجوزها و تراکنش‌ها تغییر نکردند.

| ورودی | پیش از اصلاح | پس از اصلاح طبق A-007 |
| --- | --- | --- |
| List: page=/size=/status= | 200 و پیش‌فرض/بدون فیلتر | 400 BAD_REQUEST |
| History: page=/size= | 200 و پیش‌فرض | 400 BAD_REQUEST |
| History: status= | 200؛ نادیده گرفته می‌شد | 400 BAD_REQUEST طبق قاعدهٔ تأییدشده |
| DELETE: expectedVersion= | از قبل 400 | 400 با همان قالب؛ رفتار تازه‌ای لازم نداشت |
| List/History بدون page/size | 200 با 0/20 | بدون تغییر |
| DELETE بدون expectedVersion | 400 | بدون تغییر |

History پارامتر/فیلتر status در قرارداد ندارد؛ رد status خالی طبق درخواست صریح کاربر، فیلتر status جدیدی ایجاد نکرده است. سایر queryهای موجود فقط همین چهار نام‌اند. رفتار پارامترهای ناشناخته یا غیرخالی خارج از این قاعده تغییر نکرده است.

### تست دائمی و Test-First اصلاح

ApiContractIT و assertionهای قرمز آن بدون تغییر حفظ شدند. در PaginationIT تست دائمی presentEmptyQueriesFailWhileOmittedPaginationKeepsDefaults افزوده شد: شش query خالی List/History، DELETE خالی و غایب، پیش‌فرض‌های هر دو صفحه و صفر تغییر/Audit را با HTTP و PostgreSQL واقعی بررسی می‌کند.

قبل از اصلاح کد:

```bash
./mvnw -Dit.test=ApiContractIT#emptyQueryValuesAreNotValidIntegerOrStatusParameters,PaginationIT#presentEmptyQueriesFailWhileOmittedPaginationKeepsDefaults -Dtest=NoTests -Dsurefire.failIfNoSpecifiedTests=false verify
```

```text
ApiContractIT: Multiple Failures (5 failures)
PaginationIT: Multiple Failures (6 failures)
Tests run: 2, Failures: 2, Errors: 0, Skipped: 0
BUILD FAILURE
Total time: 38.176 s
```

exit=1؛ شکست رفتاری بود. همین اجرای قرمز ثابت کرد DELETE خالی و غایب از قبل 400 می‌دهند و پیش‌فرض‌های غایب برقرارند؛ شکست‌ها در assertAll شش query بعدی بودند.

پس از اصلاح:

```bash
./mvnw -Dit.test=ApiContractIT,PaginationIT,HistoryIT,DeleteDraftIT -Dtest=ArchitectureTest verify
```

```text
Surefire: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
Failsafe: Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 01:14 min
```

exit=0. page منفی، size=0/101، page/size غیرعددی و status ناشناخته در List از قبل در PaginationIT/ApiContractIT تست داشتند؛ HistoryIT نیز page/size منفی، خارج از حدود و غیرعددی را تست می‌کند. DeleteDraftIT برای نسخهٔ غایب/منفی/غیرعددی/overflow تست دارد. این تست‌ها بدون تضعیف یا افزودن سناریوهای نامرتبط دوباره اجرا شدند. status ناشناخته در History پارامتر تعریف‌شده‌ای نیست؛ رفتار غیرخالی آن تغییر نکرد و تست تازه‌ای برای آن ساخته نشد.

## T060 — CI و README

Workflow فقط build/test است: push و pull_request، runner Ubuntu، Temurin JDK21، docker info، فرمان دقیق ./mvnw verify و artifact گزارش‌های Surefire/Failsafe با always حتی در شکست. permissions فقط contents:read و checkout بدون persist credentials؛ بدون secret یا environment انتشار. ساختار با parser و assertion بررسی شد؛ فرمان README جداگانه با test-run واقعی smoke شد. **خود CI اجرا نشده است.**

## verify نهایی A-007

فرمان واقعی نهایی؛ فقط مسیر cache محلی Maven با environment تعیین شد و هیچ filter یا skip تست وجود نداشت:

```bash
MAVEN_USER_HOME=/tmp/changeflow-maven-user MAVEN_OPTS=-Dmaven.repo.local=/tmp/changeflow-m2 ./mvnw verify
```

خروج واقعی:

```text
[INFO] Tests run: 66, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 97, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time: 09:25 min
[INFO] Finished at: 2026-11-16T14:11:15+03:30
```

**exit code=0**؛ مجموع ۱۶۳ تست موفق، صفر شکست/خطا/skip. XMLهای Surefire و Failsafe جداگانه جمع زده شدند و اعداد بالا را تأیید کردند. تمام تست‌های قبلی Change Request، Identity/Authorization، معماری، رقابت‌های واقعی PostgreSQL، لغو نقش، dev و بسته‌بندی اجرا شدند. خروجی کامل آخرین اجرا در [polish-t058-t060-verify.txt](polish-t058-t060-verify.txt) ذخیره شد. T059 فقط پس از همین موفقیت [X] شد؛ T058/T060 کامل باقی ماندند و T061 تا T064 دست‌نخورده و بازند. CI اجرا نشده است.

## فایل‌ها و محدوده

فایل‌های همین اصلاح A-007:

- docs/assumptions.md
- README.md
- src/main/java/com/changeflow/web/QueryParameterValidationConfiguration.java (جدید)
- src/test/java/com/changeflow/changerequest/api/PaginationIT.java
- specs/001-change-request-management/tasks.md (فقط T059 پس از verify سبز)
- specs/001-change-request-management/validation/polish-t058-t060.md
- specs/001-change-request-management/validation/polish-t058-t060-verify.txt (خروج آخرین verify)

فایل‌های قبلی همین محدودهٔ T058 تا T060 نیز در working tree هستند: .github/workflows/ci.yml، DevApplication.java، DevServicesConfiguration.java، DevBootstrapIT.java، DevConfigurationTest.java، SetupPackagingIT.java، ApiContractIT.java و src/test/resources/application-dev.yaml. فهرست کامل این محدوده ۱۵ فایل است: ۱۰ فایل جدید و ۵ فایل تغییرکرده نسبت به HEAD.

گسترش محدودهٔ T059 از تقویت تست به اصلاح محدود ورودی و ثبت A-007 با اجازهٔ صریح کاربر انجام شد. dependency، endpoint، DTO، Migration یا تصمیم معماری جدید اضافه نشد؛ T061 تا T064 اجرا نشدند. Spec، semantics، OpenAPI و ADRها بدون تغییرند.

## منابع رسمی بررسی‌شده

- [Boot 4.0.9 و Testcontainers](https://docs.spring.io/spring-boot/4.0/reference/testing/testcontainers.html)
- [Maven plugin 4.0.9 و test-run](https://docs.spring.io/spring-boot/4.0/maven-plugin/run.html)
- [Boot 4.0.9 و WebMvcConfigurer بدون EnableWebMvc](https://docs.spring.io/spring-boot/4.0/reference/web/servlet.html)
- [ثبت MVC interceptor](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/interceptors.html)
- [setup-java v4](https://github.com/actions/setup-java/tree/v4)، [checkout v4](https://github.com/actions/checkout/tree/v4)، [upload-artifact v4](https://github.com/actions/upload-artifact/tree/v4)

### بررسی قابل‌تکرار ساختار workflow

```bash
python3 - <<'PY'
from pathlib import Path
import yaml
text=Path('.github/workflows/ci.yml').read_text()
w=yaml.safe_load(text)
assert set(w.get('on',w.get(True)))=={'push','pull_request'}
assert w['permissions']=={'contents':'read'}
j=w['jobs']['verify']
assert j['runs-on']=='ubuntu-24.04' and 'environment' not in j
steps=j['steps']
assert any(s.get('run')=='docker info' for s in steps)
assert any(s.get('run')=='./mvnw verify' for s in steps)
java=next(s for s in steps if s.get('uses','').startswith('actions/setup-java@'))
assert java['with']['java-version']=='21'
artifact=next(s for s in steps if s.get('uses','').startswith('actions/upload-artifact@'))
assert artifact['if']=='always()'
assert artifact['with']['path'].splitlines()==['target/surefire-reports/','target/failsafe-reports/']
assert 'secrets.' not in text and 'environment:' not in text
assert './mvnw spring-boot:test-run -Dspring-boot.run.profiles=dev' in Path('README.md').read_text()
print('PASS — structure only; CI not executed')
PY
```

بررسی ساختار محلی exit=0 داشت؛ PyYAML فقط ابزار بررسی بود و dependency پروژه نشد.
