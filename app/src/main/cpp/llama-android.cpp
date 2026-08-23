#include <jni.h>
#include <string>
#include <vector>
#include <atomic>
#include <chrono>
#include <android/log.h>
#include "llama.h"
#include "common.h"
#include "sampling.h"

#define TAG "SkilloraLLM-Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

struct LlamaState {
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    std::atomic<bool> cancelled{false};

    LlamaState() = default;

    ~LlamaState() {
        if (ctx != nullptr) {
            llama_free(ctx);
            ctx = nullptr;
        }
        if (model != nullptr) {
            llama_model_free(model);
            model = nullptr;
        }
    }
};

static void llama_log_callback(ggml_log_level level, const char * text, void * user_data) {
    if (level == GGML_LOG_LEVEL_ERROR) {
        LOGE("llama.cpp: %s", text);
    } else if (level == GGML_LOG_LEVEL_WARN) {
        LOGI("llama.cpp (WARN): %s", text);
    }
    // Ignore INFO and DEBUG logs to prevent LOG_FLOWCTRL flooding Android logcat
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_simats_skillora_data_llm_LocalLLMEngine_loadModelNative(JNIEnv *env, jobject thiz,
                                                                 jstring model_path,
                                                                 jint context_size,
                                                                 jint threads) {
    llama_log_set(llama_log_callback, nullptr);
    llama_backend_init();

    LOGI("System Info: %s", llama_print_system_info());

    const char *path = env->GetStringUTFChars(model_path, nullptr);
    LOGI("Loading model from: %s", path);

    // 1. File checks
    FILE *file = fopen(path, "rb");
    if (!file) {
        LOGE("File checking failed: Could not open file at %s. File might not exist or lacks read permissions.", path);
        env->ReleaseStringUTFChars(model_path, path);
        return -1; // -1 for MODEL_NOT_FOUND
    }
    
    fseek(file, 0, SEEK_END);
    long long size = ftell(file);
    fclose(file);
    
    LOGI("File exists: true");
    LOGI("File size: %lld bytes (%.2f MB / %.2f GB)", size, (double)size / (1024.0 * 1024.0), (double)size / (1024.0 * 1024.0 * 1024.0));

    if (size < 1024 * 1024) {
        LOGE("File is suspiciously small. It's likely a dummy/placeholder or incomplete download.");
        env->ReleaseStringUTFChars(model_path, path);
        return -2; // -2 for MODEL_INVALID
    }

    LOGI("Initializing llama.cpp with context size: %d, threads: %d", context_size, threads);

    llama_model_params model_params = llama_model_default_params();
    llama_model * model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(model_path, path);

    if (!model) {
        LOGE("Model load failed: llama_model_load_from_file returned nullptr.");
        return -3; // -3 for MODEL_LOAD_FAILED
    }

    // 2. Metadata checks
    char arch_buf[128];
    int res = llama_model_meta_val_str(model, "general.architecture", arch_buf, sizeof(arch_buf));
    if (res >= 0) {
        LOGI("Model architecture: %s", arch_buf);
    } else {
        LOGI("Model architecture: unknown");
    }

    LOGI("Vocab size: %d", llama_vocab_n_tokens(llama_model_get_vocab(model)));

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = context_size;
    ctx_params.n_batch = 512;
    ctx_params.n_ubatch = 512;
    ctx_params.n_threads = threads;
    ctx_params.n_threads_batch = threads;

    llama_context * ctx = llama_init_from_model(model, ctx_params);
    if (!ctx) {
        LOGE("Failed to create context: Out of Memory or invalid configuration.");
        llama_model_free(model);
        return -4; // -4 for OUT_OF_MEMORY
    }

    LlamaState * state = new LlamaState();
    state->model = model;
    state->ctx = ctx;

    LOGI("Model loaded successfully. Context initialized.");
    LOGI("Model pointer: %p", (void*)model);
    LOGI("Context pointer: %p", (void*)ctx);
    LOGI("LlamaState pointer: %p", (void*)state);
    LOGI("MODEL_LOAD_NATIVE_RESULT=SUCCESS");
    
    return reinterpret_cast<jlong>(state);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_simats_skillora_data_llm_LocalLLMEngine_generateNative(JNIEnv *env, jobject thiz,
                                                                jlong context_ptr,
                                                                jstring prompt, jint max_tokens,
                                                                jfloat temperature) {
    LlamaState * state = reinterpret_cast<LlamaState *>(context_ptr);
    if (!state || !state->ctx) {
        return env->NewStringUTF("");
    }
    state->cancelled = false;

    const char *prompt_str = env->GetStringUTFChars(prompt, nullptr);
    std::string prompt_std(prompt_str);
    env->ReleaseStringUTFChars(prompt, prompt_str);

    // Tokenize
    auto start_time = std::chrono::high_resolution_clock::now();
    
    const bool add_special = llama_vocab_get_add_bos(llama_model_get_vocab(state->model));
    std::vector<llama_token> tokens = common_tokenize(state->ctx, prompt_std, add_special, true);
    
    auto tokenization_time = std::chrono::high_resolution_clock::now();
    LOGI("Tokenization completed. Prompt tokens: %zu", tokens.size());

    // Init batch
    llama_batch batch = llama_batch_init(tokens.size(), 0, 1);
    for (size_t i = 0; i < tokens.size(); ++i) {
        common_batch_add(batch, tokens[i], i, {0}, false);
    }
    batch.logits[batch.n_tokens - 1] = true;

    if (llama_decode(state->ctx, batch) != 0) {
        LOGE("Failed to decode prompt");
        llama_batch_free(batch);
        return env->NewStringUTF("");
    }

    auto prompt_decode_time = std::chrono::high_resolution_clock::now();
    double prompt_sec = std::chrono::duration<double>(prompt_decode_time - tokenization_time).count();
    double prompt_speed = (prompt_sec > 0) ? (tokens.size() / prompt_sec) : 0.0;
    
    LOGI("Prompt decode time: %.2f sec", prompt_sec);
    LOGI("Prompt tokens/sec: %.2f", prompt_speed);

    // Init sampler
    common_params_sampling sparams;
    sparams.temp = temperature;
    auto * sampler = common_sampler_init(state->model, sparams);

    std::string response = "";
    llama_token curr_token = common_sampler_sample(sampler, state->ctx, -1);
    common_sampler_accept(sampler, curr_token, true);

    int n_generated = 0;
    while (n_generated < max_tokens && !state->cancelled) {
        if (llama_vocab_is_eog(llama_model_get_vocab(state->model), curr_token)) {
            break;
        }

        response += common_token_to_piece(state->ctx, curr_token);

        common_batch_clear(batch);
        common_batch_add(batch, curr_token, tokens.size() + n_generated, {0}, true);

        if (llama_decode(state->ctx, batch) != 0) {
            LOGE("Failed to decode token");
            break;
        }

        curr_token = common_sampler_sample(sampler, state->ctx, -1);
        common_sampler_accept(sampler, curr_token, true);
        n_generated++;
    }

    auto generation_end_time = std::chrono::high_resolution_clock::now();
    double gen_sec = std::chrono::duration<double>(generation_end_time - prompt_decode_time).count();
    double speed = (gen_sec > 0) ? (n_generated / gen_sec) : 0.0;

    LOGI("GENERATION COMPLETE.");
    LOGI("Prompt tokens: %zu", tokens.size());
    LOGI("Generated tokens: %d", n_generated);
    LOGI("Generation time: %.2f sec", gen_sec);
    LOGI("Generation tokens/sec: %.2f", speed);

    common_sampler_free(sampler);
    llama_batch_free(batch);

    return env->NewStringUTF(response.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_simats_skillora_data_llm_LocalLLMEngine_releaseNative(JNIEnv *env, jobject thiz,
                                                               jlong context_ptr) {
    LlamaState * state = reinterpret_cast<LlamaState *>(context_ptr);
    if (state) {
        delete state;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_simats_skillora_data_llm_LocalLLMEngine_cancelNative(JNIEnv *env, jobject thiz,
                                                              jlong context_ptr) {
    LlamaState * state = reinterpret_cast<LlamaState *>(context_ptr);
    if (state) {
        state->cancelled = true;
    }
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_simats_skillora_data_llm_LocalLLMEngine_countTokensNative(JNIEnv *env, jobject thiz,
                                                                   jlong context_ptr, jstring text) {
    LlamaState * state = reinterpret_cast<LlamaState *>(context_ptr);
    if (!state || !state->ctx) {
        return 0;
    }
    
    const char * text_c = env->GetStringUTFChars(text, nullptr);
    std::string text_str(text_c);
    env->ReleaseStringUTFChars(text, text_c);
    
    const bool add_special = llama_vocab_get_add_bos(llama_model_get_vocab(state->model));
    std::vector<llama_token> tokens = common_tokenize(state->ctx, text_str, add_special, true);

    return static_cast<jint>(tokens.size());
}
