import {onRequest} from 'firebase-functions/v2/https';
import {onSchedule} from 'firebase-functions/v2/scheduler';
import {initializeApp} from 'firebase-admin/app';
import {getAppCheck} from 'firebase-admin/app-check';
import {getFirestore, Timestamp} from 'firebase-admin/firestore';
import {validateReport} from './validation.js';
initializeApp();
const db = getFirestore();
const collection = 'suggestionReports';
// No client reads or direct writes. Only this App Check-protected endpoint accepts reports.
export const reportSuggestion = onRequest({region:'europe-west3',maxInstances:1,concurrency:10,memory:'256MiB',timeoutSeconds:30,invoker:'public'},async(req,res)=>{
  res.set('Cache-Control','no-store');
  if(req.method!=='POST')return res.status(405).end();
  if(!req.is('application/json') || Number(req.get('content-length') || 0)>12000)return res.status(400).end();
  let report;
  try{report=validateReport(req.body);}catch{return res.status(400).json({error:'INVALID_REPORT'});}
  try{await getAppCheck().verifyToken(req.get('X-Firebase-AppCheck') || '');}catch{return res.status(401).json({error:'APP_CHECK_REQUIRED'});}
  try{
    const now=Date.now(),day=new Date(now).toISOString().slice(0,10);
    await db.runTransaction(async tx=>{
      const ref=db.collection(collection).doc(report.id),limit=db.collection('reportLimits').doc(day);
      const [existing,count]=await Promise.all([tx.get(ref),tx.get(limit)]);
      if(existing.exists)return; // An explicit retry cannot create duplicate reports.
      if((count.data()?.count||0)>=100)throw Error('LIMIT');
      const {id,...content}=report;
      tx.create(ref,{...content,createdAt:Timestamp.fromMillis(now),expiresAt:Timestamp.fromMillis(now+30*86400000)});
      tx.set(limit,{count:(count.data()?.count||0)+1,expiresAt:Timestamp.fromMillis(now+2*86400000)});
    });
    return res.status(200).json({received:true});
  }catch(error){return res.status(error.message==='LIMIT'?429:503).json({error:'TRY_LATER'});}
});
// Hourly removal bounds retention independently of asynchronous Firestore TTL cleanup.
export const purgeSuggestionReports = onSchedule({region:'europe-west3',schedule:'every 60 minutes',timeZone:'UTC',maxInstances:1,memory:'256MiB',timeoutSeconds:120},async()=>{
  for(const name of [collection,'reportLimits']){
    for(;;){const rows=await db.collection(name).where('expiresAt','<=',Timestamp.now()).limit(400).get();if(rows.empty)break;const batch=db.batch();rows.docs.forEach(doc=>batch.delete(doc.ref));await batch.commit();if(rows.size<400)break;}
  }
});
