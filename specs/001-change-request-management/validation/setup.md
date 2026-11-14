# Setup checkpoint — T001 تا T004

تاریخ: 2026-11-14. Spec، Plan و Tasks به‌صراحت توسط کاربر تأیید شدند؛ محدودهٔ مجاز فقط Setup است. Foundation اجرا نشده است.

## منابع رسمی و نسخه‌ها

- [Boot 4.0.9 / Java و Maven](https://docs.spring.io/spring-boot/4.0/system-requirements.html): Java 21 سازگار است؛ حداقل Maven برابر 3.6.3 است.
- [Boot starters و BOM](https://docs.spring.io/spring-boot/4.0/reference/using/build-systems.html) و [نسخه‌های مدیریت‌شده](https://docs.spring.io/spring-boot/4.0/appendix/dependency-versions/coordinates.html).
- [BOM منتشرشدهٔ 4.0.9](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.9/spring-boot-dependencies-4.0.9.pom) نیز مستقیماً دریافت و بررسی شد.
- [PostgreSQL 17.12](https://www.postgresql.org/docs/release/17.12/) با تاریخ انتشار 2026-11-12 مطابق Plan است؛ در Setup هیچ DB راه‌اندازی نشد.
- [Maven 3.9.16](https://maven.apache.org/download.cgi) برای Wrapper ثابت شد؛ [Wrapper رسمی](https://maven.apache.org/tools/wrapper/) نسخهٔ 3.3.4 با only-script تولید شد.
- [ArchUnit 1.5.1 و JUnit 6](https://www.archunit.org/userguide/html/000_Index.html#_junit_5_6).
- [پیکربندی رسمی Boot](https://docs.spring.io/spring-boot/4.0/appendix/application-properties/index.html) و [چرخهٔ Failsafe](https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html).

| وابستگی | نسخه / منبع |
| --- | --- |
| Boot parent و starters | 4.0.9 |
| PostgreSQL JDBC | 42.7.14، BOM |
| Flyway و PostgreSQL module | 11.14.1، BOM |
| JUnit Jupiter | 6.0.3، BOM |
| Spring Security | 7.0.8، BOM |
| Testcontainers | 2.0.5، BOM |
| ArchUnit JUnit 6 | 1.5.1، نسخهٔ رسمی صریح |

هیچ نسخهٔ مشخص‌شده در Plan تغییر نکرد. تنها استثنای عبارت کلی «نسخه‌ها از BOM» در T001، ArchUnit است که Boot آن را مدیریت نمی‌کند؛ نسخهٔ رسمی آن در pom.xml ثابت شد، بدون override وابستگی‌های BOM. نسخهٔ patch Maven طبق اختیار Plan انتخاب شد.

## بررسی‌ها و حدود شاهد

- تنها خط Status در Spec تغییر کرد؛ مقایسهٔ متن با HEAD پس از جایگزینی همین خط، برابر بود.
- وضعیت هر سه ADR پذیرفته‌شده شد و سه مدخل docs/README.md با آن‌ها تطبیق داده شد؛ A-006 همچنان قابل بازبینی است.
- Wrapper با `./mvnw --version`، Maven 3.9.16 و JDK 21.0.13.0.3 را گزارش کرد؛ exit code برابر 0.
- POM با parser XML بررسی شد: Java 21، parent 4.0.9، dependencyهای تحت BOM بدون version مستقل، Surefire برای *Test و Failsafe برای *IT، بدون skip پیش‌فرض.
- Packageهای feature تنها با package-info.java ساخته شدند؛ هیچ قواعد دامنه، endpoint، Security wiring، Entity، Migration یا adapter از Foundation ساخته نشده است.
- Clock استاندارد UTC در composition root قابل تزریق است؛ smoke compilation در verify انجام می‌شود.
- تنظیمات: Flyway فعال، Hibernate validate، open-in-view خاموش، TLS پیش‌فرض؛ dev تنها HTTP روی 127.0.0.1. اتصال DB از SPRING_DATASOURCE_URL/USERNAME/PASSWORD بیرونی یا ServiceConnection آتی تأمین می‌شود. در محیط مشترک CHANGEFLOW_TLS_BUNDLE و تعریف بیرونی bundle لازم است؛ secret یا certificate واقعی در مخزن نیست.
- تست no-store با servlet test doubles برای پاسخ داده/خطا انجام می‌شود؛ DB mock وجود ندارد. این شاهد جای API/Security integration واقعی در Foundation نیست.
- SetupPackagingIT در Failsafe، manifest executable JAR، حضور application/config و عدم بسته‌بندی کلاس تست را بررسی می‌کند؛ تست API/Persistence نیست.
- دریافت Maven artifacts در sandbox به‌علت DNS شکست خورد؛ build با شبکهٔ مجاز و cacheهای موقت اجرا شد. MAVEN_USER_HOME=/tmp/changeflow-maven-user و MAVEN_OPTS=-Dmaven.repo.local=/tmp/changeflow-m2 فقط محل cache را تعیین می‌کنند و هیچ تستی را skip نمی‌کنند.

## نتیجهٔ اجرا

`./mvnw verify` با همان cacheهای موقت: exit code **0**، BUILD SUCCESS.

- Surefire: 11 تست؛ failures=0، errors=0، skipped=0.
- Failsafe: 1 تست بسته‌بندی؛ failures=0، errors=0، skipped=0.
- اجرای نخست شامل دریافت وابستگی‌ها: 03:43 دقیقه، exit code 0.
- اجرای نهایی با cache موجود در sandbox: 5.333 ثانیه، exit code 0.
- [خروجی کامل اجرای نهایی](setup-verify.txt)، گزارش‌های ماشینی در target/surefire-reports و target/failsafe-reports؛ artifact در target/change-flow-0.0.1-SNAPSHOT.jar.
- git diff --check و بررسی XML/اسناد exit code 0 داشتند.
- T001 تا T004 با [X] ثبت شدند؛ T005 و تمام Taskهای Foundation باز مانده‌اند. شکست باقی‌مانده در Setup وجود ندارد؛ startup کامل و تست API/Persistence در محدودهٔ این checkpoint نیستند.

## فایل‌های ساخته‌شده

- `.dockerignore`
- `.gitignore`
- `.mvn/wrapper/maven-wrapper.properties`
- `mvnw`
- `mvnw.cmd`
- `pom.xml`
- `specs/001-change-request-management/validation/setup-verify.txt`
- `specs/001-change-request-management/validation/setup.md`
- `src/main/java/com/changeflow/ChangeFlowApplication.java`
- `src/main/java/com/changeflow/changerequest/api/package-info.java`
- `src/main/java/com/changeflow/changerequest/application/package-info.java`
- `src/main/java/com/changeflow/changerequest/domain/package-info.java`
- `src/main/java/com/changeflow/changerequest/infrastructure/package-info.java`
- `src/main/java/com/changeflow/identity/application/package-info.java`
- `src/main/java/com/changeflow/identity/domain/package-info.java`
- `src/main/java/com/changeflow/identity/infrastructure/package-info.java`
- `src/main/java/com/changeflow/web/NoStoreFilter.java`
- `src/main/resources/application.yaml`
- `src/test/java/com/changeflow/SetupPackagingIT.java`
- `src/test/java/com/changeflow/web/NoStoreFilterTest.java`
- `src/test/java/com/changeflow/web/SetupConfigurationTest.java`
