# Tasks: سامانه مدیریت درخواست‌های تغییر

**Input**: [Spec](spec.md)، [Plan](plan.md)، [Research](research.md)، [Data Model](data-model.md)، [OpenAPI](contracts/openapi.yaml)، [API semantics](contracts/semantics.md)، [Quickstart](quickstart.md) و Constitution 1.0.0.

**Prerequisites**: طراحی آماده است؛ پیش از نوشتن کد، تأیید Spec، Plan و Tasks طبق Constitution بررسی شود. فرض‌های A-001 تا A-007 در `docs/assumptions.md` قابل بازبینی‌اند. فرمان Tasks به‌تنهایی به معنی اجرای پیاده‌سازی یا تأیید خودکار اسناد نیست.

**Tests**: به الزام Constitution بخش ۷ و معیارهای پذیرش Spec، تست الزامی است. برای قواعد دامنه، ابتدا تست نوشته و شکست معنادار آن ثبت، سپس کد پیاده‌سازی و تست سبز شود؛ صرف خطای compile شاهد کافی رفتار نیست. هیچ Task بدون تست مرتبط یا بررسی مستنداتی قابل‌تکرار Done نیست.

**Organization**: اول Setup و Foundation؛ سپس P1ها به ترتیب US1، US2، US5 و P2ها به ترتیب US3، US4؛ شمارهٔ US مطابق Spec حفظ می‌شود. وضعیت checkboxها فقط با شواهد واقعی به‌روز می‌شود؛ پذیرش انسانی T061 جدا از تکمیل فنی باقی می‌ماند.

## Format: `[ID] [P?] [Story] Description`

`[P]` یعنی پس از تکمیل پیش‌نیازهای همان دسته، فایل‌ها مستقل‌اند؛ مجوز اجرای زودهنگام یا نادیده‌گرفتن وابستگی نیست. برچسب US فقط برای فاز داستان است. هر Task مسیر دقیق و FR یا Acceptance Scenario دارد.

## Path Conventions

همهٔ مسیرها نسبت به ریشهٔ مخزن‌اند؛ منبع Java زیر `src/main/java/com/changeflow/` و تست زیر `src/test/java/com/changeflow/` است. یک Maven module؛ domain بدون Spring/JPA، DTO جدا از Entity و Constructor Injection. نسخه‌ها طبق Plan، بدون تغییر Scope یا انتخاب معماری تازه.


## Phase 1: Setup (Shared Infrastructure)

**Purpose**: ساخت پروژه و یک مسیر اجرای تکرارپذیر؛ هیچ رفتار کسب‌وکار در این فاز اضافه نمی‌شود.

- [X] T001 پروژهٔ یک‌ماژولی Java 21 / Boot 4.0.9 و dependencyهای رسمی Plan، PostgreSQL driver، Flyway PostgreSQL، تست و ArchUnit را در `pom.xml` تعریف کنید؛ نسخه‌ها از BOM، build با Maven و بررسی dependency resolution (FR-019؛ Constitution قیدهای فنی).

- [X] T002 Maven Wrapper با patch ثابت از سری 3.9 در `mvnw`، `mvnw.cmd` و `.mvn/wrapper/maven-wrapper.properties` بسازید؛ `*Test` با Surefire و `*IT` با Failsafe در verify، بدون skip پیش‌فرض، در `pom.xml` تنظیم و اجرای Wrapper بررسی شود (SC-001، SC-003؛ Constitution بخش ۸).

- [X] T003 composition root و Clock تزریق‌پذیر را در `src/main/java/com/changeflow/ChangeFlowApplication.java` ایجاد کنید؛ packageهای feature طبق Plan و smoke compilation بررسی شود؛ domain به config وابسته نشود (FR-016؛ Constitution بخش ۵).

- [X] T004 تنظیمات بدون secret ثابت، Hibernate validate، Flyway فعال، open-in-view خاموش و پاسخ no-store را در `src/main/resources/application.yaml` و فایل‌های محلی در `.gitignore` تنظیم کنید؛ HTTP فقط dev loopback، TLS در محیط مشترک؛ بررسی پیکربندی (FR-014، FR-019).


## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: زیرساخت مشترک هویت، ذخیره‌سازی، Audit و خطا؛ تمام داستان‌ها به پایان این فاز وابسته‌اند. تست‌ها قبل از adapter متناظر نوشته و قرمز شوند. نوع‌های مشترک حداقلی در T014 و Portهای مصرف‌کننده در T015 تعریف می‌شوند؛ منطق دامنه فقط پس از تست قرمز US1 افزوده می‌شود.

**دلیل انتخاب I1**: تعریف شکل داده در Foundation، بدون انتقال منطق کسب‌وکار، وابستگی T014/T015 را با کمترین جابه‌جایی و بدون Task جدید رفع می‌کند.

- [X] T005 fixture مشترک PostgreSQL 17.12 با ServiceConnection، دو connection مستقل و کاربران بدون credential ثابت در `src/test/java/com/changeflow/support/PostgresTestSupport.java` بسازید؛ شروع container و اتصال واقعی را بررسی کنید (FR-014، FR-015، FR-016).

- [X] T006 [P] تست Schema از DB خالی، FK، status check، نسخه و unique Audit را پیش از Migration در `src/test/java/com/changeflow/changerequest/infrastructure/SchemaIT.java` بنویسید؛ عدم استفاده از H2/mock را بررسی کنید (FR-002، FR-005، FR-016).

- [X] T007 [P] تست authentication معتبر/نامعتبر، identity از Context، CSRF cookie/header، no-session و Problem برای 401/403 را در `src/test/java/com/changeflow/identity/SecurityIT.java` قبل از Security adapter بنویسید؛ فیلدهای هویتی ورودی قابل اعتماد نباشند (FR-002، FR-014، FR-019).

- [X] T008 Migration `src/main/resources/db/migration/V1__initial_schema.sql` را با قیود Data Model بسازید: idها UUID/PK؛ title/description/reason «nullable text» و بدون unique title؛ requesterId «NOT NULL»، ثابت و FK؛ status «فقط Draft، PendingReview، Approved، ChangesRequested»؛ version «NOT NULL، مقدار اولیه 0»؛ indexهای (requester_id,id)/(status,id). حساب actorId پایدار، username unique، passwordHash یک‌طرفه، enabled و دو نقش boolean، accessVersion؛ History با requestId/actorId و FK، occurredAt «timestamptz، زمان UTC از Clock سرور»، from/to «NOT NULL؛ فقط یکی از چهار زوج مجاز»، explanation «برای RequestChanges غیرخالی؛ برای بقیه مقدار خالی مجاز»، resultingVersion bigint و unique(request_id,resulting_version)، FK درخواست RESTRICT؛ بدون secret/seed و بدون timestamp اضافی درخواست؛ T006 سبز شود (FR-002، FR-005، FR-016).

