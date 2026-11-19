import fs from 'node:fs';
const root = new URL('../', import.meta.url);
const labels = JSON.parse(fs.readFileSync(new URL('shared/src/commonMain/composeResources/files/icon-labels.json',root),'utf8'));
const quote = s => JSON.stringify(s).replace(/\$/g, '\\$');
const rows = Object.entries(labels).map(([id,label])=>`    ${quote(id)} to (${quote(label.en)} to ${quote(label.de)}),`).join('\n');
const output = `package dev.jamescullimore.starjar.routinebuilder.shared\n\n// Generated from composeResources/files/icon-labels.json. Run node tools/generate-icon-labels.mjs.\ninternal val iconLabels = mapOf(\n${rows}\n)\n\ninternal fun iconLabel(id: String, language: String): String {\n    val labels = iconLabels[id] ?: return id\n    return if (language == "de") labels.second else labels.first\n}\n`;
fs.writeFileSync(new URL('shared/src/commonMain/kotlin/dev/jamescullimore/starjar/routinebuilder/shared/IconLabels.kt',root),output);
