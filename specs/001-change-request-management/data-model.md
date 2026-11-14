# Data Model — مدیریت درخواست تغییر

## ChangeRequest

| فیلد | نوع طراحی | قید و دلیل |
| --- | --- | --- |
| id | UUID | کلید اصلی و ترتیب ثابت فهرست |
| title | nullable text | Draft می‌تواند ناقص باشد؛ بدون unique constraint |
| description | nullable text | شرح تغییر؛ در Submit غیرخالی |
| reason | nullable text | دلیل تغییر؛ در Submit غیرخالی |
| requesterId | UUID | NOT NULL، هویت ثبت‌کننده ثابت؛ FK به هویت پایدار |
| status | enum متنی | فقط Draft، PendingReview، Approved، ChangesRequested |
| version | bigint | NOT NULL، مقدار اولیه 0؛ نسخهٔ فنی برای Optimistic Concurrency |

زمان ایجاد/به‌روزرسانی اضافه نمی‌شود، چون نیاز مشخصی در Spec ندارد. version فیلد فنی است و نیاز محصول جدید نیست. همهٔ متن‌ها Unicode؛ خالی‌بودن در Submit با بررسی code pointهای whitespace/space separator انجام شود. Draft و ChangesRequested می‌توانند در ویرایش ناقص ذخیره شوند؛ Submit دوباره اعتبارسنجی می‌کند. مقدار غایب یا null متن در ایجاد Draft مجاز است؛ edit از نوع جایگزینی کامل سه فیلد است.

مدل domain مستقل از Entity JPA است؛ version در adapter ذخیره و هنگام ذخیرهٔ مدل با expectedVersion مقایسه می‌شود. Entity با `@Version`؛ UPDATE و DELETE باید نسخه را در شرط داشته باشند. حذف bulk بدون version ممنوع. index روی (requester_id,id) و (status,id) برای queryهای دیده‌شدن؛ title index یکتا ندارد.

## IdentityAccount / CurrentAccess

| فیلد | قید |
| --- | --- |
| actorId | UUID ثابت، PK؛ پس از حذف نقش هم باقی می‌ماند |
| username | unique، برای authentication؛ در پاسخ درخواست تغییر منتشر نمی‌شود |
| passwordHash | یک‌طرفه با PasswordEncoder؛ هرگز در API یا لاگ |
| enabled | فعال‌بودن هویت |
| requester / reviewer | دو boolean برای دو نقش فعلی، هر دو یا هیچ‌کدام مجاز |
| accessVersion | bigint، optimistic concurrency برای تغییر تنظیم دسترسی |

محدودکردن نقش‌ها به همین دو boolean برای این Scope کافی است؛ RBAC framework عمومی ساخته نمی‌شود. provision کاربر و تغییر نقش مسیر عملیاتی کنترل‌شده‌اند، نه endpoint محصول. Migration فقط ساختار می‌سازد؛ هیچ password یا hash ثابت مشترکی در repo قرار نمی‌گیرد. FKها هویت را حفظ می‌کنند؛ حذف حساب فیزیکی در Scope نیست.

## StatusHistoryEntry

| فیلد | قید و دلیل |
| --- | --- |
| id | UUID، PK |
| requestId | FK درخواست، NOT NULL |
| actorId | FK هویت عامل، NOT NULL |
| occurredAt | timestamptz، زمان UTC از Clock سرور |
| fromStatus / toStatus | NOT NULL؛ فقط یکی از چهار زوج مجاز |
| explanation | text، برای RequestChanges غیرخالی؛ برای بقیه مقدار خالی مجاز |
| resultingVersion | bigint، نسخهٔ درخواست پس از گذار؛ برای ترتیب قطعی تاریخچه |

unique(request_id,resulting_version) مانع دو تاریخچه برای یک نسخه است. رکورد فقط append؛ هیچ update/delete endpoint ندارد. ترتیب تاریخچه resultingVersion است و ممکن است به علت edit بین دو گذار فاصله داشته باشد. FK درخواست RESTRICT؛ Draft حذف‌پذیر طبق قواعد اصلاً سابقهٔ گذار ندارد. متن explanation در پاسخ تاریخچهٔ مجاز نمایش داده می‌شود ولی در لاگ نوشته نمی‌شود.

