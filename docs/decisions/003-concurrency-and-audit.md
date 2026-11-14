# ADR-003 — نسخهٔ مورد انتظار و Audit اتمیک

تاریخ: 2026-11-14 | وضعیت: پیشنهادی در Plan

## Context

FR-015 از بازنویسی دادهٔ جدیدتر جلوگیری و FR-016 ثبت اتمیک گذار و تاریخچه را الزام می‌کند. تکرار Submit/Approve باید خطای گذار نامعتبر و بدون اثر جدید بدهد.

## Decision

ChangeRequest دارای version با JPA @Version است؛ کلاینت در هر edit/command/delete نسخهٔ مورد انتظار را می‌فرستد. ابتدا هویت/مجوز، سپس وضعیت، سپس نسخه بررسی می‌شود؛ stale write در وضعیت مجاز 409 CONCURRENT_MODIFICATION و تکرار در مقصد 409 INVALID_TRANSITION است. race زمان flush نیز به 409 ترجمه می‌شود؛ حذف باید شرط نسخه داشته باشد. create یک شناسهٔ جدید و version=0 دارد و چیزی را بازنویسی نمی‌کند.

application transaction دادهٔ درخواست و History را با هم می‌نویسد؛ commit قبل از پاسخ موفق، rollback هر دو در شکست. History شامل resultingVersion با unique(request_id,resulting_version) است؛ append-only و بدون REST mutation. هیچ پیام یا سرویس خارجی در مسیر تراکنش نیست.

تکرار Approve توسط همان عامل موفق قبلی با نقش جاری، حتی پس از خروج درخواست از visibility عمومی Reviewer، صرفاً رسید خطای عمومی می‌گیرد؛ هیچ DTO یا جزئیات منبع برنمی‌گردد و GET مستقل همچنان 404 است. سایر عامل‌های فاقد visibility، 404 می‌گیرند. این مرز برای برآورده‌کردن هم‌زمان FR-020 و سیاست دیده‌شدن در تست بررسی می‌شود.

## Alternatives / Consequences

If-Match صحیح به 412 نیاز دارد؛ برای الزام 409 از expectedVersion صریح استفاده می‌شود. last-write-wins و retry خودکار تصمیم رد می‌شوند. قفل انحصاری درخواست نیاز نیست؛ guard کوتاه مجوز در ADR-002 مستقل است. شکست Audit باید update وضعیت را rollback کند و با integration test واقعی اثبات شود.

منبع: [Jakarta Persistence Version](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/Version.html).
