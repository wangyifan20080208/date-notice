package com.smartagenda.ingest

enum class SourceApp(val packageName: String, val displayName: String) {
    WECHAT("com.tencent.mm", "微信"),
    QQ("com.tencent.mobileqq", "QQ");

    companion object {
        fun fromPackage(packageName: String): SourceApp? =
            entries.firstOrNull { it.packageName == packageName }
    }
}