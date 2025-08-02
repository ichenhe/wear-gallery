import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

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
                    into(project.rootProject.layout.buildDirectory.dir(TARGET_APK_FOLDER))
                    include("*.apk")
                }
            }

            project.tasks.register("copyMapping", Copy::class.java) {
                from(project.layout.buildDirectory.file("outputs/mapping/${variant.buildType}/mapping.txt"))
                into(project.rootProject.layout.buildDirectory.dir(TARGET_MAPPING_FOLDER + project.name))
                include("mapping.txt")
            }

            project.tasks.register("copyReports", Copy::class.java) {
                from(project.layout.buildDirectory.dir("reports"))
                into(project.rootProject.layout.buildDirectory.dir(TARGET_REPORT_FOLDER + project.name))
            }
        }

    }
}