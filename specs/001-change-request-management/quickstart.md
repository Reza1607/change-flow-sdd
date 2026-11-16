# Quickstart و راهنمای اعتبارسنجی

این راهنما مسیر فعلی Build/Test و اجرای محلی پروژه است؛ pom.xml، Maven Wrapper، پروفایل dev، Migration و endpointها موجودند. verify و test-run در T058 تا T060 عملاً اجرا شده‌اند؛ نتیجهٔ clean verify و smoke فعلی در [گزارش نهایی](validation/final.md) ثبت شده است. پذیرش انسانی SC-006/T061 باز است.

## پیش‌نیاز

JDK 21، Docker-compatible runtime فعال، curl و jq. Maven Wrapper با توزیع 3.9.16 توسط پروژه تأمین می‌شود؛ نسخهٔ Spring Boot در pom.xml برابر 4.0.9 است. تست‌ها PostgreSQL 17.12 واقعی را با Testcontainers راه می‌اندازند؛ نصب DB محلی لازم نیست. دسترسی دریافت Maven artifacts و container image لازم است.

## Build و تست یک‌دست

از ریشهٔ پروژه:

```bash
./mvnw verify
./mvnw clean verify
```

انتظار: Unit Testها، architecture testها و API/Persistence Integration Testها، از جمله Migration از DB خالی، اجرا شوند؛ skip پیش‌فرض Integration Test مجاز نیست. Surefire نتیجهٔ unit و Failsafe نتیجهٔ integration را ثبت می‌کند. workflow CI برای همین verify با JDK 21 و container runtime پیکربندی شده است؛ خود CI در محیط اعتبارسنجی محلی اجرا نشده است.

## اجرای محلی

پروفایل dev از test-run و container DB استفاده می‌کند؛ beanهای Testcontainers و provision فقط در test source set هستند و در JAR production قرار نمی‌گیرند. سه کاربر `requester`، `reviewer` و `dual` از secret محلی با hash استاندارد provision می‌شوند؛ نقش‌هایشان به‌ترتیب Requester، Reviewer و هر دو است. کاربر Reviewer دوم برای تست رقابت در fixtureهای integration ساخته می‌شود. رمز ثابت یا credential قابل‌استفاده در repo ثبت نمی‌شود.

در یک terminal:

```bash
read -rsp 'Local development password: ' CHANGEFLOW_DEV_PASSWORD
export CHANGEFLOW_DEV_PASSWORD
./mvnw spring-boot:test-run -Dspring-boot.run.profiles=dev
```

پروفایل dev فقط روی 127.0.0.1:8080 bind می‌شود و اگر secret خالی باشد شروع نمی‌شود. HTTP محلی استثناء توسعه است؛ محیط اشتراکی TLS لازم دارد. پس از اجرا، secret را از shell پاک کنید:

```bash
unset CHANGEFLOW_DEV_PASSWORD
```

## چرخهٔ اصلی

در terminal دوم، curl با `-u username` رمز را تعاملی می‌پرسد تا رمز در تاریخچهٔ فرمان قرار نگیرد. cookie jarهای محلی را خارج Git با مجوز محدود نگه دارید:

```bash
umask 077
requester_cookie=$(mktemp)
reviewer_cookie=$(mktemp)
requester_csrf=$(curl -fsS -u requester -c "$requester_cookie" http://127.0.0.1:8080/api/v1/csrf | jq -r .token)
reviewer_csrf=$(curl -fsS -u reviewer -c "$reviewer_cookie" http://127.0.0.1:8080/api/v1/csrf | jq -r .token)
draft=$(curl -fsS -u requester -b "$requester_cookie" -H "X-XSRF-TOKEN: $requester_csrf" -H 'Content-Type: application/json' -d '{"title":"Improve review","description":"Make the review steps explicit","reason":"Reduce ambiguity"}' http://127.0.0.1:8080/api/v1/change-requests)
request_id=$(printf '%s' "$draft" | jq -r .id)
request_version=$(printf '%s' "$draft" | jq -r .version)
submitted=$(curl -fsS -u requester -b "$requester_cookie" -H "X-XSRF-TOKEN: $requester_csrf" -H 'Content-Type: application/json' -d "{\"expectedVersion\":$request_version}" "http://127.0.0.1:8080/api/v1/change-requests/$request_id/submit")
request_version=$(printf '%s' "$submitted" | jq -r .version)
curl -fsS -u reviewer -b "$reviewer_cookie" -H "X-XSRF-TOKEN: $reviewer_csrf" -H 'Content-Type: application/json' -d "{\"expectedVersion\":$request_version,\"explanation\":\"Clarify the impact\"}" "http://127.0.0.1:8080/api/v1/change-requests/$request_id/request-changes"
```

