# API semantics

منبع قرارداد: [openapi.yaml](openapi.yaml). الزامات محصول: FR-001 تا FR-021 در Spec. این سند تصمیم فنی است و رفتار محصول را توسعه نمی‌دهد.

## هویت و امنیت

همهٔ مسیرها احراز هویت Basic می‌خواهند؛ TLS در محیط غیرمحلی ضروری است. هیچ actorId، requesterId، role یا status قابل‌نوشتنی در ورودی وجود ندارد؛ فیلد ناشناخته 400 است. `SecurityContext` تنها منبع هویت؛ نقش‌ها از CurrentAccess تازه خوانده می‌شوند. session authentication، login/logout endpoint و مدیریت کاربران در قرارداد نیست.

GET `/api/v1/csrf` با CookieCsrfTokenRepository یک cookie و token/headerName می‌دهد. mutationها cookie همان کلاینت و header برگشتی `X-XSRF-TOKEN` را لازم دارند. token endpoint نتیجهٔ no-store دارد. CSRF token و Authorization header در لاگ یا مخزن ثبت نمی‌شوند. CORS باز نیست و Basic به‌تنهایی مجوز خاموش‌کردن CSRF نیست.

## درخواست و پاسخ

- POST collection با `{}`، متن ناقص یا null Draft می‌سازد: 201، Location و DTO با version=0. بدنه الزامی است؛ عنوان تکراری مجاز.
- PUT جایگزینی کامل title/description/reason با expectedVersion است؛ برای پاک‌کردن متن null مجاز. در Draft و ChangesRequested ناقص‌بودن مانع ذخیره نیست. مالکیت و نقش Requester لازم است. پاسخ 200 با version افزوده؛ حتی ذخیرهٔ موفق با مقدار یکسان version را افزایش می‌دهد.
- DELETE Draft فقط با expectedVersion query → 204؛ حذف دوباره 404. تلاش حذف visible غیرDraft → 409 INVALID_TRANSITION؛ غیرمالک visible → 403.
- Submit و Approve بدنهٔ expectedVersion و explanation اختیاری دارند. RequestChanges explanation غیرخالی الزامی دارد. پاسخ موفق 200 با DTO نسخهٔ جدید؛ این DTO رسید همان عملیات مجاز است، حتی اگر Reviewer بعد از خروج درخواست از PendingReview دیگر حق GET مستقل نداشته باشد.
- هیچ endpoint مستقیم set-status وجود ندارد؛ Approved edit ممنوع و حذف آن نیز ممنوع است.
- لیست درخواست‌ها: page از صفر، size پیش‌فرض 20 و حداکثر 100؛ status اختیاری فقط فیلتر محدودکننده؛ sort ثابت id صعودی. همهٔ countها فقط موارد مجاز. صفحهٔ بعد از انتها 200 با items خالی؛ page/size نامعتبر 400.
- تاریخچه pagination مشابه، ترتیب resultingVersion صعودی؛ بدون edit/delete endpoint. تاریخچهٔ Draft خالی است.
- تاریخ‌ها ISO-8601 UTC؛ DTO درخواست، username، passwordHash یا جزییات داخلی JPA ندارد. همهٔ پاسخ‌های داده و خطا no-store هستند.

## تقدم کنترل‌ها و خطاها

Security chain ابتدا هویت و CSRF را بررسی می‌کند؛ بدون هویت 401 با Basic challenge، فقدان CSRF در mutation معتبر 403. parse/type/query نامعتبر 400. برای یک ورودی سالم، application به ترتیب مجوز جاری و visibility، مجازبودن عامل، وضعیت، نسخه و اعتبار محتوای دامنه را بررسی می‌کند. در guard نهایی، مجوز مجدداً خوانده می‌شود؛ هر تغییر بعدی نیز با نسخهٔ مورد انتظار محافظت می‌شود.

