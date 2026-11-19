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
import localParentInstructions from "./parent-local-instructions.txt?raw";
import parentInstructions from "./parent-instructions.txt?raw";
import {
  validateParentRequest,
  parseSuggestions,
  icons,
  colors,
  frequencies,
} from "./parent-plan.js";
import { localPlanningContext, localPlanningResult, supportsLocalPlanning } from "./local-planning.js";
import { featureChoices } from "./task-options.js";
import { request, draft } from "./core.js";
function localLanguageOptions(language) {
  return {
    expectedInputs: [{ type: "text", languages: language === "de" ? ["en", "de"] : ["en"] }],
    expectedOutputs: [{ type: "text", languages: [language === "de" ? "de" : "en"] }],
  };
}
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
export async function capability(language = "en") {
  if (typeof LanguageModel === "undefined") return "unavailable";
  let timer;
  try {
    return await Promise.race([
      LanguageModel.availability(localLanguageOptions(language)),
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
  r.parentPlanning ? validateParentRequest(r) : request(r);
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
  const baseProperties = {
    name: Schema.string(),
    description: Schema.string(),
    pointAmount: Schema.integer(),
    icon: Schema.string({
      description: "Supported StarJar icon ID from the prompt.",
    }),
    color: Schema.enumString({ enum: colors }),
    limitType: Schema.enumString({ enum: frequencies }),
    limitValue: Schema.integer(),
    requireApproval: Schema.boolean(),
  };
  const itemSchema = Schema.object({ properties: baseProperties });
  const f = featureChoices(r.features);
  const taskProperties = { ...baseProperties };
  if (f.badgeCount)
    taskProperties.badges = Schema.array({
      items: Schema.object({
        properties: {
          completionsRequired: Schema.integer(),
          title: Schema.string(),
          bonusPoints: Schema.integer(),
        },
      }),
    });
  if (f.streaks)
    taskProperties.streakBonus = Schema.object({
      nullable: true,
      properties: {
        bonusPoints: Schema.integer(),
        everyValue: Schema.integer(),
        everyUnit: Schema.enumString({
          enum: ["TIME", "DAY", "WEEK", "MONTH"],
        }),
        forValue: Schema.integer(),
        forUnit: Schema.enumString({ enum: ["TIME", "DAY", "WEEK", "MONTH"] }),
      },
    });
  if (f.reminders)
    taskProperties.reminder = Schema.object({
      nullable: true,
      properties: {
        frequencyValue: Schema.integer(),
        frequencyUnit: Schema.enumString({ enum: ["DAY", "WEEK", "MONTH"] }),
        daysOfWeek: Schema.array({ items: Schema.integer() }),
        time: Schema.string({ nullable: true }),
        startDate: Schema.integer({ nullable: true }),
        endType: Schema.enumString({
          enum: ["NEVER", "ON_DATE", "AFTER_OCCURRENCES"],
        }),
        endDate: Schema.integer({ nullable: true }),
        endOccurrences: Schema.integer({ nullable: true }),
      },
    });
  if (f.urgency) {
    taskProperties.isUrgent = Schema.boolean();
    taskProperties.dueByDate = Schema.integer({ nullable: true });
    taskProperties.urgencyBonus = Schema.integer({ nullable: true });
  }
  const taskSchema = Schema.object({ properties: taskProperties });
  const schema = r.parentPlanning
    ? Schema.object({
        properties: {
          tasks: Schema.array({ items: taskSchema }),
          rewards: Schema.array({ items: itemSchema }),
          reason: Schema.string(),
        },
      })
    : Schema.object({ properties });
  const advanced = Boolean(
    f.badgeCount || f.frequency || f.streaks || f.reminders || f.urgency,
  );
  if (r.parentPlanning && !supportsLocalPlanning(r.language) && !cloudAllowed)
    throw Error("LOCAL_LANGUAGE_UNAVAILABLE");
  if (r.parentPlanning && advanced && !cloudAllowed)
    throw Error("ADVANCED_CLOUD");
  // Rich task settings exceed the tested local model response window. They use
  // the protected cloud route only with explicit session permission.
  const localReady =
    cloudAllowed && !advanced && (!r.parentPlanning || supportsLocalPlanning(r.language)) ? (await capability(r.language)) === "available" : false;
  if (signal?.aborted) throw Error("CANCELLED");
  const simpleLocal = r.parentPlanning && (!cloudAllowed || localReady);
  const ideaSchema = Schema.object({ properties: {
    name: Schema.string({ description: "Short action title, at most 60 characters" }),
    description: Schema.string({ description: "One simple sentence, at most 180 characters" }),
  } });
  const responseSchema = simpleLocal ? Schema.object({ properties: {
    tasks: Schema.array({ items: ideaSchema }),
    rewards: Schema.array({ items: ideaSchema }),
  } }) : schema;
  const model = getGenerativeModel(ai(cloudAllowed), {
    mode: cloudAllowed
      ? localReady
        ? InferenceMode.PREFER_ON_DEVICE
        : InferenceMode.ONLY_IN_CLOUD
      : InferenceMode.ONLY_ON_DEVICE,
    onDeviceParams: {
      createOptions: { ...localLanguageOptions(r.language), temperature: 0.2, topK: 3 },
      promptOptions: { responseConstraint: responseSchema },
    },
    inCloudParams: {
      model: "gemini-3.5-flash-lite",
      generationConfig: {
        temperature: 0.2,
        maxOutputTokens: r.parentPlanning ? 16384 : r.planning ? 512 : 256,
        responseMimeType: "application/json",
        responseSchema,
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
      (r.parentPlanning
        ? !cloudAllowed || localReady
          ? localParentInstructions
          : parentInstructions
        : r.planning
          ? planningInstructions
          : instructions) +
        "\nREQUEST_DATA:\n" +
        JSON.stringify(
          simpleLocal ? localPlanningContext(r) : r.parentPlanning ? { ...r, iconIds: icons, features: f } : r,
        ) +
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
    draft: r.parentPlanning
      ? simpleLocal
        ? localPlanningResult(JSON.parse(response.text()), r.language, r.features)
        : parseSuggestions(JSON.parse(response.text()), r.features)
      : draft(JSON.parse(response.text()), r),
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