- [X] T009 هویت و دو نقش را در `src/main/java/com/changeflow/identity/domain/ActorId.java`، `Role.java` و `AccessSnapshot.java`، و Portها را در `src/main/java/com/changeflow/identity/application/CurrentActor.java` و `CurrentAccess.java` تعریف کنید؛ بدون framework در domain؛ قرارداد guard تا پایان transaction و fail-closed مشخص باشد (FR-014).

- [X] T010 Entity حساب و adapter هویت جاری را در `src/main/java/com/changeflow/identity/infrastructure/IdentityAccountEntity.java`، `IdentityAccountRepository.java` و `DatabaseIdentityAdapter.java` بسازید؛ hash با PasswordEncoder، account پایدار پس از حذف نقش، هیچ نقش cacheشده مبنای مجوز نباشد؛ با T007 بررسی شود (FR-002، FR-014).

- [X] T011 Problem mapper مشترک را در `src/main/java/com/changeflow/web/ProblemResponses.java` و `src/main/java/com/changeflow/web/ApiExceptionHandler.java` بسازید؛ RFC 9457 و codeهای قرارداد برای 400/401/403/404/409/422/500/503؛ خطای 404 ناموجود/نامرئی یکسان، errors بدون دادهٔ حساس؛ ابتدا تست `src/test/java/com/changeflow/web/ProblemMappingTest.java` نوشته و قرمز، سپس mapper عمومی و این تست سبز شوند؛ فقط خطاهای استاندارد در این مرحله؛ نگاشت خطاهای feature پس از تعریف نوع‌هایشان در T024/T030 تکمیل و تست Security در T017 سبز شود؛ root وب به لایه‌های داخلی وابسته است و هیچ feature به آن وابسته نمی‌شود (FR-019).

- [X] T012 HTTP Basic بدون session authentication، CORS بسته، CSRF فعال با CookieCsrfTokenRepository و handlerهای Security با mapper مشترک T011 در Composition Root را در `src/main/java/com/changeflow/web/SecurityConfiguration.java` و endpoint token احرازشده را در `src/main/java/com/changeflow/web/CsrfController.java` بسازید؛ اتصال adapter هویت از طریق Port و پیکربندی root انجام شود؛ identity.infrastructure هیچ import از web یا changerequest.api نداشته باشد؛ cookie/header و Secure در TLS مطابق قرارداد؛ T007 سبز شود (FR-014، FR-019).

- [X] T013 تست fresh role، عدم اعتماد به authority قدیمی و contract تغییر نقش با accessVersion/قفل مشترک را در `src/test/java/com/changeflow/identity/CurrentAccessIT.java` بنویسید؛ سپس guard اشتراکی، re-read بدون cache و writer داخلی انحصاری را در `src/main/java/com/changeflow/identity/infrastructure/DatabaseCurrentAccess.java` و `AccessProvisioner.java` بسازید؛ writer بدون endpoint عمومی، commit مرز اثر A-006؛ تست‌ها سبز (FR-014، AS-22).

- [X] T014 ابتدا فقط شکل داده و امضای نوع‌های بدون وابستگی framework را در `src/main/java/com/changeflow/changerequest/domain/ChangeRequest.java`، `ChangeRequestStatus.java` و `StatusHistoryEntry.java` تعریف کنید؛ دقیقاً فیلدها/nullableها/چهار status در Data Model، بدون قواعد ایجاد/ویرایش/گذار؛ سپس Entityهای درخواست/تاریخچه و نگاشت دستی بین آن‌ها و این نوع‌ها را در `src/main/java/com/changeflow/changerequest/infrastructure/ChangeRequestEntity.java` و `StatusHistoryEntity.java` بسازید؛ قیود دقیق T008، @Version، resultingVersion و append-only؛ تست `src/test/java/com/changeflow/changerequest/infrastructure/EntityMappingIT.java` پیش از نگاشت نوشته و قرمز شود، سپس با تست schema تطابق DB واقعی و round-trip داده بررسی و سبز شود؛ Foundation فقط داده و نگاشت دارد و به پیاده‌سازی آیندهٔ US1 وابسته نیست (FR-002، FR-015، FR-016).

- [X] T015 Port ذخیره و Audit با قرارداد CAS روی update/delete و همان transaction را در `src/main/java/com/changeflow/changerequest/application/ChangeRequestStore.java` و `AuditTrail.java` پس از نوع‌های حداقلی T014 تعریف کنید؛ امضاها فقط همان نوع‌ها را مصرف کنند و در Foundation adapter کسب‌وکاری ساخته نشود؛ expectedVersion اجباری، شکست Audit rollback، بدون bulk delete بی‌نسخه؛ adapterها در داستان مربوط تکمیل می‌شوند (FR-015، FR-016).

- [X] T016 [P] تست مرزهای package، domain مستقل از Spring/JPA، وابستگی به داخل و عدم انتشار Entity در API را در `src/test/java/com/changeflow/architecture/ArchitectureTest.java` بسازید؛ featureها MUST NOT به `com.changeflow.web` وابسته باشند؛ root وب برای wiring و نگاشت خطا به لایه‌های داخلی وابسته می‌شود؛ identity.infrastructure به changerequest.api وابسته نباشد؛ فقط interfaceهای feature عمومی مجاز، Controller بدون Business Logic؛ پیش‌نیاز هر افزودن بعدی (FR-003، FR-011؛ Constitution بخش ۵).

- [X] T017 لاگ ساخت‌یافتهٔ correlationId/operation/outcome/duration بدون متن کسب‌وکار، token، Authorization یا secret را در `src/main/java/com/changeflow/changerequest/infrastructure/RequestLoggingFilter.java` و `src/main/resources/logback-spring.xml` تنظیم کنید؛ تست عدم افشا در `src/test/java/com/changeflow/changerequest/api/LogPrivacyIT.java`؛ T007/T011/T012 و suite Foundation سبز (FR-019).


## Phase 3: User Story 1 — آماده‌سازی و ارسال درخواست (Priority: P1) — MVP

**Goal**: Draft ناقص → اصلاح → Submit معتبر. **Independent Test**: Requester تنها با API واقعی درخواست ایجاد و اصلاح کند؛ Submit ناقص 422 و کامل PendingReview با یک Audit شود (AS-01 تا AS-05).

- [X] T018 [P] [US1] تست‌های Test-First ایجاد، مالکیت و ویرایش، nullable متن Draft، Submit و whitespace Unicode را در `src/test/java/com/changeflow/changerequest/domain/DraftRulesTest.java` بنویسید و پیش از پیاده‌سازی شکست رفتاری آن‌ها را مشاهده کنید؛ عنوان تکراری مجاز، requester ثابت، فقط Draft/ChangesRequested قابل ویرایش، PendingReview/Approved منع شوند (FR-001 تا FR-005، AS-01 تا AS-05).

