# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in F:\android-sdks/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

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

# Your application may contain more items that need to be preserved;
# typically classes that are dynamically created using Class.forName:

-keep class android.support.log4j2.** { *; }
-dontwarn android.support.log4j2.**

-keep class org.apache.logging.log4j.** { *; }
-dontwarn org.apache.logging.log4j.**
