# Optimus Proguard Rules

# Keep BouncyCastle crypto providers
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep sshj classes
-keep class com.hierynomus.sshj.** { *; }
-dontwarn com.hierynomus.sshj.**
-dontwarn net.schmizz.**

# Keep SQLCipher and SQLite
-keep class net.zetetic.database.sqlcipher.** { *; }
-keep class androidx.sqlite.db.** { *; }

# Keep Room entities
-keep class com.optimus.ssh.data.db.entity.** { *; }