- [X] T019 [P] [US1] تست HTTP واقعی createDraft، editRequest، submitRequest با قرارداد JSON، expectedVersion مفقود/منفی، unknown actorId/status، version=0، Location و error 422 را در `src/test/java/com/changeflow/changerequest/api/DraftFlowIT.java` بنویسید و پیش از پیاده‌سازی شکست رفتاری آن‌ها را مشاهده کنید؛ DB/Security واقعی و CSRF لازم (FR-001، FR-002، FR-003، FR-004، FR-005، FR-019).

- [X] T020 [US1] پس از تست قرمز T018، قواعد کسب‌وکاری مدل مستقل و status enum موجود از T014 را در `src/main/java/com/changeflow/changerequest/domain/ChangeRequest.java` و `ChangeRequestStatus.java` تکمیل کنید؛ نوع‌ها دوباره تعریف نشوند؛ title/description/reason nullable در Draft، id و requesterId ثابت، ویرایش فقط دو وضعیت مجاز؛ Submit با متن غیرخالی و بررسی code pointهای whitespace/space separator؛ T018 سبز شود (FR-001 تا FR-005).

- [X] T021 [US1] ذخیره‌سازی create/load/update با مقایسهٔ expectedVersion و @Version، افزایش نسخه حتی edit هم‌مقدار و نگاشت دستی را در `src/main/java/com/changeflow/changerequest/infrastructure/JpaChangeRequestStore.java` و `ChangeRequestRepository.java` تکمیل کنید؛ تست واقعی `src/test/java/com/changeflow/changerequest/infrastructure/RequestStoreIT.java` بدون overwrite بی‌صدا (FR-002، FR-003، FR-015).

- [X] T022 [US1] نوع دادهٔ تاریخچهٔ موجود از T014 و adapter append را در `src/main/java/com/changeflow/changerequest/domain/StatusHistoryEntry.java` و `src/main/java/com/changeflow/changerequest/infrastructure/JpaAuditTrail.java` تکمیل کنید؛ مدل پایه دوباره ساخته نشود؛ عامل/UTC/مبدأ/مقصد/توضیح/resultingVersion و قیود T008؛ تست `src/test/java/com/changeflow/changerequest/infrastructure/AuditStoreIT.java` با DB واقعی (FR-016).

- [X] T023 [US1] use case ایجاد/ویرایش/Submit Draft را در `src/main/java/com/changeflow/changerequest/application/DraftRequestService.java` و مرز مشترک تراکنش را در `RequestMutationExecutor.java` بسازید؛ CurrentActor، مجوز اولیه و guard نهایی تازه، domain rule، state سپس version سپس validation، mutation+Audit اتمیک؛ create version=0 و بدون Audit؛ تست دامنهٔ T018 سبز؛ T019 تا پیاده‌سازی endpoint در T024 قرمز می‌ماند و checkpoint کامل US1 در T026 است (FR-001، FR-002، FR-003، FR-004، FR-014، FR-015، FR-016).

- [X] T024 [US1] Records ورودی/خروجی مستقل، unknown-property rejection و controller POST/PUT/Submit را در `src/main/java/com/changeflow/changerequest/api/ChangeRequestDtos.java` و `DraftRequestController.java` بسازید؛ PUT جایگزینی هر سه فیلد با null مجاز، Submit expectedVersion و explanation اختیاری؛ نقش/قواعد فقط سرویس؛ نگاشت خطاهای تعریف‌شدهٔ US1 در `src/main/java/com/changeflow/web/ApiExceptionHandler.java` تکمیل شود؛ پس از T023 و این Task، T019 سبز (FR-001، FR-003، FR-004، FR-019).

- [X] T025 [US1] query مشاهدهٔ تکی با predicate نقش جاری/مالکیت و guard خواندن را در `src/main/java/com/changeflow/changerequest/application/GetRequestService.java`، `src/main/java/com/changeflow/changerequest/infrastructure/JpaRequestReader.java` و `src/main/java/com/changeflow/changerequest/api/RequestReadController.java` بسازید؛ GET فقط DTO و منبع پنهان 404؛ تست `src/test/java/com/changeflow/changerequest/api/GetRequestIT.java` (FR-012، FR-013، FR-014، FR-019، AS-14، AS-15).

- [X] T026 [US1] checkpoint US1 را در `specs/001-change-request-management/validation/us1.md` با اجرای تست‌های T018 تا T025 و مشاهدهٔ Draft ناقص/Submit نامعتبر/Submit معتبر، عنوان تکراری و نبود Audit شکست ثبت کنید؛ گزارش واقعی اجرای build و نه ادعای بدون اجرا (FR-001 تا FR-005، SC-001، SC-004).


## Phase 4: User Story 2 — بررسی و تأیید مستقل (Priority: P1)

**Goal**: تأیید PendingReview شخص دیگر، منع Self-Approval و حفظ Approved. **Independent Test**: fixture مستقیم PendingReview بدون نیاز به مسیر Create؛ یک Reviewer دیگر تأیید کند و ثبت‌کنندهٔ دو‌نقشه رد شود؛ failure Audit rollback شود (AS-06 تا AS-10).

- [X] T027 [P] [US2] تست Test-First ماتریس ۴ وضعیت × ۳ command را در `src/test/java/com/changeflow/changerequest/domain/TransitionRulesTest.java` بنویسید و پیش از پیاده‌سازی شکست رفتاری آن‌ها را مشاهده کنید؛ فقط چهار گذار Spec، همهٔ گذارهای دیگر از جمله same-state رد؛ شروط مالکیت/نقش، هویت متفاوت برای هر دو عمل بررسی و Approved ثابت؛ RequestChanges rule اکنون تست می‌شود ولی endpoint در US3 می‌آید (FR-006، FR-007، FR-009، FR-010، FR-011، FR-021، AS-07 تا AS-09، AS-23).

- [X] T028 [P] [US2] تست HTTP تأیید دیگری/منع خود دو‌نقشه، Approved edit و version، Audit کامل و تکرار Approve با نسخهٔ قدیمی/جاری را در `src/test/java/com/changeflow/changerequest/api/ApprovalIT.java` بنویسید و پیش از پیاده‌سازی شکست رفتاری آن‌ها را مشاهده کنید؛ تکرار همان عامل موفق 409 بدون DTO، GET بعدی 404، دیگر Reviewerها 404؛ تست `src/test/java/com/changeflow/changerequest/api/RepeatedSubmitIT.java` نیز اکنون نوشته و قرمز شود؛ توضیح Approve اختیاری (FR-006، FR-007، FR-011، FR-020، FR-021، AS-06 تا AS-08، AS-21).

