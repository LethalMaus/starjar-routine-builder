import { test } from 'node:test';
import assert from 'node:assert/strict';
import { icons } from '../src/icon-catalog.js';
import { strings } from '../src/strings.js';
import labels from '../../shared/src/commonMain/composeResources/files/icon-labels.json' with { type: 'json' };

test('every selectable icon has an explicit English and German accessible name', () => {
  assert.deepEqual(Object.keys(labels).sort(), [...icons].sort());
  for (const id of icons) for (const language of ['en','de'])
    assert.ok(strings[language]['icon_'+id]?.trim(), language + ' ' + id);
  assert.equal(strings.de.icon_Dishwasher, 'Geschirrspüler');
  assert.equal(strings.en.icon_School, 'Graduation cap');
});
