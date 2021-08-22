import java.io.File
import java.util.*

class Signing(private val rootDir: File) {

    data class Config(
        val storeFile: File,
        val storePassword: String,
        val keyAlias: String,
        val keyPassword: String,
    )

    fun readConfig(): Config? {
        val keystoreDir = File(rootDir, "keystore")
        if (!keystoreDir.isDirectory) {
            return null // key dir does not exist
        }
        val keyFile = File(keystoreDir, "release.jks")
        if (!keyFile.isFile) {
            return null // key file does not exist
        }

        // read config file
        val prop = Properties()
        val propFile = File(keystoreDir, "config.properties")
        if (propFile.isFile) {
            propFile.inputStream().use { prop.load(it) }
        }

        val storePwd = System.getenv("WG_SIGNING_STORE_PWD") ?: prop.getProperty("storePwd", null)
        val keyAlias = System.getenv("WG_SIGNING_KEY_ALIAS") ?: prop.getProperty("keyAlias", null)
        val keyPwd = System.getenv("WG_SIGNING_KEY_PWD") ?: prop.getProperty("keyPwd", null)
        if (storePwd.isNullOrEmpty() || keyAlias.isNullOrEmpty() || keyPwd.isNullOrEmpty()) {
            return null
        }

        return Config(keyFile, storePwd, keyAlias, keyPwd)
    }
}