- [X] T029 [US2] تابع گذار خالص و خطاهای domain را در `src/main/java/com/changeflow/changerequest/domain/TransitionPolicy.java` و `DomainFailure.java` بسازید؛ فقط زوج‌های مجاز، actorId ≠ requesterId برای هر دو command بررسی، nonblank explanation برای RequestChanges و nonblank سه متن برای Submit؛ T027 سبز (FR-004، FR-006، FR-007، FR-009، FR-010، FR-011، FR-021).

- [X] T030 [US2] Approve با executor مشترک، guard نهایی، CAS و Audit را در `src/main/java/com/changeflow/changerequest/application/ReviewRequestService.java` و endpoint را در `src/main/java/com/changeflow/changerequest/api/ReviewRequestController.java` بسازید؛ response فقط پس از commit، DTO رسید عملیات بدون ایجاد GET visibility؛ نگاشت خطاهای domain تعریف‌شده در T029 در `src/main/java/com/changeflow/web/ApiExceptionHandler.java` تکمیل شود؛ فقط زیرمجموعهٔ تأیید پایهٔ T028 سبز؛ کل T028 تا T031 قرمز می‌ماند و checkpoint US2 پس از T033 در T034 است (FR-006، FR-007، FR-014، FR-015، FR-016).

- [X] T031 [US2] تشخیص تکرار Approve همان actor موفق با نقش جاری از Audit و تکرار Submit مالک در مقصد را در `src/main/java/com/changeflow/changerequest/application/RepeatedTransitionPolicy.java` و query حداقلی در `src/main/java/com/changeflow/changerequest/infrastructure/StatusHistoryRepository.java` بسازید؛ state نامعتبر قبل از stale version، پاسخ 409 INVALID_TRANSITION بدون انتشار داده و بدون retry/اثر؛ پس از T030 و این Task، T028 و تست ازقبل‌قرمز `src/test/java/com/changeflow/changerequest/api/RepeatedSubmitIT.java` سبز (FR-014، FR-019، FR-020).

- [X] T032 [P] [US2] تست fault injection داخلیِ شکست insert Audit پس از update وضعیت با transaction واقعی و Clock ثابت را در `src/test/java/com/changeflow/changerequest/infrastructure/AuditAtomicityIT.java` بنویسید؛ assert rollback نسخه/وضعیت/تاریخچه و یک رکورد دقیق در موفقیت، بدون fault endpoint عمومی (FR-016، AS-10، SC-004).

- [X] T033 [US2] در صورت نیاز مرز rollback/flush/commit را در `src/main/java/com/changeflow/changerequest/application/RequestMutationExecutor.java` و `src/main/java/com/changeflow/changerequest/infrastructure/JpaAuditTrail.java` تکمیل کنید تا T032 سبز شود؛ هیچ خطای persistence به پاسخ موفق یا Audit ناقص تبدیل نشود (FR-015، FR-016، FR-019).

- [X] T034 [US2] checkpoint US2 شامل همهٔ ترکیب‌های transition، Self-Approval دو‌نقشه، Approved ثابت و Audit rollback را پس از سبزشدن تست‌ها در `specs/001-change-request-management/validation/us2.md` ثبت کنید؛ تست حذف Approved با endpoint حذف در T050/T054 تکمیل خواهد شد (FR-006، FR-007، FR-011، FR-016، SC-001، SC-002، SC-004).


## Phase 5: User Story 5 — رقابت، تکرار و لغو مجوز (Priority: P1)

**Goal**: هیچ overwrite خاموش یا پذیرش با نقش لغوشده رخ ندهد. **Independent Test**: با fixtureهای واقعی و دو connection مستقل، edit/edit و Approve/Approve و دو ترتیب لغو نقش آزموده شوند. تکمیل نهایی US5 به mixed race در T046 و delete race در T056 نیز وابسته است؛ phase priority این وابستگی را پنهان نمی‌کند.

- [X] T035 [P] [US5] تست دو connection/EntityManager مستقل و barrier بدون sleep را در `src/test/java/com/changeflow/changerequest/infrastructure/ConcurrentMutationIT.java` بنویسید؛ edit/edit، edit/Submit، Approve/Approve، نسخهٔ قدیمی HTTP و حتی edit هم‌مقدار؛ هر race ۲۰ بار، دقیقاً یک پذیرش و حفظ نسخه/داده/Audit (FR-015، AS-19، AS-20، SC-003).

- [X] T036 [P] [US5] تست لغو نقش با barrier پیش از guard نهایی در `src/test/java/com/changeflow/identity/InFlightRevocationIT.java` بنویسید؛ commit لغو قبل از ادامه → 403 و صفر اثر؛ guard زودتر → commit عملیات قبل از مؤثرشدن لغو؛ سپس همهٔ عملیات بعدی رد؛ یک نقش dual لغو شود و نقش دیگر مستقل بماند (FR-014، AS-22، A-006).

- [X] T037 [US5] مسیرهای CAS/flush و نگاشت optimistic failure به 409 CONCURRENT_MODIFICATION را در `src/main/java/com/changeflow/changerequest/infrastructure/JpaChangeRequestStore.java` و `src/main/java/com/changeflow/web/ApiExceptionHandler.java` با تست T035 تکمیل کنید؛ reload و retry شفاف تصمیم ممنوع؛ count Audit رقیب بازنده صفر (FR-015، FR-016، FR-019).

- [X] T038 [US5] guard shared در mutation و read، re-read نقش بدون first-level cache و writer exclusive با accessVersion را در `src/main/java/com/changeflow/identity/infrastructure/DatabaseCurrentAccess.java` و `AccessProvisioner.java` با تست T036 تکمیل کنید؛ ترتیب قفل identity قبل از write درخواست، enabled/role تازه، fail closed و بدون endpoint مدیریت نقش (FR-014، A-006).

- [X] T039 [P] [US5] تست end-to-end تکرار Submit/Approve و RequestChanges fixture، نسخهٔ جاری/قدیمی، prior actor و غیرactor و role لغوشده را در `src/test/java/com/changeflow/changerequest/api/IdempotencyIT.java` اضافه کنید؛ RequestChanges HTTP در T046 تکمیل می‌شود؛ بدون Audit جدید یا افشای DTO (FR-011، FR-014، FR-019، FR-020، AS-21).

- [X] T040 [US5] تست failure منبع مجوز/DB در `src/test/java/com/changeflow/identity/AuthorizationFailureIT.java` و مدیریت fail-closed در `src/main/java/com/changeflow/web/ProblemResponses.java` تکمیل کنید؛ نگاشت خطای identity در root وب انجام شود و adapter هویت فقط خطای داخلی/Port را برگرداند، بدون import از API feature یا web؛ پاسخ عمومی 503/500 متناسب، بدون commit یا خطای حاوی secret، و ثبات Problem در Security و API (FR-014، FR-019).

