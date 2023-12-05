package com.intellij.ideolog.intentions

import com.intellij.ideolog.highlighting.LogEvent
import com.intellij.ideolog.highlighting.settings.DefaultSettingsStoreItems
import com.intellij.ideolog.lex.LogFileFormat
import com.intellij.ideolog.lex.RegexLogParser
import com.intellij.ideolog.util.ideologContext
import com.intellij.mock.MockDocument
import com.intellij.testFramework.RunsInEdt
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

@RunsInEdt
internal class DefaultSettingsStoreItemsTests: BasePlatformTestCase() {
  fun `test match info log category`() {
    val event = LogEvent(
      "2023-05-02 23:09:07,110 [    142]   INFO - #c.i.i.StartupUtil - JVM: 17.0.3+7-b469.32 (OpenJDK 64-Bit Server VM)",
      0,
      LogFileFormat(
        RegexLogParser(
          DefaultSettingsStoreItems.IntelliJIDEA.uuid,
          Pattern.compile(DefaultSettingsStoreItems.IntelliJIDEA.pattern, Pattern.DOTALL),
          Pattern.compile(DefaultSettingsStoreItems.IntelliJIDEA.lineStartPattern),
          DefaultSettingsStoreItems.IntelliJIDEA,
          SimpleDateFormat(DefaultSettingsStoreItems.IntelliJIDEA.timePattern)
        )
      )
    )
    assertEquals("2023-05-02 23:09:07,110", event.date)
    assertEquals("INFO", event.level)
    assertEquals("#c.i.i.StartupUtil", event.category)
    assertEquals("JVM: 17.0.3+7-b469.32 (OpenJDK 64-Bit Server VM)", event.message)
  }
  fun `test match severe log category`() {
    val event = LogEvent(
      "2023-05-03 20:02:44,311 [  49719] SEVERE - #c.i.o.u.ObjectTree - Memory leak detected: 'newDisposable' of class com.intellij.openapi.util.Disposer$1 is registered in Disposer but wasn't disposed.",
      0,
      LogFileFormat(
        RegexLogParser(
          DefaultSettingsStoreItems.IntelliJIDEA.uuid,
          Pattern.compile(DefaultSettingsStoreItems.IntelliJIDEA.pattern, Pattern.DOTALL),
          Pattern.compile(DefaultSettingsStoreItems.IntelliJIDEA.lineStartPattern),
          DefaultSettingsStoreItems.IntelliJIDEA,
          SimpleDateFormat(DefaultSettingsStoreItems.IntelliJIDEA.timePattern)
        )
      )
    )
    assertEquals("2023-05-03 20:02:44,311", event.date)
    assertEquals("ERROR", event.level)
    assertEquals("#c.i.o.u.ObjectTree", event.category)
    assertEquals("Memory leak detected: 'newDisposable' of class com.intellij.openapi.util.Disposer\$1 is registered in Disposer but wasn't disposed.", event.message)
  }