انتظار: Draft → PendingReview → ChangesRequested. Requester با GET همان درخواست نسخهٔ جدید و با GET `/history` توضیح بازگشت را می‌بیند. PUT با هر سه متن و expectedVersion برای اصلاح، سپس Submit و Approve توسط reviewer، مطابق [قرارداد](contracts/openapi.yaml)، چرخه را Approved می‌کند. پس از هر موفقیت version پاسخ را بگیرید؛ مقدار ثابت برای مراحل بعد ننویسید. secret و پاسخ حاوی دادهٔ واقعی را در issue/log اشتراکی paste نکنید.

پس از آزمایش:

```bash
rm -f "$requester_cookie" "$reviewer_cookie"
unset requester_csrf reviewer_csrf draft submitted
```

## جدول اعتبارسنجی

| سناریو | عمل | نتیجهٔ مورد انتظار |
| --- | --- | --- |
| Draft ناقص | ایجاد `{}`؛ سپس Submit با نسخهٔ جاری | ایجاد 201؛ Submit با 422 و errors فیلدها؛ وضعیت Draft، تاریخچه خالی |
| عنوان یکسان | ایجاد دو Draft با یک عنوان | دو شناسهٔ مستقل، بدون خطای duplicate |
| مالکیت | ویرایش/حذف Draft توسط دیگری | 404 چون نامرئی؛ بدون mutation |
| دو نقش | dual درخواست خود را Submit و سپس Approve/RequestChanges کند | هر دو 403 SELF_REVIEW_FORBIDDEN |
| اصلاح | RequestChanges غیرخالی؛ ویرایش مالک؛ Submit | توضیح قابل مشاهده، سه گذار با تاریخچهٔ محفوظ |
| تغییرناپذیری | مالک با نقش جاری Approved را PUT/DELETE کند | 409 INVALID_TRANSITION؛ بدون تغییر |
| تکرار | Submit دوباره در PendingReview؛ Approve دوباره توسط همان Reviewer موفق | 409 INVALID_TRANSITION؛ شمار Audit ثابت؛ DTO منبع منتشر نشود |
| دیده‌شدن | requester، reviewer، dual روی چهار وضعیت؛ list/detail/history | مطابق شرط Visibility؛ Draft دیگران و Approved دیگران برای Reviewer نامرئی |
| pagination | بیش از یک صفحه دادهٔ مجاز/نامجاز، مجموعهٔ ثابت | فقط مجازها، count صحیح، ترتیب ثابت، صفحهٔ خالی بدون خطا |
| خطای ورودی | JSON خراب، فیلد actorId اضافی، version مفقود | 400 Problem Details، بدون افشا |
| احراز هویت/CSRF | بدون credential؛ یا credential معتبر بدون CSRF | به‌ترتیب 401 و 403 با Problem Details |
| همزمانی | تست مستقل با دو connection و barrier | دقیقاً یک موفقیت؛ دیگری 409؛ نسخه و Audit سازگار؛ ۲۰ بار برای هر race |
| لغو نقش جاری | توقف عملیات قبل از guard؛ commit لغو؛ ادامه | 403، بدون mutation یا Audit؛ ترتیب معکوس مطابق A-006 |
| شکست Audit | خطای درج Audit پس از update در transaction | rollback وضعیت و Audit؛ هیچ وضعیت بی‌تاریخچه |

تست‌های رقابت و لغو نقش باید fixture کنترل‌شده داشته باشند؛ endpoint مدیریتی یا fault injection عمومی برای این تست‌ها ساخته نمی‌شود. نتایج تست‌های خودکار جایگزین SC-006 نیستند: ۵ کارمند باید چرخهٔ کامل را با Reviewer دیگر انجام و دلیل بازگشت را درست گزارش کنند؛ نتیجهٔ پذیرش دستی ثبت شود.