- [X] T041 [US5] نتایج واقعی raceها و دو ترتیب revocation را در `specs/001-change-request-management/validation/us5.md` ثبت کنید؛ T035/T036/T039/T040 سبز؛ وابستگی completion به T046 و T056 را باز نگه دارید و US5 پیش از آن‌ها Done اعلام نشود (FR-014، FR-015، FR-020، SC-003، SC-005).


## Phase 6: User Story 3 — اصلاح و ارسال مجدد (Priority: P2)

**Goal**: Reviewer دیگر با توضیح برگرداند، مالک دلیل را ببیند و اصلاح و ارسال مجدد کند. **Independent Test**: fixture PendingReview → RequestChanges → edit → Submit، حفظ کل تاریخچه و منع RequestChanges خود (AS-11 تا AS-13 و AS-23).

- [X] T042 [P] [US3] تست HTTP RequestChanges توضیح مفقود/خالی/Unicode whitespace و منع self-review dual را در `src/test/java/com/changeflow/changerequest/api/RequestChangesIT.java` بنویسید و پیش از پیاده‌سازی شکست رفتاری آن‌ها را مشاهده کنید؛ شرح معتبر 200 و یک Audit، نامعتبر 422 و صفر اثر، خود 403، در ماتریس فعلی A-001 و FR-020، عاملِ مصداق برای «تکرار مجاز برای مشاهده‌کننده 409» وجود ندارد؛ تکرار RequestChanges توسط Reviewer قبلیِ فاقد دید 404، توسط مالک دو‌نقشه 403، و شکست رقابت همزمان 409 است (FR-009، FR-011، FR-019، FR-021، AS-11، AS-12، AS-23).

- [X] T043 [P] [US3] تست چرخهٔ اصلاح، nullable متن در edit، Submit ناقص/کامل و تاریخچهٔ محفوظ با resultingVersion مرتب را در `src/test/java/com/changeflow/changerequest/api/ResubmissionIT.java` بنویسید و پیش از پیاده‌سازی شکست رفتاری آن‌ها را مشاهده کنید؛ تست `src/test/java/com/changeflow/changerequest/api/HistoryVisibilityIT.java` نیز پیش از reader تاریخچه نوشته و قرمز شود؛ غیرمالک/بدون نقش جاری مجاز نباشد و هر Submit موفق یک Audit (FR-003، FR-004، FR-010، FR-016، FR-017، AS-13).

- [X] T044 [US3] RequestChanges با توضیح nonblank و هویت متفاوت را در `src/main/java/com/changeflow/changerequest/application/ReviewRequestService.java` و `src/main/java/com/changeflow/changerequest/api/ReviewRequestController.java` تکمیل کنید؛ guard+version+Audit اتمیک؛ T042 سبز (FR-009، FR-014، FR-015، FR-016، FR-021).

- [X] T045 [US3] ویرایش ChangesRequested و ارسال مجدد با همان شروط مالکیت/نقش/Validation و transition policy را در `src/main/java/com/changeflow/changerequest/application/DraftRequestService.java` تکمیل کنید؛ بخش edit/Submit از T043 سبز؛ بخش مشاهدهٔ تاریخچه تا T047 قرمز می‌ماند و checkpoint کامل پس از T048 در T049 است؛ هیچ reset تاریخچه یا امکان حذف ChangesRequested اضافه نشود (FR-003، FR-008، FR-010، FR-016).

- [X] T046 [US3] mixed race واقعی Approve/RequestChanges و double RequestChanges را با دو Reviewer و دو connection مستقل، هر مورد ۲۰ بار، در `src/test/java/com/changeflow/changerequest/infrastructure/MixedReviewRaceIT.java` و تکرار HTTP RequestChanges را در `src/test/java/com/changeflow/changerequest/api/IdempotencyIT.java` تکمیل کنید؛ دقیقاً یک موفقیت/یک Audit، دیگری 409؛ تکرار برای عامل فاقد visibility مطابق قرارداد 404، بدون اثر (FR-009، FR-011، FR-015، FR-020، AS-19، SC-003).

- [X] T047 [US3] نمایش تاریخچهٔ مجاز شامل توضیح بازگشت را در `src/main/java/com/changeflow/changerequest/application/HistoryQueryService.java`، `src/main/java/com/changeflow/changerequest/infrastructure/JpaHistoryReader.java` و `src/main/java/com/changeflow/changerequest/api/HistoryController.java` بسازید؛ join visibility والد، actor/UTC/from/to/explanation/resultingVersion، صفحه‌بندی و append-only؛ تست `src/test/java/com/changeflow/changerequest/api/HistoryIT.java` ابتدا نوشته و قرمز شود؛ پس از reader و endpoint، T043 همراه این تست سبز شود (FR-016، FR-017، FR-018).

- [X] T048 [US3] تست history برای والد نامرئی، Reviewer پس از خروج از PendingReview، تاریخچهٔ Draft خالی، ترتیب با edit بین گذارها و خطای no-leak را در تست ازقبل‌قرمز T043 در `src/test/java/com/changeflow/changerequest/api/HistoryVisibilityIT.java` اجرا کنید و در صورت نیاز reader T047 را تکمیل کنید تا تست سبز شود؛ هیچ update/delete تاریخچه exposed نباشد (FR-012، FR-013، FR-017، FR-019، A-001 تا A-003).

- [X] T049 [US3] checkpoint US3 و mixed race تکمیل‌شده را در `specs/001-change-request-management/validation/us3.md` و `validation/us5.md` ثبت کنید؛ چرخهٔ واقعی و نمایش دلیل بازگشت، منع خود و حفظ تمام Auditها؛ وابستگی delete race US5 تا T056 باز می‌ماند (FR-009، FR-010، FR-016، FR-021، SC-001، SC-003، SC-004).


## Phase 7: User Story 4 — دسترسی، فهرست و حذف Draft (Priority: P2)

**Goal**: فهرست/مشاهده/تاریخچه فقط مطابق نقش و مالکیت، و حذف فقط Draft خود. **Independent Test**: دو مالک × دو نقش/dual/بدون نقش × چهار وضعیت؛ صفحه‌بندی، GET مستقیم، تاریخچه و delete، از جمله Approved و ChangesRequested (AS-14 تا AS-18).

- [X] T050 [P] [US4] تست HTTP حذف Draft مالک، غیرمالک، بدون نقش، تمام وضعیت‌های غیرDraft، version قدیمی/مفقود، حذف دوباره و Approved بدون اثر را در `src/test/java/com/changeflow/changerequest/api/DeleteDraftIT.java` بنویسید؛ Draft پنهان 404، عمل نامجاز visible 403، وضعیت نامجاز 409، version مفقود 400، موفق 204 بدون Audit (FR-008، FR-015، FR-019، AS-08، AS-16).

