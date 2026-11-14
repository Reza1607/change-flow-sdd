# ADR-001 — Runtime، معماری و ذخیره‌سازی

تاریخ: 2026-11-14 | وضعیت: پیشنهادی در Plan

## Context

Constitution Java 21، Spring Boot 4، Maven، relational storage و Migration نسخه‌دار را الزام می‌کند؛ انتخاب محصول و ابزار به Plan سپرده شده است.

## Decision

Boot 4.0.9 با BOM خودش، Maven Wrapper 3.9، PostgreSQL 17.12 و Flyway SQL. یک executable JAR و یک Maven module با package-by-feature و مدل دامنه مستقل از Entity JPA. DTOهای Records و نگاشت دستی؛ schema فقط با Migration و Hibernate validate. Testcontainers همان PostgreSQL را برای API/Persistence integration اجرا می‌کند.

## Alternatives / Consequences

Liquibase ممکن است اما نیاز اضافی ندارد؛ H2 و mock رفتار قفل PostgreSQL را ثابت نمی‌کنند. چند سرویس یا generic workflow engine لازم نیست. هزینهٔ نگاشت دستی کوچک است و وابستگی domain به JPA را حذف می‌کند. نسخه‌های patch وابستگی‌ها تحت BOM ثابت می‌شوند؛ تغییر major تابع Constitution است.

شواهد رسمی و تصمیم‌های dependency در [research.md](../../specs/001-change-request-management/research.md) ثبت شده‌اند.