| HTTP | code | معنا |
| --- | --- | --- |
| 400 | BAD_REQUEST | JSON خراب، فیلد ناشناخته، پارامتر/نوع نامعتبر، expectedVersion مفقود |
| 401 | UNAUTHENTICATED | هویت نامعتبر یا مفقود |
| 403 | FORBIDDEN / SELF_REVIEW_FORBIDDEN / CSRF_INVALID | نقش لازم نیست یا لغو شده؛ مالکیت/عمل مجاز نیست؛ بررسی خود؛ CSRF نامعتبر |
| 404 | NOT_FOUND | درخواست ناموجود یا نامرئی، بدون تمایز بیرونی |
| 409 | INVALID_TRANSITION | وضعیت فعلی مبدأ مجاز نیست، شامل تکرار در مقصد |
| 409 | CONCURRENT_MODIFICATION | وضعیت هنوز مجاز ولی نسخه قدیمی، یا optimistic failure در write/delete |
| 422 | VALIDATION_FAILED | فیلدهای Submit یا توضیح RequestChanges خالی؛ errors فقط نام فیلد و کد، بدون بازتاب دادهٔ حساس |
| 500 | INTERNAL_ERROR | شکست غیرمنتظره؛ rollback و پیام عمومی |
| 503 | DEPENDENCY_UNAVAILABLE | دسترسی به منبع هویت/ذخیره‌سازی ممکن نیست؛ fail closed |

همهٔ خطاها `application/problem+json` با type، title، status و code هستند؛ detail و instance اختیاری و پاک‌سازی‌شده‌اند. Security filter و ControllerAdvice خروجی یکسان دارند؛ stacktrace، SQL، credential یا اطلاعات درخواست نامرئی افشا نمی‌شود. type=`about:blank` و title عنوان استاندارد HTTP؛ code برای تمایز ماشینی. نمونهٔ 409 در OpenAPI تعارض همزمانی را نشان می‌دهد؛ پاسخ 409 دیگر از code گذار نامعتبر استفاده می‌کند.

## تکرار، رقابت و لغو مجوز

Submit روی PendingReview و Approve روی Approved با 409 INVALID_TRANSITION و بدون Audit جدید رد می‌شوند، حتی اگر نسخهٔ ارسال‌شده قدیمی باشد. برای تکرار Approve توسط همان Reviewer موفق قبلی که همچنان نقش Reviewer دارد، تاریخچه صرفاً برای تشخیص همان عامل/عمل بررسی و خطای عمومی INVALID_TRANSITION بدون DTO، وضعیت یا جزئیات تاریخچه بازگردانده می‌شود؛ این رسید تکرار، دسترسی GET به درخواست یا تاریخچه ایجاد نمی‌کند. برای هر عامل دیگری، منبع نامرئی همچنان 404 است. لغو نقش بر این رسید نیز مقدم است. هیچ نتیجهٔ موفق مصنوعی و هیچ idempotency-key store ساخته نمی‌شود.

در race پس از خواندن، optimistic conflict → 409 CONCURRENT_MODIFICATION، نه retry خودکار. در race با لغو نقش، guard مطابق ADR-002 نقش را در نقطهٔ پذیرش نهایی می‌سنجد؛ لغو commitشدهٔ زودتر مانع mutation جاری می‌شود. fail هر قسمت گذار یا Audit کل transaction را rollback می‌کند.

مسیر تشخیص رسید تکرار تنها برای تطبیق عاملِ گذار موفق پیشین است و دادهٔ درخواست را منتشر نمی‌کند؛ تست مستقلی تضمین می‌کند این مسیر راهی برای مشاهدهٔ درخواست Approved دیگران نمی‌شود.


## ردیابی قرارداد

| Operation | Requirements |
| --- | --- |
| createDraft | FR-001، FR-002، FR-005، FR-014 |
| editRequest | FR-003، FR-014، FR-015 |
| deleteDraft | FR-008، FR-014، FR-015 |
| submitRequest | FR-004، FR-010، FR-011، FR-014 تا FR-016، FR-020 |
| approveRequest | FR-006، FR-007، FR-011، FR-014 تا FR-016، FR-020، FR-021 |
| requestChanges | FR-009، FR-011، FR-014 تا FR-016، FR-021 |
| listRequests / getRequest | FR-012 تا FR-014، FR-018 |
| listHistory | FR-016، FR-017، FR-018 |
| getCsrf و تمام خطاها | اصل امنیت Constitution و FR-019 |