- [X] T051 [P] [US4] تست ماتریس کامل visibility و صفحه‌بندی list/detail/history، role dual و لغو یک نقش، status narrowing، count فقط مجاز، page خالی/نامعتبر را در `src/test/java/com/changeflow/changerequest/api/VisibilityMatrixIT.java` و `PaginationIT.java` بنویسید؛ حد size «پیش‌فرض 20 و حداکثر 100»، page از صفر، ترتیب id ثابت (FR-012، FR-013، FR-014، FR-017، FR-018، AS-14، AS-15، AS-17، AS-18).

- [X] T052 [US4] فهرست با predicate `(requesterRole AND requesterId=actorId) OR (reviewerRole AND status=PendingReview)`، فیلتر قبل از count/limit و status فقط محدودکننده را در `src/main/java/com/changeflow/changerequest/infrastructure/JpaRequestReader.java` و `src/main/java/com/changeflow/changerequest/application/ListRequestsService.java` بسازید؛ page/size مطابق T051 و sort فقط id صعودی (FR-012، FR-013، FR-018).

- [X] T053 [US4] DTO صفحه و GET collection را در `src/main/java/com/changeflow/changerequest/api/RequestReadController.java` و `ChangeRequestDtos.java` تکمیل کنید؛ totalElements/totalPages فقط دادهٔ مجاز، query invalid 400؛ T051 سبز، query GET مستقیم و history همان visibility را حفظ کنند (FR-012، FR-013، FR-017، FR-018، FR-019).

- [X] T054 [US4] delete با مالک دارای نقش جاری Requester و فقط Draft را در `src/main/java/com/changeflow/changerequest/application/DeleteDraftService.java` و `src/main/java/com/changeflow/changerequest/api/DraftRequestController.java` بسازید؛ guard نهایی، expectedVersion query، DELETE version-checked در `JpaChangeRequestStore.java`، بدون Audit؛ T050 سبز (FR-008، FR-014، FR-015).

- [X] T055 [US4] منع مالکیت به‌تنهایی پس از لغو Requester، read/list/history با نقش تازه و guard خواندن، و پیام یکسان 404 نامرئی/ناموجود را در `src/main/java/com/changeflow/changerequest/application/GetRequestService.java`، `ListRequestsService.java` و `HistoryQueryService.java` با T051 و `src/test/java/com/changeflow/identity/InFlightRevocationIT.java` تکمیل کنید (FR-012، FR-013، FR-014، FR-017، FR-019، A-001، A-006).

- [X] T056 [US4] race edit/delete و Submit/delete و delete/delete با دو connection، نسخهٔ مورد انتظار و هر مورد ۲۰ اجرا را در `src/test/java/com/changeflow/changerequest/infrastructure/DeleteRaceIT.java` بنویسید و adapter حذف را سبز کنید؛ فقط یک نتیجه پذیرفته، stale write 409، read بعد از حذف 404، هیچ Audit یتیم؛ شواهد نهایی US5 در `specs/001-change-request-management/validation/us5.md` به‌روز شوند (FR-008، FR-015، FR-016، AS-20، SC-003).

- [X] T057 [US4] checkpoint US4 با تست کامل نقش/مالکیت/وضعیت، pagination و حذف را در `specs/001-change-request-management/validation/us4.md` ثبت کنید؛ US5 فقط پس از سبزشدن T046 و T056 تکمیل اعلام شود (FR-008، FR-012، FR-013، FR-017، FR-018، SC-002، SC-005).


## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: تطابق نهایی، اجرای Quickstart، پذیرش و مستندات؛ به تمام داستان‌ها وابسته است.

- [X] T058 dev test-run با PostgreSQL واقعی و provision فقط در test source set را در `src/test/java/com/changeflow/DevApplication.java` و `DevServicesConfiguration.java` و `src/test/resources/application-dev.yaml` بسازید؛ کاربران requester/reviewer/dual، secret از CHANGEFLOW_DEV_PASSWORD، فقدان secret خطای startup، hash استاندارد، فقط loopback؛ عدم بسته‌بندی bootstrap در JAR را تست کنید (FR-002، FR-014، FR-019؛ quickstart.md).

- [X] T059 تطابق هر ۱۰ operationId با OpenAPI، status/body/header/content-type، unknown fields، 400/401/403/404/409/422 و CSRF/no-store را در `src/test/java/com/changeflow/changerequest/api/ApiContractIT.java` بررسی کنید؛ 500/503 faultها و رفتار prior Approve actor بدون GET access پوشش داده شوند (FR-001 تا FR-021؛ contracts/openapi.yaml).

- [X] T060 [P] CI با JDK 21 و container runtime و همان `./mvnw verify` را در `.github/workflows/ci.yml` و Quickstart عملی را در `README.md` ثبت کنید؛ گزارش Surefire/Failsafe نگه‌داری شود، هیچ secret یا محیط انتشار اضافه نشود؛ بررسی workflow و smoke دستور README (SC-001، SC-003؛ Constitution بخش ۸).

- [ ] T061 ۵ کارمند و Reviewer دیگر باید SC-006 را با چرخهٔ واقعی انجام دهند و دلیل بازگشت را شناسایی کنند؛ نتیجهٔ واقعی و تعداد موفقیت در `specs/001-change-request-management/validation/acceptance.md` ثبت شود؛ تا حضور افراد و اجرای واقعی، این Task باز بماند و نتیجه جعل یا با تست خودکار جایگزین نشود (SC-006، AS-11، AS-13).

- [X] T062 README، AGENTS و راهنمای اجرا را با `specs/001-change-request-management/quickstart.md` تطبیق دهید، فرض‌های A-001 تا A-007 در `docs/assumptions.md` را بازبینی و ADRهای `docs/decisions/001-runtime-and-storage.md`، `002-identity-and-live-authorization.md` و `003-concurrency-and-audit.md` را مطابق نتیجهٔ واقعی تکمیل کنید؛ هیچ رفتار تازه بدون اصلاح Spec اضافه نشود؛ لینک‌ها و سازگاری بررسی شوند (FR-014، FR-015، FR-020، FR-021؛ Constitution بخش ۱ و ۸).

- [X] T063 Quickstart را از محیط تمیز اجرا و `./mvnw verify` کامل را پس از ادغام همهٔ داستان‌ها انجام دهید؛ نتیجه، شمار اجرای race، schema migration، privacy/architecture و قیود بدون mock DB در `specs/001-change-request-management/validation/final.md` ثبت شود؛ failureها قبل از Done رفع و SC-006 جدا ارجاع داده شود (SC-001، SC-002، SC-003، SC-004، SC-005).

- [X] T064 ممیزی نهایی ردیابی FR-001 تا FR-021 و AS-01 تا AS-23 و DoD را در `specs/001-change-request-management/tasks.md` و `validation/final.md` ثبت کنید؛ فقط کارهای واقعاً انجام‌شده و تست‌دار تیک بخورند؛ Spec برآورده، تست‌ها سبز، فرض‌ها و مستندات به‌روز؛ پذیرش انسانی باز مانده باشد، completion کامل گزارش نشود (SC-001 تا SC-006؛ Constitution بخش ۹).


