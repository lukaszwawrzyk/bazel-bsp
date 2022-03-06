package org.jetbrains.bsp.bazel.server;

import ch.epfl.scala.bsp4j.BuildClient;
import io.grpc.ServerBuilder;
import java.nio.file.Paths;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.jetbrains.bsp.bazel.bazelrunner.BazelDataResolver;
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bsp.bazel.bazelrunner.data.BazelData;
import org.jetbrains.bsp.bazel.server.bep.BepServer;
import org.jetbrains.bsp.bazel.server.bsp.BazelBspServerLifetime;
import org.jetbrains.bsp.bazel.server.bsp.BspImplementationHub;
import org.jetbrains.bsp.bazel.server.bsp.BspIntegrationData;
import org.jetbrains.bsp.bazel.server.bsp.BspRequestsRunner;
import org.jetbrains.bsp.bazel.server.bsp.config.BazelBspServerConfig;
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspAspectsManager;
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspCompilationManager;
import org.jetbrains.bsp.bazel.server.bsp.services.CppBuildServerService;
import org.jetbrains.bsp.bazel.server.bsp.utils.InternalAspectsResolver;
import org.jetbrains.bsp.bazel.server.loggers.BuildClientLogger;
import org.jetbrains.bsp.bazel.server.sync.BazelPathsResolver;
import org.jetbrains.bsp.bazel.server.sync.BazelProjectMapper;
import org.jetbrains.bsp.bazel.server.sync.BepServerProjectListener;
import org.jetbrains.bsp.bazel.server.sync.BspProjectMapper;
import org.jetbrains.bsp.bazel.server.sync.ExecuteService;
import org.jetbrains.bsp.bazel.server.sync.ProjectProvider;
import org.jetbrains.bsp.bazel.server.sync.ProjectResolver;
import org.jetbrains.bsp.bazel.server.sync.ProjectSyncService;
import org.jetbrains.bsp.bazel.server.sync.ProjectViewProvider;
import org.jetbrains.bsp.bazel.server.sync.TargetKindResolver;
import org.jetbrains.bsp.bazel.server.sync.languages.LanguagePluginsService;
import org.jetbrains.bsp.bazel.server.sync.languages.cpp.CppLanguagePlugin;
import org.jetbrains.bsp.bazel.server.sync.languages.java.JavaLanguagePlugin;
import org.jetbrains.bsp.bazel.server.sync.languages.scala.ScalaLanguagePlugin;

public class BazelBspServer {

  private final BazelBspServerConfig bazelBspServerConfig;
  private final BazelRunner bazelRunner;
  private final BazelData bazelData;

  private BspImplementationHub bspImplementationHub;
  private BazelBspCompilationManager bazelBspCompilationManager;
  private ProjectProvider projectProvider;

  public BazelBspServer(BazelBspServerConfig bazelBspServerConfig) {
    this.bazelBspServerConfig = bazelBspServerConfig;
    this.bazelRunner = new BazelRunner(bazelBspServerConfig.getBazelPath());
    BazelDataResolver bazelDataResolver = new BazelDataResolver(bazelRunner);
    this.bazelData = bazelDataResolver.resolveBazelData();
  }

  public void startServer(BspIntegrationData bspIntegrationData) {
    var serverLifetime = new BazelBspServerLifetime();
    var bspRequestsRunner = new BspRequestsRunner(serverLifetime);

    var bazelBspCompilationManager = new BazelBspCompilationManager(bazelRunner, bazelData);
    var internalAspectsResolver =
        new InternalAspectsResolver(
            bazelData.getBspProjectRoot(), Paths.get(bazelData.getWorkspaceRoot()));
    var bazelBspAspectsManager =
        new BazelBspAspectsManager(
            bazelBspCompilationManager, bazelRunner, internalAspectsResolver);
    var bazelPathsResolver = new BazelPathsResolver(bazelData);
    var javaLanguagePlugin = new JavaLanguagePlugin(bazelPathsResolver, bazelData);
    var scalaLanguagePlugin = new ScalaLanguagePlugin(javaLanguagePlugin, bazelPathsResolver);
    var cppLanguagePlugin = new CppLanguagePlugin();
    var languagePluginsService =
        new LanguagePluginsService(scalaLanguagePlugin, javaLanguagePlugin, cppLanguagePlugin);
    var targetKindResolver = new TargetKindResolver();
    var bazelProjectMapper =
        new BazelProjectMapper(languagePluginsService, bazelPathsResolver, targetKindResolver);
    var projectViewProvider = new ProjectViewProvider(bazelBspServerConfig.getProjectView());
    var projectResolver =
        new ProjectResolver(bazelBspAspectsManager, projectViewProvider, bazelProjectMapper);
    this.projectProvider = new ProjectProvider(projectResolver);
    var bspProjectMapper = new BspProjectMapper(languagePluginsService);
    var projectSyncService = new ProjectSyncService(bspProjectMapper, projectProvider);
    var executeService =
        new ExecuteService(bazelBspCompilationManager, projectProvider, bazelRunner);
    var cppBuildServerService = new CppBuildServerService(bazelBspAspectsManager);

    this.bspImplementationHub =
        new BspImplementationHub(
            serverLifetime,
            bspRequestsRunner,
            projectSyncService,
            executeService,
            cppBuildServerService);

    integrateBsp(bspIntegrationData);
  }

  private void integrateBsp(BspIntegrationData bspIntegrationData) {
    Launcher<BuildClient> launcher =
        new Launcher.Builder<BuildClient>()
            .traceMessages(bspIntegrationData.getTraceWriter())
            .setOutput(bspIntegrationData.getStdout())
            .setInput(bspIntegrationData.getStdin())
            .setLocalService(bspImplementationHub)
            .setRemoteInterface(BuildClient.class)
            .setExecutorService(bspIntegrationData.getExecutor())
            .create();

    bspIntegrationData.setLauncher(launcher);
    BuildClientLogger buildClientLogger = new BuildClientLogger(launcher.getRemoteProxy());

    var bepServer = new BepServer(bazelData, launcher.getRemoteProxy(), buildClientLogger);
    bazelBspCompilationManager.setBepServer(bepServer);
    projectProvider.addListener(new BepServerProjectListener(bepServer));
    bazelRunner.setLogger(buildClientLogger);

    bspIntegrationData.setServer(ServerBuilder.forPort(0).addService(bepServer).build());
  }

  public void setBesBackendPort(int port) {
    bazelRunner.setBesBackendPort(port);
  }
}
