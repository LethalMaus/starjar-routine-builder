export function validateReport(body) {
  if (!body || typeof body !== 'object' || Array.isArray(body)) throw Error('Invalid report');
  const keys = ['id', 'name', 'description', 'comment'];
  if (Object.keys(body).some(key => !keys.includes(key))) throw Error('Unexpected fields');
  if (!/^[a-f0-9-]{36}$/.test(body.id)) throw Error('Invalid ID');
  for (const [key, max] of [['name', 120], ['description', 2000], ['comment', 600]]) {
    if (typeof body[key] !== 'string' || body[key].length > max) throw Error('Invalid text');
  }
  if (!body.name.trim() && !body.description.trim()) throw Error('Empty suggestion');
  return Object.fromEntries(keys.map(key => [key, body[key]]));
}