  fun `test should detect IntelliJPattern`() {
    val document = MockDocument()
    document.replaceText(
      "2023-05-02 23:09:06,970 [      2]   INFO - #c.i.i.StartupUtil - ------------------------------------------------------ IDE STARTED ------------------------------------------------------\n" +
        "2023-05-02 23:09:07,029 [     61]   INFO - #c.i.i.p.PluginManager - Using broken plugins file from IDE distribution\n" +
        "2023-05-02 23:09:07,074 [    106]   INFO - #c.i.i.StartupUtil - JNA library (64-bit) loaded in 64 ms\n" +
        "2023-05-02 23:09:07,081 [    113]   INFO - #c.i.o.u.i.w.IdeaWin32 - Native filesystem for Windows is operational\n" +
        "2023-05-02 23:09:07,108 [    140]   INFO - #c.i.i.StartupUtil - IDE: IntelliJ IDEA (build #IC-222.3345.118, 26 Jul 2022 09:01)\n" +
        "2023-05-02 23:09:07,109 [    141]   INFO - #c.i.i.StartupUtil - OS: Windows 11 (10.0, amd64)\n" +
        "2023-05-02 23:09:07,110 [    142]   INFO - #c.i.i.StartupUtil - JRE: 17.0.3+7-b469.32 (JetBrains s.r.o.)\n" +
        "2023-05-02 23:09:07,110 [    142]   INFO - #c.i.i.StartupUtil - JVM: 17.0.3+7-b469.32 (OpenJDK 64-Bit Server VM)\n" +
        "2023-05-02 23:09:07,118 [    150]   INFO - #c.i.i.StartupUtil - PID: 15672\n" +
        "2023-05-02 23:09:07,119 [    151]   INFO - #c.i.i.StartupUtil - JVM options: [-Didea.auto.reload.plugins=true, -Didea.classpath.index.enabled=false, -Didea.config.path=C:\\Work\\github\\ideolog\\build\\idea-sandbox\\config, -Didea.is.internal=true, -Didea.log.path=C:\\Work\\github\\ideolog\\build\\idea-sandbox\\system\\log, -Didea.platform.prefix=Idea, -Didea.plugin.in.sandbox.mode=true, -Didea.plugins.path=C:\\Work\\github\\ideolog\\build\\idea-sandbox\\plugins, -Didea.required.plugins.id=com.intellij.ideolog, -Didea.system.path=C:\\Work\\github\\ideolog\\build\\idea-sandbox\\system, -Didea.vendor.name=JetBrains, -Djava.system.class.loader=com.intellij.util.lang.PathClassLoader, -Djdk.attach.allowAttachSelf=true, -Djdk.http.auth.tunneling.disabledSchemes=, -Djdk.module.illegalAccess.silent=true, -Dkotlinx.coroutines.debug=off, -Dsun.io.useCanonCaches=false, -Dsun.java2d.metal=true, -XX:ReservedCodeCacheSize=512m, -XX:+UseG1GC, -XX:SoftRefLRUPolicyMSPerMB=50, -XX:CICompilerCount=2, -XX:+HeapDumpOnOutOfMemoryError, -XX:-OmitStackTraceInFastThrow, -XX:+IgnoreUnrecognizedVMOptions, --add-opens=java.base/java.io=ALL-UNNAMED, --add-opens=java.base/java.lang=ALL-UNNAMED, --add-opens=java.base/java.lang.ref=ALL-UNNAMED, --add-opens=java.base/java.lang.reflect=ALL-UNNAMED, --add-opens=java.base/java.net=ALL-UNNAMED, --add-opens=java.base/java.nio=ALL-UNNAMED, --add-opens=java.base/java.nio.charset=ALL-UNNAMED, --add-opens=java.base/java.text=ALL-UNNAMED, --add-opens=java.base/java.time=ALL-UNNAMED, --add-opens=java.base/java.util=ALL-UNNAMED, --add-opens=java.base/java.util.concurrent=ALL-UNNAMED, --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED, --add-opens=java.base/jdk.internal.vm=ALL-UNNAMED, --add-opens=java.base/sun.nio.ch=ALL-UNNAMED, --add-opens=java.base/sun.nio.fs=ALL-UNNAMED, --add-opens=java.base/sun.security.ssl=ALL-UNNAMED, --add-opens=java.base/sun.security.util=ALL-UNNAMED, --add-opens=java.desktop/java.awt=ALL-UNNAMED, --add-opens=java.desktop/java.awt.dnd.peer=ALL-UNNAMED, --add-opens=java.desktop/java.awt.event=ALL-UNNAMED, --add-opens=java.desktop/java.awt.image=ALL-UNNAMED, --add-opens=java.desktop/java.awt.peer=ALL-UNNAMED, --add-opens=java.desktop/java.awt.font=ALL-UNNAMED, --add-opens=java.desktop/javax.swing=ALL-UNNAMED, --add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED, --add-opens=java.desktop/javax.swing.text.html=ALL-UNNAMED, --add-opens=java.desktop/sun.awt.datatransfer=ALL-UNNAMED, --add-opens=java.desktop/sun.awt.image=ALL-UNNAMED, --add-opens=java.desktop/sun.awt=ALL-UNNAMED, --add-opens=java.desktop/sun.font=ALL-UNNAMED, --add-opens=java.desktop/sun.java2d=ALL-UNNAMED, --add-opens=java.desktop/sun.swing=ALL-UNNAMED, --add-opens=jdk.attach/sun.tools.attach=ALL-UNNAMED, --add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED, --add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED, --add-opens=jdk.jdi/com.sun.tools.jdi=ALL-UNNAMED, --add-opens=java.desktop/sun.awt.windows=ALL-UNNAMED, -Xms128m, -Xmx750m, -Dfile.encoding=windows-1252, -Duser.country=US, -Duser.language=en, -Duser.variant, -ea]\n" +
        "2023-05-02 23:09:07,120 [    152]   INFO - #c.i.i.StartupUtil - args: []\n" +
        "2023-05-02 23:09:07,120 [    152]   INFO - #c.i.i.StartupUtil - library path: C:\\Users\\mfilippov\\.gradle\\caches\\modules-2\\files-2.1\\com.jetbrains\\jbre\\jbr_jcef-17.0.3-windows-x64-b469.32\\extracted\\jbr_jcef-17.0.3-x64-b469\\bin;C:\\Windows\\Sun\\Java\\bin;C:\\Windows\\system32;C:\\Windows;C:\\Program Files\\Oculus\\Support\\oculus-runtime;C:\\Program Files\\Microsoft\\jdk-11.0.16.101-hotspot\\bin;C:\\Windows\\system32;C:\\Windows;C:\\Windows\\System32\\Wbem;C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\;C:\\Windows\\System32\\OpenSSH\\;C:\\Program Files (x86)\\Gpg4win\\..\\GnuPG\\bin;C:\\Program Files (x86)\\NVIDIA Corporation\\PhysX\\Common;C:\\Program Files\\WireGuard\\;C:\\Program Files\\dotnet\\;C:\\Program Files\\Docker\\Docker\\resources\\bin;C:\\Program Files\\Microsoft SQL Server\\150\\Tools\\Binn\\;C:\\Program Files\\Microsoft SQL Server\\Client SDK\\ODBC\\170\\Tools\\Binn\\;C:\\Program Files\\GitHub CLI\\;C:\\Users\\mfilippov\\AppData\\Local\\Microsoft\\WindowsApps;C:\\Work\\apps\\bin;C:\\Work\\apps\\cmake-3.25.2-windows-x86_64\\bin;C:\\Work\\apps\\go1.20.1.windows-amd64\\go\\bin;C:\\Work\\apps\\microsoft-jdk-17.0.6-windows-x64\\jdk-17.0.6+10\\bin;C:\\Work\\apps\\node-v18.13.0-win-x64;C:\\Work\\apps\\PortableGit-2.39.2-64-bit\\bin;C:\\Work\\apps\\SysinternalsSuite;C:\\Work\\apps\\systeminformer-3.0.5988-bin\\amd64;C:\\Work\\apps\\totalcmd;C:\\Work\\apps\\TreeSizeFree-Portable;C:\\Program Files\\Sublime Text;C:\\Program Files (x86)\\GnuPG\\bin;C:\\Users\\mfilippov\\.dotnet;C:\\Users\\mfilippov\\.dotnet\\tools;C:\\Users\\mfilippov\\AppData\\Local\\JetBrains\\Toolbox\\scripts;C:\\Users\\mfilippov\\.dotnet\\tools;.\n" +
        "2023-05-02 23:09:07,120 [    152]   INFO - #c.i.i.StartupUtil - boot library path: C:\\Users\\mfilippov\\.gradle\\caches\\modules-2\\files-2.1\\com.jetbrains\\jbre\\jbr_jcef-17.0.3-windows-x64-b469.32\\extracted\\jbr_jcef-17.0.3-x64-b469\\bin\n" +
        "2023-05-02 23:09:07,133 [    165]   INFO - #c.i.i.StartupUtil - locale=en_US JNU=Cp1252 file.encoding=windows-1252\n" +
        "  idea.config.path=C:\\Work\\github\\ideolog\\build\\idea-sandbox\\config\n" +
        "  idea.system.path=C:\\Work\\github\\ideolog\\build\\idea-sandbox\\system\n" +
        "  idea.plugins.path=C:\\Work\\github\\ideolog\\build\\idea-sandbox\\plugins\n" +
        "  idea.log.path=C:\\Work\\github\\ideolog\\build\\idea-sandbox\\system\\log\n" +
        "2023-05-02 23:09:07,161 [    193]   INFO - #c.i.i.StartupUtil - CPU cores: 32; ForkJoinPool.commonPool: java.util.concurrent.ForkJoinPool@577457a8[Running, parallelism = 31, size = 5, active = 3, running = 3, steals = 3, tasks = 0, submissions = 0]; factory: com.intellij.concurrency.IdeaForkJoinWorkerThreadFactory@54ca4c12\n" +
        "2023-05-02 23:09:07,312 [    344]   INFO - #c.i.i.p.PluginManager - Plugin PluginDescriptor(name=Groovy, id=org.intellij.groovy, descriptorPath=plugin.xml, path=~\\.gradle\\caches\\modules-2\\files-2.1\\com.jetbrains.intellij.idea\\ideaIC\\2022.2\\42c296374014a649785bb84aa6d8dc2d18f2ca0e\\ideaIC-2022.2\\plugins\\Groovy, version=222.3345.118, package=org.jetbrains.plugins.groovy, isBundled=true) misses optional descriptor duplicates-groovy.xml\n" +
        "2023-05-02 23:09:07,312 [    344]   INFO - #c.i.i.p.PluginManager - Plugin PluginDescriptor(name=Groovy, id=org.intellij.groovy, descriptorPath=plugin.xml, path=~\\.gradle\\caches\\modules-2\\files-2.1\\com.jetbrains.intellij.idea\\ideaIC\\2022.2\\42c296374014a649785bb84aa6d8dc2d18f2ca0e\\ideaIC-2022.2\\plugins\\Groovy, version=222.3345.118, package=org.jetbrains.plugins.groovy, isBundled=true) misses optional descriptor duplicates-detection-groovy.xml\n" +
        "2023-05-02 23:09:07,417 [    449]   INFO - #c.i.i.p.PluginManager - Module intellij.space.gateway is not enabled because dependency com.jetbrains.gateway is not available\n" +
        "Module intellij.space.php is not enabled because dependency com.jetbrains.php is not available\n" +
        "Module kotlin-ultimate.javascript.nodeJs is not enabled because dependency NodeJS is not available\n" +
        "Module kotlin-ultimate.javascript.debugger is not enabled because dependency JavaScriptDebugger is not available\n",
      0
    )
    val format = document.ideologContext.detectLogFileFormat()
    assertEquals(DefaultSettingsStoreItems.IntelliJIDEA.uuid, format.myRegexLogParser?.uuid)
  }

