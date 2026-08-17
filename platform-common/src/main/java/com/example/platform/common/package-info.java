/**
 * platform-common — 共用工具層。
 *
 * <p>本套件下的類別不可依賴任何 Quarkus / Hibernate / Kogito 等 framework API,
 * 確保 platform-common 可被任何模組重用而不污染 classpath。
 *
 * <p>目前保留為空殼。未來若有多個業務域需要共用的純 Java 工具
 * (例: {@code JsonNodeExtensions}、{@code ErrorShape}、{@code DateUtils}),
 * 應放在本模組,而非各 domain 各自重複實作。
 */
package com.example.platform.common;
