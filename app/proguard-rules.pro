# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class io.ipinfo.api.** { *; }
-keepattributes SourceFile,LineNumberTable

# OkHttp and SSL/TLS classes
-keep class okhttp3.** { *; }
-keep class org.bouncycastle.** { *; }
-keep class org.conscrypt.** { *; }
-keep class org.openjsse.** { *; }

# SSL/TLS configuration
-keep class javax.net.ssl.** { *; }
-keep class com.android.org.conscrypt.** { *; }

# Platform specific implementations
-keep class okhttp3.internal.platform.** { *; }

# BouncyCastle specific classes
-keep class org.bouncycastle.jsse.BCSSLParameters { *; }
-keep class org.bouncycastle.jsse.BCSSLSocket { *; }
-keep class org.bouncycastle.jsse.provider.BouncyCastleJsseProvider { *; }

# Conscrypt specific classes
-keep class org.conscrypt.Conscrypt { *; }
-keep class org.conscrypt.Conscrypt$Version { *; }
-keep class org.conscrypt.ConscryptHostnameVerifier { *; }

# OpenJSSE specific classes
-keep class org.openjsse.javax.net.ssl.SSLParameters { *; }
-keep class org.openjsse.javax.net.ssl.SSLSocket { *; }
-keep class org.openjsse.net.ssl.OpenJSSE { *; }

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn com.android.org.conscrypt.SSLParametersImpl
-dontwarn javax.naming.NamingEnumeration
-dontwarn javax.naming.NamingException
-dontwarn javax.naming.directory.Attribute
-dontwarn javax.naming.directory.Attributes
-dontwarn javax.naming.directory.DirContext
-dontwarn javax.naming.directory.InitialDirContext
-dontwarn javax.naming.directory.SearchControls
-dontwarn javax.naming.directory.SearchResult
-dontwarn org.apache.harmony.xnet.provider.jsse.SSLParametersImpl
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