  fun `test should detect Pipe-separated pattern`() {
    val document = MockDocument()
    document.replaceText(
      "03:17:00.000|INFO|Log started|This is info message\n" +
        "03:17:00.000|INFO|Log started|This is multiline\n" +
        "info\n" +
        "message\n" +
        "03:17:00.000|ERROR|Some error type|This is error message\n",
      0
    )
    val format = document.ideologContext.detectLogFileFormat()
    assertEquals(DefaultSettingsStoreItems.PipeSeparated.uuid, format.myRegexLogParser?.uuid)
  }

  fun `test should detect TeamCity pattern`() {
    val document = MockDocument()
    document.replaceText(
      "[23:54:03]i: TeamCity server version is 9.1.5 (build 37377)\n" +
        "[23:54:03]W: bt4 (2m:17s)\n" +
        "[23:54:03] : projectId:project55 projectExternalId:StackExchangeNetwork_NewYork buildTypeId:bt4 buildTypeExternalId:StackExchangeNetwork_NewYork_SENetworkDev\n" +
        "[23:54:03] : Collecting changes in 2 VCS roots (1s)\n" +
        "[23:54:03] :\t [Collecting changes in 2 VCS roots] VCS Root details\n" +
        "[23:54:03] :\t\t [VCS Root details] \"Gitlab - TeamCity Build Configs\" {instance id=968, parent internal id=158, parent id=GitlabTeamCityBuildConfigs, description: \"git@git:shared/teamcity-build-configs.git#refs/heads/master\"}\n" +
        "[23:54:03] :\t\t [VCS Root details] \"Gitlab - Stack Exchange Network\" {instance id=939, parent internal id=99, parent id=GitlabStackExchangeNetwork, description: \"git@git:core/stackoverflow.git#refs/heads/master\"}\n" +
        "[23:54:04]i:\t [Collecting changes in 2 VCS roots] Waiting for completion of current operations for the VCS root 'Gitlab - TeamCity Build Configs'\n" +
        "[23:54:04]i:\t [Collecting changes in 2 VCS roots] Waiting for completion of current operations for the VCS root 'Gitlab - Stack Exchange Network'\n" +
        "[23:54:04]i:\t [Collecting changes in 2 VCS roots] Detecting changes in VCS root 'Gitlab - Stack Exchange Network' (used in 'Release', 'SENetwork - Dev' and 36 other configurations)\n" +
        "[23:54:04]i:\t [Collecting changes in 2 VCS roots] Will collect changes for 'Gitlab - Stack Exchange Network' starting from revision 52173e6ad55dd55da435aaf7dd3343894afb3f1a\n" +
        "[23:54:04]i:\t [Collecting changes in 2 VCS roots] Detecting changes in VCS root 'Gitlab - TeamCity Build Configs' (used in 'ADTools - Dev', 'ADTools - Dev' and 257 other configurations)\n" +
        "[23:54:04]i:\t [Collecting changes in 2 VCS roots] Will collect changes for 'Gitlab - TeamCity Build Configs' starting from revision 35ddc3e78bcae9b7bbb71638dad519abd7d12249",
      0
    )
    val format = document.ideologContext.detectLogFileFormat()
    assertEquals(DefaultSettingsStoreItems.TeamCityBuildLog.uuid, format.myRegexLogParser?.uuid)
  }
}