## Dependencies & Execution Order

### Phase Dependencies

```text
Setup T001–T004
  → Foundation T005–T017
    → US1 T018–T026
      → US2 T027–T034
        → US5 T035–T041 (هستهٔ رقابت؛ completion هنوز باز)
          → US3 T042–T049 (mixed race T046)
            → US4 T050–T057 (delete race T056)
              → Polish T058–T064
```

ترتیب بالا مسیر پیش‌فرض اجرایی است؛ وابستگی فنی و قابلیت اجرای موازی محدود در پایین مشخص شده‌اند. صرف قرارگرفتن در همان فاز، استقلال همهٔ Taskها نیست.

### User Story Dependencies

| Story | پیش‌نیاز فنی | معیار تست مستقل / checkpoint |
| --- | --- | --- |
| US1 | Foundation | ایجاد ناقص، edit خود، Submit ناقص/کامل و عنوان یکسان؛ T026 |
| US2 | مدل، ذخیره و executor در US1؛ fixture می‌تواند PendingReview مستقیم بسازد | Approve دیگری، منع self، چهار وضعیت و rollback Audit؛ T034 |
| US5 | US1+US2 برای مسیرهای edit/Submit/Approve؛ completion نیازمند T046 و T056 | دو connection، ۲۰ تکرار هر race، دو ترتیب revocation؛ T041 و تکمیل T057 |
| US3 | transition policy و Review service از US2؛ edit/Submit از US1 | برگشت با توضیح، اصلاح، ارسال مجدد، تاریخچه محفوظ و منع self؛ T049 |
| US4 | GET از US1؛ History reader از US3 | همهٔ ترکیب‌های role/owner/state، count صفحات و delete؛ T057 |

استقلال تست به معنی نبود وابستگی تولیدی نیست: fixture از وضعیت موردنیاز شروع می‌کند و سناریو به اجرای زنجیره‌ای تست دیگری متکی نیست. هیچ داستانی برای پاس‌کردن تست به endpoint خارج از Scope نیاز ندارد.

### پیش‌نیازهای دقیق و checkpointهای اصلاح‌شده

| کار | پیش‌نیاز | زمان سبزشدن تست |
| --- | --- | --- |
| T011 | T007 برای نوشتن تست Security؛ تست mapper در خود T011 قبل از کد | mapper عمومی در T011؛ Security پس از T012/T013 و در T017 |
| T014 → T015 | Schema در T008؛ نوع‌های صرفاً داده‌ای در T014 پیش از نگاشت و Port | round-trip نگاشت در T014؛ هیچ منطق US1 در Foundation لازم نیست |
| T018 → T020 → T021/T022 → T023 → T024 | T015 و شکل دادهٔ T014؛ T019 پیشاپیش قرمز | domain در T020؛ HTTP پس از T024؛ checkpoint در T026 |
| T027/T028 → T029 → T030 → T031 | US1؛ تست RepeatedSubmit در T028 قرمز | تأیید پایه در T030؛ کل T028/RepeatedSubmit پس از T031؛ Audit پس از T033؛ checkpoint T034 |
| T042/T043 → T044/T045 → T047 → T048 | US2؛ T046 تست mixed race مستقل پس از T044/T045 | edit/Submit در T045؛ تاریخچه پس از T047؛ visibility پس از T048؛ checkpoint T049 |

شناسه‌ها و تعداد Taskها ثابت‌اند. جدول، پیش‌نیاز و زمان تکمیل را مشخص می‌کند؛ هیچ شرط سبزشدن زودتر از endpoint یا adapter لازم وجود ندارد. T046 و T056 همچنان پیش‌نیاز تکمیل نهایی US5 هستند.

### Within Each Story

تست مرتبط → مشاهدهٔ شکست رفتار → domain → adapter/service → endpoint → تست DB/API واقعی → checkpoint. خطای تغییر نسخه، فقدان مجوز و شکست Audit نباید با mock DB پنهان شوند. قبل از اجرای تست وابسته، زیرساخت موردنیاز آن باید ساخته شده باشد. فایل‌های مشترک مانند `RequestMutationExecutor.java`، Controller و adapterها ترتیبی و با بازبینی تغییر می‌کنند.

### Parallel Opportunities

| گروه | Taskهای مستقل پس از پیش‌نیاز |
| --- | --- |
| Foundation | T006 و T007 پس از T005؛ T016 پس از T003 و dependency تست T001/T002 |
| US1 | T018 و T019 پس از Foundation؛ فایل تست domain و HTTP جدا |
| US2 | T027 و T028 پس از US1؛ T032 پس از T030 در فایل مستقل |
| US5 | T035 و T036 پس از US2؛ T039 پس از T031 و قبل از تکمیل T046 |
| US3 | T042 و T043 پس از US2؛ بدون تغییر هم‌زمان Review/Draft service |
| US4 | T050 و T051 پس از US3؛ ماتریس read و delete مستقل |
| Polish | T060 پس از تثبیت runtime و T058؛ مستقل از فایل تست قرارداد T059 |

علامت [P] فقط برای این فرصت‌هاست؛ اجرای هم‌زمان writerهای یک فایل یا استفاده از schema/dataset مشترک تغییرپذیر مجاز نیست. در اجرای تست موازی، isolation داده/تراکنش برای هر تست حفظ شود.

## Parallel Example: User Story 1

پس از Foundation: T018 در `DraftRulesTest.java` و T019 در `DraftFlowIT.java` قابل انجام هم‌زمان‌اند؛ سپس T020 تا T025 به ترتیب وابستگی. دو تست روی فایل‌های مستقل‌اند؛ تولید هم‌زمان کد مشترک توصیه نمی‌شود.

## Parallel Example: User Story 2

T027 در `TransitionRulesTest.java` و T028 در `ApprovalIT.java`؛ پس از T030، T032 در `AuditAtomicityIT.java` مستقل از کار روی policy تکرار T031 است. ادغام executor در T033 ترتیبی انجام شود.

## Parallel Example: User Story 5

T035 در `ConcurrentMutationIT.java` و T036 در `InFlightRevocationIT.java`؛ هرکدام container dataset مستقل و connectionهای واقعی خود را دارند. اصلاح adapter درخواست T037 و adapter هویت T038 پس از تست متناظر انجام شوند.

## Parallel Example: User Story 3

T042 در `RequestChangesIT.java` و T043 در `ResubmissionIT.java`؛ هر دو بر fixture PendingReview مستقل اجرا می‌شوند. T044/T045 فایل سرویس جدا دارند، اما executor مشترک نباید هم‌زمان بازنویسی شود.

## Parallel Example: User Story 4

T050 در `DeleteDraftIT.java` و T051 در `VisibilityMatrixIT.java`/`PaginationIT.java`؛ T052/T053 ابتدا قرارداد reader را تثبیت و سپس تغییر serviceهای مشترک T055 ادغام کند.

