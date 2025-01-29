package com.intellij.ideolog.intentions

import com.intellij.ideolog.file.LogFileEditor
import com.intellij.ideolog.file.LogFileEditorProvider
import com.intellij.ideolog.highlighting.LogEvent
import com.intellij.ideolog.highlighting.settings.DefaultSettingsStoreItems
import com.intellij.ideolog.highlighting.settings.LogHighlightingSettingsStore
import com.intellij.ideolog.highlighting.settings.LogParsingPattern
import com.intellij.ideolog.lex.LogFileFormat
import com.intellij.ideolog.lex.RegexLogParser
import com.intellij.ideolog.util.ideologContext
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.text.SimpleDateFormat
import java.util.regex.Pattern

class DefaultSettingsStoreItemsTest: BasePlatformTestCase() {
  private lateinit var parsingPatternsBackup: List<LogParsingPattern>
  private lateinit var editor: LogFileEditor

  override fun setUp() {
    super.setUp()
    parsingPatternsBackup = LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.map { it.copy() }
  }

  override fun tearDown() {
    try {
      LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.clear()
      LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.addAll(parsingPatternsBackup)
      if (::editor.isInitialized) {
        Disposer.dispose(editor)
      }
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  fun testMatchInfoLogCategory() {
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
  fun testMatchSevereLogCategory() {
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

  fun testShouldDetectIntelliJPattern() {
    val document = configureLogFileDocument(
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
        "Module kotlin-ultimate.javascript.debugger is not enabled because dependency JavaScriptDebugger is not available\n"
    )
    val format = document.ideologContext.detectLogFileFormat()
    assertEquals(DefaultSettingsStoreItems.IntelliJIDEA.uuid, format.myRegexLogParser?.uuid)
  }

  fun testShouldDetectPipeSeparatedPattern() {
    val document = configureLogFileDocument(
      "03:17:00.000|INFO|Log started|This is info message\n" +
        "03:17:00.000|INFO|Log started|This is multiline\n" +
        "info\n" +
        "message\n" +
        "03:17:00.000|ERROR|Some error type|This is error message\n"
    )
    val format = document.ideologContext.detectLogFileFormat()
    assertEquals(DefaultSettingsStoreItems.PipeSeparated.uuid, format.myRegexLogParser?.uuid)
  }

  fun testShouldDetectTeamCityPattern() {
    val document = configureLogFileDocument(
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
        "[23:54:04]i:\t [Collecting changes in 2 VCS roots] Will collect changes for 'Gitlab - TeamCity Build Configs' starting from revision 35ddc3e78bcae9b7bbb71638dad519abd7d12249"
    )
    val format = document.ideologContext.detectLogFileFormat()
    assertEquals(DefaultSettingsStoreItems.TeamCityBuildLog.uuid, format.myRegexLogParser?.uuid)
  }

  fun testShouldDetectLaravelFormat() {
    val document = configureLogFileDocument(
      "[2023-12-05 09:15:23] local.ERROR: Uncaught Exception: Division by zero {\"exception\":\"[object] (ErrorException(code: 0): Division by zero at /path/to/laravel/project/app/Http/Controllers/SomeController.php:55)\n" +
        "[stacktrace]\n" +
        "#0 /path/to/laravel/project/app/Http/Controllers/SomeController.php(55): divisionByZeroFunction()\n" +
        "#1 [internal function]: App\\\\Http\\\\Controllers\\\\SomeController->index()\n" +
        "#2 /path/to/laravel/project/vendor/laravel/framework/src/Illuminate/Routing/Controller.php(54): call_user_func_array()\n" +
        "#3 /path/to/laravel/project/vendor/laravel/framework/src/Illuminate/Routing/ControllerDispatcher.php(45): Illuminate\\\\Routing\\\\Controller->callAction()\n" +
        "#4 /path/to/laravel/project/vendor/laravel/framework/src/Illuminate/Routing/Route.php(239): Illuminate\\\\Routing\\\\ControllerDispatcher->dispatch()\n" +
        "#5 /path/to/laravel/project/vendor/laravel/framework/src/Illuminate/Routing/Route.php(196): Illuminate\\\\Routing\\\\Route->runController()\n" +
        "#6 /path/to/laravel/project/vendor/laravel/framework/src/Illuminate/Routing/Router.php(685): Illuminate\\\\Routing\\\\Route->run()\n" +
        "\n" +
        "[2023-12-05 09:16:47] local.WARNING: User 123 attempted to access restricted area.\n" +
        "[stacktrace]\n" +
        "#0 /path/to/laravel/project/app/Http/Middleware/CheckRole.php(27): checkUserRole()\n" +
        "#1 /path/to/laravel/project/vendor/laravel/framework/src/Illuminate/Pipeline/Pipeline.php(167): App\\\\Http\\\\Middleware\\\\CheckRole->handle()\n" +
        "#2 /path/to/laravel/project/vendor/laravel/framework/src/Illuminate/Routing/Middleware/SubstituteBindings.php(41): Illuminate\\\\Pipeline\\\\Pipeline->Illuminate\\\\Pipeline\\\\{closure}()\n" +
        "\n" +
        "[2023-12-05 09:17:05] local.INFO: User 456 logged in successfully.\n" +
        "[stacktrace]\n" +
        "#0 /path/to/laravel/project/app/Http/Controllers/Auth/LoginController.php(72): attemptLogin()\n" +
        "#1 [internal function]: App\\\\Http\\\\Controllers\\\\Auth\\\\LoginController->login()\n" +
        "#2 /path/to/laravel/project/vendor/laravel/framework/src/Illuminate/Routing/Controller.php(54): call_user_func_array()\n" +
        "#3 /path/to/laravel/project/vendor/laravel/framework/src/Illuminate/R\n" +
        "\n"
    )
    val format = document.ideologContext.detectLogFileFormat()
    assertEquals(DefaultSettingsStoreItems.LaravelLog.uuid, format.myRegexLogParser?.uuid)
  }

  fun testShouldDetectLogcatFormat() {
    val document = configureLogFileDocument(
      "01-01 10:00:01.123  1234  5678 D MyAppTag: Starting app\n" +
        "01-01 10:00:02.456  1234  5678 I MyAppTag: Initializing user interface\n" +
        "01-01 10:00:03.789  1234  5678 W MyAppTag: Network connection is slow\n" +
        "01-01 10:00:04.012  1234  5678 E MyAppTag: Failed to load user data\n" +
        "01-01 10:00:05.345  1234  5678 I MyAppTag: App ready"
    )
    val format = document.ideologContext.detectLogFileFormat()
    assertEquals(DefaultSettingsStoreItems.Logcat.uuid, format.myRegexLogParser?.uuid)
  }

  fun testShouldDetectLoguruFormat() {
    val document = configureLogFileDocument(
      "2023-07-08 16:30:13.780 | SUCCESS | __main__:login:64 - \n" +
        "    User authentication successful\n" +
        "    Session token generated\n" +
        "2023-07-08 16:30:14.137 | INFO    | __main__:change_steps:103 - \n" +
        "    Steps updated to new value\n" +
        "    Previous: 1000, New: 1200\n" +
        "2023-07-08 16:30:15.488 | INFO    | __main__:update_user_data:80 - \n" +
        "    User data synchronization started\n" +
        "    Fetching data from remote server\n" +
        "2023-07-08 16:52:43.963 | INFO    | __main__:login:62 - \n" +
        "    Login process initiated by user\n" +
        "2023-07-08 16:52:43.964 | ERROR   | __main__:login:63 - \n" +
        "    Error encountered during login:\n" +
        "    List index out of range - index 5 of a 4-item list\n" +
        "2023-07-08 16:52:43.967 | WARNING | __main__:module:142 - \n" +
        "    Unexpected data format received\n" +
        "    Expected JSON, received XML\n" +
        "2023-07-08 16:52:44.439 | INFO    | __main__:sbs_api_info:121 - \n" +
        "    API information retrieved successfully\n" +
        "    Endpoint: /api/v1/info\n"
    )
    val format = document.ideologContext.detectLogFileFormat()
    assertEquals(DefaultSettingsStoreItems.Loguru.uuid, format.myRegexLogParser?.uuid)
  }

  fun testShouldDetectSymfonyFormat() {
    val document = configureLogFileDocument(
      "[2023-06-05T09:57:38.056800+00:00] php.DEBUG: Warning: filemtime(): stat failed for /app/config/routes/framework.yaml {\"exception\":\"[object] (Symfony\\\\Component\\\\ErrorHandler\\\\Error\\\\SilencedErrorContext {...})\"} []\n" +
        "[2023-06-05T09:57:38.057300+00:00] request.ERROR: Uncaught PHP Exception Symfony\\\\Component\\\\HttpKernel\\\\Exception\\\\NotFoundHttpException: \"No route found for \"GET https://example.com/nonexistent\" [] []\n" +
        "[2023-06-05T09:57:38.101820+00:00] request.ERROR: Uncaught PHP Exception Symfony\\\\Component\\\\HttpKernel\\\\Exception\\\\NotFoundHttpException: \"No route found for \"GET https://example.com/failedroute\" [] []\n" +
        "[2023-06-05T09:57:38.130130+00:00] request.CRITICAL: Uncaught PHP Exception Twig\\\\Error\\\\RuntimeError: \"An exception has been thrown during the rendering of a template (\\\"Failed to fetch user profile data\\\").\" at /path/to/template/file.twig line 15 [] []\n" +
        "[2023-06-05T09:57:39.355650+00:00] deprecation.INFO: User Deprecated: The \"url\" connection parameter is deprecated. Please use Doctrine\\\\DBAL\\\\Tools\\\\DsnParser to parse a DSN string instead. {\"exception\":\"[object] (ErrorException(code: 0): User Deprecated: The \\\"url\\\" connection parameter is deprecated at /path/to/deprecated/code:123)\"} []\n"
    )
    val format = document.ideologContext.detectLogFileFormat()
    assertEquals(DefaultSettingsStoreItems.Symfony.uuid, format.myRegexLogParser?.uuid)
  }

  fun testShouldNotDetectTeamCityFormat() {
    val document = configureLogFileDocument(
      "[03:48:03] :    [VCS Root details] \"IntelliJ (241.12345)\" {instance id=12345, parent internal id=123456, parent id=parentid, description: \"ssh://git@address/git#refs/heads/241.12345\"}"
    )
    LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.removeIf { parsingPattern ->
      parsingPattern.uuid != DefaultSettingsStoreItems.LaravelLog.uuid
    }
    val format = document.ideologContext.detectLogFileFormat()
    assertNull(format.myRegexLogParser)
  }

  fun testRemoveFormatWithoutRestarting() {
    val document = configureLogFileDocument(
      "[03:48:03] :    [VCS Root details] \"IntelliJ (241.12345)\" {instance id=12345, parent internal id=123456, parent id=parentid, description: \"ssh://git@address/git#refs/heads/241.12345\"}"
    )
    val format = document.ideologContext.detectLogFileFormat()
    assertEquals(DefaultSettingsStoreItems.TeamCityBuildLog.uuid, format.myRegexLogParser?.uuid)
    LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.clear()
    val unknownFormat = document.ideologContext.detectLogFileFormat()
    assertNull(unknownFormat.myRegexLogParser)
  }

  fun testDisableFormatWithoutRestarting() {
    val document = configureLogFileDocument(
      "[03:48:03] :    [VCS Root details] \"IntelliJ (241.12345)\" {instance id=12345, parent internal id=123456, parent id=parentid, description: \"ssh://git@address/git#refs/heads/241.12345\"}"
    )
    val format = document.ideologContext.detectLogFileFormat()
    assertEquals(DefaultSettingsStoreItems.TeamCityBuildLog.uuid, format.myRegexLogParser?.uuid)
    LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.first { it.uuid == DefaultSettingsStoreItems.TeamCityBuildLog.uuid }.enabled = false
    val unknownFormat = document.ideologContext.detectLogFileFormat()
    assertNull(unknownFormat.myRegexLogParser)
  }

  fun testEnableFormatWithoutRestarting() {
    val document = configureLogFileDocument(
      "[03:48:03] :    [VCS Root details] \"IntelliJ (241.12345)\" {instance id=12345, parent internal id=123456, parent id=parentid, description: \"ssh://git@address/git#refs/heads/241.12345\"}"
    )
    LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.forEach { it.enabled = false }
    val unknownFormat = document.ideologContext.detectLogFileFormat()
    assertNull(unknownFormat.myRegexLogParser)
    LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.first { it.uuid == DefaultSettingsStoreItems.TeamCityBuildLog.uuid }.enabled = true
    mockSaveSettings()
    val format = document.ideologContext.detectLogFileFormat()
    assertEquals(DefaultSettingsStoreItems.TeamCityBuildLog.uuid, format.myRegexLogParser?.uuid)
  }

  fun testAddFormatWithoutRestarting() {
    val document = configureLogFileDocument(
      "[03:48:03] :    [VCS Root details] \"IntelliJ (241.12345)\" {instance id=12345, parent internal id=123456, parent id=parentid, description: \"ssh://git@address/git#refs/heads/241.12345\"}"
    )
    LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.clear()
    val unknownFormat = document.ideologContext.detectLogFileFormat()
    assertNull(unknownFormat.myRegexLogParser)
    LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.add(DefaultSettingsStoreItems.TeamCityBuildLog)
    mockSaveSettings()
    val format = document.ideologContext.detectLogFileFormat()
    assertEquals(DefaultSettingsStoreItems.TeamCityBuildLog.uuid, format.myRegexLogParser?.uuid)
  }

  fun testOpenFileDisableFormatCloseFileEnableFormatOpenFile() {
    val file = configureLogFile(
      "[03:48:03] :    [VCS Root details] \"IntelliJ (241.12345)\" {instance id=12345, parent internal id=123456, parent id=parentid, description: \"ssh://git@address/git#refs/heads/241.12345\"}"
    )
    val format = file.fileDocument.ideologContext.detectLogFileFormat()
    assertEquals(DefaultSettingsStoreItems.TeamCityBuildLog.uuid, format.myRegexLogParser?.uuid)

    LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.clear()
    val formatAfterDisabling = file.fileDocument.ideologContext.detectLogFileFormat()
    assertNull(formatAfterDisabling.myRegexLogParser?.uuid)

    Disposer.dispose(editor)
    LogHighlightingSettingsStore.getInstance().myState.parsingPatterns.add(DefaultSettingsStoreItems.TeamCityBuildLog)

    editor = LogFileEditorProvider().createEditor(project, file.virtualFile) as LogFileEditor
    val formatOnReopenedFileAfterEnabling = file.fileDocument.ideologContext.detectLogFileFormat()
    assertEquals(DefaultSettingsStoreItems.TeamCityBuildLog.uuid, formatOnReopenedFileAfterEnabling.myRegexLogParser?.uuid)
  }

  private fun configureLogFile(text: String): PsiFile {
    val file = myFixture.configureByText("file.log", text)
    editor = LogFileEditorProvider().createEditor(project, file.virtualFile) as LogFileEditor
    return file
  }

  private fun configureLogFileDocument(text: String): Document = configureLogFile(text).fileDocument

  private fun mockSaveSettings() {
    LogHighlightingSettingsStore.getInstance().loadState(LogHighlightingSettingsStore.getInstance().state)
  }
}
