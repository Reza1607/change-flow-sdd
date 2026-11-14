# ADR-002 — هویت قابل‌جایگزینی و مجوز جاری

تاریخ: 2026-11-14 | وضعیت: پیشنهادی در Plan؛ مرز زمانی در A-006 قابل بازبینی

## Context

FR-014 لغو نقش را برای عملیات بعدی و عملیات آغازشده ولی هنوز پذیرفته‌نشده لازم می‌داند. یک بار بررسی نقش در authentication بازهٔ race باقی می‌گذارد. مدیریت کاربران/نقش‌ها قابلیت محصول نیست، ولی adapter معتبر برای خواندن و تغییر کنترل‌شدهٔ آن لازم است.

## Decision

HTTP Basic روی HTTPS و PasswordEncoder استاندارد؛ بدون session احراز هویت. CurrentActor پشت Port از Security Context شناسه می‌گیرد؛ CurrentAccess نقش را از DB می‌خواند. CSRF فعال با CookieCsrfTokenRepository، endpoint احرازشدهٔ token و header/cookie برای mutation؛ CORS بسته. هیچ secret ثابت در Migration یا Git نیست.

یک ردیف پایدار برای مجوز هر actor باقی می‌ماند. پیش از پذیرش نهایی mutation، تراکنش guard اشتراکی این ردیف را می‌گیرد و enabled/role را مجدداً می‌خواند؛ تا commit نگه می‌دارد. تغییر نقش با guard انحصاری و expected accessVersion، در تراکنش مستقل commit می‌شود. خواندن fresh تحت READ COMMITTED انجام می‌شود و از first-level cache قبلی استفاده نمی‌کند. تمام writerهای نقش، از جمله ابزار provision عملیاتی، همین قرارداد را رعایت می‌کنند. شکست منبع مجوز fail closed است.

اثر لغو = commit موفق آن؛ اگر لغو زودتر commit شود عملیات جاری رد می‌شود؛ اگر عملیات guard را زودتر بگیرد، قبل از commit لغو تمام می‌شود. این الگو لغو از لحظهٔ اعلام intent یا بازپس‌گیری اثر commitشده را تضمین نمی‌کند. A-006 این مرز را صریح ثبت می‌کند؛ برداشت متفاوت محصول نیازمند بازبینی همین فرض است.

## Alternatives / Consequences

JWT نقش‌دار بدون مراجعهٔ سرور، cache مجوز و صرف recheck بدون قفل رد می‌شوند. session ابطال‌پذیر ممکن است اما پیچیدگی بیشتری دارد. guard فقط هویت را کوتاه هماهنگ می‌کند و جایگزین optimistic concurrency درخواست نیست. ابزار عملیاتی نقش نباید DB را بدون رعایت guard و نسخه تغییر دهد. read/list نیز نقش تازه می‌خواهند.

تست با barrier واقعی: عملیات پس از authentication متوقف، نقش در تراکنش دیگر لغو و commit، سپس ادامهٔ عملیات → 403 و بدون mutation/Audit. ترتیب معکوس: guard عملیات گرفته شده؛ لغو تا commit آن منتظر می‌ماند؛ پس از commit لغو تمام عملیات بعدی رد می‌شوند. دو connection مستقل و بدون sleep لازم‌اند.

منابع رسمی: [Spring Basic](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/basic.html)، [CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)، [PostgreSQL locks](https://www.postgresql.org/docs/17/explicit-locking.html).
