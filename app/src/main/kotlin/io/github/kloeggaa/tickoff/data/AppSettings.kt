package io.github.kloeggaa.tickoff.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.kloeggaa.tickoff.api.AnthropicClient
import io.github.kloeggaa.tickoff.api.OpenAIClient
import io.github.kloeggaa.tickoff.storage.TaskFileLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** App-wide settings: where the task file lives and which AI provider extracts recorded tasks. */
class AppSettings(private val context: Context) {
    companion object {
        const val DEFAULT_TASK_FILE_NAME = "TasksAI.md"
        const val PROVIDER_OPENAI = "openai"
        const val PROVIDER_ANTHROPIC = "anthropic"

        val VAULT_TREE_URI = stringPreferencesKey("vault_tree_uri")
        val TASK_FOLDER = stringPreferencesKey("task_folder")
        val TASK_FILE_NAME = stringPreferencesKey("task_file_name")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val OPENAI_MODEL = stringPreferencesKey("openai_model")
        val ANTHROPIC_API_KEY = stringPreferencesKey("anthropic_api_key")
        val ANTHROPIC_MODEL = stringPreferencesKey("anthropic_model")

        /** "Tasks//Inbox/" -> "Tasks/Inbox"; "." and ".." segments are dropped. */
        fun normalizeFolder(input: String): String =
            input.replace('\\', '/')
                .split('/')
                .map { it.trim() }
                .filter { it.isNotEmpty() && it != "." && it != ".." }
                .joinToString("/")

        /** Blank -> default name; a name without extension gets ".md"; path separators are removed. */
        fun normalizeFileName(input: String): String {
            val name = input.trim().replace("/", "").replace("\\", "")
            if (name.isEmpty()) return DEFAULT_TASK_FILE_NAME
            return if (name.contains('.')) name else "$name.md"
        }
    }

    val vaultTreeUriFlow: Flow<String?> = context.dataStore.data.map { it[VAULT_TREE_URI] }
    val taskFolderFlow: Flow<String> = context.dataStore.data.map { it[TASK_FOLDER].orEmpty() }
    val taskFileNameFlow: Flow<String> = context.dataStore.data.map { it[TASK_FILE_NAME] ?: DEFAULT_TASK_FILE_NAME }
    val aiProviderFlow: Flow<String> = context.dataStore.data.map { it[AI_PROVIDER] ?: PROVIDER_OPENAI }
    // API keys are stored encrypted (ApiKeyCipher); a key that can't be decrypted reads as no key.
    val openAiApiKeyFlow: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[OPENAI_API_KEY]?.let(ApiKeyCipher::decrypt) }
        .flowOn(Dispatchers.IO)
    val openAiModelFlow: Flow<String> = context.dataStore.data.map { it[OPENAI_MODEL] ?: OpenAIClient.DEFAULT_MODEL }
    val anthropicApiKeyFlow: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[ANTHROPIC_API_KEY]?.let(ApiKeyCipher::decrypt) }
        .flowOn(Dispatchers.IO)
    val anthropicModelFlow: Flow<String> = context.dataStore.data.map {
        it[ANTHROPIC_MODEL] ?: AnthropicClient.DEFAULT_MODEL
    }

    /** The configured task file, or null while no vault folder has been picked. */
    suspend fun taskFileLocation(): TaskFileLocation? {
        val prefs = context.dataStore.data.first()
        val vault = prefs[VAULT_TREE_URI]?.takeIf { it.isNotBlank() } ?: return null
        return TaskFileLocation(
            vaultTreeUri = vault,
            folder = prefs[TASK_FOLDER].orEmpty(),
            fileName = prefs[TASK_FILE_NAME] ?: DEFAULT_TASK_FILE_NAME
        )
    }

    suspend fun saveVaultTreeUri(uri: String?) {
        context.dataStore.edit {
            if (uri.isNullOrBlank()) it.remove(VAULT_TREE_URI) else it[VAULT_TREE_URI] = uri
        }
    }

    suspend fun saveTaskFile(folder: String, fileName: String) {
        context.dataStore.edit {
            it[TASK_FOLDER] = normalizeFolder(folder)
            it[TASK_FILE_NAME] = normalizeFileName(fileName)
        }
    }

    /**
     * Whether a key is stored for [provider] but can't be decrypted on this device, e.g. after the
     * app's data was restored from a copy. Settings then asks for the key again.
     */
    suspend fun isStoredKeyUnreadable(provider: String): Boolean {
        val stored = context.dataStore.data.first()[apiKeyPref(provider)] ?: return false
        return withContext(Dispatchers.IO) { ApiKeyCipher.decrypt(stored) } == null
    }

    /** The usable stored key for [provider], or null. Never shown in the UI; only sent to the provider. */
    suspend fun apiKey(provider: String): String? =
        if (provider == PROVIDER_ANTHROPIC) anthropicApiKeyFlow.first() else openAiApiKeyFlow.first()

    /**
     * Keys are write-only in the UI: a blank key leaves the stored one unchanged. Use [removeApiKey]
     * to delete it.
     */
    suspend fun saveAi(provider: String, openAiKey: String, openAiModel: String, anthropicKey: String, anthropicModel: String) {
        context.dataStore.edit {
            it[AI_PROVIDER] = provider
            putKeyIfEntered(it, OPENAI_API_KEY, openAiKey)
            putKeyIfEntered(it, ANTHROPIC_API_KEY, anthropicKey)
            it[OPENAI_MODEL] = openAiModel.trim().ifBlank { OpenAIClient.DEFAULT_MODEL }
            it[ANTHROPIC_MODEL] = anthropicModel.trim().ifBlank { AnthropicClient.DEFAULT_MODEL }
        }
    }

    suspend fun removeApiKey(provider: String) {
        context.dataStore.edit { it.remove(apiKeyPref(provider)) }
    }

    private fun apiKeyPref(provider: String) = if (provider == PROVIDER_ANTHROPIC) ANTHROPIC_API_KEY else OPENAI_API_KEY

    private fun putKeyIfEntered(prefs: androidx.datastore.preferences.core.MutablePreferences, key: Preferences.Key<String>, value: String) {
        val normalized = value.trim()
        if (normalized.isNotBlank()) prefs[key] = ApiKeyCipher.encrypt(normalized)
    }
}