## State transitions

| From | Command | To | Policy |
| --- | --- | --- | --- |
| Draft | Submit | PendingReview | مالک + نقش جاری Requester + عنوان/شرح/دلیل غیرخالی |
| ChangesRequested | Submit | PendingReview | همان شروط Submit |
| PendingReview | Approve | Approved | Reviewer جاری + actorId ≠ requesterId |
| PendingReview | RequestChanges | ChangesRequested | Reviewer جاری + actorId ≠ requesterId + توضیح غیرخالی |

edit فقط مالک دارای نقش Requester در Draft/ChangesRequested؛ delete فقط مالک دارای نقش Requester در Draft. Approved هیچ mutation مجازی ندارد. همهٔ ترکیب‌های دیگر رد می‌شوند.

## Visibility

شرط query برابر است با `(requesterRole AND requesterId=actorId) OR (reviewerRole AND status=PendingReview)`؛ هیچ نقش معتبر → منع دسترسی. Reviewer تنها که نقش Requester او لغو شده، مالکیت به‌تنهایی دسترسی خارج PendingReview نمی‌دهد. تاریخچه با همان شرط، در query join به request فیلتر می‌شود. فیلتر قبل از count/limit اعمال شود. منبع ناموجود و نامرئی هر دو 404؛ منبع قابل‌دیدن با عمل غیرمجاز 403.

## Transaction algorithm

1. هویت از CurrentActor؛ خواندن دادهٔ جاری و مجوز اولیه برای جلوگیری از افشا. دسترسی نامعلوم fail closed می‌شود.
2. دامنه مالکیت، عمل و وضعیت را بررسی کند؛ command تکراری در مقصد INVALID_TRANSITION است، سپس expectedVersion با نسخهٔ جاری مقایسه شود. Validation دامنه اعمال شود.
3. پیش از نوشتن، guard اشتراکی پایدار روی IdentityAccount گرفته و enabled و نقش لازم دوباره از DB خوانده شوند؛ cache یا snapshot authentication مبنا نیست. snapshot قبلی request با دادهٔ جدید silently جایگزین نمی‌شود.
4. ذخیره یا حذف با expectedVersion؛ در گذار، insert history با نسخهٔ نتیجه و زمان سرور در همان تراکنش. flush و commit قبل از پاسخ موفق؛ هر خطا rollback کامل.
5. guard تا commit باقی است. تغییر نقش با guard انحصاری روی همان ردیف، accessVersion مورد انتظار و commit انجام می‌شود؛ هیچ writer مجوزی نمی‌تواند guard را دور بزند. اگر چند actor لازم شد ترتیب actorId ثابت؛ در این نسخه یک actor است.

برای read/list/history نیز snapshot مجوز تازه در تراکنش کوتاه خواندن و guard اشتراکی تا پایان تهیهٔ نتیجه استفاده شود. این کار اجازهٔ دسترسی بعد از commit لغو را از بین می‌برد، اما بازپس‌گیری پاسخ از شبکه یا لغوِ عملیات پیش از commit لغو، تعهد نیست (A-006).

در رقابت روی درخواست، optimistic failure همیشه 409 است؛ اگر mutation رقیب request را حذف کرده باشد نتیجهٔ write قدیمی 409 و read تازه 404 خواهد بود. هیچ retry شفاف انجام نمی‌شود. هیچ lock طولانی در زمان تعامل کاربر، I/O بیرونی یا نمایش داده نگه داشته نمی‌شود.

## Migration strategy

V1 ساخت حساب/نقش، ChangeRequest، History، FK، checkها و indexها؛ نسخه‌های بعدی فقط Migration جدید. bootstrap هویت در ابزار/پروفایل عملیاتی کنترل‌شده از secret محلی، خارج Migration؛ test fixture مستقل و فقط test scope. تست Migration از DB خالی و validate schema، همراه با بررسی constraints الزامی است.
