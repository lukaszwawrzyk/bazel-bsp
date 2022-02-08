package org.jetbrains.bsp.bazel.server;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildServer;
import ch.epfl.scala.bsp4j.CppBuildServer;
import ch.epfl.scala.bsp4j.JavaBuildServer;
import ch.epfl.scala.bsp4j.JvmBuildServer;
import ch.epfl.scala.bsp4j.ScalaBuildServer;
import io.grpc.ServerBuilder;
import java.nio.file.Paths;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.jetbrains.bsp.bazel.bazelrunner.BazelDataResolver;
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bsp.bazel.bazelrunner.data.BazelData;
import org.jetbrains.bsp.bazel.server.bep.BepServer;
import org.jetbrains.bsp.bazel.server.bsp.BazelBspServerLifetime;
import org.jetbrains.bsp.bazel.server.bsp.BazelBspServerRequestHelpers;
import org.jetbrains.bsp.bazel.server.bsp.BspImplementationHub;
import org.jetbrains.bsp.bazel.server.bsp.BspIntegrationData;
import org.jetbrains.bsp.bazel.server.bsp.config.BazelBspServerConfig;
import org.jetbrains.bsp.bazel.server.bsp.impl.BuildServerImpl;
import org.jetbrains.bsp.bazel.server.bsp.impl.CppBuildServerImpl;
import org.jetbrains.bsp.bazel.server.bsp.impl.JavaBuildServerImpl;
import org.jetbrains.bsp.bazel.server.bsp.impl.JvmBuildServerImpl;
import org.jetbrains.bsp.bazel.server.bsp.impl.ScalaBuildServerImpl;
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspAspectsManager;
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspCompilationManager;
import org.jetbrains.bsp.bazel.server.bsp.services.BuildServerService;
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
    BazelBspServerLifetime serverLifetime = new BazelBspServerLifetime();
    BazelBspServerRequestHelpers serverRequestHelpers =
        new BazelBspServerRequestHelpers(serverLifetime);

    BazelBspCompilationManager bazelBspCompilationManager =
        new BazelBspCompilationManager(bazelRunner, bazelData);
    InternalAspectsResolver internalAspectsResolver =
        new InternalAspectsResolver(
            bazelData.getBspProjectRoot(), Paths.get(bazelData.getWorkspaceRoot()));
    BazelBspAspectsManager bazelBspAspectsManager =
        new BazelBspAspectsManager(
            bazelBspCompilationManager, bazelRunner, internalAspectsResolver);
    BazelPathsResolver bazelPathsResolver = new BazelPathsResolver(bazelData);
    JavaLanguagePlugin javaLanguagePlugin = new JavaLanguagePlugin(bazelPathsResolver, bazelData);
    ScalaLanguagePlugin scalaLanguagePlugin =
        new ScalaLanguagePlugin(javaLanguagePlugin, bazelPathsResolver);
    CppLanguagePlugin cppLanguagePlugin = new CppLanguagePlugin();
    LanguagePluginsService languagePluginsService =
        new LanguagePluginsService(scalaLanguagePlugin, javaLanguagePlugin, cppLanguagePlugin);
    TargetKindResolver targetKindResolver = new TargetKindResolver();
    BazelProjectMapper bazelProjectMapper =
        new BazelProjectMapper(languagePluginsService, bazelPathsResolver, targetKindResolver);
    ProjectResolver projectResolver =
        new ProjectResolver(
            bazelBspAspectsManager,
            new ProjectViewProvider(bazelBspServerConfig.getProjectView()),
            bazelProjectMapper);
    this.projectProvider = new ProjectProvider(projectResolver);
    BspProjectMapper bspProjectMapper = new BspProjectMapper(languagePluginsService);
    ProjectSyncService projectSyncService =
        new ProjectSyncService(bspProjectMapper, projectProvider);

    ExecuteService executeService =
        new ExecuteService(bazelBspCompilationManager, projectProvider, bazelRunner);

    BuildServerService buildServerService =
        new BuildServerService(serverRequestHelpers, serverLifetime, executeService);

    CppBuildServerService cppBuildServerService = new CppBuildServerService(bazelBspAspectsManager);

    JvmBuildServer jvmBuildServer =
        new JvmBuildServerImpl(projectSyncService, serverRequestHelpers);
    ScalaBuildServer scalaBuildServer =
        new ScalaBuildServerImpl(serverRequestHelpers, projectSyncService);
    JavaBuildServer javaBuildServer =
        new JavaBuildServerImpl(projectSyncService, serverRequestHelpers);
    CppBuildServer cppBuildServer =
        new CppBuildServerImpl(cppBuildServerService, serverRequestHelpers);
    BuildServer buildServer =
        new BuildServerImpl(buildServerService, serverRequestHelpers, projectSyncService);

    this.bspImplementationHub =
        new BspImplementationHub(
            buildServer, jvmBuildServer, scalaBuildServer, javaBuildServer, cppBuildServer);

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

    BepServer bepServer = new BepServer(bazelData, launcher.getRemoteProxy(), buildClientLogger);
    bazelBspCompilationManager.setBepServer(bepServer);
    projectProvider.addListener(new BepServerProjectListener(bepServer));
    bazelRunner.setLogger(buildClientLogger);

    bspIntegrationData.setServer(ServerBuilder.forPort(0).addService(bepServer).build());
  }

  public void setBesBackendPort(int port) {
    bazelRunner.setBesBackendPort(port);
  }
}
