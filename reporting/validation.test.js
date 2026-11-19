import {test} from 'node:test';
import assert from 'node:assert/strict';
import {validateReport} from './validation.js';
const valid = {id:'12345678-1234-1234-1234-123456789012',name:'Synthetic suggestion',description:'Synthetic test',comment:''};
test('accepts only the confirmed suggestion and optional comment',()=>assert.deepEqual(validateReport(valid),valid));
test('rejects hidden context or identifiers',()=>{for(const field of ['goal','age','email','deviceId','plan'])assert.throws(()=>validateReport({...valid,[field]:'private'}));});
test('bounds content and requires a suggestion',()=>{assert.throws(()=>validateReport({...valid,comment:'x'.repeat(601)}));assert.throws(()=>validateReport({...valid,name:'',description:''}));assert.throws(()=>validateReport({...valid,id:'../escape'}));});