## Implementation Strategy

### MVP First (User Story 1 Only)

Setup + Foundation + US1 یک MVP قابل نمایشِ آماده‌سازی و ارسال است؛ فقط Draft/Submit را ارائه می‌دهد و محصول کامل بررسی مستقل محسوب نمی‌شود. checkpoint T026 باید سبز باشد؛ این فهرست هیچ deploy/publish خودکاری درخواست نمی‌کند.

### Incremental Delivery

پس از MVP، US2 تأیید مستقل را اضافه می‌کند؛ US5 رقابت و لغو نقش را با شاهد واقعی می‌سنجد؛ US3 چرخهٔ اصلاح و mixed race و US4 دیده‌شدن کامل و delete race را تکمیل می‌کنند. هیچ mutation موقتاً بدون guard/version/Audit لازم عرضه نمی‌شود؛ این‌ها از executor مشترک US1 برقرارند. معیار محصول کامل، تمام ۵ داستان و Polish است.

### Definition of Done

هر checkbox تنها با تحقق رفتار، تست مرتبط سبز و مستندات سازگار تیک می‌خورد. اعداد T/FR/AS بدون تغییر شناسه حفظ شوند. A-006 هنوز «قابل بازبینی» است؛ اگر معنای لغو از intent موردنظر شد، ابتدا Spec/Plan/ADR اصلاح و سپس Task مربوط بازنویسی شود. Task T061 وابسته به مشارکت واقعی افراد است؛ نبود افراد مجوز ساخت شاهد پذیرش نیست.


## ممیزی پایانی T062 تا T064 — 2026-11-16

وضعیت اعلام‌شده: **کامل از نظر فنی، پذیرش انسانی SC-006 باز**. T061 باز می‌ماند؛ ممیزی فنی یا smoke خودکار جایگزین ۵ کارمند واقعی نیست و DoD کل پروژه بسته اعلام نمی‌شود.

دامنهٔ اجرای T062 مطابق دستور فعلی شامل README، AGENTS، Quickstart و بازبینی A-001 تا A-007 بود. به دستور صریح کاربر، Spec، قراردادها و متن ADRها محفوظ ماندند؛ نتیجهٔ بازبینی ADRها و کمبود دلیل مستقل CSRF در ADR-002 در final.md گزارش شد. توضیح بی‌مصداق T042 از قبل در همین فایل و validation/us3.md موجود است و دوباره تغییر نکرد.

[گزارش کامل، خروج واقعی clean verify، شمار raceها و DoD](validation/final.md) مرجع این ممیزی است. ردیابی زیر عطف‌های صریح AS→FR در Spec را ثبت می‌کند؛ methodهای دقیق و حدود پوشش در جدول‌های کامل final.md آمده‌اند و با XML همین اجرا تطبیق داده شدند.

| FR | ASهای مرتبط در متن Spec | شاهد خودکار / نتیجهٔ ممیزی |
| --- | --- | --- |
| [FR-001](validation/final.md) | AS-01 | DraftFlowIT؛ شاهد اجراشده موجود |
| [FR-002](validation/final.md) | AS-01 | DraftFlowIT, SecurityIT؛ شاهد اجراشده موجود |
| [FR-003](validation/final.md) | AS-02, AS-13 | DraftFlowIT, CorrectionSafetyIT؛ شاهد اجراشده موجود |
| [FR-004](validation/final.md) | AS-03, AS-04 | DraftFlowIT؛ شاهد اجراشده موجود |
| [FR-005](validation/final.md) | AS-05 | DraftFlowIT؛ شاهد اجراشده موجود |
| [FR-006](validation/final.md) | AS-06 | ApprovalIT؛ شاهد اجراشده موجود |
| [FR-007](validation/final.md) | AS-07, AS-15 | ApprovalIT؛ شاهد اجراشده موجود |
| [FR-008](validation/final.md) | AS-08, AS-16 | DeleteDraftIT؛ شاهد اجراشده موجود |
| [FR-009](validation/final.md) | AS-11, AS-12 | RequestChangesIT, ResubmissionIT؛ شاهد اجراشده موجود |
| [FR-010](validation/final.md) | AS-13 | ResubmissionIT؛ شاهد اجراشده موجود |
| [FR-011](validation/final.md) | AS-08, AS-09 | TransitionRulesTest, ApprovalIT؛ شاهد اجراشده موجود |
| [FR-012](validation/final.md) | AS-14 | VisibilityMatrixIT؛ شاهد اجراشده موجود |
| [FR-013](validation/final.md) | AS-15 | VisibilityMatrixIT, GetRequestIT؛ شاهد اجراشده موجود |
| [FR-014](validation/final.md) | AS-17, AS-22 | InFlightRevocationIT, CorrectionSafetyIT؛ شاهد اجراشده موجود |
| [FR-015](validation/final.md) | AS-19, AS-20 | ConcurrentMutationIT, MixedReviewRaceIT, DeleteRaceIT؛ شاهد اجراشده موجود |
| [FR-016](validation/final.md) | AS-03, AS-06, AS-10 | AuditAtomicityIT, DraftFlowIT, CorrectionSafetyIT؛ شاهد اجراشده موجود |
| [FR-017](validation/final.md) | AS-10 | HistoryIT, HistoryVisibilityIT؛ شاهد اجراشده موجود |
| [FR-018](validation/final.md) | AS-18 | PaginationIT, VisibilityMatrixIT؛ شاهد اجراشده موجود |
| [FR-019](validation/final.md) | AS-04, AS-12, AS-17 | ApiContractIT, LogPrivacyIT, GetRequestIT؛ شاهد اجراشده موجود |
| [FR-020](validation/final.md) | AS-21 | IdempotencyIT؛ شاهد اجراشده موجود |
| [FR-021](validation/final.md) | AS-23 | TransitionRulesTest, RequestChangesIT, ApprovalIT؛ شاهد اجراشده موجود |

AS-01 تا AS-23 همگی در جدول مستقل final.md ردیابی شده‌اند؛ FR/AS بدون شاهد خودکار شناسایی نشد. این ممیزی درصد line/branch coverage یا تمام interleavingها نیست؛ محدودیت تست اختصاصی دو ترتیب Create/RequestChanges در final.md ثبت شد. SC-006 فاقد شاهد انسانی و T061 باز است.

DoD فنی: clean compile/package، ۱۶۳ تست بدون failure/error/skip، Migration واقعی، ۱۸۰ رقابت نسخه و ۲۸۰ ترتیب guard/revocation، privacy/architecture، بدون mock DB و مستندات بازبینی‌شده. DoD کلی: **باز به‌علت پذیرش انسانی SC-006**. CI فقط ساختاری بررسی شده و اجرای CI یا استقرار ادعا نمی‌شود.
