import { initializeApp } from "firebase/app";
import {
  initializeAppCheck,
  ReCaptchaEnterpriseProvider,
  getToken,
} from "firebase/app-check";
import {
  getAI,
  getGenerativeModel,
  GoogleAIBackend,
  InferenceMode,
  InferenceSource,
  Schema,
} from "firebase/ai";
import instructions from "../../androidApp/src/main/assets/routine-instructions.txt?raw";
import planningInstructions from "./planning-instructions.txt?raw";
import { request, draft } from "./core.js";
const env = import.meta.env;
export const cloudConfigured = Boolean(
  env.VITE_FIREBASE_APP_ID &&
    env.VITE_FIREBASE_API_KEY &&
    env.VITE_RECAPTCHA_SITE_KEY,
);
let local, cloud, appCheck;
function ai(useCloud) {
  if (!useCloud) {
    local ??= getAI(
      initializeApp(
        {
          projectId: "routine-local-probe",
          apiKey: "local-only-placeholder",
          appId: "local-only-placeholder",
        },
        "local",
      ),
      { backend: new GoogleAIBackend() },
    );
    return local;
  }
  if (!cloudConfigured) throw Error("CLOUD_CONFIG");
  if (!cloud) {
    const app = initializeApp(
      {
        projectId: env.VITE_FIREBASE_PROJECT_ID,
        apiKey: env.VITE_FIREBASE_API_KEY,
        appId: env.VITE_FIREBASE_APP_ID,
      },
      "cloud",
    );
    // A token may be injected by a private local test harness before loading the page. Never bundle it.
    appCheck = initializeAppCheck(app, {
      provider: new ReCaptchaEnterpriseProvider(env.VITE_RECAPTCHA_SITE_KEY),
      isTokenAutoRefreshEnabled: true,
    });
    cloud = getAI(app, { backend: new GoogleAIBackend() });
  }
  return cloud;
}
export async function capability() {
  if (typeof LanguageModel === "undefined") return "unavailable";
  let timer;
  try {
    return await Promise.race([
      LanguageModel.availability(),
      new Promise((resolve) => {
        timer = setTimeout(() => resolve("unknown"), 3000);
      }),
    ]);
  } catch {
    return "unavailable";
  } finally {
    clearTimeout(timer);
  }
}
export async function generate(
  r,
  { cloudAllowed = false, signal, onProgress = () => {} } = {},
) {
  request(r);
  if (signal?.aborted) throw Error("CANCELLED");
  const properties = {
    tasks: Schema.array({
      items: Schema.string({
        description: "Short task name, at most 80 characters.",
      }),
      maxItems: r.taskCount,
    }),
  };
  if (r.planning)
    properties.reason = Schema.string({
      description:
        "One brief sentence explaining the choice, at most 160 characters.",
    });
  if (r.includeReward) properties.reward = Schema.string();
  const schema = Schema.object({ properties });
  // A slow browser capability probe must not indefinitely block an approved fallback.
  const localReady = cloudAllowed
    ? (await capability()) === "available"
    : false;
  if (signal?.aborted) throw Error("CANCELLED");
  const model = getGenerativeModel(ai(cloudAllowed), {
    mode: cloudAllowed
      ? localReady
        ? InferenceMode.PREFER_ON_DEVICE
        : InferenceMode.ONLY_IN_CLOUD
      : InferenceMode.ONLY_ON_DEVICE,
    onDeviceParams: {
      createOptions: { temperature: 0.2, topK: 3 },
      promptOptions: { responseConstraint: schema },
    },
    inCloudParams: {
      model: "gemini-3.5-flash-lite",
      generationConfig: {
        temperature: 0.2,
        maxOutputTokens: r.planning ? 512 : 256,
        responseMimeType: "application/json",
        responseSchema: schema,
      },
    },
  });
  // Fail closed if App Check cannot attest; do not silently make unprotected cloud requests.
  if (cloudAllowed) {
    const attestation = await getToken(appCheck, false);
    if (!attestation.token) throw Error("APP_CHECK");
  }
  if (!cloudAllowed) await model.initializeDeviceModel(onProgress);
  if (signal?.aborted) throw Error("CANCELLED");
  const start = performance.now();
  const response = (
    await model.generateContent(
      (r.planning ? planningInstructions : instructions) +
        "\nREQUEST_DATA:\n" +
        JSON.stringify(r) +
        "\nOUTPUT LANGUAGE: " +
        (r.language === "de"
          ? "German. Use natural German with correct umlauts."
          : "English. Use English for every task and reward."),
      { signal, timeout: 90000 },
    )
  ).response;
  // SDK completion may outlive cancellation. Never apply a late result to the editor.
  if (signal?.aborted) throw Error("CANCELLED");
  if (
    ![InferenceSource.ON_DEVICE, InferenceSource.IN_CLOUD].includes(
      response.inferenceSource,
    )
  )
    throw Error("SOURCE");
  if (!cloudAllowed && response.inferenceSource !== InferenceSource.ON_DEVICE)
    throw Error("SOURCE");
  return {
    draft: draft(JSON.parse(response.text()), r),
    receipt: {
      source:
        response.inferenceSource === InferenceSource.ON_DEVICE
          ? "ON_DEVICE"
          : "IN_CLOUD",
      sdkSource: response.inferenceSource,
      model:
        response.inferenceSource === InferenceSource.IN_CLOUD
          ? "gemini-3.5-flash-lite"
          : "Chrome on-device model",
      durationMs: Math.round(performance.now() - start),
    },
  };
}
