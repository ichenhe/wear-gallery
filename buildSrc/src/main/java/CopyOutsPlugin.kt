import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import java.io.File

private const val TARGET_APK_FOLDER = "uni-outs/apk/"
private const val TARGET_REPORT_FOLDER = "uni-outs/report/"
private const val TARGET_MAPPING_FOLDER = "uni-outs/mapping/"

class CopyOutsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("copyOuts", CopyOutsExtension::class.java)
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)

        androidComponents.onVariants(
            androidComponents.selector().withBuildType("release")
        ) { variant ->
            val apkDir = variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.APK)

            if (ext.copyApk) {
                project.tasks.register("copyApk", Copy::class.java) {
                    from(apkDir)
                    into(File(project.rootProject.buildDir, TARGET_APK_FOLDER))
                    include("*.apk")
                }
            }

            project.tasks.register("copyMapping", Copy::class.java) {
                from(File(project.buildDir, "outputs/mapping/${variant.buildType}/mapping.txt"))
                into(File(project.rootProject.buildDir, TARGET_MAPPING_FOLDER + project.name))
                include("mapping.txt")
            }

            project.tasks.register("copyReports", Copy::class.java) {
                from(File(project.buildDir, "reports"))
                into(File(project.rootProject.buildDir, TARGET_REPORT_FOLDER + project.name))
            }
        }

    }
}